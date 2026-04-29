import SwiftUI
import shared

// MARK: - Swift-side data types

struct CalendarDayDetailData: Equatable {
    let date: String
    let summary: DailySummaryData
    let meals: [MealEntryData]
    let mealContainers: [MealData]
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

    @ObservationIgnored private var stateTask: Task<Void, Never>?
    @ObservationIgnored private var effectTask: Task<Void, Never>?

    init() {
        self.sharedVm = KoinHelper().getCalendarViewModel()

        stateTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<CalendarUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                self?.state = Self.mapState(newState)
            }
        }

        effectTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<CalendarEffect> = observeFlow(sharedVm.effects)
            for await effect in stream {
                self?.handleEffect(effect)
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
                    summary: mapToDailySummaryData(d.summary),
                    meals: d.meals.map(mapToMealEntryData),
                    mealContainers: d.mealContainers.map(mapToMealData),
                    waterEntries: d.waterEntries.map(mapToWaterEntryData)
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

}
