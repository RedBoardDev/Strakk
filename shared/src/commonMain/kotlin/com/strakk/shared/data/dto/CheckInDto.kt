package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CheckInDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("week_label") val weekLabel: String,
    @SerialName("covered_dates") val coveredDates: List<String>,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("shoulders_cm") val shouldersCm: Double? = null,
    @SerialName("chest_cm") val chestCm: Double? = null,
    @SerialName("arm_left_cm") val armLeftCm: Double? = null,
    @SerialName("arm_right_cm") val armRightCm: Double? = null,
    @SerialName("waist_cm") val waistCm: Double? = null,
    @SerialName("hips_cm") val hipsCm: Double? = null,
    @SerialName("thigh_left_cm") val thighLeftCm: Double? = null,
    @SerialName("thigh_right_cm") val thighRightCm: Double? = null,
    @SerialName("feeling_tags") val feelingTags: List<String>? = null,
    @SerialName("mental_feeling") val mentalFeeling: String? = null,
    @SerialName("physical_feeling") val physicalFeeling: String? = null,
    @SerialName("avg_protein") val avgProtein: Double? = null,
    @SerialName("avg_calories") val avgCalories: Double? = null,
    @SerialName("avg_fat") val avgFat: Double? = null,
    @SerialName("avg_carbs") val avgCarbs: Double? = null,
    @SerialName("avg_water") val avgWater: Int? = null,
    @SerialName("nutrition_days") val nutritionDays: Int? = null,
    @SerialName("ai_summary") val aiSummary: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("checkin_photos") val checkinPhotos: List<CheckInPhotoDto>? = null,
)
