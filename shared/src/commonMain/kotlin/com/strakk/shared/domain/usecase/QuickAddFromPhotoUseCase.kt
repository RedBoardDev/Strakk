package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.repository.MealRepository
import com.strakk.shared.domain.repository.NutritionRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Quick-add flow for a photo: analyze via `analyze-meal-single`, then persist
 * the resulting [MealEntry] as an orphan entry for today.
 */
class QuickAddFromPhotoUseCase(
    private val mealRepository: MealRepository,
    private val nutritionRepository: NutritionRepository,
) {
    suspend operator fun invoke(imageBase64: String, hint: String?, logDate: String? = null): Result<MealEntry> =
        runSuspendCatching {
            val dateToUse = logDate ?: Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
                .toString()
            val analyzed = mealRepository.analyzePhotoForQuickAdd(imageBase64, hint, dateToUse)
            nutritionRepository.addMeal(analyzed)
        }
}
