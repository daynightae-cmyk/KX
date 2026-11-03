package com.kx.vpn.domain.tunnel

/**
 * The tunnel session state machine.
 *
 * `docs/ENGINEERING_CONTRACT.md` requires a stable state set and that connection state
 * be represented *only* from backend/platform facts. That requirement is the reason this
 * lives here as a pure reducer rather than inside a ViewModel: `CONNECTED` can only be
 * produced by a `TunnelUp` event, which the controller emits when the WireGuard backend
 * reports `Tunnel.State.UP`. There is no timer, no delay and no optimistic write in this
 * file that can put the UI into a connected shape, and acceptance criterion 1 of
 * TASK_02 ("state displayed cannot be falsified by timers") is enforced by the absence of
 * any time source at all.
 */
enum class TunnelState {
    /** No usable profile exists, so connecting is not offered. */
    UNCONFIGURED,

    /** A profile exists and no tunnel is up. */
    DISCONNECTED,

    /** The platform VPN consent check is in flight. */
    PREPARING_PERMISSION,

    /** The user must grant VPN consent before anything can happen. */
    AWAITING_PERMISSION,

    /** The backend has been asked to bring the tunnel up and has not answered. */
    CONNECTING,

    /** The backend reported the tunnel UP. Only `TunnelUp` can produce this. */
    CONNECTED,

    /** A teardown is in flight. */
    DISCONNECTING,

    /** The last attempt failed. Always carries a reason. */
    FAILED,

    /** Connectivity dropped mid-session; the tunnel is being re-established. */
    RECONNECTING,

    /** Android revoked our VPN permission; the session is gone whether we liked it or not. */
    REVOKED,
}

enum class FailureReason {
    PermissionDenied,
    BackendUnavailable,
    InvalidConfiguration,
    TunnelStartFailed,
    NetworkLost,
    RevokedBySystem,
    AlreadyInUse,
}

sealed interface TunnelEvent {
    data object ProfileAvailable : TunnelEvent
    data object ProfileRemoved : TunnelEvent

    data object ConnectRequested : TunnelEvent
    data object DisconnectRequested : TunnelEvent

    data object PermissionRequired : TunnelEvent
    data object PermissionGranted : TunnelEvent
    data object PermissionDenied : TunnelEvent

    /** The backend says the tunnel is up. The only path to CONNECTED. */
    data object TunnelUp : TunnelEvent

    /** The backend says the tunnel is down. */
    data object TunnelDown : TunnelEvent

    data class BackendFailed(val reason: FailureReason) : TunnelEvent

    data object NetworkLost : TunnelEvent
    data object NetworkRestored : TunnelEvent
    data object RevokeReceived : TunnelEvent
    data object RetryRequested : TunnelEvent
}

/**
 * Work the platform layer must perform. Returning effects instead of executing them
 * keeps the reducer total and testable, and makes it impossible for a test to pass
 * because a real service happened to be absent.
 */
sealed interface TunnelEffect {
    data object RequestPermission : TunnelEffect
    data object StartTunnel : TunnelEffect
    data object StopTunnel : TunnelEffect
    data class AnnounceFailure(val reason: FailureReason) : TunnelEffect
    data object ClearSession : TunnelEffect
}

data class TunnelTransition(
    val state: TunnelState,
    val failure: FailureReason? = null,
    val effects: List<TunnelEffect> = emptyList(),
)

/**
 * Total reducer: every (state, event) pair returns something. Unknown pairs are
 * explicitly ignored rather than left to throw, because a stray backend callback
 * arriving after a disconnect must not crash the session.
 */
