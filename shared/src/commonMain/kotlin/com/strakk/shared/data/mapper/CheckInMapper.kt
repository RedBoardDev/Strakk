package com.strakk.shared.data.mapper

import com.strakk.shared.data.dto.CheckInDto
import com.strakk.shared.data.dto.CheckInPhotoDto
import com.strakk.shared.domain.model.CheckIn
import com.strakk.shared.domain.model.CheckInListItem
import com.strakk.shared.domain.model.CheckInMeasurements
import com.strakk.shared.domain.model.CheckInPhoto
import com.strakk.shared.domain.model.CheckInSeriesPoint
import com.strakk.shared.domain.model.NutritionSummary

internal fun CheckInDto.toDomain(): CheckIn = CheckIn(
    id = id,
    weekLabel = weekLabel,
    coveredDates = coveredDates,
    weight = weightKg,
    shoulders = shouldersCm,
    chest = chestCm,
    armLeft = armLeftCm,
    armRight = armRightCm,
    waist = waistCm,
    hips = hipsCm,
    thighLeft = thighLeftCm,
    thighRight = thighRightCm,
    feelingTags = feelingTags.orEmpty(),
    mentalFeeling = mentalFeeling,
    physicalFeeling = physicalFeeling,
    nutritionSummary = if (nutritionDays != null && nutritionDays > 0) {
        NutritionSummary(
            avgProtein = avgProtein ?: 0.0,
            avgCalories = avgCalories ?: 0.0,
            avgFat = avgFat ?: 0.0,
            avgCarbs = avgCarbs ?: 0.0,
            avgWater = avgWater ?: 0,
            nutritionDays = nutritionDays,
            aiSummary = aiSummary,
        )
    } else {
        null
    },
    photos = checkinPhotos?.map { it.toDomain() }.orEmpty(),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun CheckInPhotoDto.toDomain(): CheckInPhoto = CheckInPhoto(
    id = id,
    storagePath = storagePath,
    position = position,
)

internal fun CheckInDto.toListItem(): CheckInListItem = CheckInListItem(
    id = id,
    weekLabel = weekLabel,
    weight = weightKg,
    photoCount = checkinPhotos?.size ?: 0,
    hasAiSummary = aiSummary != null,
    createdAt = createdAt,
)

internal fun CheckInDto.toMeasurements(): CheckInMeasurements = CheckInMeasurements(
    weight = weightKg,
    shoulders = shouldersCm,
    chest = chestCm,
    armLeft = armLeftCm,
    armRight = armRightCm,
    waist = waistCm,
    hips = hipsCm,
    thighLeft = thighLeftCm,
    thighRight = thighRightCm,
)

internal fun CheckInDto.toSeriesPoint(): CheckInSeriesPoint = CheckInSeriesPoint(
    weekLabel = weekLabel,
    weight = weightKg,
    shoulders = shouldersCm,
    chest = chestCm,
    armLeft = armLeftCm,
    armRight = armRightCm,
    waist = waistCm,
    hips = hipsCm,
    thighLeft = thighLeftCm,
    thighRight = thighRightCm,
)
