package com.strakk.shared.domain.model

/**
 * Intention for creating a fully-known [MealEntry].
 *
 * UI layers provide food facts and context; shared code owns normalization,
 * date/default handling, and final [MealEntry] construction.
 */
sealed interface MealEntryInput {
    val logDate: String?
    val mealId: String?

    data class Known(
        val name: String,
        val protein: Double,
        val calories: Double,
        val fat: Double?,
        val carbs: Double?,
        val quantity: String?,
        val source: EntrySource,
        override val logDate: String? = null,
        override val mealId: String? = null,
    ) : MealEntryInput
}
