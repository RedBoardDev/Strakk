package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.NutritionSummary
import com.strakk.shared.domain.repository.CheckInRepository

class ComputeNutritionSummaryUseCase(
    private val checkInRepository: CheckInRepository,
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

            NutritionSummary(
                avgProtein = averages.avgProtein,
                avgCalories = averages.avgCalories,
                avgFat = averages.avgFat,
                avgCarbs = averages.avgCarbs,
                avgWater = averages.avgWater,
                nutritionDays = averages.nutritionDays,
                aiSummary = null,
                dailyData = averages.dailyData,
            )
        }
}
