package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.repository.MealRepository

/**
 * Deletes a meal container along with its entries (DB CASCADE) and their
 * associated Storage photos.
 *
 * This is distinct from [DeleteMealUseCase], which removes a single orphan
 * [com.strakk.shared.domain.model.MealEntry] (quick-add).
 */
class DeleteMealContainerUseCase(private val mealRepository: MealRepository) {
    suspend operator fun invoke(mealId: String): Result<Unit> = runSuspendCatching {
        mealRepository.deleteMeal(mealId)
    }
}
