package com.kx.vpn.ui.onboarding

import com.kx.vpn.data.LaunchStateRepository
import com.kx.vpn.domain.LaunchSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The acceptance path, including what happens when the write fails.
 *
 * The failure case is the one that matters: a UI that navigates to Home after an
 * unwritten flag would show the user a connected-shaped app that forgot their choice on
 * the next launch. That is a forged state, just a quiet one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class FakeRepository : LaunchStateRepository {
        val snapshotFlow = MutableStateFlow(LaunchSnapshot())
        var throwOnWrite = false
        var writeCount = 0

        override val snapshot: Flow<LaunchSnapshot> = snapshotFlow

        override suspend fun completeOnboarding() {
            writeCount++
            if (throwOnWrite) error("disk full")
            snapshotFlow.value = snapshotFlow.value.copy(onboardingCompleted = true)
        }
    }

    @Test
    fun continueIsLockedUntilTheDisclosureIsAcknowledged() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = OnboardingViewModel(repository)
        assertFalse("Continue must start locked", viewModel.state.value.canComplete)

        viewModel.completeOnboarding()
        runCurrent()

        assertEquals("tapping Continue before acknowledging must not write", 0, repository.writeCount)
        assertFalse(viewModel.state.value.completed)
    }

    @Test
    fun acknowledgingUnlocksContinueButDoesNotItselfComplete() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = OnboardingViewModel(repository)

        viewModel.acknowledgeDisclosure()
        assertTrue(viewModel.state.value.canComplete)
        assertFalse(viewModel.state.value.completed)
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun completingWritesOnceAndMarksDone() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = OnboardingViewModel(repository)
        viewModel.acknowledgeDisclosure()

        viewModel.completeOnboarding()
        runCurrent()

        assertEquals(1, repository.writeCount)
        assertTrue(viewModel.state.value.completed)
        assertFalse(viewModel.state.value.busy)
        assertEquals(
            LaunchSnapshot(onboardingCompleted = true, storageDegraded = false),
            repository.snapshotFlow.value,
        )
    }

    /** A double tap must not produce two writes. */
    @Test
    fun secondTapWhileBusyIsIgnored() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = OnboardingViewModel(repository)
        viewModel.acknowledgeDisclosure()

        viewModel.completeOnboarding()
        viewModel.completeOnboarding()
        runCurrent()

        assertEquals(1, repository.writeCount)
    }

    @Test
    fun aFailedWriteKeepsTheUserHereAndReportsIt() = runTest(dispatcher) {
        val repository = FakeRepository().apply { throwOnWrite = true }
        val viewModel = OnboardingViewModel(repository)
        viewModel.acknowledgeDisclosure()

        viewModel.completeOnboarding()
        runCurrent()

        val state = viewModel.state.value
        assertFalse("a failed write must not report completion", state.completed)
        assertEquals(OnboardingFailure.StorageWrite, state.failure)
        assertFalse(state.busy)
        assertFalse("nothing was persisted, so the store must still say incomplete",
            repository.snapshotFlow.value.onboardingCompleted)
    }

    @Test
    fun retryAfterFailureSucceeds() = runTest(dispatcher) {
        val repository = FakeRepository().apply { throwOnWrite = true }
        val viewModel = OnboardingViewModel(repository)
        viewModel.acknowledgeDisclosure()

        viewModel.completeOnboarding()
        runCurrent()
        assertEquals(OnboardingFailure.StorageWrite, viewModel.state.value.failure)

        repository.throwOnWrite = false
        viewModel.completeOnboarding()
        runCurrent()

        assertTrue(viewModel.state.value.completed)
        assertEquals(null, viewModel.state.value.failure)
        assertEquals(2, repository.writeCount)
    }

    @Test
    fun acknowledgingAgainClearsAStaleFailure() = runTest(dispatcher) {
        val repository = FakeRepository().apply { throwOnWrite = true }
        val viewModel = OnboardingViewModel(repository)
        viewModel.acknowledgeDisclosure()
        viewModel.completeOnboarding()
        runCurrent()

        viewModel.acknowledgeDisclosure()
        assertEquals(null, viewModel.state.value.failure)
    }

}
