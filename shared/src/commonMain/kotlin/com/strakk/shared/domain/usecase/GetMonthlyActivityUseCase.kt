package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.repository.NutritionRepository

/**
 * Returns the distinct dates within a calendar month that have at least
 * one meal or water entry logged.
 *
 * @return [Result.success] with a sorted list of ISO-8601 date strings.
 * @return [Result.failure] on any network or auth error.
 */
class GetMonthlyActivityUseCase(private val nutritionRepository: NutritionRepository) {
    suspend operator fun invoke(
        monthStart: String,
        monthEnd: String,
    ): Result<List<String>> = runSuspendCatching {
        nutritionRepository.getActiveCalendarDays(
            monthStart = monthStart,
            monthEnd = monthEnd,
        )
    }
}
