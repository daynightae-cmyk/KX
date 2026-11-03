package com.kx.vpn.data

import com.kx.vpn.domain.servers.EndpointProfile
import com.kx.vpn.domain.servers.ServerSwitchContext
import com.kx.vpn.domain.servers.ServerSwitchEffect
import com.kx.vpn.domain.servers.ServerSwitchEvent
import com.kx.vpn.domain.servers.ServerSwitchFailure
import com.kx.vpn.domain.servers.ServerSwitchState
import com.kx.vpn.domain.servers.reduceServerSwitch
import com.kx.vpn.domain.tunnel.TunnelState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

/**
 * Makes the endpoint the user tapped the endpoint the tunnel actually uses.
 *
 * The rules live in [reduceServerSwitch]; this class only performs its effects, which
 * is the same split the tunnel session uses. Three properties matter and are tested:
 *
 *  * **A selection is only accepted when the configuration exists on this device.**
 *    An endpoint whose profile is missing fails with `UnknownEndpoint` and changes
 *    nothing — the app never reports a server as chosen when it could not connect to it.
 *  * **A live switch tears the old tunnel down before applying the new one, and rolls
 *    back if the new one does not come up.**
 *  * **Nothing here can claim a connection.** `CONNECTED` is only reached when the
 *    session manager's own state says the platform reported the tunnel up.
 *
 * After a failed switch the previous endpoint is restored as the *selection*, but the
 * tunnel is not silently re-started: the user asked to leave it, and reconnecting
 * without being asked would hide the failure they need to see.
 */
