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
            waterGoal: s.waterGoal?.intValue
        )
    }

    static func mealEntry(_ m: MealEntry) -> MealEntryData {
        MealEntryData(
            id: m.id,
            name: m.name,
            protein: m.protein,
            calories: m.calories,
            fat: m.fat?.doubleValue,
            carbs: m.carbs?.doubleValue,
            source: m.source,
            logDate: m.logDate,
            createdAt: m.createdAt,
            mealId: m.mealId,
            quantity: m.quantity
        )
    }

    static func meal(_ m: Meal) -> MealData {
        MealData(
            id: m.id,
            name: m.name,
            date: m.date,
            createdAt: m.createdAt.description,
            entries: m.entries.map(mealEntry)
        )
    }

    static func waterEntry(_ w: WaterEntry) -> WaterEntryData {
        WaterEntryData(id: w.id, amount: Int(w.amount))
    }
}
