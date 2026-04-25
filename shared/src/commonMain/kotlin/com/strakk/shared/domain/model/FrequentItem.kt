package com.strakk.shared.domain.model

/**
 * A frequently-consumed food item derived from the user's [MealEntry] history.
 *
 * Items are deduplicated by normalized name (lowercase, no accents, trimmed).
 * Nutritional values reflect the most recent occurrence's values.
 * Ranked by a `recency × frequency` score.
 */
data class FrequentItem(
    /** Normalized name used for deduplication. */
    val normalizedName: String,
    /** Display name (most recent occurrence). */
    val name: String?,
    val protein: Double,
    val calories: Double,
    val fat: Double?,
    val carbs: Double?,
    val quantity: String?,
    val occurrences: Int,
)
