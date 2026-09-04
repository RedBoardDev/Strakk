package com.strakk.shared.domain.model

import kotlinx.datetime.Instant

/**
 * A user-favorited meal template. Captures the meal's name and items at the
 * moment it was hearted; survives deletion of the originating [Meal].
 *
 * [sourceMealId] is informational — it lets the UI display a filled heart on
 * the original container if it still exists, but the favorite itself is
 * fully denormalized via [items].
 */
data class FavoriteMeal(
    val id: String,
    val name: String,
    val items: List<MealTemplateItem>,
    val sourceMealId: String? = null,
    val createdAt: Instant,
) {
    val totalCalories: Double get() = items.sumOf { it.calories }
    val totalProtein: Double get() = items.sumOf { it.protein }
}
