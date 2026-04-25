package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.Meal
import com.strakk.shared.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observes a single [Meal] by id.
 * Emits null when the meal is not in cache or has been deleted.
 */
class ObserveMealUseCase(private val mealRepository: MealRepository) {
    operator fun invoke(id: String): Flow<Meal?> = mealRepository.observeMeal(id)
}
