package com.strakk.shared.data.mapper

import com.strakk.shared.data.dto.CalculateGoalsRequestDto
import com.strakk.shared.data.dto.CalculateGoalsResponseDto
import com.strakk.shared.domain.model.AiGoalsResult
import com.strakk.shared.domain.model.CalculateGoalsRequest

internal fun CalculateGoalsRequest.toDto(): CalculateGoalsRequestDto = CalculateGoalsRequestDto(
    weightKg = weightKg,
    heightCm = heightCm,
    age = age,
    biologicalSex = biologicalSex?.name?.lowercase(),
    fitnessGoal = fitnessGoal?.name?.lowercase(),
    trainingFrequencyPerWeek = trainingFrequencyPerWeek,
    trainingTypes = trainingTypes.map { it.name.lowercase() },
    trainingIntensity = trainingIntensity?.name?.lowercase(),
    dailyActivityLevel = dailyActivityLevel?.name?.lowercase(),
)

internal fun CalculateGoalsResponseDto.toDomain(): AiGoalsResult = AiGoalsResult(
    proteinG = proteinG,
    caloriesKcal = caloriesKcal,
    fatG = fatG,
    carbsG = carbsG,
    waterMl = waterMl,
)
