package com.kx.vpn.data

import com.kx.vpn.domain.tunnel.FailureReason
import com.kx.vpn.domain.tunnel.TunnelEffect
import com.kx.vpn.domain.tunnel.TunnelEvent
import com.kx.vpn.domain.tunnel.TunnelState
import com.kx.vpn.domain.tunnel.TunnelTransition
import com.kx.vpn.domain.tunnel.canDisconnect
import com.kx.vpn.domain.tunnel.canInitiateConnect
import com.kx.vpn.domain.tunnel.isTunnelUp
import com.kx.vpn.domain.tunnel.reduceTunnel
import com.kx.vpn.platform.TunnelStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Whether the platform still needs the user to approve VPN consent.
 *
 * Modelled as a token rather than an `Intent` so the session manager stays free of
 * Android types and can be unit-tested on the JVM. The UI layer turns a
 * [ConsentRequest.Required] into the actual consent intent.
 */
sealed interface ConsentRequest {
    data object NotRequired : ConsentRequest
    data class Required(val requestId: String) : ConsentRequest
}

/**
 * The platform boundary the session manager drives.
 *
 * Every method here performs or observes a *real* platform action. There is no
 * implementation of this interface that reports success without something happening.
 */
interface TunnelPlatform {
    /** Asks whether consent is needed, and if so produces a request to show. */
    fun requestConsent(): ConsentRequest

    /** Brings the tunnel up with the given configuration text. */
    fun startTunnel(configText: String)

    /** Tears the tunnel down. */
    fun stopTunnel()

    /** The backend's current view of the tunnel. */
    fun isTunnelUp(): Boolean

    /** Real byte counters, or null when no live session exists. */
    fun statistics(): TunnelStats?
}

/** Everything the home screen renders. */
data class TunnelSessionState(
    val tunnelState: TunnelState = TunnelState.UNCONFIGURED,
    val failure: FailureReason? = null,
    val stats: TunnelStats? = null,
    val hasProfile: Boolean = false,
    val backendVersion: String? = null,
    /** Process-local monotonic timestamps; never persisted or treated as Internet proof. */
    val startedAtElapsedMillis: Long? = null,
    val elapsedMillis: Long? = null,
) {
    /** The connect button is live only when a profile exists and nothing is running. */
    val canConnect: Boolean get() = hasProfile && tunnelState.canInitiateConnect()

    val canDisconnect: Boolean get() = tunnelState.canDisconnect()

    /**
     * True only when the backend says the tunnel interface is up.
     *
     * Deliberately named `isTunnelUp` and not `isProtected`: the backend's UP means
     * the tun interface exists and the handshake is being attempted. It does **not**
     * mean traffic is flowing, that a peer completed a handshake, or that anybody is
     * protected. `docs/SECURITY_MODEL.md` requires "VPN UP" and "Internet Verified"
     * to stay distinct, so the name keeps them distinct.
     */
    val isTunnelUp: Boolean get() = tunnelState.isTunnelUp()
}

/**
 * Owns the tunnel state machine and executes its effects against the platform.
 *
 * All state transitions go through [reduceTunnel], so the rules live in one pure place
 * and this class only wires them to the platform. Commands are idempotent: a second
 * connect while connecting is ignored by the reducer, and the platform is only asked to
 * do work when the reducer actually emits an effect.
 */
