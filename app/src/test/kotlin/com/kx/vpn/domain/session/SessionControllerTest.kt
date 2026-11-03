package com.kx.vpn.domain.session

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionControllerTest {
    private class Driver : TunnelDriver {
        var starts = 0
        var stops = 0
        var succeeds = true
        var throws = false
        var pending: CompletableDeferred<Unit>? = null
        override suspend fun start(): Boolean {
            starts++
            pending?.await()
            if (throws) error("test-only untrusted backend text")
            return succeeds
        }
        override suspend fun stop(): Boolean { stops++; return succeeds }
    }

    @Test fun noConfigCannotStart() = runBlocking {
        val driver = Driver(); val session = SessionController(driver)
        session.connect()
        assertEquals(0, driver.starts)
        assertEquals(SessionPhase.Unconfigured, session.state.value.phase)
    }
    @Test fun configuredDoesNotConnect() = runBlocking {
        val driver = Driver(); val session = SessionController(driver)
        session.configured()
        assertEquals(SessionPhase.Disconnected, session.state.value.phase)
        assertEquals(0, driver.starts)
    }
    @Test fun backendUpIsRequiredAndDuplicateConnectIsIdempotent() = runBlocking {
        val driver = Driver(); val session = SessionController(driver)
        session.configured(); session.connect(); session.connect()
        assertEquals(1, driver.starts)
        assertEquals(SessionPhase.Up, session.state.value.phase)
    }
    @Test fun backendFailureNeverBecomesUp() = runBlocking {
        val driver = Driver().apply { succeeds = false }; val session = SessionController(driver)
        session.configured(); session.connect()
        assertEquals(SessionSnapshot(SessionPhase.Failed, SessionProblem.Backend), session.state.value)
    }
    @Test fun exceptionMessagesAreNotExposed() = runBlocking {
        val driver = Driver().apply { throws = true }; val session = SessionController(driver)
        session.configured(); session.connect()
        assertEquals(SessionProblem.Backend, session.state.value.problem)
        assertTrue(!session.state.value.toString().contains("untrusted"))
    }
    @Test fun deniedConsentNeverCallsBackend() = runBlocking {
        val driver = Driver(); val session = SessionController(driver)
        session.configured(); session.permissionDenied()
        assertEquals(0, driver.starts)
        assertEquals(SessionProblem.PermissionDenied, session.state.value.problem)
    }
    @Test fun repeatedDisconnectIsIdempotent() = runBlocking {
        val driver = Driver(); val session = SessionController(driver)
        session.configured(); session.connect(); session.disconnect(); session.disconnect()
        assertEquals(1, driver.stops)
        assertEquals(SessionPhase.Disconnected, session.state.value.phase)
    }
    @Test fun revokeDropsBackendFact() = runBlocking {
        val session = SessionController(Driver())
        session.configured(); session.connect(); session.backendDown()
        assertEquals(SessionPhase.Disconnected, session.state.value.phase)
    }
    @Test fun coldControllerNeverAssumesPreviousConnection() = runBlocking {
        val first = SessionController(Driver()); first.configured(); first.connect()
        val restored = SessionController(Driver()); restored.configured()
        assertEquals(SessionPhase.Disconnected, restored.state.value.phase)
    }
    @Test fun disconnectQueuedBehindStartStopsTheRealBackend() = runBlocking {
        val driver = Driver().apply { pending = CompletableDeferred() }
        val session = SessionController(driver); session.configured()
        val connecting = launch { session.connect() }
        kotlinx.coroutines.yield()
        assertEquals(SessionPhase.Connecting, session.state.value.phase)
        val disconnecting = launch { session.disconnect() }
        driver.pending!!.complete(Unit)
        connecting.join(); disconnecting.join()
        assertEquals(1, driver.stops)
        assertEquals(SessionPhase.Disconnected, session.state.value.phase)
    }
    @Test fun revocationDuringStartCannotPublishStaleUp() = runBlocking {
        val driver = Driver().apply { pending = CompletableDeferred() }
        val session = SessionController(driver); session.configured()
        val connecting = launch { session.connect() }
        kotlinx.coroutines.yield()
        session.backendDown()
        driver.pending!!.complete(Unit)
        connecting.join()
        assertEquals(SessionPhase.Failed, session.state.value.phase)
    }
}
