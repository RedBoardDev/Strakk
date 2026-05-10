package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.FoodCatalogItem
import com.strakk.shared.domain.model.GroundedMealItem
import com.strakk.shared.domain.model.MacroValues
import com.strakk.shared.domain.model.toDbString

private const val PER_HUNDRED_GRAMS = 100.0
private const val USER_CHOSEN_CONFIDENCE = 1.0

/**
 * Pure local use case — replaces the catalogue match of a [GroundedMealItem]
 * with [newMatch], recomputing macros from the new item's per-100g values and
 * the existing quantity in grams.
 *
 * Sets [GroundedMealItem.aiConfidence] to 1.0 since the match is now
 * user-chosen (no longer AI-inferred).
 */
class SwapFoodMatchUseCase {
    operator fun invoke(item: GroundedMealItem, newMatch: FoodCatalogItem): GroundedMealItem {
        val grams = item.quantity.grams
        val factor = grams / PER_HUNDRED_GRAMS
        val newMacros = MacroValues(
            kcal = newMatch.calories * factor,
            protein = newMatch.protein * factor,
            fat = (newMatch.fat ?: 0.0) * factor,
            carbs = (newMatch.carbs ?: 0.0) * factor,
        )
        return item.copy(
            catalogMatch = newMatch,
            catalogMatchId = newMatch.id,
            groundingSource = newMatch.source.toDbString(),
            computedMacros = newMacros,
            isGrounded = true,
            aiConfidence = USER_CHOSEN_CONFIDENCE,
        )
    }
}
