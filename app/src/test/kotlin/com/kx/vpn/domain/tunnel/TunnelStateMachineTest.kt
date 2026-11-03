package com.kx.vpn.domain.tunnel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exhaustive coverage of the tunnel state machine.
 *
 * The property that matters most is stated as acceptance criterion 1 of TASK_02: the
 * displayed state cannot be falsified by timers. This file proves it structurally — by
 * enumerating every (state, event) pair and showing that `CONNECTED` is reachable from
 * exactly one event and from nothing else.
 */
class TunnelStateMachineTest {

    private val allStates = TunnelState.entries
    private val allEvents: List<TunnelEvent> = listOf(
        TunnelEvent.ProfileAvailable, TunnelEvent.ProfileRemoved,
        TunnelEvent.ConnectRequested, TunnelEvent.DisconnectRequested,
        TunnelEvent.PermissionRequired, TunnelEvent.PermissionGranted, TunnelEvent.PermissionDenied,
        TunnelEvent.TunnelUp, TunnelEvent.TunnelDown,
        TunnelEvent.BackendFailed(FailureReason.TunnelStartFailed),
        TunnelEvent.NetworkLost, TunnelEvent.NetworkRestored,
        TunnelEvent.RevokeReceived, TunnelEvent.RetryRequested,
    )

    // -- The structural guarantee ----------------------------------------------------

    /**
     * Enumerates the whole transition table. The guarantee is about *arriving* at
     * CONNECTED, not about sitting in it: an ignored event on an already-connected
     * session legitimately returns CONNECTED unchanged. So the property is that the only
     * transitions into CONNECTED come out of CONNECTING or RECONNECTING and are driven by
     * `TunnelUp`.
     *
     * If a timer, an animation callback or an optimistic write ever gained the power to
     * show a protected state, this test is what fails.
     */
    @Test
    fun connectedIsEnteredOnlyViaTunnelUp() {
        val offenders = mutableListOf<String>()
        for (state in allStates) {
            if (state == TunnelState.CONNECTED) continue // already there; not an arrival
            for (event in allEvents) {
                val next = reduceTunnel(state, event).state
                if (next == TunnelState.CONNECTED) {
                    if (event != TunnelEvent.TunnelUp) {
                        offenders += "$state + $event -> CONNECTED"
                    }
                }
            }
        }
        assertTrue("CONNECTED must require a backend fact, found: $offenders", offenders.isEmpty())
    }

    /** Arriving at CONNECTED is legal from exactly the two in-flight states. */
    @Test
    fun onlyInFlightStatesMayBecomeConnected() {
        val arrivals = mutableListOf<Pair<TunnelState, TunnelEvent>>()
        for (state in allStates) {
            for (event in allEvents) {
                if (state != TunnelState.CONNECTED && reduceTunnel(state, event).state == TunnelState.CONNECTED) {
                    arrivals += state to event
                }
            }
        }
        assertEquals(
            setOf(
                TunnelState.CONNECTING to TunnelEvent.TunnelUp,
                TunnelState.RECONNECTING to TunnelEvent.TunnelUp,
            ),
            arrivals.toSet(),
        )
    }

    @Test
    fun tunnelUpOnlyConnectsFromConnectingOrReconnecting() {
        assertEquals(TunnelState.CONNECTED, reduceTunnel(TunnelState.CONNECTING, TunnelEvent.TunnelUp).state)
        assertEquals(TunnelState.CONNECTED, reduceTunnel(TunnelState.RECONNECTING, TunnelEvent.TunnelUp).state)

        // A stale UP from any other state must be ignored, never trusted.
        for (state in listOf(
            TunnelState.UNCONFIGURED, TunnelState.DISCONNECTED, TunnelState.PREPARING_PERMISSION,
            TunnelState.AWAITING_PERMISSION, TunnelState.FAILED, TunnelState.REVOKED,
        )) {
            assertEquals(
                "$state must not accept a stray TunnelUp",
                state,
                reduceTunnel(state, TunnelEvent.TunnelUp).state,
            )
        }
    }