fun reduceTunnel(current: TunnelState, event: TunnelEvent): TunnelTransition = when (event) {
    TunnelEvent.ProfileAvailable -> when (current) {
        TunnelState.UNCONFIGURED -> TunnelTransition(TunnelState.DISCONNECTED)
        else -> TunnelTransition(current)
    }

    TunnelEvent.ProfileRemoved -> when (current) {
        TunnelState.CONNECTED, TunnelState.CONNECTING, TunnelState.RECONNECTING,
        TunnelState.PREPARING_PERMISSION, TunnelState.AWAITING_PERMISSION,
        TunnelState.DISCONNECTING,
        -> TunnelTransition(TunnelState.UNCONFIGURED, effects = listOf(TunnelEffect.StopTunnel, TunnelEffect.ClearSession))

        else -> TunnelTransition(TunnelState.UNCONFIGURED, effects = listOf(TunnelEffect.ClearSession))
    }

    TunnelEvent.ConnectRequested -> when (current) {
        TunnelState.UNCONFIGURED -> TunnelTransition(current)
        TunnelState.CONNECTED, TunnelState.CONNECTING -> TunnelTransition(current)
        TunnelState.DISCONNECTING ->
            // A connect during teardown is queued by the caller, not forced here:
            // starting a second tunnel while the first is still closing is the race
            // that produces a half-owned interface.
            TunnelTransition(current)

        else -> TunnelTransition(TunnelState.PREPARING_PERMISSION, effects = listOf(TunnelEffect.RequestPermission))
    }

    TunnelEvent.PermissionRequired -> when (current) {
        TunnelState.PREPARING_PERMISSION -> TunnelTransition(TunnelState.AWAITING_PERMISSION)
        else -> TunnelTransition(current)
    }

    TunnelEvent.PermissionGranted -> when (current) {
        TunnelState.PREPARING_PERMISSION, TunnelState.AWAITING_PERMISSION ->
            TunnelTransition(TunnelState.CONNECTING, effects = listOf(TunnelEffect.StartTunnel))

        else -> TunnelTransition(current)
    }

    TunnelEvent.PermissionDenied -> when (current) {
        TunnelState.PREPARING_PERMISSION, TunnelState.AWAITING_PERMISSION ->
            TunnelTransition(
                TunnelState.FAILED,
                failure = FailureReason.PermissionDenied,
                effects = listOf(TunnelEffect.AnnounceFailure(FailureReason.PermissionDenied)),
            )

        else -> TunnelTransition(current)
    }

    TunnelEvent.TunnelUp -> when (current) {
        TunnelState.CONNECTING, TunnelState.RECONNECTING -> TunnelTransition(TunnelState.CONNECTED)
        else -> TunnelTransition(current)
    }

    TunnelEvent.TunnelDown -> when (current) {
        TunnelState.CONNECTED, TunnelState.CONNECTING, TunnelState.RECONNECTING, TunnelState.DISCONNECTING ->
            TunnelTransition(TunnelState.DISCONNECTED, effects = listOf(TunnelEffect.ClearSession))

        TunnelState.UNCONFIGURED -> TunnelTransition(current)
        else -> TunnelTransition(current)
    }

    TunnelEvent.DisconnectRequested -> when (current) {
        TunnelState.CONNECTED, TunnelState.RECONNECTING ->
            TunnelTransition(TunnelState.DISCONNECTING, effects = listOf(TunnelEffect.StopTunnel))

        TunnelState.CONNECTING ->
            // Cancelling a start that has not completed still has to stop whatever the
            // backend may already have created, otherwise the interface leaks.
            TunnelTransition(TunnelState.DISCONNECTING, effects = listOf(TunnelEffect.StopTunnel))

        else -> TunnelTransition(current)
    }

    is TunnelEvent.BackendFailed -> when (current) {
        TunnelState.CONNECTING, TunnelState.PREPARING_PERMISSION, TunnelState.AWAITING_PERMISSION,
        TunnelState.CONNECTED, TunnelState.RECONNECTING, TunnelState.DISCONNECTING,
        -> TunnelTransition(
            TunnelState.FAILED,
            failure = event.reason,
            effects = listOf(TunnelEffect.AnnounceFailure(event.reason), TunnelEffect.ClearSession),
        )

        else -> TunnelTransition(current, failure = event.reason)
    }

    TunnelEvent.NetworkLost -> when (current) {
        TunnelState.CONNECTED -> TunnelTransition(TunnelState.RECONNECTING)
        else -> TunnelTransition(current)
    }

    TunnelEvent.NetworkRestored -> when (current) {
        TunnelState.RECONNECTING -> TunnelTransition(TunnelState.CONNECTING, effects = listOf(TunnelEffect.StartTunnel))
        else -> TunnelTransition(current)
    }

    TunnelEvent.RevokeReceived -> when (current) {
        TunnelState.UNCONFIGURED, TunnelState.DISCONNECTED -> TunnelTransition(current)
        else -> TunnelTransition(
            TunnelState.REVOKED,
            failure = FailureReason.RevokedBySystem,
            effects = listOf(TunnelEffect.ClearSession),
        )
    }

    TunnelEvent.RetryRequested -> when (current) {
        TunnelState.FAILED, TunnelState.REVOKED ->
            TunnelTransition(TunnelState.PREPARING_PERMISSION, effects = listOf(TunnelEffect.RequestPermission))

        TunnelState.UNCONFIGURED -> TunnelTransition(current)
        else -> TunnelTransition(current)
    }
}

/** Whether the UI may offer a connect action at all. */
fun TunnelState.canInitiateConnect(): Boolean = when (this) {
    TunnelState.DISCONNECTED, TunnelState.FAILED, TunnelState.REVOKED -> true
    else -> false
}

fun TunnelState.canDisconnect(): Boolean = when (this) {
    TunnelState.CONNECTED, TunnelState.CONNECTING, TunnelState.RECONNECTING -> true
    else -> false
}

/**
 * The single place that decides whether the user is allowed to see anything resembling
 * "protected". Kept as one function so a later task cannot introduce a second, looser
 * definition.
 */
fun TunnelState.isTunnelUp(): Boolean = this == TunnelState.CONNECTED
