package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.DomainError
import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.repository.MealRepository
import com.strakk.shared.domain.repository.NutritionRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val MIN_LENGTH = 3
private const val MAX_LENGTH = 500

/**
 * Quick-add flow for a text description: analyze via `analyze-meal-single`,
 * then persist the resulting [MealEntry] as an orphan entry for today.
 */
class QuickAddFromTextUseCase(
    private val mealRepository: MealRepository,
    private val nutritionRepository: NutritionRepository,
) {
    suspend operator fun invoke(description: String, logDate: String? = null): Result<MealEntry> = runSuspendCatching {
        val trimmed = description.trim()
        if (trimmed.length !in MIN_LENGTH..MAX_LENGTH) {
            throw DomainError.ValidationError(
                "La description doit contenir entre $MIN_LENGTH et $MAX_LENGTH caractères.",
            )
        }
        val dateToUse = logDate ?: Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()
        val analyzed = mealRepository.analyzeTextForQuickAdd(trimmed, dateToUse)
        nutritionRepository.addMeal(analyzed)
    }
}