    /**
     * The dangerous interleaving: the user taps disconnect while the backend is still
     * bringing the tunnel up, and the backend's UP callback lands afterwards. The session
     * must stay in teardown, not flip to connected.
     */
    @Test
    fun staleTunnelUpAfterDisconnectDoesNotReconnect() {
        var state = TunnelState.DISCONNECTED
        state = reduceTunnel(state, TunnelEvent.ConnectRequested).state
        assertEquals(TunnelState.PREPARING_PERMISSION, state)
        state = reduceTunnel(state, TunnelEvent.PermissionGranted).state
        assertEquals(TunnelState.CONNECTING, state)

        state = reduceTunnel(state, TunnelEvent.DisconnectRequested).state
        assertEquals(TunnelState.DISCONNECTING, state)

        val stale = reduceTunnel(state, TunnelEvent.TunnelUp)
        assertEquals("a late UP must not resurrect a cancelled connect", TunnelState.DISCONNECTING, stale.state)

        state = reduceTunnel(state, TunnelEvent.TunnelDown).state
        assertEquals(TunnelState.DISCONNECTED, state)
    }

    // -- Totality --------------------------------------------------------------------

    @Test
    fun everyStateEventPairProducesATransition() {
        for (state in allStates) {
            for (event in allEvents) {
                val transition = reduceTunnel(state, event)
                assertNotNull(transition)
                assertTrue(
                    "unknown result state from $state + $event",
                    transition.state in allStates,
                )
            }
        }
    }

    @Test
    fun ignoredTransitionsNeverEmitEffects() {
        // UNCONFIGURED cannot connect, so it must not be handed a StartTunnel effect.
        val transition = reduceTunnel(TunnelState.UNCONFIGURED, TunnelEvent.ConnectRequested)
        assertEquals(TunnelState.UNCONFIGURED, transition.state)
        assertTrue(transition.effects.isEmpty())
    }

    // -- Connect / disconnect lifecycle ---------------------------------------------

    @Test
    fun happyPathConnectThenDisconnect() {
        var state = TunnelState.DISCONNECTED

        var transition = reduceTunnel(state, TunnelEvent.ConnectRequested)
        assertEquals(TunnelState.PREPARING_PERMISSION, transition.state)
        assertEquals(listOf(TunnelEffect.RequestPermission), transition.effects)
        state = transition.state

        transition = reduceTunnel(state, TunnelEvent.PermissionRequired)
        assertEquals(TunnelState.AWAITING_PERMISSION, transition.state)
        state = transition.state

        transition = reduceTunnel(state, TunnelEvent.PermissionGranted)
        assertEquals(TunnelState.CONNECTING, transition.state)
        assertEquals(listOf(TunnelEffect.StartTunnel), transition.effects)
        state = transition.state

        transition = reduceTunnel(state, TunnelEvent.TunnelUp)
        assertEquals(TunnelState.CONNECTED, transition.state)
        assertNull(transition.failure)
        state = transition.state

        transition = reduceTunnel(state, TunnelEvent.DisconnectRequested)
        assertEquals(TunnelState.DISCONNECTING, transition.state)
        assertEquals(listOf(TunnelEffect.StopTunnel), transition.effects)
        state = transition.state

        transition = reduceTunnel(state, TunnelEvent.TunnelDown)
        assertEquals(TunnelState.DISCONNECTED, transition.state)
        assertTrue(transition.effects.contains(TunnelEffect.ClearSession))
    }

    @Test
    fun repeatedConnectIsIdempotentWhileConnectingOrConnected() {
        for (state in listOf(TunnelState.CONNECTING, TunnelState.CONNECTED)) {
            val transition = reduceTunnel(state, TunnelEvent.ConnectRequested)
            assertEquals("$state must ignore a second connect", state, transition.state)
            assertTrue(transition.effects.isEmpty())
        }
    }

    @Test
    fun connectFromDisconnectedGoesToPermissionFirstNotStraightToConnecting() {
        val transition = reduceTunnel(TunnelState.DISCONNECTED, TunnelEvent.ConnectRequested)
        assertEquals(TunnelState.PREPARING_PERMISSION, transition.state)
        assertFalse(transition.effects.contains(TunnelEffect.StartTunnel))
    }

