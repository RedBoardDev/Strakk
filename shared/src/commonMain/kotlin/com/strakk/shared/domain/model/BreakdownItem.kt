package com.strakk.shared.domain.model

/**
 * A sub-item within a [MealEntry] produced by AI photo analysis.
 *
 * Represents an individual food component identified in the photo
 * (e.g. "Chicken breast", "Rice", "Broccoli" from a single meal photo).
 * Stored as `breakdown_json` JSONB on the `meal_entries` table.
 */
data class BreakdownItem(
    val name: String,
    val protein: Double,
    val calories: Double,
    val fat: Double?,
    val carbs: Double?,
    /** Textual quantity description, e.g. "120g", "1 portion". */
    val quantity: String?,
)
