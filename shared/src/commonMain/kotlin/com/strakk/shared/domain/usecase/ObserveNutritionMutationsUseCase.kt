package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.repository.NutritionRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observes nutrition mutation signals (add/delete/update meal or water).
 *
 * Emits [Unit] whenever a mutation completes successfully. Consumers such as
 * [CalendarViewModel] use this to trigger dependent refreshes (e.g. active days).
 *
 * @return A [Flow] emitting [Unit] on each completed nutrition mutation.
 */
class ObserveNutritionMutationsUseCase(private val nutritionRepository: NutritionRepository) {
    operator fun invoke(): Flow<Unit> = nutritionRepository.observeNutritionMutations()
}
