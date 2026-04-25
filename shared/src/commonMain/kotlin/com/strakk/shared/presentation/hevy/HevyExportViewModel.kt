package com.strakk.shared.presentation.hevy

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.model.WorkoutProgram
import com.strakk.shared.domain.usecase.ExportToHevyUseCase
import com.strakk.shared.domain.usecase.GetHevyApiKeyUseCase
import com.strakk.shared.domain.usecase.ParseWorkoutPdfUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.launch

/**
 * Manages the Hevy export flow.
 *
 * State transitions follow [HevyExportContract]:
 * - [HevyExportEvent.OnPdfSelected] → Parsing → SessionList (or Idle on error / missing key)
 * - [HevyExportEvent.OnSessionSelected] → Exporting → Done (or back to SessionList on error)
 * - [HevyExportEvent.OnExportAnother] → SessionList
 * - [HevyExportEvent.OnDismiss] → Dismiss effect
 */
class HevyExportViewModel(
    private val parseWorkoutPdf: ParseWorkoutPdfUseCase,
    private val exportToHevy: ExportToHevyUseCase,
    private val getHevyApiKey: GetHevyApiKeyUseCase,
) : MviViewModel<HevyExportUiState, HevyExportEvent, HevyExportEffect>(HevyExportUiState.Idle) {

    private var program: WorkoutProgram? = null
    private var hevyApiKey: String? = null

    override fun onEvent(event: HevyExportEvent) {
        when (event) {
            is HevyExportEvent.OnPdfSelected -> handlePdfSelected(event.pdfBase64)
            is HevyExportEvent.OnSessionSelected -> handleSessionSelected(event.sessionIndex)
            is HevyExportEvent.OnExportAnother -> handleExportAnother()
            is HevyExportEvent.OnDismiss -> emit(HevyExportEffect.Dismiss)
        }
    }

    private fun handlePdfSelected(pdfBase64: String) {
        setState { HevyExportUiState.Parsing }
        viewModelScope.launch {
            // Resolve the API key first — avoids a wasted Edge Function call.
            val keyResult = getHevyApiKey()
            val key = keyResult.getOrNull()
            if (key.isNullOrBlank()) {
                setState { HevyExportUiState.Idle }
                emit(HevyExportEffect.RequireApiKey)
                return@launch
            }
            hevyApiKey = key

            parseWorkoutPdf(pdfBase64)
                .onSuccess { parsed ->
                    program = parsed
                    setState { HevyExportUiState.SessionList(parsed.programName, parsed.sessions) }
                }
                .onFailure { error ->
                    setState { HevyExportUiState.Idle }
                    emit(HevyExportEffect.ShowError(error.message ?: "Failed to parse workout PDF."))
                }
        }
    }

    private fun handleSessionSelected(sessionIndex: Int) {
        val currentProgram = program ?: return
        val session = currentProgram.sessions.getOrNull(sessionIndex) ?: return
        val apiKey = hevyApiKey ?: run {
            emit(HevyExportEffect.RequireApiKey)
            return
        }

        setState { HevyExportUiState.Exporting(session.name) }
        viewModelScope.launch {
            exportToHevy(session, apiKey)
                .onSuccess { result ->
                    setState { HevyExportUiState.Done(result) }
                    emit(HevyExportEffect.ExportSuccess(result.routineTitle))
                }
                .onFailure { error ->
                    setState {
                        HevyExportUiState.SessionList(currentProgram.programName, currentProgram.sessions)
                    }
                    emit(HevyExportEffect.ShowError(error.message ?: "Export to Hevy failed."))
                }
        }
    }

    private fun handleExportAnother() {
        val currentProgram = program ?: return
        setState { HevyExportUiState.SessionList(currentProgram.programName, currentProgram.sessions) }
    }
}
