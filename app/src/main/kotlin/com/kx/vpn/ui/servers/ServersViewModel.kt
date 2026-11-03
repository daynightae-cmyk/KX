package com.kx.vpn.ui.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kx.vpn.data.EndpointCatalogueRepository
import com.kx.vpn.data.EndpointSwitchController
import com.kx.vpn.domain.servers.CountryNameResolver
import com.kx.vpn.domain.servers.EndpointMeasurement
import com.kx.vpn.domain.servers.ServerCatalogue
import com.kx.vpn.domain.servers.ServerFilter
import com.kx.vpn.domain.servers.ServerRow
import com.kx.vpn.domain.servers.ServerSwitchContext
import com.kx.vpn.platform.LatencyProbe
import com.kx.vpn.platform.NetworkAvailability
import com.kx.vpn.platform.TcpLatencyProbe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The rows together with the instant they were built for.
 *
 * The clock is part of the state because freshness decides three things at once —
 * whether a latency may be shown, whether it is labelled stale, and whether an endpoint
 * may be recommended. Reading the wall clock again inside the composable would let those
 * three disagree, and would make the screen impossible to render deterministically.
 */
data class ServerListState(
    val rows: List<ServerRow> = emptyList(),
    val generatedAtMillis: Long = 0L,
    val activeEndpointId: String? = null,
)

/**
 * Drives the server list.
 *
 * Measurements are taken only when the user asks for them, and only against endpoints
 * the user owns. There is no background polling of somebody else's fleet.
 */
class ServersViewModel(
    private val repository: EndpointCatalogueRepository,
    private val probe: LatencyProbe? = null,
    private val switchController: EndpointSwitchController? = null,
    private val countryNames: CountryNameResolver? = null,
    private val network: NetworkAvailability? = null,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(ServerFilter.All)
    private val measurements = MutableStateFlow<Map<String, EndpointMeasurement>>(emptyMap())
    private val measuring = MutableStateFlow(false)

    /** True while a measurement batch is running, so the UI can say so. */
    val isMeasuring: StateFlow<Boolean> = measuring.asStateFlow()

    /**
     * The platform's own answer about internet reachability.
     *
     * With no network there is nothing to measure, and a spinner that can never finish is
     * worse than a sentence saying why.
     */
    val isOffline: StateFlow<Boolean> = (
        network?.isOnline?.map { !it } ?: flowOf(false)
        ).stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val currentQuery: StateFlow<String> = query.asStateFlow()
    val currentFilter: StateFlow<ServerFilter> = filter.asStateFlow()

    val state: StateFlow<ServerListState> = combine(
        repository.state,
        query,
        filter,
        measurements,
    ) { catalogue, searchQuery, activeFilter, currentMeasurements ->
        val now = System.currentTimeMillis()
        val built = ServerCatalogue.buildRows(
            profiles = catalogue.endpoints,
            measurements = currentMeasurements,
            favorites = catalogue.favorites,
            now = now,
            countryNames = countryNames,
        )
        val searched = ServerCatalogue.search(built, searchQuery, countryNames)
        val filtered = ServerCatalogue.filter(searched, activeFilter, now)
        ServerListState(
            rows = ServerCatalogue.sort(filtered, now),
            generatedAtMillis = now,
            activeEndpointId = catalogue.activeEndpointId,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ServerListState())

    /** The switch in flight, or the last failure the user should be told about. */
    val switch: StateFlow<ServerSwitchContext>? = switchController?.switch

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilter(value: ServerFilter) {
        filter.value = value
    }

    fun toggleFavorite(row: ServerRow) {
        viewModelScope.launch { repository.toggleFavorite(row.profile.id) }
    }

    /**
     * Chooses the endpoint the tunnel will use.
     *
     * With no switch controller wired — the preview and test path — this only records
     * the preference. It never fakes the part that needs the platform.
     */
    fun select(row: ServerRow) {
        val controller = switchController
        if (controller == null) {
            viewModelScope.launch { repository.setActiveEndpoint(row.profile.id) }
            return
        }
        controller.select(row.profile)
    }

    fun acknowledgeSwitchFailure() {
        switchController?.acknowledgeFailure()
    }

    /** Measures reachability of the user's own endpoints. */
    fun measure() {
        val latencyProbe = probe ?: return
        if (measuring.value) return
        // Refused before any socket is opened: a batch of connects on a device with no
        // network is a long way to arrive at the same sentence.
        if (network != null && !network.currentlyOnline()) return
        viewModelScope.launch {
            measuring.value = true
            try {
                val endpoints = repository.state.first().endpoints
                measurements.value = latencyProbe.measure(endpoints)
            } finally {
                measuring.value = false
            }
        }
    }
}
