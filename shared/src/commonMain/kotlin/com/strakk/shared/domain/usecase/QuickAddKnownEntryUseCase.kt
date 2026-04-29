package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.EntrySource
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.model.MealEntryInput
import com.strakk.shared.domain.repository.NutritionRepository

/**
 * Persists a fully-known food item as an orphan quick-add entry.
 */
class QuickAddKnownEntryUseCase(
    private val nutritionRepository: NutritionRepository,
    private val buildMealEntry: BuildMealEntryUseCase,
) {
    suspend operator fun invoke(input: MealEntryInput.Known): Result<MealEntry> =
        runSuspendCatching {
            nutritionRepository.addMeal(buildMealEntry(input))
        }

    suspend fun addKnown(
        name: String,
        protein: Double,
        calories: Double,
        fat: Double?,
        carbs: Double?,
        quantity: String?,
        source: EntrySource,
        logDate: String? = null,
    ): Result<MealEntry> = invoke(
        MealEntryInput.Known(
            name = name,
            protein = protein,
            calories = calories,
            fat = fat,
            carbs = carbs,
            quantity = quantity,
            source = source,
            logDate = logDate,
        ),
    )
}
