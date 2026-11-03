package com.kx.vpn.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kx.vpn.data.LaunchStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Why the acceptance write did not land. Never carries a value from storage. */
enum class OnboardingFailure { StorageWrite }

/**
 * The only place in the app allowed to mark onboarding as accepted.
 *
 * TASK_00 shipped the persistence contract but deliberately had no caller for it; this
 * is that caller. One write path means one place to audit.
 */
class OnboardingViewModel(
    private val repository: LaunchStateRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun acknowledgeDisclosure() {
        _state.value = _state.value.copy(disclosureAcknowledged = true, failure = null)
    }

    /**
     * Persists acceptance and reports the outcome. If the write fails the UI stays here
     * with a truthful error instead of navigating onward and implying the user's choice
     * was recorded — a silent-success path would be exactly the "forged state" failure
     * the security model names.
     */
    fun completeOnboarding() {
        val current = _state.value
        if (current.busy || !current.disclosureAcknowledged) return

        _state.value = current.copy(busy = true, failure = null)
        viewModelScope.launch {
            val outcome = runCatching { repository.completeOnboarding() }
            _state.value = if (outcome.isSuccess) {
                _state.value.copy(busy = false, completed = true, failure = null)
            } else {
                _state.value.copy(busy = false, completed = false, failure = OnboardingFailure.StorageWrite)
            }
        }
    }
}

data class OnboardingUiState(
    val disclosureAcknowledged: Boolean = false,
    val busy: Boolean = false,
    val completed: Boolean = false,
    val failure: OnboardingFailure? = null,
) {
    /** The Continue control is only live once the disclosure has been read. */
    val canComplete: Boolean
        get() = disclosureAcknowledged && !busy && !completed
}
