package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching

import com.strakk.shared.domain.repository.NutritionRepository

/**
 * Deletes the water entry with the given [id].
 *
 * @return [Result.success] on deletion, or [Result.failure] on error.
 */
class DeleteWaterUseCase(
    private val nutritionRepository: NutritionRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> = runSuspendCatching {
        nutritionRepository.deleteWater(id)
    }
}
