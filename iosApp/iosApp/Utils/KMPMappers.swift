import shared

// MARK: - KMP → Swift data-type mappers

enum KMPMappers {
    static func dailySummary(_ s: DailySummary) -> DailySummaryData {
        DailySummaryData(
            totalProtein: s.totalProtein,
            totalCalories: s.totalCalories,
            totalFat: s.totalFat,
            totalCarbs: s.totalCarbs,
            totalWater: Int(s.totalWater),
            proteinGoal: s.proteinGoal?.intValue,
            calorieGoal: s.calorieGoal?.intValue,
            fatGoal: s.fatGoal?.intValue,
            carbGoal: s.carbGoal?.intValue,
            waterGoal: s.waterGoal?.intValue
        )
    }

    static func mealEntry(_ entry: MealEntry) -> MealEntryData {
        MealEntryData(
            id: entry.id,
            name: entry.name,
            protein: entry.protein,
            calories: entry.calories,
            fat: entry.fat?.doubleValue,
            carbs: entry.carbs?.doubleValue,
            source: entry.source,
            logDate: entry.logDate,
            createdAt: entry.createdAt,
            mealId: entry.mealId,
            quantity: entry.quantity
        )
    }

    static func meal(_ meal: Meal) -> MealData {
        MealData(
            id: meal.id,
            name: meal.name,
            date: meal.date,
            createdAt: meal.createdAt.description,
            entries: meal.entries.map(mealEntry)
        )
    }

    static func waterEntry(_ w: WaterEntry) -> WaterEntryData {
        WaterEntryData(id: w.id, amount: Int(w.amount))
    }
}
