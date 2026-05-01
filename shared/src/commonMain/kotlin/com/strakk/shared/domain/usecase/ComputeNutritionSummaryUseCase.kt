package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.NutritionGoals
import com.strakk.shared.domain.model.NutritionSummary
import com.strakk.shared.domain.repository.CheckInRepository
import com.strakk.shared.domain.repository.ProfileRepository

class ComputeNutritionSummaryUseCase(
    private val checkInRepository: CheckInRepository,
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(
        coveredDates: List<String>,
        weightKg: Double? = null,
        feelingTags: List<String> = emptyList(),
        mentalFeeling: String = "",
        physicalFeeling: String = "",
    ): Result<NutritionSummary> =
        runSuspendCatching {
            val averages = checkInRepository.computeNutritionAverages(coveredDates)
            if (averages.nutritionDays == 0) {
                return@runSuspendCatching NutritionSummary(
                    avgProtein = 0.0,
                    avgCalories = 0.0,
                    avgFat = 0.0,
                    avgCarbs = 0.0,
                    avgWater = 0,
                    nutritionDays = 0,
                    aiSummary = null,
                )
            }

            val profile = profileRepository.getProfile()
            val goals = NutritionGoals(
                proteinGoal = profile?.proteinGoal,
                calorieGoal = profile?.calorieGoal,
                fatGoal = profile?.fatGoal,
                carbGoal = profile?.carbGoal,
                waterGoal = profile?.waterGoal,
            )

            val aiSummary = try {
                checkInRepository.generateAiSummary(
                    averages = averages,
                    goals = goals,
                    weightKg = weightKg,
                    feelingTags = feelingTags,
                    mentalFeeling = mentalFeeling,
                    physicalFeeling = physicalFeeling,
                )
            } catch (_: Exception) {
                null
            }

            NutritionSummary(
                avgProtein = averages.avgProtein,
                avgCalories = averages.avgCalories,
                avgFat = averages.avgFat,
                avgCarbs = averages.avgCarbs,
                avgWater = averages.avgWater,
                nutritionDays = averages.nutritionDays,
                aiSummary = aiSummary,
                dailyData = averages.dailyData,
            )
        }
}
