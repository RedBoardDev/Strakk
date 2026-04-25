package com.strakk.shared.presentation.meal

import com.strakk.shared.domain.model.MealEntry

/**
 * Form state for the manual-entry screen.
 *
 * All numeric fields are stored as [String] so the UI can display raw user
 * input (including in-progress decimals) without premature coercion.
 */
data class ManualEntryUiState(
    val name: String = "",
    val protein: String = "",
    val calories: String = "",
    val fat: String = "",
    val carbs: String = "",
    val quantity: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
) {
    val isSubmittable: Boolean
        get() = name.isNotBlank() &&
            protein.toDoubleOrNull() != null &&
            calories.toDoubleOrNull() != null
}

sealed interface ManualEntryEvent {
    data class NameChanged(val value: String) : ManualEntryEvent
    data class ProteinChanged(val value: String) : ManualEntryEvent
    data class CaloriesChanged(val value: String) : ManualEntryEvent
    data class FatChanged(val value: String) : ManualEntryEvent
    data class CarbsChanged(val value: String) : ManualEntryEvent
    data class QuantityChanged(val value: String) : ManualEntryEvent
    data object Submit : ManualEntryEvent
    data object Cancel : ManualEntryEvent
}

sealed interface ManualEntryEffect {
    data class Submitted(val entry: MealEntry) : ManualEntryEffect
    data object Cancelled : ManualEntryEffect
    data class ShowError(val message: String) : ManualEntryEffect
}
