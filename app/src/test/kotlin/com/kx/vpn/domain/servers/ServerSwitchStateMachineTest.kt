package com.kx.vpn.domain.servers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The endpoint switch flow, including the path that matters most: what happens when
 * the new endpoint fails.
 */
class ServerSwitchStateMachineTest {

    private fun step(
        context: ServerSwitchContext,
        event: ServerSwitchEvent,
    ): Pair<ServerSwitchContext, ServerSwitchTransition> = reduceServerSwitch(context, event)

    @Test
    fun aSwitchDisconnectsTheOldTunnelFirst() {
        val (context, transition) = step(
            ServerSwitchContext(),
            ServerSwitchEvent.SwitchRequested("new"),
        )
        assertEquals(ServerSwitchState.DISCONNECTING_OLD, transition.state)
        assertEquals(listOf(ServerSwitchEffect.DisconnectOld), transition.effects)
        assertEquals("new", context.pendingEndpointId)
    }

    @Test
    fun theHappyPathReachesConnected() {
        var context = ServerSwitchContext()
        var transition: ServerSwitchTransition

        context = step(context, ServerSwitchEvent.SwitchRequested("new")).also { transition = it.second }.first
        context = step(context, ServerSwitchEvent.OldDisconnected).also { transition = it.second }.first
        assertEquals(ServerSwitchState.APPLYING_NEW, transition.state)
        assertEquals(listOf(ServerSwitchEffect.ApplyNew("new")), transition.effects)

        context = step(context, ServerSwitchEvent.NewConfigApplied).also { transition = it.second }.first
        assertEquals(ServerSwitchState.AWAITING_PERMISSION, transition.state)

        context = step(context, ServerSwitchEvent.PermissionGranted).also { transition = it.second }.first
        assertEquals(ServerSwitchState.CONNECTING, transition.state)
        assertEquals(listOf(ServerSwitchEffect.ConnectNew), transition.effects)

        context = step(context, ServerSwitchEvent.Connected).also { transition = it.second }.first
        assertEquals(ServerSwitchState.CONNECTED, transition.state)
        assertEquals("new", context.previousEndpointId)
    }

    @Test
    fun aFailedConnectRollsBackToThePreviousEndpoint() {
        var context = ServerSwitchContext(previousEndpointId = "old")
        var transition: ServerSwitchTransition

        context = step(context, ServerSwitchEvent.SwitchRequested("new")).also { transition = it.second }.first
        context = step(context, ServerSwitchEvent.OldDisconnected).also { transition = it.second }.first
        context = step(context, ServerSwitchEvent.NewConfigApplied).also { transition = it.second }.first
        context = step(context, ServerSwitchEvent.PermissionGranted).also { transition = it.second }.first

        context = step(
            context,
            ServerSwitchEvent.Failed(ServerSwitchFailure.ConnectFailed),
        ).also { transition = it.second }.first

        assertEquals(ServerSwitchState.ROLLING_BACK, transition.state)
        assertEquals(ServerSwitchFailure.ConnectFailed, transition.failure)
        assertEquals(
            listOf(ServerSwitchEffect.RestorePrevious),
            transition.effects,
        )

        context = step(context, ServerSwitchEvent.RollbackCompleted).also { transition = it.second }.first
        assertEquals(ServerSwitchState.IDLE, transition.state)
        assertEquals(
            "the rollback must still report why the switch failed",
            ServerSwitchFailure.ConnectFailed,
            transition.failure,
        )
    }

    @Test
    fun deniedPermissionRollsBackRatherThanLeavingTheSessionHalfSwitched() {
        var context = ServerSwitchContext(previousEndpointId = "old")
        var transition: ServerSwitchTransition

        context = step(context, ServerSwitchEvent.SwitchRequested("new")).also { transition = it.second }.first
        context = step(context, ServerSwitchEvent.OldDisconnected).also { transition = it.second }.first
        context = step(context, ServerSwitchEvent.NewConfigApplied).also { transition = it.second }.first
        context = step(context, ServerSwitchEvent.PermissionDenied).also { transition = it.second }.first

        assertEquals(ServerSwitchState.ROLLING_BACK, transition.state)
        assertEquals(ServerSwitchFailure.PermissionDenied, transition.failure)
        assertTrue(transition.effects.contains(ServerSwitchEffect.RestorePrevious))
    }

    /**
     * A rollback must never leave the session claiming to be connected. The only way
     * to reach CONNECTED is a real Connected event.
     */
    @Test
    fun noFailurePathCanEndInConnected() {
        val events = listOf(
            ServerSwitchEvent.SwitchRequested("new"),
            ServerSwitchEvent.OldDisconnected,
            ServerSwitchEvent.NewConfigApplied,
            ServerSwitchEvent.PermissionDenied,
            ServerSwitchEvent.Failed(ServerSwitchFailure.ApplyFailed),
            ServerSwitchEvent.Failed(ServerSwitchFailure.ConnectFailed),
            ServerSwitchEvent.RollbackCompleted,
        )
        var context = ServerSwitchContext()
        for (event in events) {
            val result = step(context, event)
            context = result.first
            assertTrue(
                "reached CONNECTED after $event",
                result.second.state != ServerSwitchState.CONNECTED,
            )
        }
    }

    @Test
    fun aSecondSwitchRequestDuringASwitchIsIgnored() {
        val (context, transition) = step(
            ServerSwitchContext(),
            ServerSwitchEvent.SwitchRequested("first"),
        )
        val (after, second) = step(context, ServerSwitchEvent.SwitchRequested("second"))
        assertEquals(ServerSwitchState.DISCONNECTING_OLD, second.state)
        assertTrue("the running switch must not be replaced", second.effects.isEmpty())
        assertEquals("first", after.pendingEndpointId)
    }

    @Test
    fun aMissingEndpointIdFailsTheSwitch() {
        val (context, _) = step(ServerSwitchContext(), ServerSwitchEvent.SwitchRequested("new"))
        // The pending id is dropped, so the apply step has nothing to apply.
        val broken = context.copy(pendingEndpointId = null)
        val (_, transition) = step(broken, ServerSwitchEvent.OldDisconnected)
        assertEquals(ServerSwitchState.FAILED, transition.state)
        assertEquals(ServerSwitchFailure.UnknownEndpoint, transition.failure)
    }
}
