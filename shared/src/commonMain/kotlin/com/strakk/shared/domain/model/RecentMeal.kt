package com.strakk.shared.domain.model

import kotlinx.datetime.Instant

/**
 * A previously-logged meal, surfaced as a re-addable template in the search
 * drawer's "Recent meals" section. Distinct meals are kept once per
 * normalized name (latest occurrence wins) within the lookback window.
 */
data class RecentMeal(
    /** The originating meal id (kept so the UI can mark the current container favorited). */
    val mealId: String,
    val name: String,
    val items: List<MealTemplateItem>,
    val createdAt: Instant,
) {
    val totalCalories: Double get() = items.sumOf { it.calories }
    val totalProtein: Double get() = items.sumOf { it.protein }
}
