package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.repository.MealRepository
import com.strakk.shared.domain.repository.NutritionRepository

/**
 * Updates the editable fields of a committed meal entry (orphan or inside a meal).
 *
 * Writes to Supabase via [NutritionRepository] then updates the [MealRepository]
 * cache for entries that belong to a meal container.
 */
class UpdateMealEntryUseCase(
    private val nutritionRepository: NutritionRepository,
    private val mealRepository: MealRepository,
) {
    suspend operator fun invoke(entry: MealEntry): Result<MealEntry> = runSuspendCatching {
        val updated = nutritionRepository.updateMealEntry(entry)
        if (entry.mealId != null) {
            mealRepository.updateEntryInCache(updated)
        }
        updated
    }
}
