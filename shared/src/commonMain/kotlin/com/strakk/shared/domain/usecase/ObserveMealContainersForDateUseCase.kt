package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.Meal
import com.strakk.shared.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observes the Meal containers (Processed repas, with their entries embedded)
 * for a given [date].
 *
 * Separate from [ObserveMealsForDateUseCase] which observes orphan
 * [com.strakk.shared.domain.model.MealEntry] rows (quick-adds).  The Today
 * timeline combines both streams client-side.
 */
class ObserveMealContainersForDateUseCase(
    private val mealRepository: MealRepository,
) {
    operator fun invoke(date: String): Flow<List<Meal>> =
        mealRepository.observeMealsForDate(date)
}
