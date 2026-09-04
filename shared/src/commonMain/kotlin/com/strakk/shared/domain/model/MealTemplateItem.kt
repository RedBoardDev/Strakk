package com.strakk.shared.domain.model

/**
 * A single food item inside a meal template (used by [FavoriteMeal] and
 * [RecentMeal]). Denormalized — no FK to [MealEntry].
 */
data class MealTemplateItem(
    val name: String,
    val protein: Double,
    val calories: Double,
    val fat: Double?,
    val carbs: Double?,
    val quantity: String?,
)
