package com.kx.vpn.domain.servers

/**
 * Switching to a different endpoint.
 *
 * The order is not negotiable: the old tunnel is torn down **before** the new
 * configuration is applied, because two tunnels cannot own the same platform VPN
 * interface. If the new one fails at any point, the previous endpoint is restored —
 * and the session reports `FAILED` in the meantime rather than claiming a connection
 * that does not exist.
 */
enum class ServerSwitchState {
    IDLE,

    /** The old tunnel is being closed. */
    DISCONNECTING_OLD,

    /** The new configuration is being handed to the backend. */
    APPLYING_NEW,

    /** The platform wants VPN consent again. */
    AWAITING_PERMISSION,

    /** The new tunnel is coming up. */
    CONNECTING,

    /** The new endpoint is up. */
    CONNECTED,

    /** The new endpoint failed; the previous one is being restored. */
    ROLLING_BACK,

    /** The switch failed and could not be rolled back either. */
    FAILED,
}

enum class ServerSwitchFailure {
    PermissionDenied,
    ApplyFailed,
    ConnectFailed,
    RollbackFailed,
    UnknownEndpoint,

    /**
     * Another tunnel operation is already running.
     *
     * Choosing a server while the tunnel is connecting, reconnecting or being torn down
     * would change the configuration underneath an operation the platform is still
     * answering, so the choice is refused and the user is told to wait.
     */
    Busy,
}

sealed interface ServerSwitchEvent {
    data class SwitchRequested(val endpointId: String) : ServerSwitchEvent
    data object OldDisconnected : ServerSwitchEvent
    data object NewConfigApplied : ServerSwitchEvent
    data object PermissionRequired : ServerSwitchEvent
    data object PermissionGranted : ServerSwitchEvent
    data object PermissionDenied : ServerSwitchEvent
    data object Connected : ServerSwitchEvent
    data class Failed(val reason: ServerSwitchFailure) : ServerSwitchEvent
    data object RollbackCompleted : ServerSwitchEvent
}

sealed interface ServerSwitchEffect {
    data object DisconnectOld : ServerSwitchEffect
    data class ApplyNew(val endpointId: String) : ServerSwitchEffect
    data object RequestPermission : ServerSwitchEffect
    data object ConnectNew : ServerSwitchEffect
    data object RestorePrevious : ServerSwitchEffect
}

data class ServerSwitchTransition(
    val state: ServerSwitchState,
    val failure: ServerSwitchFailure? = null,
    val effects: List<ServerSwitchEffect> = emptyList(),
)

/**
 * The context a switch runs in: which endpoint is being moved to, and which one was
 * active before, so a rollback has something to restore.
 */
data class ServerSwitchContext(
    val state: ServerSwitchState = ServerSwitchState.IDLE,
    val pendingEndpointId: String? = null,
    val previousEndpointId: String? = null,
    /** Why the switch failed, so the rollback can report it when it finishes. */
    val failure: ServerSwitchFailure? = null,
)

/**
 * Pure reducer for the switch flow.
 *
 * Like the tunnel state machine, it contains no clock and no I/O: the caller performs
 * the effects and reports back. That is what makes the rollback path testable without
 * a device.
 */
fun reduceServerSwitch(
    context: ServerSwitchContext,
    event: ServerSwitchEvent,
): Pair<ServerSwitchContext, ServerSwitchTransition> {
    fun done(
        state: ServerSwitchState,
        failure: ServerSwitchFailure? = null,
        effects: List<ServerSwitchEffect> = emptyList(),
        pending: String? = context.pendingEndpointId,
        previous: String? = context.previousEndpointId,
    ) = context.copy(
        state = state,
        pendingEndpointId = pending,
        previousEndpointId = previous,
        failure = failure ?: context.failure.takeIf { state == ServerSwitchState.ROLLING_BACK },
    ) to ServerSwitchTransition(state, failure, effects)

    return when (event) {

        is ServerSwitchEvent.SwitchRequested -> {
            // A switch in flight is not interrupted by another switch request; the
            // user gets the outcome of the one already running.
            if (context.state != ServerSwitchState.IDLE &&
                context.state != ServerSwitchState.CONNECTED &&
                context.state != ServerSwitchState.FAILED
            ) {
                done(context.state)
            } else {
                done(
                    state = ServerSwitchState.DISCONNECTING_OLD,
                    pending = event.endpointId,
                    previous = context.previousEndpointId,
                    effects = listOf(ServerSwitchEffect.DisconnectOld),
                )
            }
        }

        ServerSwitchEvent.OldDisconnected -> when (context.state) {
            ServerSwitchState.DISCONNECTING_OLD -> {
                val pending = context.pendingEndpointId
                    ?: return done(
                        ServerSwitchState.FAILED,
                        ServerSwitchFailure.UnknownEndpoint,
                    )
                done(
                    state = ServerSwitchState.APPLYING_NEW,
                    effects = listOf(ServerSwitchEffect.ApplyNew(pending)),
                )
            }

            else -> done(context.state)
        }

        ServerSwitchEvent.NewConfigApplied -> when (context.state) {
            ServerSwitchState.APPLYING_NEW ->
                done(
                    state = ServerSwitchState.AWAITING_PERMISSION,
                    effects = listOf(ServerSwitchEffect.RequestPermission),
                )

            else -> done(context.state)
        }

        ServerSwitchEvent.PermissionRequired -> done(context.state)

        ServerSwitchEvent.PermissionGranted -> when (context.state) {
            ServerSwitchState.AWAITING_PERMISSION -> done(
                state = ServerSwitchState.CONNECTING,
                effects = listOf(ServerSwitchEffect.ConnectNew),
            )

            else -> done(context.state)
        }

        ServerSwitchEvent.PermissionDenied -> when (context.state) {
            ServerSwitchState.AWAITING_PERMISSION -> done(
                state = ServerSwitchState.ROLLING_BACK,
                failure = ServerSwitchFailure.PermissionDenied,
                effects = listOf(ServerSwitchEffect.RestorePrevious),
            )

            else -> done(context.state)
        }

        ServerSwitchEvent.Connected -> when (context.state) {
            ServerSwitchState.CONNECTING -> done(
                state = ServerSwitchState.CONNECTED,
                // The switch succeeded, so the pending endpoint becomes the current one.
                previous = context.pendingEndpointId,
                pending = null,
            )

            else -> done(context.state)
        }

        is ServerSwitchEvent.Failed -> when (context.state) {
            ServerSwitchState.CONNECTING,
            ServerSwitchState.AWAITING_PERMISSION,
            ServerSwitchState.APPLYING_NEW,
            -> done(
                state = ServerSwitchState.ROLLING_BACK,
                failure = event.reason,
                effects = listOf(ServerSwitchEffect.RestorePrevious),
            )

            else -> done(ServerSwitchState.FAILED, event.reason)
        }

        ServerSwitchEvent.RollbackCompleted -> when (context.state) {
            ServerSwitchState.ROLLING_BACK -> done(
                state = ServerSwitchState.IDLE,
                failure = context.failure ?: ServerSwitchFailure.ConnectFailed,
                pending = null,
            )

            else -> done(context.state)
        }
    }
}
