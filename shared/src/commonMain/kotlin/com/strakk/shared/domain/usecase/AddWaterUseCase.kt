package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching

import com.strakk.shared.domain.model.WaterEntry
import com.strakk.shared.domain.repository.NutritionRepository

/**
 * Inserts a new water entry for the given date.
 *
 * @return [Result.success] with the created entry, or [Result.failure] on error.
 */
class AddWaterUseCase(
    private val nutritionRepository: NutritionRepository,
) {
    suspend operator fun invoke(logDate: String, amount: Int): Result<WaterEntry> = runSuspendCatching {
        nutritionRepository.addWater(logDate = logDate, amount = amount)
    }
}
