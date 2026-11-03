package com.kx.vpn.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kx.vpn.data.session.WireGuardSessionRepository
import com.kx.vpn.domain.config.BoundedFileReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(val repository: WireGuardSessionRepository) : ViewModel() {
    val session = repository.state
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val mutableError = MutableStateFlow(false)
    val error = mutableError.asStateFlow()

    init { viewModelScope.launch { repository.restore() } }

    fun importConfig(read: suspend () -> BoundedFileReader.Outcome) {
        if (mutableBusy.value) return
        mutableBusy.value = true
        mutableError.value = false
        viewModelScope.launch {
            try {
                when (val outcome = read()) {
                    is BoundedFileReader.Outcome.Read -> try {
                        mutableError.value = !repository.saveOwnedConfig(outcome.bytes)
                    } finally { outcome.bytes.fill(0) }
                    else -> mutableError.value = true
                }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { mutableError.value = true
            } finally { mutableBusy.value = false }
        }
    }
    fun actionFailed() { mutableError.value = true }
    fun disconnect() { viewModelScope.launch { repository.disconnect() } }
}
