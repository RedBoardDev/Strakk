package com.strakk.shared.data.mapper

import com.strakk.shared.data.dto.ProfileDto
import com.strakk.shared.domain.model.BiologicalSex
import com.strakk.shared.domain.model.DailyActivityLevel
import com.strakk.shared.domain.model.FitnessGoal
import com.strakk.shared.domain.model.TrainingIntensity
import com.strakk.shared.domain.model.TrainingType
import com.strakk.shared.domain.model.UserProfile
import kotlinx.datetime.LocalDate

internal fun ProfileDto.toDomain(): UserProfile = UserProfile(
    id = id,
    weightKg = weightKg,
    heightCm = heightCm,
    birthDate = birthDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    biologicalSex = biologicalSex?.uppercase()?.let { runCatching { BiologicalSex.valueOf(it) }.getOrNull() },
    fitnessGoal = fitnessGoal?.uppercase()?.let { runCatching { FitnessGoal.valueOf(it) }.getOrNull() },
    trainingFrequency = trainingFrequency,
    trainingTypes = trainingTypes
        ?.mapNotNull { it.uppercase().let { s -> runCatching { TrainingType.valueOf(s) }.getOrNull() } }
        ?.toSet()
        ?: emptySet(),
    trainingIntensity = trainingIntensity?.uppercase()?.let { runCatching { TrainingIntensity.valueOf(it) }.getOrNull() },
    dailyActivityLevel = dailyActivityLevel?.uppercase()?.let { runCatching { DailyActivityLevel.valueOf(it) }.getOrNull() },
    proteinGoal = proteinGoal,
    calorieGoal = calorieGoal,
    fatGoal = fatGoal,
    carbGoal = carbGoal,
    waterGoal = waterGoal,
    onboardingCompleted = onboardingCompleted,
)
