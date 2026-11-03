package com.knoux.kx.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

class StartupViewModel(repository: StartupRepository) : ViewModel() {
    private val controller = StartupController(repository, viewModelScope)
    val state = controller.state
    init { controller.load() }
    fun retry() = controller.load()
}
