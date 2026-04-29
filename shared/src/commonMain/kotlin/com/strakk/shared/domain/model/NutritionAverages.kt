package com.strakk.shared.domain.model

data class NutritionAverages(
    val avgProtein: Double,
    val avgCalories: Double,
    val avgFat: Double,
    val avgCarbs: Double,
    val avgWater: Int,
    val nutritionDays: Int,
    val topFoods: List<String> = emptyList(),
    val proteinPerDay: List<Int> = emptyList(),
    val daysWithWater: Int = 0,
    val dailyData: List<DailyNutrition> = emptyList(),
)
