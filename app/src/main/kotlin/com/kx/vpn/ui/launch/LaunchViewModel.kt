package com.kx.vpn.ui.launch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kx.vpn.data.LaunchStateRepository
import com.kx.vpn.domain.LaunchDestination
import com.kx.vpn.domain.LaunchSnapshot
import com.kx.vpn.domain.decideLaunchDestination
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Launch gate state.
 *
 * [LaunchUiState.Loading] is the honest "the store has not answered yet" state. It is
 * what keeps the system splash alive; it is never replaced by an assumed value.
 */
sealed interface LaunchUiState {
    data object Loading : LaunchUiState
    data class Resolved(
        val destination: LaunchDestination,
        val storageDegraded: Boolean,
    ) : LaunchUiState
}

class LaunchViewModel(
    repository: LaunchStateRepository,
) : ViewModel() {

    val state: StateFlow<LaunchUiState> = repository.snapshot
        .map<LaunchSnapshot, LaunchUiState> { snapshot ->
            LaunchUiState.Resolved(
                destination = decideLaunchDestination(snapshot),
                storageDegraded = snapshot.storageDegraded,
            )
        }
        .stateIn(
            scope = viewModelScope,
            // Eagerly, not WhileSubscribed: the platform splash reads this state
            // before Compose has subscribed, so the store read has to start on its
            // own. A subscription-gated share would leave it stuck at Loading until
            // the first frame, which is exactly the stall this gate exists to avoid.
            started = SharingStarted.Eagerly,
            initialValue = LaunchUiState.Loading,
        )
}
