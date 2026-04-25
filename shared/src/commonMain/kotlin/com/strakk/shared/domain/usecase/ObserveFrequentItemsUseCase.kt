package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.FrequentItem
import com.strakk.shared.domain.repository.NutritionRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observes the top [limit] most-frequent items logged by the current user,
 * deduplicated and ranked by recency × frequency.
 */
class ObserveFrequentItemsUseCase(
    private val nutritionRepository: NutritionRepository,
) {
    operator fun invoke(limit: Int = DEFAULT_LIMIT): Flow<List<FrequentItem>> =
        nutritionRepository.observeFrequentItems(limit)

    companion object {
        private const val DEFAULT_LIMIT = 20
    }
}
