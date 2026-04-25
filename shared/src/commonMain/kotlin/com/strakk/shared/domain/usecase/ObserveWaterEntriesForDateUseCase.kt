package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.WaterEntry
import com.strakk.shared.domain.repository.NutritionRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observes the list of water entries for a given date.
 *
 * The underlying repository maintains a cache and fetches from the network on first
 * subscription. The Flow updates whenever a mutation (add/delete) is committed.
 *
 * @return A [Flow] emitting the current list of [WaterEntry] for [date].
 */
class ObserveWaterEntriesForDateUseCase(private val nutritionRepository: NutritionRepository) {
    operator fun invoke(date: String): Flow<List<WaterEntry>> =
        nutritionRepository.observeWaterEntriesForDate(date)
}
