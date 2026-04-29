import SwiftUI
import shared

// MARK: - Swift-side data types

struct DailySummaryData: Equatable {
    let totalProtein: Double
    let totalCalories: Double
    let totalFat: Double
    let totalCarbs: Double
    let totalWater: Int
    let proteinGoal: Int?
    let calorieGoal: Int?
    let waterGoal: Int?
}

struct MealEntryData: Identifiable, Equatable {
    let id: String
    let name: String?
    let protein: Double
    let calories: Double
    let fat: Double?
    let carbs: Double?
    let source: EntrySource
    let logDate: String
    let createdAt: String
    let mealId: String?
    let quantity: String?
}

struct WaterEntryData: Identifiable, Equatable {
    let id: String
    let amount: Int
}

struct MealData: Identifiable, Equatable {
    let id: String
    let name: String
    let date: String
    let createdAt: String
    let entries: [MealEntryData]

    var totalCalories: Double { entries.reduce(0) { $0 + $1.calories } }
    var totalProtein: Double { entries.reduce(0) { $0 + $1.protein } }
}

enum TimelineItemData: Identifiable {
    case mealContainer(MealData)
    case orphanEntry(MealEntryData)

    var id: String {
        switch self {
        case .mealContainer(let m): return "meal-\(m.id)"
        case .orphanEntry(let e): return "entry-\(e.id)"
        }
    }

    var createdAt: String {
        switch self {
        case .mealContainer(let m): return m.createdAt
        case .orphanEntry(let e): return e.createdAt
        }
    }
}

struct ActiveDraftData: Equatable {
    let id: String
    let name: String
    let resolvedCount: Int
    let pendingCount: Int
    let totalCalories: Double
    let totalProtein: Double
}

enum TodayState {
    case loading
    case ready(
        dateLabel: String,
        summary: DailySummaryData,
        timeline: [TimelineItemData],
        waterEntries: [WaterEntryData],
        activeDraft: ActiveDraftData?
    )
}

// MARK: - Wrapper

@MainActor
@Observable
final class TodayViewModelWrapper {
    private let sharedVm: TodayViewModel

    var state: TodayState = .loading
    var errorMessage: String?

    @ObservationIgnored private var stateTask: Task<Void, Never>?
    @ObservationIgnored private var effectTask: Task<Void, Never>?

    init() {
        self.sharedVm = KoinHelper().getTodayViewModel()
        self.state = Self.mapState(sharedVm.uiState.value as? TodayUiState)

        stateTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<TodayUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                self?.state = Self.mapState(newState)
            }
        }

        effectTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<TodayEffect> = observeFlow(sharedVm.effects)
            for await effect in stream {
                self?.handleEffect(effect)
            }
        }
    }

    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
    }

    func onEvent(_ event: TodayEvent) {
        sharedVm.onEvent(event: event)
    }

    // MARK: - Effect handling

    private func handleEffect(_ effect: TodayEffect) {
        if let showError = effect as? TodayEffectShowError {
            errorMessage = showError.message
        }
    }

    // MARK: - Mapping

    private static func mapState(_ kmpState: TodayUiState?) -> TodayState {
        guard let kmpState else { return .loading }

        if kmpState is TodayUiStateLoading {
            return .loading
        } else if let ready = kmpState as? TodayUiStateReady {
            let timeline = ready.timeline.compactMap { item -> TimelineItemData? in
                if let container = item as? TimelineItemMealContainer {
                    return .mealContainer(mapToMealData(container.meal))
                } else if let orphan = item as? TimelineItemOrphanEntry {
                    return .orphanEntry(mapToMealEntryData(orphan.entry))
                }
                return nil
            }

            let activeDraft = ready.activeDraft.map { draft -> ActiveDraftData in
                let resolved = draft.items.filter { $0 is DraftItemResolved }
                let pending = draft.items.filter { !($0 is DraftItemResolved) }
                let totalCal = resolved.compactMap { $0 as? DraftItemResolved }
                    .reduce(0.0) { $0 + $1.entry.calories }
                let totalProt = resolved.compactMap { $0 as? DraftItemResolved }
                    .reduce(0.0) { $0 + $1.entry.protein }
                return ActiveDraftData(
                    id: draft.id,
                    name: draft.name,
                    resolvedCount: resolved.count,
                    pendingCount: pending.count,
                    totalCalories: totalCal,
                    totalProtein: totalProt
                )
            }

            return .ready(
                dateLabel: ready.dateLabel,
                summary: mapToDailySummaryData(ready.summary),
                timeline: timeline,
                waterEntries: ready.waterEntries.map(mapToWaterEntryData),
                activeDraft: activeDraft
            )
        }
        return .loading
    }
}
