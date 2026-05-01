package com.strakk.shared.presentation.meal

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.common.DomainError
import com.strakk.shared.domain.model.ManualEntryDraft
import com.strakk.shared.domain.usecase.QuickAddManualUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.launch

/**
 * Manual-entry form for a food item outside of a Draft.
 *
 * Validates input client-side before calling [QuickAddManualUseCase], which
 * enforces its own validation too — any thrown [DomainError.ValidationError]
 * is surfaced inline via [ManualEntryUiState.errorMessage].
 */
class ManualEntryViewModel(
    private val quickAddManual: QuickAddManualUseCase,
) : MviViewModel<ManualEntryUiState, ManualEntryEvent, ManualEntryEffect>(ManualEntryUiState()) {

    override fun onEvent(event: ManualEntryEvent) = when (event) {
        is ManualEntryEvent.NameChanged -> setState { copy(name = event.value, errorMessage = null) }
        is ManualEntryEvent.ProteinChanged -> setState { copy(protein = event.value, errorMessage = null) }
        is ManualEntryEvent.CaloriesChanged -> setState { copy(calories = event.value, errorMessage = null) }
        is ManualEntryEvent.FatChanged -> setState { copy(fat = event.value, errorMessage = null) }
        is ManualEntryEvent.CarbsChanged -> setState { copy(carbs = event.value, errorMessage = null) }
        is ManualEntryEvent.QuantityChanged -> setState { copy(quantity = event.value, errorMessage = null) }
        is ManualEntryEvent.Submit -> handleSubmit(event.logDate)
        ManualEntryEvent.Cancel -> emit(ManualEntryEffect.Cancelled)
    }

    private fun handleSubmit(logDate: String? = null) {
        val state = uiState.value
        if (!state.isSubmittable || state.isSubmitting) return

        setState { copy(isSubmitting = true, errorMessage = null) }

        viewModelScope.launch {
            quickAddManual(buildDraft(state, logDate))
                .onSuccess { emit(ManualEntryEffect.Submitted(it)) }
                .onFailure { error ->
                    val message = (error as? DomainError.ValidationError)?.message
                        ?: error.message
                        ?: "An error occurred"
                    setState { copy(errorMessage = message) }
                    emit(ManualEntryEffect.ShowError(message))
                }
            setState { copy(isSubmitting = false) }
        }
    }

    private fun buildDraft(state: ManualEntryUiState, logDate: String?): ManualEntryDraft = ManualEntryDraft(
        name = state.name,
        protein = state.protein.toDoubleOrNull() ?: 0.0,
        calories = state.calories.toDoubleOrNull() ?: 0.0,
        fat = state.fat.toDoubleOrNull(),
        carbs = state.carbs.toDoubleOrNull(),
        quantity = state.quantity.takeIf { it.isNotBlank() },
        logDate = logDate,
    )
}