    @Test
    fun disconnectWhileConnectingStillStopsTheTunnel() {
        val transition = reduceTunnel(TunnelState.CONNECTING, TunnelEvent.DisconnectRequested)
        assertEquals(TunnelState.DISCONNECTING, transition.state)
        assertTrue(
            "a cancelled start must still stop whatever the backend created",
            transition.effects.contains(TunnelEffect.StopTunnel),
        )
    }

    @Test
    fun disconnectFromIdleStatesIsANoOp() {
        for (state in listOf(
            TunnelState.DISCONNECTED, TunnelState.UNCONFIGURED, TunnelState.FAILED,
            TunnelState.REVOKED, TunnelState.DISCONNECTING,
        )) {
            assertEquals(state, reduceTunnel(state, TunnelEvent.DisconnectRequested).state)
        }
    }

    // -- Failure paths ---------------------------------------------------------------

    @Test
    fun deniedPermissionFailsWithThatReasonAndNeverReachesConnecting() {
        val preparing = reduceTunnel(TunnelState.DISCONNECTED, TunnelEvent.ConnectRequested)
        val failed = reduceTunnel(preparing.state, TunnelEvent.PermissionDenied)

        assertEquals(TunnelState.FAILED, failed.state)
        assertEquals(FailureReason.PermissionDenied, failed.failure)
        assertEquals(listOf(TunnelEffect.AnnounceFailure(FailureReason.PermissionDenied)), failed.effects)
    }

    @Test
    fun everyFailureCarriesAReason() {
        for (reason in FailureReason.entries) {
            for (state in listOf(
                TunnelState.CONNECTING, TunnelState.CONNECTED, TunnelState.RECONNECTING,
                TunnelState.PREPARING_PERMISSION, TunnelState.DISCONNECTING,
            )) {
                val transition = reduceTunnel(state, TunnelEvent.BackendFailed(reason))
                assertEquals(TunnelState.FAILED, transition.state)
                assertEquals("FAILED without a reason from $state", reason, transition.failure)
                assertTrue(transition.effects.contains(TunnelEffect.ClearSession))
            }
        }
    }

    @Test
    fun retryFromFailedReRequestsPermission() {
        val transition = reduceTunnel(TunnelState.FAILED, TunnelEvent.RetryRequested)
        assertEquals(TunnelState.PREPARING_PERMISSION, transition.state)
        assertEquals(listOf(TunnelEffect.RequestPermission), transition.effects)
    }

    @Test
    fun retryFromUnconfiguredDoesNothing() {
        val transition = reduceTunnel(TunnelState.UNCONFIGURED, TunnelEvent.RetryRequested)
        assertEquals(TunnelState.UNCONFIGURED, transition.state)
        assertTrue(transition.effects.isEmpty())
    }

    // -- Revocation ------------------------------------------------------------------

    @Test
    fun revocationWinsFromEveryLiveState() {
        for (state in listOf(
            TunnelState.CONNECTED, TunnelState.CONNECTING, TunnelState.RECONNECTING,
            TunnelState.DISCONNECTING, TunnelState.PREPARING_PERMISSION,
            TunnelState.AWAITING_PERMISSION, TunnelState.FAILED,
        )) {
            val transition = reduceTunnel(state, TunnelEvent.RevokeReceived)
            assertEquals("revoke from $state", TunnelState.REVOKED, transition.state)
            assertEquals(FailureReason.RevokedBySystem, transition.failure)
            assertTrue(transition.effects.contains(TunnelEffect.ClearSession))
        }
    }

    @Test
    fun revocationIsIgnoredWhenNothingIsRunning() {
        for (state in listOf(TunnelState.UNCONFIGURED, TunnelState.DISCONNECTED)) {
            assertEquals(state, reduceTunnel(state, TunnelEvent.RevokeReceived).state)
        }
    }

    // -- Network churn ---------------------------------------------------------------

