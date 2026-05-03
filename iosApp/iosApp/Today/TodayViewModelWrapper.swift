import SwiftUI
import shared

// MARK: - Trial banner state

enum TrialBannerData: Equatable {
    case expiringIn(daysRemaining: Int)
}

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
        case .mealContainer(let meal): return "meal-\(meal.id)"
        case .orphanEntry(let entry): return "entry-\(entry.id)"
        }
    }

    var createdAt: String {
        switch self {
        case .mealContainer(let meal): return meal.createdAt
        case .orphanEntry(let entry): return entry.createdAt
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
        activeDraft: ActiveDraftData?,
        trialBanner: TrialBannerData?
    )
}

// MARK: - Wrapper

@MainActor
@Observable
final class TodayViewModelWrapper {
    private let sharedVm: TodayViewModel

    var state: TodayState = .loading
    var errorMessage: String?
    var showPaywall: Bool = false

    @ObservationIgnored private var stateTask: Task<Void, Never>?
    @ObservationIgnored private var effectTask: Task<Void, Never>?

    init() {
        self.sharedVm = KoinBridge.shared.getTodayViewModel()
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
        } else if effect is TodayEffectNavigateToPaywall {
            showPaywall = true
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
                    return .mealContainer(KMPMappers.meal(container.meal))
                } else if let orphan = item as? TimelineItemOrphanEntry {
                    return .orphanEntry(KMPMappers.mealEntry(orphan.entry))
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

            let trialBanner: TrialBannerData? = {
                guard let banner = ready.trialBanner else { return nil }
                if let expiring = banner as? TrialBannerStateExpiringIn {
                    return .expiringIn(daysRemaining: Int(expiring.daysRemaining))
                }
                return nil
            }()

            return .ready(
                dateLabel: ready.dateLabel,
                summary: KMPMappers.dailySummary(ready.summary),
                timeline: timeline,
                waterEntries: ready.waterEntries.map(KMPMappers.waterEntry),
                activeDraft: activeDraft,
                trialBanner: trialBanner
            )
        }
        return .loading
    }
}
