package com.strakk.shared.presentation.meal

import com.strakk.shared.domain.model.EntrySource
import com.strakk.shared.domain.model.FeatureAccess
import com.strakk.shared.domain.model.MealEntry
data class QuickAddUiState(
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface QuickAddEvent {
    data class AddKnown(
        val name: String,
        val protein: Double,
        val calories: Double,
        val fat: Double?,
        val carbs: Double?,
        val quantity: String?,
        val source: EntrySource,
        val logDate: String? = null,
    ) : QuickAddEvent

    data class AddFromText(
        val description: String,
        val logDate: String? = null,
    ) : QuickAddEvent

    data class AddFromPhoto(
        val imageBase64: String,
        val hint: String?,
        val logDate: String? = null,
    ) : QuickAddEvent

    data object ClearError : QuickAddEvent
}

sealed interface QuickAddEffect {
    data class Completed(val entry: MealEntry) : QuickAddEffect
    data class ShowError(val message: String) : QuickAddEffect
    data class FeatureGated(val access: FeatureAccess) : QuickAddEffect
}
