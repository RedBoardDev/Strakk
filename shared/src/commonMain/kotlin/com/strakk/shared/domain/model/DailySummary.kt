package com.strakk.shared.domain.model

/**
 * Aggregated nutrition and hydration totals for a single day, plus the
 * user's configured daily goals (nullable when not yet set).
 */
data class DailySummary(
    val totalProtein: Double,
    val totalCalories: Double,
    val totalFat: Double,
    val totalCarbs: Double,
    val totalWater: Int,
    val proteinGoal: Int?,
    val calorieGoal: Int?,
    val waterGoal: Int?,
)
