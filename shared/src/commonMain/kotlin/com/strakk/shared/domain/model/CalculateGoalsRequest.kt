package com.strakk.shared.domain.model

data class CalculateGoalsRequest(
    val weightKg: Double,
    val heightCm: Int?,
    val age: Int?,
    val biologicalSex: BiologicalSex?,
    val fitnessGoal: FitnessGoal?,
    val trainingFrequencyPerWeek: Int?,
    val trainingTypes: Set<TrainingType>,
    val trainingIntensity: TrainingIntensity?,
    val dailyActivityLevel: DailyActivityLevel?,
)
