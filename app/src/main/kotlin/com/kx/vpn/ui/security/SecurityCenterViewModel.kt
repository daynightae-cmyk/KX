package com.kx.vpn.ui.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kx.vpn.data.SecuritySettings
import com.kx.vpn.data.SecuritySettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SecurityCenterViewModel(
    private val repository: SecuritySettingsRepository,
) : ViewModel() {

    val settings: StateFlow<SecuritySettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SecuritySettings(),
        )

    fun setKillSwitch(enabled: Boolean) {
        viewModelScope.launch { repository.setKillSwitch(enabled) }
    }

    fun setDnsLeakProtection(enabled: Boolean) {
        viewModelScope.launch { repository.setDnsLeakProtection(enabled) }
    }

    fun setIpv6Protection(enabled: Boolean) {
        viewModelScope.launch { repository.setIpv6Protection(enabled) }
    }

    fun setAutoReconnect(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoReconnect(enabled) }
    }

    fun setTrackerBlocking(enabled: Boolean) {
        viewModelScope.launch { repository.setTrackerBlocking(enabled) }
    }

    fun setAlwaysOnVpn(enabled: Boolean) {
        viewModelScope.launch { repository.setAlwaysOnVpn(enabled) }
    }
}

class SecurityCenterViewModelFactory(
    private val repository: SecuritySettingsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SecurityCenterViewModel(repository) as T
}
