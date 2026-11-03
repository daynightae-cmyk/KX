package com.kx.vpn.ui.servers

import com.kx.vpn.data.EndpointCatalogueRepository
import com.kx.vpn.data.InMemoryPreferencesDataStore
import com.kx.vpn.domain.servers.EndpointMeasurement
import com.kx.vpn.domain.servers.EndpointProfile
import com.kx.vpn.domain.servers.ServerFilter
import com.kx.vpn.platform.LatencyProbe
import com.kx.vpn.platform.NetworkAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * What the server list shows, and what it refuses to do.
 *
 * The two properties worth testing here are that a measurement only ever comes from the
 * probe — never from a default — and that no socket is opened when the platform already
 * says there is no network.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ServersViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class RecordingProbe(var result: Map<String, EndpointMeasurement> = emptyMap()) : LatencyProbe {
        var calls = 0
        var lastBatch: List<EndpointProfile> = emptyList()

        override suspend fun measure(profiles: List<EndpointProfile>): Map<String, EndpointMeasurement> {
            calls++
            lastBatch = profiles
            return result
        }
    }

    private class FakeNetwork(private val online: Boolean) : NetworkAvailability {
        override fun currentlyOnline(): Boolean = online
        override val isOnline: Flow<Boolean> = MutableStateFlow(online).map { it }
    }

    private fun profile(id: String, label: String = "Node $id") = EndpointProfile(
        id = id,
        displayName = label,
        countryCode = null,
        host = "203.0.113.$id",
        port = 51820,
        profileId = id,
    )

    private suspend fun repositoryWith(vararg ids: String): EndpointCatalogueRepository {
        val repository = EndpointCatalogueRepository(InMemoryPreferencesDataStore())
        ids.forEach { repository.upsert(profile(it)) }
        return repository
    }

    @Test
    fun nothingIsMeasuredWithoutAValidatedNetwork() = runTest(dispatcher) {
        val probe = RecordingProbe()
        val viewModel = ServersViewModel(
            repository = repositoryWith("1", "2"),
            probe = probe,
            network = FakeNetwork(online = false),
        )
        runCurrent()

        viewModel.measure()
        runCurrent()

        assertEquals("no socket batch may be started on a device with no network", 0, probe.calls)
        assertTrue(viewModel.isOffline.value)
        assertFalse(viewModel.isMeasuring.value)
    }

    @Test
    fun aRealMeasurementBecomesTheDisplayedLatencyAndTheRecommendation() = runTest(dispatcher) {
        val probe = RecordingProbe()
        val viewModel = ServersViewModel(
            repository = repositoryWith("1", "2"),
            probe = probe,
            network = FakeNetwork(online = true),
        )
        runCurrent()
        val before = viewModel.state.value.rows
        assertEquals("the user's own endpoints are listed before anything is measured", 2, before.size)
        assertTrue(
            "and none of them shows a latency yet",
            before.all { it.measurement == null },
        )

        probe.result = mapOf(
            "1" to EndpointMeasurement.success(120, System.currentTimeMillis()),
            "2" to EndpointMeasurement.success(30, System.currentTimeMillis()),
        )
        viewModel.measure()
        runCurrent()

        val rows = viewModel.state.value.rows
        assertEquals(listOf("2", "1"), rows.map { it.profile.id })
        assertEquals(listOf(30L, 120L), rows.map { it.measurement?.latencyMillis })
        // Only the endpoint with the lowest fresh measured latency is recommended.
        assertEquals(listOf("2"), rows.filter { it.isRecommended }.map { it.profile.id })
        assertEquals(1, probe.calls)
        assertEquals(
            "the batch is the user's own endpoints and nothing else",
            setOf("1", "2"),
            probe.lastBatch.map { it.id }.toSet(),
        )
    }

    @Test
    fun anOldMeasurementIsNotShownAsALatencyOrAsARecommendation() = runTest(dispatcher) {
        val longAgo = System.currentTimeMillis() - EndpointMeasurement.STALE_AFTER_MILLIS - 60_000
        val probe = RecordingProbe(
            result = mapOf("1" to EndpointMeasurement.success(30, longAgo)),
        )
        val viewModel = ServersViewModel(
            repository = repositoryWith("1"),
            probe = probe,
            network = FakeNetwork(online = true),
        )
        runCurrent()
        viewModel.measure()
        runCurrent()

        val row = viewModel.state.value.rows.first()
        assertFalse("a stale measurement must not be presented as current", row.isRecommended)
        assertEquals(
            "the row keeps the measurement, and the screen derives '--' from its freshness",
            longAgo,
            row.measurement?.measuredAtEpochMillis,
        )
        assertNull(
            "the screen derives '--' from the measurement's own freshness, not from a missing value",
            row.measurement?.displayLatency(viewModel.state.value.generatedAtMillis),
        )
    }

    @Test
    fun selectingWithoutASwitchControllerRecordsThePreferenceOnly() = runTest(dispatcher) {
        val repository = repositoryWith("1", "2")
        val viewModel = ServersViewModel(repository = repository)
        runCurrent()

        val row = viewModel.state.value.rows.first { it.profile.id == "2" }
        viewModel.select(row)
        runCurrent()

        assertEquals("2", repository.state.first().activeEndpointId)
    }

    @Test
    fun theFastestFilterUsesTheSameFreshnessAsTheRows() = runTest(dispatcher) {
        val longAgo = System.currentTimeMillis() - EndpointMeasurement.STALE_AFTER_MILLIS - 60_000
        val probe = RecordingProbe(
            result = mapOf(
                "1" to EndpointMeasurement.success(10, longAgo),
                "2" to EndpointMeasurement.success(90, System.currentTimeMillis()),
            ),
        )
        val viewModel = ServersViewModel(
            repository = repositoryWith("1", "2"),
            probe = probe,
            network = FakeNetwork(online = true),
        )
        runCurrent()
        viewModel.measure()
        runCurrent()
        viewModel.setFilter(ServerFilter.Fastest)
        runCurrent()

        assertEquals(
            "a stale reading cannot win the Fastest filter",
            listOf("2"),
            viewModel.state.value.rows.map { it.profile.id },
        )
    }
}
