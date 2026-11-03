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
        assertEquals(SessionSnapshot(SessionPhase.Failed, SessionProblem.CleanupRequired), session.state.value)
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
    @Test fun serviceCleanupCannotHideConnectionFailure() = runBlocking {
        val driver = Driver().apply { throws = true }
        val session = SessionController(driver); session.configured(); session.connect()
        val failed = session.state.value
        val stopsBeforeShutdown = driver.stops
        session.disconnect(preserveFailure = true)
        assertEquals(stopsBeforeShutdown + 1, driver.stops)
        assertEquals(failed, session.state.value)
    }
    @Test fun cancelledStartStillStopsBackendAndPropagatesCancellation() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        var stopped = false
        val session = SessionController(object : TunnelDriver {
            override suspend fun start(): Boolean {
                entered.complete(Unit)
                kotlinx.coroutines.awaitCancellation()
            }
            override suspend fun stop(): Boolean {
                kotlinx.coroutines.yield() // Must work despite the cancelled parent.
                stopped = true
                return true
            }
        })
        session.configured()
        val connecting = launch { session.connect() }
        entered.await()
        connecting.cancel()
        connecting.join()
        assertTrue(connecting.isCancelled)
        assertTrue("Cancelling connect must clean up the backend, not just UI state", stopped)
        assertEquals(SessionPhase.Failed, session.state.value.phase)
    }

    @Test fun thrownStartAttemptsBackendCleanupBeforeReturningFailure() = runBlocking {
        val driver = Driver().apply { throws = true }
        val session = SessionController(driver)
        session.configured(); session.connect()
        assertEquals("start could have partially established a tunnel", 1, driver.stops)
        assertEquals(SessionProblem.Backend, session.state.value.problem)
    }
    @Test fun unconfirmedCleanupBlocksNewStartAndProfileReplacementUntilRetrySucceeds() = runBlocking {
        val driver = Driver().apply { succeeds = false }
        val session = SessionController(driver)
        session.configured(); session.connect()
        val attempts = driver.starts
        session.connect(); session.permissionDenied()
        assertEquals(attempts, driver.starts)
        assertEquals(SessionProblem.CleanupRequired, session.state.value.problem)
        var replacementRejected = false
        try { session.configured() } catch (_: IllegalStateException) { replacementRejected = true }
        assertTrue(replacementRejected)
        driver.succeeds = true
        session.disconnect()
        assertEquals(SessionPhase.Disconnected, session.state.value.phase)
        session.configured(); session.connect()
        assertEquals(SessionPhase.Up, session.state.value.phase)
    }

    @Test fun shutdownExceptionRetainsUnknownCleanupAndDoesNotLeakItsMessage() = runBlocking {
        val session = SessionController(object : TunnelDriver {
            override suspend fun start() = false
            override suspend fun stop(): Boolean = error("test-only sensitive backend error")
        })
        session.configured(); session.connect()
        assertEquals(SessionProblem.CleanupRequired, session.state.value.problem)
        session.disconnect()
        assertEquals(SessionProblem.CleanupRequired, session.state.value.problem)
        assertTrue(!session.state.value.toString().contains("sensitive"))
    }

    @Test fun cancelledDisconnectRetriesShutdownOutsideCancelledOwner() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        var stops = 0
        val session = SessionController(object : TunnelDriver {
            override suspend fun start() = true
            override suspend fun stop(): Boolean {
                stops++
                if (stops == 1) {
                    entered.complete(Unit)
                    kotlinx.coroutines.awaitCancellation()
                }
                kotlinx.coroutines.yield()
                return true
            }
        })
        session.configured(); session.connect()
        val disconnecting = launch { session.disconnect() }
        entered.await(); disconnecting.cancel(); disconnecting.join()
        assertTrue(disconnecting.isCancelled)
        assertEquals(2, stops)
        assertEquals(SessionSnapshot(SessionPhase.Failed, SessionProblem.Backend), session.state.value)
    }
    @Test fun nativeStyleIoCompletionAfterCancellationIsTornDown() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = java.util.concurrent.CountDownLatch(1)
        var backendUp = false
        var stops = 0
        val session = SessionController(object : TunnelDriver {
            override suspend fun start(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                entered.complete(Unit)
                check(release.await(5, java.util.concurrent.TimeUnit.SECONDS))
                backendUp = true // JNI-like work completed even though the owner cancelled.
                true
            }
            override suspend fun stop(): Boolean {
                stops++
                backendUp = false
                return true
            }
        })
        session.configured()
        val connecting = launch { session.connect() }
        try {
            entered.await()
            connecting.cancel()
        } finally { release.countDown() }
        connecting.join()
        assertTrue(connecting.isCancelled)
        assertEquals(1, stops)
        assertTrue(!backendUp)
        assertEquals(SessionPhase.Failed, session.state.value.phase)
    }

    @Test fun cancellingQueuedCommandDoesNotTearDownAnotherOwnersTunnel() = runBlocking {
        val driver = Driver().apply { pending = CompletableDeferred() }
        val session = SessionController(driver); session.configured()
        val connecting = launch { session.connect() }
        kotlinx.coroutines.yield()
        val queued = launch { session.connect() }
        kotlinx.coroutines.yield()
        queued.cancel(); queued.join()
        driver.pending!!.complete(Unit)
        connecting.join()
        assertEquals(1, driver.starts)
        assertEquals(0, driver.stops)
        assertEquals(SessionPhase.Up, session.state.value.phase)
    }
    @Test fun idleRemovalCallsStorageAndReturnsToUnconfigured() = runBlocking {
        val session = SessionController(Driver()); session.configured()
        var removals = 0
        assertTrue(session.removeProfile { removals++; true })
        assertEquals(1, removals)
        assertEquals(SessionSnapshot(), session.state.value)
    }

    @Test fun activeRemovalDoesNotTouchStoredProfile() = runBlocking {
        val session = SessionController(Driver()); session.configured(); session.connect()
        var touched = false
        assertTrue(!session.removeProfile { touched = true; true })
        assertTrue(!touched)
        assertEquals(SessionPhase.Up, session.state.value.phase)
    }

    @Test fun uncertainShutdownCannotEraseProfile() = runBlocking {
        val session = SessionController(Driver().apply { succeeds = false })
        session.configured(); session.connect()
        var touched = false
        assertTrue(!session.removeProfile { touched = true; true })
        assertTrue(!touched)
        assertEquals(SessionProblem.CleanupRequired, session.state.value.problem)
    }

    @Test fun failedStorageRemovalNeverReportsUnconfigured() = runBlocking {
        val session = SessionController(Driver()); session.configured()
        assertTrue(!session.removeProfile { false })
        assertEquals(SessionPhase.Disconnected, session.state.value.phase)
        try { session.removeProfile { throw java.io.IOException("synthetic removal error") } }
        catch (_: java.io.IOException) { /* Must propagate, not claim success. */ }
        assertEquals(SessionPhase.Disconnected, session.state.value.phase)
    }

    @Test fun cancelledRemovalKeepsThePreviousSnapshot() = runBlocking {
        val session = SessionController(Driver()); session.configured()
        val entered = CompletableDeferred<Unit>()
        val deleting = launch {
            session.removeProfile { entered.complete(Unit); kotlinx.coroutines.awaitCancellation() }
        }
        entered.await(); deleting.cancel(); deleting.join()
        assertTrue(deleting.isCancelled)
        assertEquals(SessionPhase.Disconnected, session.state.value.phase)
    }
}
