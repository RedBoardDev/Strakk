package com.strakk.shared.presentation.meal

import com.strakk.shared.domain.model.CookingMethod
import com.strakk.shared.domain.model.FeatureAccess
import com.strakk.shared.domain.model.FoodCatalogItem
import com.strakk.shared.domain.model.GroundedMealItem
import com.strakk.shared.domain.model.Meal

// =============================================================================
// UiState
// =============================================================================

/**
 * Linear flow state for the photo-to-meal analysis screen.
 *
 * The flow progresses: [Capturing] → [Identifying] → [Grounding] → [Reviewing].
 * Any error or cancellation resets to [Capturing].
 */
sealed interface PhotoMealUiState {
    /** Photo has just been taken — not yet uploaded or analysed. */
    data object Capturing : PhotoMealUiState

    /** Upload in progress, or Claude identification in progress. */
    data class Identifying(val hint: String? = null) : PhotoMealUiState

    /**
     * Grounding (embedding + vector search) in progress.
     * [identifiedNames] are shown as a skeleton while results load.
     */
    data class Grounding(val identifiedNames: List<String>) : PhotoMealUiState

    /**
     * Interactive review — the user can edit items before saving.
     *
     * [isSaving] prevents double-taps on the save action.
     */
    data class Reviewing(
        val items: List<GroundedMealItem>,
        val mealName: String,
        val isSaving: Boolean = false,
    ) : PhotoMealUiState {
        /** Totals computed only from visible (non-hidden) items. */
        val totals: PhotoMealTotals
            get() {
                val visible = items.filter { !it.isHidden }
                return PhotoMealTotals(
                    kcal = visible.sumOf { it.computedMacros.kcal },
                    protein = visible.sumOf { it.computedMacros.protein },
                    fat = visible.sumOf { it.computedMacros.fat },
                    carbs = visible.sumOf { it.computedMacros.carbs },
                )
            }
        val groundedCount: Int get() = items.count { it.isGrounded && !it.isHidden }
        val hiddenCount: Int get() = items.count { it.isHidden }
        val hasVisibleItems: Boolean get() = items.any { !it.isHidden }
    }
}

data class PhotoMealTotals(
    val kcal: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
)

// =============================================================================
// Events (UI → ViewModel)
// =============================================================================

sealed interface PhotoMealEvent {
    /**
     * Triggered from PhotoHintView when the user confirms a photo + optional hint.
     *
     * @param imageBase64 JPEG encoded as base64, no `data:` prefix (≤ 300 KB).
     * @param hint Optional user-provided description to improve AI accuracy.
     * @param date Meal date in "yyyy-MM-dd" format.
     * @param mealName Display name for the meal.
     */
    data class StartAnalysis(
        val imageBase64: String,
        val hint: String?,
        val date: String,
        val mealName: String,
    ) : PhotoMealEvent

    /**
     * Text-only analysis — no photo, just a textual description.
     *
     * @param description User-provided textual description of the meal.
     * @param date Meal date in "yyyy-MM-dd" format.
     * @param mealName Display name for the meal.
     */
    data class StartTextAnalysis(
        val description: String,
        val date: String,
        val mealName: String,
    ) : PhotoMealEvent

    /** Review: user adjusts the quantity of an item. */
    data class AdjustQuantity(
        val itemIndex: Int,
        val newGrams: Double,
    ) : PhotoMealEvent

    /** Review: user changes the cooking method of an item. */
    data class ChangeCookingMethod(
        val itemIndex: Int,
        val method: CookingMethod?,
    ) : PhotoMealEvent

    /** Review: user swaps the matched food for an item with a catalogue entry. */
    data class SwapFood(
        val itemIndex: Int,
        val newMatch: FoodCatalogItem,
    ) : PhotoMealEvent

    /** Review: user removes an item. Removing the last item resets to [PhotoMealUiState.Capturing]. */
    data class RemoveItem(val itemIndex: Int) : PhotoMealEvent

    /** Review: user soft-hides a scan item (item stays grayed out, not removed). */
    data class HideItem(val itemIndex: Int) : PhotoMealEvent

    /** Review: user restores a previously hidden item. */
    data class RestoreItem(val itemIndex: Int) : PhotoMealEvent

    /** Review: user adds an item manually from the review screen. */
    data class AddManualItem(
        val name: String,
        val kcal: Double,
        val protein: Double,
        val fat: Double,
        val carbs: Double,
        val grams: Double,
    ) : PhotoMealEvent

    /** Review: user adds an item from catalogue search from the review screen. */
    data class AddSearchItem(
        val match: FoodCatalogItem,
        val grams: Double,
    ) : PhotoMealEvent

    /** Review: user edits the macros and quantity of an item. */
    data class EditItemMacros(
        val itemIndex: Int,
        val name: String,
        val kcal: Double,
        val protein: Double,
        val fat: Double,
        val carbs: Double,
        val grams: Double,
    ) : PhotoMealEvent

    /** Review: user confirms and saves the meal. */
    data object Save : PhotoMealEvent

    /** Cancellation at any stage — resets to [PhotoMealUiState.Capturing]. */
    data object Cancel : PhotoMealEvent
}

// =============================================================================
// Effects (ViewModel → UI, one-shot)
// =============================================================================

sealed interface PhotoMealEffect {
    data class MealSaved(val meal: Meal) : PhotoMealEffect
    data object Cancelled : PhotoMealEffect
    data class ShowError(val message: String) : PhotoMealEffect
    data class FeatureGated(val access: FeatureAccess) : PhotoMealEffect
}
