package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.GroundedMealItem
import com.strakk.shared.domain.model.Meal
import com.strakk.shared.domain.repository.MealRepository

/**
 * Persists a fully grounded meal to Supabase.
 *
 * Validates that [items] is not empty before delegating to the repository.
 */
class SaveGroundedMealUseCase(private val mealRepository: MealRepository) {
    suspend operator fun invoke(
        name: String,
        date: String,
        items: List<GroundedMealItem>,
        photoPathByPhotoIndex: Map<Int, String> = emptyMap(),
    ): Result<Meal> = runSuspendCatching {
        require(items.isNotEmpty()) { "Cannot save an empty meal" }
        mealRepository.saveMealWithGroundedEntries(name, date, items, photoPathByPhotoIndex)
    }
}
