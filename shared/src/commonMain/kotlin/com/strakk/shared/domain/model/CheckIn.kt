package com.strakk.shared.domain.model

data class CheckIn(
    val id: String,
    val weekLabel: String,
    val coveredDates: List<String>,
    val weight: Double?,
    val shoulders: Double?,
    val chest: Double?,
    val armLeft: Double?,
    val armRight: Double?,
    val waist: Double?,
    val hips: Double?,
    val thighLeft: Double?,
    val thighRight: Double?,
    val feelingTags: List<String>,
    val mentalFeeling: String?,
    val physicalFeeling: String?,
    val nutritionSummary: NutritionSummary?,
    val photos: List<CheckInPhoto>,
    val createdAt: String,
    val updatedAt: String,
)
