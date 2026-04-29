package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.DailySummary
import com.strakk.shared.domain.model.Meal
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.model.UserProfile
import com.strakk.shared.domain.model.WaterEntry
import com.strakk.shared.domain.repository.MealRepository
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
    private val mealRepository: MealRepository,
    private val profileRepository: ProfileRepository,
) {
    operator fun invoke(date: String): Flow<DailySummary> =
        combine(
            nutritionRepository.observeMealsForDate(date),
            mealRepository.observeMealsForDate(date),
            nutritionRepository.observeWaterEntriesForDate(date),
            profileRepository.observeProfile(),
        ) { orphanEntries: List<MealEntry>, meals: List<Meal>, waterEntries: List<WaterEntry>, profile: UserProfile? ->
            val entries = orphanEntries + meals.flatMap { it.entries }
            DailySummary(
                totalProtein = entries.sumOf { it.protein },
                totalCalories = entries.sumOf { it.calories },
                totalFat = entries.sumOf { it.fat ?: 0.0 },
                totalCarbs = entries.sumOf { it.carbs ?: 0.0 },
                totalWater = waterEntries.sumOf { it.amount },
                proteinGoal = profile?.proteinGoal,
                calorieGoal = profile?.calorieGoal,
                waterGoal = profile?.waterGoal,
            )
        }
}
