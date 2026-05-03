package com.strakk.shared.presentation.meal

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.common.Logger
import com.strakk.shared.domain.model.Feature
import com.strakk.shared.domain.model.FeatureAccess
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.usecase.CheckFeatureAccessUseCase
import com.strakk.shared.domain.usecase.QuickAddFromPhotoUseCase
import com.strakk.shared.domain.usecase.QuickAddFromTextUseCase
import com.strakk.shared.domain.usecase.QuickAddKnownEntryUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.launch

private const val TAG = "QuickAddVM"

class QuickAddViewModel(
    private val quickAddKnownEntry: QuickAddKnownEntryUseCase,
    private val quickAddFromText: QuickAddFromTextUseCase,
    private val quickAddFromPhoto: QuickAddFromPhotoUseCase,
    private val checkFeatureAccess: CheckFeatureAccessUseCase,
    private val logger: Logger,
) : MviViewModel<QuickAddUiState, QuickAddEvent, QuickAddEffect>(QuickAddUiState()) {

    override fun onEvent(event: QuickAddEvent) = when (event) {
        is QuickAddEvent.AddKnown -> addKnown(event)
        is QuickAddEvent.AddFromText -> addFromText(event.description, event.logDate)
        is QuickAddEvent.AddFromPhoto -> addFromPhoto(event.imageBase64, event.hint, event.logDate)
        QuickAddEvent.ClearError -> setState { copy(errorMessage = null) }
    }

    private fun addKnown(event: QuickAddEvent.AddKnown) {
        launchQuickAdd {
            quickAddKnownEntry.addKnown(
                name = event.name,
                protein = event.protein,
                calories = event.calories,
                fat = event.fat,
                carbs = event.carbs,
                quantity = event.quantity,
                source = event.source,
                logDate = event.logDate,
            )
        }
    }

    private fun addFromText(description: String, logDate: String?) {
        viewModelScope.launch {
            when (val access = checkFeatureAccess(Feature.AI_TEXT_ANALYSIS)) {
                is FeatureAccess.Granted -> launchQuickAdd { quickAddFromText(description, logDate) }
                else -> emit(QuickAddEffect.FeatureGated(access))
            }
        }
    }

    private fun addFromPhoto(imageBase64: String, hint: String?, logDate: String?) {
        viewModelScope.launch {
            when (val access = checkFeatureAccess(Feature.AI_PHOTO_ANALYSIS)) {
                is FeatureAccess.Granted -> launchQuickAdd {
                    quickAddFromPhoto(imageBase64 = imageBase64, hint = hint, logDate = logDate)
                }
                else -> emit(QuickAddEffect.FeatureGated(access))
            }
        }
    }

    private fun launchQuickAdd(block: suspend () -> Result<MealEntry>) {
        if (uiState.value.isProcessing) return

        setState { copy(isProcessing = true, errorMessage = null) }
        viewModelScope.launch {
            block()
                .onSuccess { entry ->
                    emit(QuickAddEffect.Completed(entry))
                }
                .onFailure { error ->
                    val message = error.message ?: "An error occurred"
                    logger.e(TAG, "Quick-add FAILED: $message", error)
                    setState { copy(errorMessage = message) }
                    emit(QuickAddEffect.ShowError(message))
                }
            setState { copy(isProcessing = false) }
        }
    }
}
