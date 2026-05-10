import SwiftUI

// MARK: - ProgressSection — adapter from `DailySummaryData` to `MacroProgressGrid`.
//
// The macro card and 2x2 layout live in `Theme/MacroCard.swift` so Today and
// Calendar Day stay visually identical.

struct ProgressSection: View {
    let summary: DailySummaryData

    var body: some View {
        MacroProgressGrid(
            totalProtein: summary.totalProtein,
            totalCalories: summary.totalCalories,
            totalFat: summary.totalFat,
            totalCarbs: summary.totalCarbs,
            proteinGoal: summary.proteinGoal,
            calorieGoal: summary.calorieGoal,
            fatGoal: summary.fatGoal,
            carbGoal: summary.carbGoal
        )
    }
}

#Preview("Mid-day") {
    ProgressSection(summary: DailySummaryData(
        totalProtein: 142,
        totalCalories: 1840,
        totalFat: 54,
        totalCarbs: 210,
        totalWater: 1500,
        proteinGoal: 160,
        calorieGoal: 2200,
        fatGoal: 70,
        carbGoal: 240,
        waterGoal: 2500
    ))
    .padding()
    .background(Color.strakkBackground)
}

#Preview("Empty") {
    ProgressSection(summary: DailySummaryData(
        totalProtein: 0,
        totalCalories: 0,
        totalFat: 0,
        totalCarbs: 0,
        totalWater: 0,
        proteinGoal: 105,
        calorieGoal: 2200,
        fatGoal: nil,
        carbGoal: nil,
        waterGoal: 2500
    ))
    .padding()
    .background(Color.strakkBackground)
}

#Preview("Reached") {
    ProgressSection(summary: DailySummaryData(
        totalProtein: 165,
        totalCalories: 2150,
        totalFat: 68,
        totalCarbs: 248,
        totalWater: 2300,
        proteinGoal: 160,
        calorieGoal: 2200,
        fatGoal: 70,
        carbGoal: 240,
        waterGoal: 2500
    ))
    .padding()
    .background(Color.strakkBackground)
}
