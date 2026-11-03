package com.knoux.kx.startup

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupControllerTest {
    private fun scope() = CoroutineScope(Job() + Dispatchers.Unconfined)

    @Test fun initialStateIsLoading() {
        val scope = scope()
        try {
            val controller = StartupController({ false }, scope)
            assertEquals(StartupState.Loading, controller.state.value)
        } finally { scope.cancel() }
    }

    @Test fun freshInstallRoutesToFirstRun() {
        val scope = scope()
        try {
            val controller = StartupController({ false }, scope)
            controller.load()
            assertEquals(StartupState.Ready(LaunchDestination.FirstRun), controller.state.value)
        } finally { scope.cancel() }
    }

    @Test fun acceptedSetupRoutesToReturning() {
        val scope = scope()
        try {
            val controller = StartupController({ true }, scope)
            controller.load()
            assertEquals(StartupState.Ready(LaunchDestination.Returning), controller.state.value)
        } finally { scope.cancel() }
    }

    @Test fun diskFailureIsNotTreatedAsFirstRun() {
        val scope = scope()
        try {
            val controller = StartupController({ throw IOException("test-only") }, scope)
            controller.load()
            assertEquals(StartupState.ReadFailure, controller.state.value)
        } finally { scope.cancel() }
    }

    @Test fun retryReadsAgainWithoutResettingPreferences() {
        val scope = scope()
        try {
            var reads = 0
            val controller = StartupController({
                reads++
                if (reads == 1) throw IOException("test-only")
                true
            }, scope)
            controller.load()
            assertEquals(StartupState.ReadFailure, controller.state.value)
            controller.load()
            assertEquals(2, reads)
            assertEquals(StartupState.Ready(LaunchDestination.Returning), controller.state.value)
        } finally { scope.cancel() }
    }

    @Test fun duplicateLoadDoesNotStartConcurrentRead() = runBlocking {
        val scope = scope()
        try {
            val pending = CompletableDeferred<Boolean>()
            var reads = 0
            val controller = StartupController({ reads++; pending.await() }, scope)
            controller.load()
            controller.load()
            assertEquals(1, reads)
            assertEquals(StartupState.Loading, controller.state.value)
            pending.complete(false)
            assertEquals(StartupState.Ready(LaunchDestination.FirstRun), controller.state.value)
        } finally { scope.cancel() }
    }

    @Test fun cancellationDoesNotBecomeErrorOrReady() {
        val scope = scope()
        val pending = CompletableDeferred<Boolean>()
        val controller = StartupController({ pending.await() }, scope)
        controller.load()
        scope.cancel()
        pending.complete(true)
        assertEquals(StartupState.Loading, controller.state.value)
    }

    @Test fun newControllerRereadsPersistentDecisionAfterProcessDeath() {
        val scope = scope()
        try {
            var accepted = false
            val store = StartupRepository { accepted }
            val first = StartupController(store, scope)
            first.load()
            assertEquals(StartupState.Ready(LaunchDestination.FirstRun), first.state.value)
            // Only a future completed onboarding flow writes acceptance, never startup.
            accepted = true
            val restored = StartupController(store, scope)
            restored.load()
            assertEquals(StartupState.Ready(LaunchDestination.Returning), restored.state.value)
            assertTrue(accepted)
        } finally { scope.cancel() }
    }

    @Test fun repeatedFirstRunLaunchNeverAcceptsOnboarding() {
        val scope = scope()
        try {
            val accepted = false
            repeat(3) {
                val controller = StartupController({ accepted }, scope)
                controller.load()
                assertEquals(StartupState.Ready(LaunchDestination.FirstRun), controller.state.value)
            }
            assertFalse(accepted)
        } finally { scope.cancel() }
    }
}
