package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.repository.NutritionRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observes the list of meal entries for a given date.
 *
 * The underlying repository maintains a cache and fetches from the network on first
 * subscription. The Flow updates whenever a mutation (add/delete/update) is committed.
 *
 * @return A [Flow] emitting the current list of [MealEntry] for [date].
 */
class ObserveMealsForDateUseCase(private val nutritionRepository: NutritionRepository) {
    operator fun invoke(date: String): Flow<List<MealEntry>> =
        nutritionRepository.observeMealsForDate(date)
}
