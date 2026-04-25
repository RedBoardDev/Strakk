package com.strakk.shared.domain.model

/**
 * A collection of meal entries grouped by calendar date.
 *
 * @property date ISO-8601 date string ("yyyy-MM-dd").
 * @property meals All meals logged on that date, ordered by creation time.
 */
data class DailyMealLog(
    val date: String,
    val meals: List<MealEntry>,
)
