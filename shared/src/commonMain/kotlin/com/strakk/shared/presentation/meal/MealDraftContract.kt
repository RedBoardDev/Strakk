package com.strakk.shared.presentation.meal

import com.strakk.shared.domain.model.ActiveMealDraft
import com.strakk.shared.domain.model.DraftItem
import com.strakk.shared.domain.model.Meal

// =============================================================================
// UiState
// =============================================================================

/**
 * State of the active meal draft screen.
 *
 * @property Loading First load or no-draft in progress.
 * @property Empty No draft active — the UI should offer to start one.
 * @property Editing A draft is active with [draft] items.
 */
sealed interface MealDraftUiState {
    data object Loading : MealDraftUiState
    data object Empty : MealDraftUiState
    data class Editing(
        val draft: ActiveMealDraft,
        val isProcessing: Boolean = false,
    ) : MealDraftUiState {
        val resolvedCount: Int get() = draft.items.count { it is DraftItem.Resolved }
        val pendingCount: Int get() = draft.items.size - resolvedCount

        /** Macro totals from the Resolved items only (pending items are excluded). */
        val totals: MealDraftTotals
            get() {
                var protein = 0.0
                var calories = 0.0
                var fat = 0.0
                var carbs = 0.0
                draft.items
                    .filterIsInstance<DraftItem.Resolved>()
                    .forEach { resolved ->
                        protein += resolved.entry.protein
                        calories += resolved.entry.calories
                        fat += resolved.entry.fat ?: 0.0
                        carbs += resolved.entry.carbs ?: 0.0
                    }
                return MealDraftTotals(protein, calories, fat, carbs)
            }
    }
}

data class MealDraftTotals(
    val protein: Double,
    val calories: Double,
    val fat: Double,
    val carbs: Double,
)

// =============================================================================
// Events (UI → ViewModel)
// =============================================================================

sealed interface MealDraftEvent {
    data class StartDraft(val initialName: String? = null, val date: String? = null) : MealDraftEvent
    data class Rename(val name: String) : MealDraftEvent
    data class RemoveItem(val itemId: String) : MealDraftEvent
    data class AddResolvedItem(val item: DraftItem.Resolved) : MealDraftEvent
    data class AddPendingPhoto(val imageBase64: String, val hint: String?) : MealDraftEvent
    data class AddPendingText(val description: String) : MealDraftEvent
    data object Discard : MealDraftEvent
    /** Runs AI extraction on pending items and opens the Review screen on success. */
    data object Process : MealDraftEvent
    /** Commits the processed draft to Supabase and clears the local draft. */
    data object Commit : MealDraftEvent
}

// =============================================================================
// Effects (ViewModel → UI, one-shot)
// =============================================================================

sealed interface MealDraftEffect {
    data object NavigateToReview : MealDraftEffect
    data class Committed(val meal: Meal) : MealDraftEffect
    data object Discarded : MealDraftEffect
    data class ShowError(val message: String) : MealDraftEffect
}
