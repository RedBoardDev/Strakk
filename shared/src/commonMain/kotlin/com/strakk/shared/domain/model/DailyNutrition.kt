package com.strakk.shared.domain.model

data class DailyNutrition(
    val date: String,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val waterMl: Int,
)
