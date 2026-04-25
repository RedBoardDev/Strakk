import SwiftUI
import shared

// MARK: - Swift-side data types

struct CalendarDayDetailData: Equatable {
    let date: String
    let summary: DailySummaryData
    let meals: [MealEntryData]
    let waterEntries: [WaterEntryData]
}

enum CalendarState {
    case loading
    case ready(
        year: Int,
        month: Int,
        activeDays: Set<String>,
        selectedDay: String?,
        dayDetail: CalendarDayDetailData?
    )
}

// MARK: - Wrapper

@MainActor
@Observable
final class CalendarViewModelWrapper {
    private let sharedVm: CalendarViewModel

    var state: CalendarState = .loading
    var errorMessage: String?
    var openMealEntryForDate: String?

    nonisolated(unsafe) private var stateTask: Task<Void, Never>?
    nonisolated(unsafe) private var effectTask: Task<Void, Never>?

    init() {
        self.sharedVm = KoinHelper().getCalendarViewModel()

        stateTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<CalendarUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                await MainActor.run {
                    self?.state = Self.mapState(newState)
                }
            }
        }

        effectTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<CalendarEffect> = observeFlow(sharedVm.effects)
            for await effect in stream {
                await MainActor.run {
                    self?.handleEffect(effect)
                }
            }
        }
    }

    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
    }

    func onEvent(_ event: CalendarEvent) {
        sharedVm.onEvent(event: event)
    }

    // MARK: - Private

    private func handleEffect(_ effect: CalendarEffect) {
        if let openEntry = effect as? CalendarEffectOpenMealEntryForDay {
            openMealEntryForDate = openEntry.date
        } else if let showError = effect as? CalendarEffectShowError {
            errorMessage = showError.message
        }
    }

    private static func mapState(_ s: CalendarUiState) -> CalendarState {
        if s is CalendarUiStateLoading {
            return .loading
        } else if let ready = s as? CalendarUiStateReady {
            let detail: CalendarDayDetailData? = ready.dayDetail.map { d in
                CalendarDayDetailData(
                    date: d.date,
                    summary: mapSummary(d.summary),
                    meals: d.meals.map(mapMeal),
                    waterEntries: d.waterEntries.map(mapWater)
                )
            }
            let activeDaysSet: Set<String> = ready.activeDays
            return .ready(
                year: Int(ready.year),
                month: Int(ready.month),
                activeDays: activeDaysSet,
                selectedDay: ready.selectedDay,
                dayDetail: detail
            )
        }
        return .loading
    }

    private static func mapSummary(_ s: DailySummary) -> DailySummaryData {
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

    private static func mapMeal(_ m: MealEntry) -> MealEntryData {
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

    private static func mapWater(_ w: WaterEntry) -> WaterEntryData {
        WaterEntryData(id: w.id, amount: Int(w.amount))
    }
}
