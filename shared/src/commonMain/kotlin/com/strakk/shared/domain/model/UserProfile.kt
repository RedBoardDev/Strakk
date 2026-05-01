package com.strakk.shared.domain.model

import kotlinx.datetime.LocalDate

data class UserProfile(
    val id: String,
    val weightKg: Double?,
    val heightCm: Int?,
    val birthDate: LocalDate?,
    val biologicalSex: BiologicalSex?,
    val fitnessGoal: FitnessGoal?,
    val trainingFrequency: Int?,
    val trainingTypes: Set<TrainingType>,
    val trainingIntensity: TrainingIntensity?,
    val dailyActivityLevel: DailyActivityLevel?,
    val proteinGoal: Int?,
    val calorieGoal: Int?,
    val fatGoal: Int?,
    val carbGoal: Int?,
    val waterGoal: Int?,
    val onboardingCompleted: Boolean,
)
