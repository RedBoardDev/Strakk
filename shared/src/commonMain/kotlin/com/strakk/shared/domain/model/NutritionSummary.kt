package com.strakk.shared.domain.model

data class NutritionSummary(
    val avgProtein: Double,
    val avgCalories: Double,
    val avgFat: Double,
    val avgCarbs: Double,
    val avgWater: Int,
    val nutritionDays: Int,
    val aiSummary: String?,
    val dailyData: List<DailyNutrition> = emptyList(),
)
