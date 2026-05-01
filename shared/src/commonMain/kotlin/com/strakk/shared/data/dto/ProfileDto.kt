package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ProfileDto(
    val id: String,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("height_cm") val heightCm: Int? = null,
    @SerialName("birth_date") val birthDate: String? = null,
    @SerialName("biological_sex") val biologicalSex: String? = null,
    @SerialName("fitness_goal") val fitnessGoal: String? = null,
    @SerialName("training_frequency") val trainingFrequency: Int? = null,
    @SerialName("training_types") val trainingTypes: List<String>? = null,
    @SerialName("training_intensity") val trainingIntensity: String? = null,
    @SerialName("daily_activity_level") val dailyActivityLevel: String? = null,
    @SerialName("protein_goal") val proteinGoal: Int? = null,
    @SerialName("calorie_goal") val calorieGoal: Int? = null,
    @SerialName("fat_goal") val fatGoal: Int? = null,
    @SerialName("carb_goal") val carbGoal: Int? = null,
    @SerialName("water_goal") val waterGoal: Int? = null,
    @SerialName("onboarding_completed") val onboardingCompleted: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
