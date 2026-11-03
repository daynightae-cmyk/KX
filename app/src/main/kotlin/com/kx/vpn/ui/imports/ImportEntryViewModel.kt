package com.kx.vpn.ui.imports

import androidx.lifecycle.ViewModel
import com.kx.vpn.domain.config.BoundedFileReader
import com.kx.vpn.domain.config.ConfigIssue
import com.kx.vpn.domain.config.ConfigValidation
import com.kx.vpn.domain.config.TunnelSummary
import com.kx.vpn.domain.config.WireGuardConfigValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State for the first-run configuration entry.
 *
 * Nothing here is persisted. TASK_01 establishes a real, bounded, honest *inspection*
 * path; storing a tunnel profile securely is TASK_02's job and the rich import workflow
 * is TASK_09's. Claiming to have saved a configuration would be the fake closure this
 * whole file exists to avoid.
 */
sealed interface ImportUiState {
    data object Idle : ImportUiState
    data object Reading : ImportUiState

    data class Inspected(
        val summary: TunnelSummary,
        val warnings: List<ConfigIssue>,
        /**
         * The parsed configuration text. It is held in memory only and is never
         * logged; it is passed straight to the encrypted vault when the user saves.
         */
        val configText: String,
    ) : ImportUiState

    data class Rejected(val issues: List<ConfigIssue>) : ImportUiState

    /** The file could not be read at all — distinct from "read and invalid". */
    data class Unreadable(val reason: BoundedFileReader.FailureReason) : ImportUiState
}

class ImportEntryViewModel : ViewModel() {

    private val _state = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    fun onReadStarted() {
        _state.value = ImportUiState.Reading
    }

    fun onBytesRead(outcome: BoundedFileReader.Outcome) {
        _state.value = when (outcome) {
            is BoundedFileReader.Outcome.Failed -> ImportUiState.Unreadable(outcome.reason)

            is BoundedFileReader.Outcome.OverLimit -> ImportUiState.Rejected(
                listOf(
                    ConfigIssue(
                        ConfigIssue.Severity.Error,
                        "document",
                        com.kx.vpn.domain.config.IssueMessage.TooLarge,
                    ),
                ),
            )

            is BoundedFileReader.Outcome.Read ->
                when (val validation = WireGuardConfigValidator.validate(outcome.bytes)) {
                    is ConfigValidation.Valid -> ImportUiState.Inspected(
                        summary = validation.summary,
                        warnings = validation.warnings,
                        configText = String(outcome.bytes, Charsets.UTF_8),
                    )

                    is ConfigValidation.Invalid -> ImportUiState.Rejected(validation.errors)
                }
        }
    }

    fun reset() {
        _state.value = ImportUiState.Idle
    }
}