class TunnelSessionManager(
    private val platform: TunnelPlatform,
    private val elapsedRealtimeMillis: () -> Long = { System.nanoTime() / 1_000_000L },
    private val profileSource: () -> String? = { null },
) {

    private val statisticsGeneration = java.util.concurrent.atomic.AtomicLong()
    private val _state = MutableStateFlow(TunnelSessionState())
    val state: StateFlow<TunnelSessionState> = _state.asStateFlow()

    /** Emitted when the platform wants the user to approve VPN consent. */
    var onConsentRequired: ((ConsentRequest) -> Unit)? = null

    init {
        refreshProfilePresence()
    }

    /** Called after a profile is saved, removed, or the app is restored. */
    fun refreshProfilePresence() {
        statisticsGeneration.incrementAndGet()
        val hasProfile = profileSource() != null
        _state.update { current ->
            val next = if (hasProfile) {
                if (current.tunnelState == TunnelState.UNCONFIGURED) TunnelState.DISCONNECTED
                else current.tunnelState
            } else {
                TunnelState.UNCONFIGURED
            }
            current.copy(hasProfile = hasProfile, tunnelState = next,
                stats = current.stats.takeIf { next.isTunnelUp() },
                startedAtElapsedMillis = current.startedAtElapsedMillis.takeIf { next.isTunnelUp() },
                elapsedMillis = current.elapsedMillis.takeIf { next.isTunnelUp() })
        }
    }

    fun connect() {
        val current = _state.value
        if (!current.hasProfile) return
        val transition = reduceTunnel(current.tunnelState, TunnelEvent.ConnectRequested)
        apply(transition)
    }

    fun disconnect() {
        apply(reduceTunnel(_state.value.tunnelState, TunnelEvent.DisconnectRequested))
    }

    fun retry() {
        apply(reduceTunnel(_state.value.tunnelState, TunnelEvent.RetryRequested))
    }

    /**
     * The platform's report of the tunnel coming up or down. This is the only path by
     * which the session can become CONNECTED.
     */
    fun onPlatformStateChanged(up: Boolean) {
        val event = if (up) TunnelEvent.TunnelUp else TunnelEvent.TunnelDown
        apply(reduceTunnel(_state.value.tunnelState, event))
    }

    fun onPermissionDenied() {
        apply(reduceTunnel(_state.value.tunnelState, TunnelEvent.PermissionDenied))
    }

    fun onPermissionGranted() {
        apply(reduceTunnel(_state.value.tunnelState, TunnelEvent.PermissionGranted))
    }

    fun onNetworkLost() {
        apply(reduceTunnel(_state.value.tunnelState, TunnelEvent.NetworkLost))
    }

    fun onNetworkRestored() {
        apply(reduceTunnel(_state.value.tunnelState, TunnelEvent.NetworkRestored))
    }

    fun onRevoked() {
        apply(reduceTunnel(_state.value.tunnelState, TunnelEvent.RevokeReceived))
    }

    fun onBackendFailure(reason: FailureReason) {
        apply(reduceTunnel(_state.value.tunnelState, TunnelEvent.BackendFailed(reason)))
    }

    /** Refreshes the byte counters from the backend. Null means "no live measurement". */
    fun refreshStatistics() {
        refreshElapsedTime()
        val generation = statisticsGeneration.get()
        if (!_state.value.tunnelState.isTunnelUp()) {
            _state.update { it.copy(stats = null) }
            return
        }
        // Read outside StateFlow.update: update lambdas may be evaluated more than
        // once under contention and must not repeat native/backend side effects.
        val measured = try { platform.statistics() }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { null }
        val valid = measured?.takeIf { it.totalRx >= 0 && it.totalTx >= 0 }
        _state.update { current ->
            if (generation == statisticsGeneration.get() && current.tunnelState.isTunnelUp()) {
                current.copy(stats = valid)
            } else current // Never attach old tunnel counters to a newer session.
        }
    }

    /** UI refresh rate is not the clock: missed ticks/background time are measured. */
    fun refreshElapsedTime() {
        val now = elapsedRealtimeMillis()
        _state.update { current ->
            val start = current.startedAtElapsedMillis
            if (!current.isTunnelUp || start == null) current
            else {
                val duration = if (now >= start) now - start else 0L
                current.copy(elapsedMillis = maxOf(current.elapsedMillis ?: 0L, duration))
            }
        }
    }

    private fun apply(transition: TunnelTransition) {
        statisticsGeneration.incrementAndGet()
        val now = elapsedRealtimeMillis()
        _state.update { current ->
            val remainsUp = transition.state.isTunnelUp() && current.isTunnelUp
            current.copy(
                tunnelState = transition.state,
                startedAtElapsedMillis = when {
                    !transition.state.isTunnelUp() -> null
                    remainsUp -> current.startedAtElapsedMillis
                    else -> now
                },
                elapsedMillis = when {
                    !transition.state.isTunnelUp() -> null
                    remainsUp -> current.elapsedMillis
                    else -> 0L
                },
                stats = current.stats.takeIf { transition.state.isTunnelUp() && current.tunnelState.isTunnelUp() },
                failure = transition.failure ?: current.failure.takeIf { transition.state == TunnelState.FAILED },
            )
        }
        transition.effects.forEach { effect -> execute(effect) }
    }

    private fun execute(effect: TunnelEffect) {
        when (effect) {
            TunnelEffect.RequestPermission -> {
                val request = platform.requestConsent()
                if (request == ConsentRequest.NotRequired) {
                    // Already granted: go straight to starting, without a second prompt.
                    apply(reduceTunnel(_state.value.tunnelState, TunnelEvent.PermissionGranted))
                } else {
                    apply(reduceTunnel(_state.value.tunnelState, TunnelEvent.PermissionRequired))
                    onConsentRequired?.invoke(request)
                }
            }

            TunnelEffect.StartTunnel -> {
                val configText = profileSource()
                if (configText == null) {
                    apply(
                        reduceTunnel(
                            _state.value.tunnelState,
                            TunnelEvent.BackendFailed(FailureReason.InvalidConfiguration),
                        ),
                    )
                    return
                }
                platform.startTunnel(configText)
            }

            TunnelEffect.StopTunnel -> platform.stopTunnel()

            is TunnelEffect.AnnounceFailure -> Unit // the reason is already in state

            TunnelEffect.ClearSession -> {
                _state.update { it.copy(stats = null) }
            }
        }
    }
}
