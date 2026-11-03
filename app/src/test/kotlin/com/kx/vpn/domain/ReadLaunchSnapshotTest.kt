package com.kx.vpn.domain

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadLaunchSnapshotTest {
    private val unknown = LaunchSnapshot(onboardingCompleted = false, storageDegraded = true)

    @Test fun freshStoreRemainsIncompleteAndKnown() = runBlocking {
        val snapshot = LaunchSnapshot(onboardingCompleted = false, storageDegraded = false)
        assertEquals(snapshot, readLaunchSnapshot(flowOf(snapshot)))
    }

    @Test fun acceptedStoreRemainsAccepted() = runBlocking {
        val snapshot = LaunchSnapshot(onboardingCompleted = true, storageDegraded = false)
        assertEquals(snapshot, readLaunchSnapshot(flowOf(snapshot)))
    }

    @Test fun silentStoreReachesDegradedWelcomeAtDeadline() = runBlocking {
        val result = readLaunchSnapshot(flow { awaitCancellation() }, timeoutMillis = 25)
        assertEquals(unknown, result)
        assertEquals(LaunchDestination.Welcome, decideLaunchDestination(result))
    }

    @Test fun emptyStoreIsUnknownRatherThanFresh() = runBlocking {
        assertEquals(unknown, readLaunchSnapshot(emptyFlow()))
    }

    @Test fun emittedValueDoesNotWaitForDeadline() = runBlocking {
        val snapshot = LaunchSnapshot(onboardingCompleted = true, storageDegraded = false)
        val result = readLaunchSnapshot(flow {
            emit(snapshot)
        })
        assertEquals(snapshot, result)
    }

    @Test fun timedOutCollectionIsCancelledAndCannotAcceptLateSetup() = runBlocking {
        var closed = false
        val result = readLaunchSnapshot(flow {
            try { awaitCancellation() } finally { closed = true }
        }, timeoutMillis = 20)
        assertEquals(unknown, result)
        assertTrue(closed)
    }

    @Test fun parentCancellationIsNotConvertedToDegradedSuccess() = runBlocking {
        val started = CompletableDeferred<Unit>()
        var returned = false
        val job = launch {
            readLaunchSnapshot(flow { started.complete(Unit); awaitCancellation() })
            returned = true
        }
        started.await()
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
        assertFalse(returned)
    }

    @Test fun upstreamFailureIsNotHiddenByTimeoutFallback() = runBlocking {
        val expected = IOException("synthetic test failure")
        try {
            readLaunchSnapshot(flow { throw expected })
            throw AssertionError("Expected upstream failure")
        } catch (actual: IOException) {
            assertSame(expected, actual)
        }
    }

    @Test fun invalidDeadlineIsRejected() = runBlocking {
        for (deadline in listOf(0L, -1L)) {
            try {
                readLaunchSnapshot(emptyFlow(), deadline)
                throw AssertionError("Expected invalid deadline failure")
            } catch (_: IllegalArgumentException) {
                // Invalid caller arguments are not storage errors.
            }
        }
    }
}
