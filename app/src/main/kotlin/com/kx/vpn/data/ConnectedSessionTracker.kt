package com.kx.vpn.data

import com.kx.vpn.domain.session.MonotonicSample
import com.kx.vpn.domain.session.SessionTelemetry
import com.kx.vpn.domain.session.Throughput
import com.kx.vpn.domain.servers.EndpointProfile
import com.kx.vpn.platform.LatencyProbe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Everything the connected screen renders. */
data class ConnectedSessionState(
    /** True only while the backend says the tunnel interface is up. */
    val isTunnelUp: Boolean = false,
    /** Monotonic session duration, or null before the tunnel came up. */
    val elapsedMillis: Long? = null,
    val rxBytes: Long? = null,
    val txBytes: Long? = null,
    /** The last valid sampling window. Null until two trustworthy samples exist. */
    val throughput: Throughput? = null,
    /** A real measured round trip to the user's own endpoint, with its timestamp. */
    val pingMillis: Long? = null,
    val pingMeasuredAtElapsed: Long? = null,
    val pingFailed: Boolean = false,
    /** The endpoint the tunnel is using, from the user's own catalogue. */
    val endpoint: EndpointProfile? = null,
    /** The tunnel-side address from the user's own configuration, never a guessed IP. */
    val tunnelAddress: String? = null,
    /**
     * Whether traffic was actually observed leaving through the tunnel to a verified exit.
     *
     * Nothing in this build sets it to true: establishing it needs an external probe
     * target and the user's consent to reach it, and neither exists here. It is a field
     * rather than a hard-coded false so that the claim is *recorded as unmet* rather than
     * silently absent — and so a later task that does verify it has one obvious place to
     * say so. The screen reads it and states the difference.
     */
    val exitVerified: Boolean = false,
)

/**
 * Turns the tunnel's own counters into a session duration and an observed throughput.
 *
 * The rules that matter:
 *
 *  * **Duration is monotonic.** `elapsedRealtime` is supplied by the caller, so a
 *    wall-clock change cannot move the timer, and a reboot that rewinds the source shows
 *    no duration rather than a negative one.
 *  * **A rate needs two trustworthy samples.** Counters that went backwards — which is
 *    what a reconnect does to per-tunnel statistics — produce no rate rather than zero.
 *  * **Nothing is sampled when the tunnel is down.** The screen cannot show a number that
 *    came from a session that does not exist.
 */
class ConnectedSessionTracker(
    private val sessionManager: TunnelSessionManager,
    private val catalogue: EndpointCatalogueRepository,
    private val profiles: ProfileRepository,
    private val probe: LatencyProbe? = null,
    private val elapsedRealtime: () -> Long,
    private val scope: CoroutineScope,
    private val sampleIntervalMillis: Long = DEFAULT_SAMPLE_INTERVAL_MILLIS,
) {

    private val _state = MutableStateFlow(ConnectedSessionState())
    val state: StateFlow<ConnectedSessionState> = _state.asStateFlow()

    private var startedAtElapsed: Long? = null
    private var baseSample: MonotonicSample? = null

    init {
        scope.launch {
            sessionManager.state.collect { session ->
                val up = session.isTunnelUp
                if (up && !_state.value.isTunnelUp) onTunnelUp()
                if (!up && _state.value.isTunnelUp) onTunnelDown()
                if (up) refreshElapsed()
                _state.update { it.copy(isTunnelUp = up) }
            }
        }
        scope.launch { resolveEndpoint() }
        scope.launch {
            while (isActive) {
                if (_state.value.isTunnelUp) {
                    refreshElapsed()
                    sample()
                }
                delay(sampleIntervalMillis)
            }
        }
    }

    /** The timer advances between samples, so the displayed duration is not quantised. */
    fun refreshElapsed() {
        val started = startedAtElapsed ?: return
        _state.update { it.copy(elapsedMillis = SessionTelemetry.elapsedMillis(started, elapsedRealtime())) }
    }

    private fun onTunnelUp() {
        startedAtElapsed = elapsedRealtime()
        baseSample = null
        _state.update {
            it.copy(
                elapsedMillis = 0L,
                throughput = null,
                rxBytes = null,
                txBytes = null,
                pingFailed = false,
            )
        }
    }

    private fun onTunnelDown() {
        startedAtElapsed = null
        baseSample = null
        _state.update {
            it.copy(
                elapsedMillis = null,
                throughput = null,
                rxBytes = null,
                txBytes = null,
                pingMillis = null,
                pingMeasuredAtElapsed = null,
                pingFailed = false,
            )
        }
    }

    private suspend fun resolveEndpoint() {
        val catalogueState = catalogue.state.first()
        val chosen = catalogueState.activeEndpointId?.let { id ->
            catalogueState.endpoints.firstOrNull { it.id == id }
        }
        val address = chosen?.profileId?.let { profiles.tunnelAddressFor(it) }
        _state.update { it.copy(endpoint = chosen, tunnelAddress = address) }
    }

    private suspend fun sample() {
        sessionManager.refreshStatistics()
        val stats = sessionManager.state.value.stats ?: return
        val current = MonotonicSample(
            elapsedRealtimeMillis = elapsedRealtime(),
            rxBytes = stats.totalRx,
            txBytes = stats.totalTx,
        )
        val (nextBase, throughput) = SessionTelemetry.nextThroughput(baseSample, current)
        baseSample = nextBase
        _state.update {
            it.copy(
                rxBytes = current.rxBytes,
                txBytes = current.txBytes,
                throughput = throughput.takeIf { value -> value.isMeasured } ?: it.throughput,
            )
        }
    }

    /**
     * Measures the round trip to the endpoint the tunnel is actually using.
     *
     * This is a real TCP connect to the user's own server, timed, with the instant it was
     * taken kept alongside it. It is not a speed test and the screen does not call it one.
     * With no endpoint selected, or no probe wired, nothing is measured and the value
     * stays `--`.
     */
    fun pingEndpoint() {
        val latencyProbe = probe ?: return
        val endpoint = _state.value.endpoint ?: return
        if (pingInFlight) return
        pingInFlight = true
        scope.launch {
            try {
                val measurements = latencyProbe.measure(listOf(endpoint))
                val measurement = measurements[endpoint.id]
                val now = elapsedRealtime()
                _state.update { state ->
                    state.copy(
                        pingMillis = measurement?.latencyMillis
                            ?.takeIf { measurement.reachable && measurement.isFresh(now) },
                        pingMeasuredAtElapsed = if (measurement != null) now else state.pingMeasuredAtElapsed,
                        pingFailed = measurement == null || !measurement.reachable,
                    )
                }
            } finally {
                pingInFlight = false
            }
        }
    }

    private var pingInFlight = false

    companion object {
        /** Two seconds is long enough for a stable rate and short enough to feel live. */
        const val DEFAULT_SAMPLE_INTERVAL_MILLIS = 2_000L
    }
}
