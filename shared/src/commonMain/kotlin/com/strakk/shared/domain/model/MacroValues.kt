package com.strakk.shared.domain.model

/**
 * Computed nutritional values for a grounded meal item.
 *
 * All values are in the standard units used throughout the app:
 * kcal for [kcal], grams for [protein], [fat], [carbs].
 */
data class MacroValues(
    val kcal: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
)