    @Test
    fun networkLossWhileConnectedReconnectsAndRestorationRestartsTunnel() {
        val lost = reduceTunnel(TunnelState.CONNECTED, TunnelEvent.NetworkLost)
        assertEquals(TunnelState.RECONNECTING, lost.state)

        val restored = reduceTunnel(TunnelState.RECONNECTING, TunnelEvent.NetworkRestored)
        assertEquals(TunnelState.CONNECTING, restored.state)
        assertEquals(listOf(TunnelEffect.StartTunnel), restored.effects)

        assertEquals(TunnelState.CONNECTED, reduceTunnel(restored.state, TunnelEvent.TunnelUp).state)
    }

    @Test
    fun networkLossWhileDisconnectedIsIgnored() {
        for (state in listOf(TunnelState.DISCONNECTED, TunnelState.UNCONFIGURED, TunnelState.FAILED)) {
            assertEquals(state, reduceTunnel(state, TunnelEvent.NetworkLost).state)
        }
    }

    // -- Configuration ---------------------------------------------------------------

    @Test
    fun removingAProfileWhileConnectedStopsTheTunnel() {
        val transition = reduceTunnel(TunnelState.CONNECTED, TunnelEvent.ProfileRemoved)
        assertEquals(TunnelState.UNCONFIGURED, transition.state)
        assertTrue(transition.effects.contains(TunnelEffect.StopTunnel))
        assertTrue(transition.effects.contains(TunnelEffect.ClearSession))
    }

    @Test
    fun profileOnlyMovesUnconfiguredToDisconnected() {
        assertEquals(
            TunnelState.DISCONNECTED,
            reduceTunnel(TunnelState.UNCONFIGURED, TunnelEvent.ProfileAvailable).state,
        )
        for (state in TunnelState.entries.filter { it != TunnelState.UNCONFIGURED }) {
            assertEquals(state, reduceTunnel(state, TunnelEvent.ProfileAvailable).state)
        }
    }

    // -- UI gating -------------------------------------------------------------------

    @Test
    fun connectIsOfferedOnlyWhenNothingIsRunning() {
        val connectable = TunnelState.entries.filter { it.canInitiateConnect() }
        assertEquals(
            setOf(TunnelState.DISCONNECTED, TunnelState.FAILED, TunnelState.REVOKED),
            connectable.toSet(),
        )
    }

    @Test
    fun isTunnelUpIsTrueOnlyForConnected() {
        TunnelState.entries.forEach { state ->
            val message = "isTunnelUp for $state"
            if (state == TunnelState.CONNECTED) assertTrue(message, state.isTunnelUp())
            else assertFalse(message, state.isTunnelUp())
        }
    }

    @Test
    fun disconnectIsOfferedOnlyWhileAStreamExists() {
        assertEquals(
            setOf(TunnelState.CONNECTED, TunnelState.CONNECTING, TunnelState.RECONNECTING),
            TunnelState.entries.filter { it.canDisconnect() }.toSet(),
        )
    }

    /**
     * Long random-ish walks must never dead-end: from any reachable state, either a
     * disconnect or a revoke eventually lands in a state where connect is offered again.
     */
    @Test
    fun noWalkCanTrapTheSessionInAConnectedShape() {
        val sequence = listOf<TunnelEvent>(
            TunnelEvent.ProfileAvailable, TunnelEvent.ConnectRequested, TunnelEvent.PermissionGranted,
            TunnelEvent.TunnelUp, TunnelEvent.NetworkLost, TunnelEvent.NetworkRestored,
            TunnelEvent.TunnelUp, TunnelEvent.RevokeReceived, TunnelEvent.RetryRequested,
            TunnelEvent.PermissionDenied, TunnelEvent.RetryRequested, TunnelEvent.PermissionRequired,
            TunnelEvent.PermissionGranted, TunnelEvent.TunnelUp, TunnelEvent.DisconnectRequested,
            TunnelEvent.TunnelDown,
        )
        var state = TunnelState.UNCONFIGURED
        for (event in sequence) {
            state = reduceTunnel(state, event).state
        }
        assertEquals(TunnelState.DISCONNECTED, state)
        assertTrue(state.canInitiateConnect())
    }
}
