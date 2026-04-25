package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.DailySummary
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.model.UserProfile
import com.strakk.shared.domain.model.WaterEntry
import com.strakk.shared.domain.repository.NutritionRepository
import com.strakk.shared.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Observes the aggregated [DailySummary] for a given date.
 *
 * Combines the reactive Flows for meals, water entries, and user profile,
 * recomputing the summary whenever any upstream emits a new value.
 *
 * @return A [Flow] emitting the current [DailySummary] for [date].
 */
class ObserveDailySummaryUseCase(
    private val nutritionRepository: NutritionRepository,
    private val profileRepository: ProfileRepository,
) {
    operator fun invoke(date: String): Flow<DailySummary> =
        combine(
            nutritionRepository.observeMealsForDate(date),
            nutritionRepository.observeWaterEntriesForDate(date),
            profileRepository.observeProfile(),
        ) { meals: List<MealEntry>, waterEntries: List<WaterEntry>, profile: UserProfile? ->
            DailySummary(
                totalProtein = meals.sumOf { it.protein },
                totalCalories = meals.sumOf { it.calories },
                totalFat = meals.sumOf { it.fat ?: 0.0 },
                totalCarbs = meals.sumOf { it.carbs ?: 0.0 },
                totalWater = waterEntries.sumOf { it.amount },
                proteinGoal = profile?.proteinGoal,
                calorieGoal = profile?.calorieGoal,
                waterGoal = profile?.waterGoal,
            )
        }
}
