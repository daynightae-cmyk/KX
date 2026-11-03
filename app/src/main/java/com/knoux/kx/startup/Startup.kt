package com.knoux.kx.startup

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Task 00 only reads this decision. Only completed onboarding may later write acceptance. */
fun interface StartupRepository {
    suspend fun isOnboardingAccepted(): Boolean
}

enum class LaunchDestination { FirstRun, Returning }

sealed interface StartupState {
    data object Loading : StartupState
    data class Ready(val destination: LaunchDestination) : StartupState
    data object ReadFailure : StartupState
}

/** One in-flight read, no timers, no network and no destructive preferences recovery. */
class StartupController(
    private val repository: StartupRepository,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow<StartupState>(StartupState.Loading)
    val state: StateFlow<StartupState> = mutableState.asStateFlow()
    private var readJob: Job? = null

    fun load() {
        if (readJob?.isActive == true) return
        readJob = scope.launch {
            mutableState.value = StartupState.Loading
            try {
                val destination = if (repository.isOnboardingAccepted()) {
                    LaunchDestination.Returning
                } else {
                    LaunchDestination.FirstRun
                }
                mutableState.value = StartupState.Ready(destination)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                mutableState.value = StartupState.ReadFailure
            }
        }
    }
}
