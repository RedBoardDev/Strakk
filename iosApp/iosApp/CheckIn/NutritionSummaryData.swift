import Foundation

struct NutritionSummaryData: Equatable {
    let avgProtein: Double
    let avgCalories: Double
    let avgFat: Double
    let avgCarbs: Double
    let avgWater: Int
    let nutritionDays: Int
    let aiSummary: String?
    let dailyData: [DailyNutritionData]
}
