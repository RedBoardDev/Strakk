package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.repository.NutritionRepository

/**
 * Persists a pre-chiffred [MealEntry] as an orphan quick-add.
 *
 * Use this when the entry has already been fully assembled (e.g. selected from
 * the [FoodCatalogItem] search or from a barcode lookup, which bypass the AI).
 */
class QuickAddEntryUseCase(
    private val nutritionRepository: NutritionRepository,
) {
    suspend operator fun invoke(entry: MealEntry): Result<MealEntry> = runSuspendCatching {
        nutritionRepository.addMeal(entry)
    }
}