class EndpointSwitchController(
    private val sessionManager: TunnelSessionManager,
    private val profiles: ProfileRepository,
    private val catalogue: EndpointCatalogueRepository,
    private val scope: CoroutineScope,
    /** How long to wait for the platform to answer a teardown or a new tunnel. */
    private val settleTimeoutMillis: Long = DEFAULT_SETTLE_TIMEOUT_MILLIS,
) {

    private val _switch = MutableStateFlow(ServerSwitchContext())

    /**
     * True once the tunnel machine has actually been asked for a connection during the
     * current switch. The observer uses it to tell "the new tunnel failed" apart from
     * "the old tunnel is down and we have not asked for the new one yet".
     */
    private var connectRequested = false

    /** The switch in flight, if any. The UI renders this rather than guessing. */
    val switch: StateFlow<ServerSwitchContext> = _switch.asStateFlow()

    /**
     * The endpoint the user has chosen, read from the catalogue each time it is needed.
     *
     * Deliberately not a collected flow: the controller writes the choice and the screen
     * reads it from the catalogue's own state, so there is exactly one source of truth and
     * no long-lived collector to keep alive.
     */
    private suspend fun currentChoice(): String? = catalogue.state.first().activeEndpointId

    /**
     * Selects [endpoint]. A call while a live switch is running is ignored, so the user
     * always learns the outcome of the switch they started instead of having it
     * overtaken underneath them.
     */
    fun select(endpoint: EndpointProfile) {
        if (_switch.value.state.isInFlight()) return
        val tunnelState = sessionManager.state.value.tunnelState
        if (!tunnelState.isSettled()) {
            // The platform is still answering something else. Swapping the
            // configuration now would change what an in-flight operation uses, so the
            // choice is refused and said out loud instead of quietly applied.
            _switch.value = ServerSwitchContext(
                state = ServerSwitchState.FAILED,
                failure = ServerSwitchFailure.Busy,
            )
            return
        }
        if (!profiles.hasStoredProfile(endpoint.profileId)) {
            // The catalogue can describe an endpoint whose configuration is not on this
            // device — a profile deleted elsewhere, or metadata restored without its
            // vault entry. Say so instead of pretending the choice took effect.
            _switch.value = ServerSwitchContext(
                state = ServerSwitchState.FAILED,
                failure = ServerSwitchFailure.UnknownEndpoint,
            )
            return
        }
        scope.launch {
            if (tunnelState == TunnelState.CONNECTED) switchLiveTunnel(endpoint)
            else applyWithoutTunnel(endpoint)
        }
    }

    /** Clears a failure the user has already been shown. */
    fun acknowledgeFailure() {
        val current = _switch.value.state
        if (current.isInFlight()) return
        if (current == ServerSwitchState.FAILED || _switch.value.failure != null) {
            _switch.value = ServerSwitchContext()
        }
    }

    /**
     * The tunnel is down, so choosing a server is a preference change and nothing more.
     * No connection is implied, and the session state is left exactly where it was.
     */
    private suspend fun applyWithoutTunnel(endpoint: EndpointProfile) {
        if (!profiles.activate(endpoint.profileId)) {
            _switch.value = ServerSwitchContext(
                state = ServerSwitchState.FAILED,
                previousEndpointId = currentChoice(),
                failure = ServerSwitchFailure.ApplyFailed,
            )
            return
        }
        catalogue.setActiveEndpoint(endpoint.id)
        sessionManager.refreshProfilePresence()
        _switch.value = ServerSwitchContext(
            state = ServerSwitchState.IDLE,
            previousEndpointId = endpoint.id,
        )
    }

    private suspend fun switchLiveTunnel(endpoint: EndpointProfile) {
        // Re-checked here: the state was read before this coroutine was scheduled, and a
        // tunnel that has meanwhile come down needs no teardown.
        if (sessionManager.state.value.tunnelState != TunnelState.CONNECTED) {
            applyWithoutTunnel(endpoint)
            return
        }
        val requested = _switch.value.state
        connectRequested = false
        // Seed the rollback target before starting: the reducer carries
        // previousEndpointId through the switch, and on the very first switch after a
        // cold start nothing has set it yet. Without this, a failed switch would have
        // nothing to restore.
        _switch.value = _switch.value.copy(previousEndpointId = currentChoice())
        dispatch(ServerSwitchEvent.SwitchRequested(endpoint.id))
        // The reducer refuses a switch while one is in flight; nothing was emitted, so
        // there is nothing to wait for.
        if (_switch.value.state == requested) return

        var observer: Job? = null
        try {
            if (!awaitTunnel { !it.isTunnelUp && it.tunnelState != TunnelState.DISCONNECTING }) {
                failWithoutRollback(ServerSwitchFailure.ConnectFailed)
                return
            }
            dispatch(ServerSwitchEvent.OldDisconnected)
            if (_switch.value.state != ServerSwitchState.APPLYING_NEW &&
                _switch.value.state != ServerSwitchState.AWAITING_PERMISSION &&
                _switch.value.state != ServerSwitchState.CONNECTING
            ) {
                // ApplyNew failed and the rollback already ran.
                return
            }

            // From here the tunnel machine owns consent and the connect attempt, so its
            // states are translated back into switch events as they happen.
            observer = scope.launch { observeTunnel() }
            val settled = awaitTunnel {
                it.tunnelState == TunnelState.CONNECTED ||
                    it.tunnelState.isTerminalFailure() ||
                    !_switch.value.state.isInFlight()
            }
            when {
                // The observer already reached an outcome (including a rollback).
                !_switch.value.state.isInFlight() -> Unit

                !settled -> dispatch(ServerSwitchEvent.Failed(ServerSwitchFailure.ConnectFailed))

                sessionManager.state.value.tunnelState == TunnelState.CONNECTED ->
                    dispatch(ServerSwitchEvent.Connected)

                else -> dispatch(ServerSwitchEvent.Failed(ServerSwitchFailure.ConnectFailed))
            }
        } finally {
            observer?.cancel()
        }
    }

    /** A switch that cannot be rolled back to anything still has to say what happened. */
    private suspend fun failWithoutRollback(reason: ServerSwitchFailure) {
        _switch.value = ServerSwitchContext(
            state = ServerSwitchState.FAILED,
            previousEndpointId = currentChoice(),
            failure = reason,
        )
    }

    private suspend fun observeTunnel() {
        sessionManager.state.collect { state ->
            val switchState = _switch.value.state
            val permissionPassed = switchState == ServerSwitchState.AWAITING_PERMISSION &&
                (state.tunnelState == TunnelState.CONNECTING || state.tunnelState == TunnelState.CONNECTED)
            val tunnelGaveUp = connectRequested &&
                (state.tunnelState.isTerminalFailure() || state.tunnelState == TunnelState.DISCONNECTED) &&
                (switchState == ServerSwitchState.CONNECTING ||
                    switchState == ServerSwitchState.AWAITING_PERMISSION)
            when {
                // Consent is held and the backend is being asked for the new tunnel.
                permissionPassed -> dispatch(ServerSwitchEvent.PermissionGranted)

                // Only after we actually asked for a connection: before that, a
                // DISCONNECTED tunnel is the normal state between teardown and apply.
                tunnelGaveUp -> dispatch(ServerSwitchEvent.Failed(ServerSwitchFailure.ConnectFailed))

                else -> Unit
            }
        }
    }

    /**
     * Advances the reducer and performs whatever it emitted.
     *
     * Recursion is bounded: ApplyNew dispatches NewConfigApplied, and RestorePrevious
     * dispatches RollbackCompleted, one level each.
     */
    private suspend fun dispatch(event: ServerSwitchEvent) {
        val (next, transition) = reduceServerSwitch(_switch.value, event)
        _switch.value = next
        transition.effects.forEach { execute(it) }
    }

    private suspend fun execute(effect: ServerSwitchEffect) {
        when (effect) {
            ServerSwitchEffect.DisconnectOld -> sessionManager.disconnect()

            is ServerSwitchEffect.ApplyNew -> {
                val endpoint = catalogue.state.first().endpoints.firstOrNull { it.id == effect.endpointId }
                val activated = endpoint != null && profiles.activate(endpoint.profileId)
                if (!activated) {
                    dispatch(ServerSwitchEvent.Failed(ServerSwitchFailure.ApplyFailed))
                    return
                }
                catalogue.setActiveEndpoint(effect.endpointId)
                sessionManager.refreshProfilePresence()
                dispatch(ServerSwitchEvent.NewConfigApplied)
            }

            // Consent is the platform's question, and the session manager is the only
            // thing allowed to ask it. connect() either starts straight away because
            // consent is already held, or surfaces the consent request.
            ServerSwitchEffect.RequestPermission -> {
                connectRequested = true
                sessionManager.connect()
            }

            ServerSwitchEffect.ConnectNew -> {
                connectRequested = true
                // Idempotent: the session manager's own reducer ignores a connect while
                // one is already in flight, so this cannot start a second tunnel.
                sessionManager.connect()
            }

            ServerSwitchEffect.RestorePrevious -> {
                val previous = _switch.value.previousEndpointId?.let { id ->
                    catalogue.state.first().endpoints.firstOrNull { it.id == id }
                }
                // Nothing to go back to, or it no longer decrypts: the choice is left as
                // it is and the failure is reported, rather than pointing the tunnel at a
                // profile that is not there.
                if (previous != null && profiles.activate(previous.profileId)) {
                    catalogue.setActiveEndpoint(previous.id)
                }
                sessionManager.refreshProfilePresence()
                dispatch(ServerSwitchEvent.RollbackCompleted)
            }
        }
    }

    private suspend fun awaitTunnel(predicate: (TunnelSessionState) -> Boolean): Boolean =
        try {
            withTimeout(settleTimeoutMillis) { sessionManager.state.first(predicate) }
            true
        } catch (_: TimeoutCancellationException) {
            false
        }

    private fun ServerSwitchState.isInFlight(): Boolean = when (this) {
        ServerSwitchState.DISCONNECTING_OLD,
        ServerSwitchState.APPLYING_NEW,
        ServerSwitchState.AWAITING_PERMISSION,
        ServerSwitchState.CONNECTING,
        ServerSwitchState.ROLLING_BACK,
        -> true

        else -> false
    }

    private fun TunnelState.isTerminalFailure(): Boolean = when (this) {
        TunnelState.FAILED, TunnelState.REVOKED -> true
        else -> false
    }

    /**
     * States where nothing is in flight, so changing the chosen endpoint is safe.
     * `CONNECTED` counts as settled because a live switch is exactly what it is for.
     */
    private fun TunnelState.isSettled(): Boolean = when (this) {
        TunnelState.UNCONFIGURED,
        TunnelState.DISCONNECTED,
        TunnelState.CONNECTED,
        TunnelState.FAILED,
        TunnelState.REVOKED,
        -> true

        else -> false
    }

    companion object {
        /** Long enough for a slow handset to answer, short enough to be worth reporting. */
        const val DEFAULT_SETTLE_TIMEOUT_MILLIS = 20_000L
    }
}
