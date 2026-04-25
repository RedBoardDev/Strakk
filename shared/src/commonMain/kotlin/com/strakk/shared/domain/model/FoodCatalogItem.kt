package com.strakk.shared.domain.model

/**
 * A food item from the CIQUAL catalogue stored in the `food_catalog` table.
 *
 * Nutritional values are per 100g.
 */
data class FoodCatalogItem(
    val id: Long,
    val name: String,
    val protein: Double,
    val calories: Double,
    val fat: Double?,
    val carbs: Double?,
    /** Default portion in grams for display purposes (default 100g). */
    val defaultPortionGrams: Double,
)
