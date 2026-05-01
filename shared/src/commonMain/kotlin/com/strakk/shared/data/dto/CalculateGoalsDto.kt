package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CalculateGoalsRequestDto(
    @SerialName("weight_kg") val weightKg: Double,
    @SerialName("height_cm") val heightCm: Int? = null,
    val age: Int? = null,
    @SerialName("biological_sex") val biologicalSex: String? = null,
    @SerialName("fitness_goal") val fitnessGoal: String? = null,
    @SerialName("training_frequency_per_week") val trainingFrequencyPerWeek: Int? = null,
    @SerialName("training_types") val trainingTypes: List<String> = emptyList(),
    @SerialName("training_intensity") val trainingIntensity: String? = null,
    @SerialName("daily_activity_level") val dailyActivityLevel: String? = null,
)

@Serializable
internal data class CalculateGoalsResponseDto(
    @SerialName("protein_g") val proteinG: Int,
    @SerialName("calories_kcal") val caloriesKcal: Int,
    @SerialName("fat_g") val fatG: Int,
    @SerialName("carbs_g") val carbsG: Int,
    @SerialName("water_ml") val waterMl: Int,
    val reasoning: String? = null,
)
