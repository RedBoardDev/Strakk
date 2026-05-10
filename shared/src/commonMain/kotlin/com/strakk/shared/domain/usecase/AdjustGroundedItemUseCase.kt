package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.CookingMethod
import com.strakk.shared.domain.model.GroundedMealItem
import com.strakk.shared.domain.model.MacroValues
import com.strakk.shared.domain.model.StructuredQuantity
import com.strakk.shared.domain.model.UnitType
import kotlin.math.roundToInt

private const val PER_HUNDRED_GRAMS = 100.0

/**
 * Pure local use case — recalculates macros after the user adjusts the quantity
 * or cooking method of a [GroundedMealItem].
 *
 * No network calls. Cooking method retention factors are post-MVP and not yet
 * applied to macro recomputation; the chosen method is stored on the item for
 * persistence at save time.
 *
 * Returns the item unchanged when [GroundedMealItem.catalogMatch] is null
 * (ungrounded item — no per-100g reference available).
 */
class AdjustGroundedItemUseCase {
    operator fun invoke(
        item: GroundedMealItem,
        newGrams: Double? = null,
        newCookingMethod: CookingMethod? = null,
    ): GroundedMealItem {
        val grams = newGrams ?: item.quantity.grams

        val newMacros = if (item.catalogMatch != null) {
            val factor = grams / PER_HUNDRED_GRAMS
            MacroValues(
                kcal = item.catalogMatch.calories * factor,
                protein = item.catalogMatch.protein * factor,
                fat = (item.catalogMatch.fat ?: 0.0) * factor,
                carbs = (item.catalogMatch.carbs ?: 0.0) * factor,
            )
        } else if (newGrams != null && item.quantity.grams > 0.0) {
            val scale = newGrams / item.quantity.grams
            MacroValues(
                kcal = item.computedMacros.kcal * scale,
                protein = item.computedMacros.protein * scale,
                fat = item.computedMacros.fat * scale,
                carbs = item.computedMacros.carbs * scale,
            )
        } else {
            item.computedMacros
        }

        val newQuantity = if (newGrams != null) {
            StructuredQuantity(
                grams = grams,
                displayLabel = "${grams.roundToInt()}g",
                unitType = UnitType.Grams,
            )
        } else {
            item.quantity
        }

        return item.copy(
            quantity = newQuantity,
            computedMacros = newMacros,
            cookingMethod = newCookingMethod ?: item.cookingMethod,
        )
    }
}
