import SwiftUI
import shared

// MARK: - Swift-side data types

struct DraftItemData: Identifiable, Equatable {
    enum Kind: Equatable {
        case resolved(entry: MealEntryData)
        case pendingPhoto(hint: String?)
        case pendingText(description: String)
    }

    let id: String
    let kind: Kind

    var isResolved: Bool {
        if case .resolved = kind { return true }
        return false
    }
}

struct DraftTotalsData: Equatable {
    let protein: Double
    let calories: Double
    let fat: Double
    let carbs: Double
}

struct EditingDraftData: Equatable {
    let id: String
    let name: String
    let items: [DraftItemData]
    let resolvedCount: Int
    let pendingCount: Int
    let totals: DraftTotalsData
    let isProcessing: Bool

    /// Items that are fully resolved — pre-filtered for review screens.
    var resolvedItems: [DraftItemData] { items.filter(\.isResolved) }
}

enum MealDraftState: Equatable {
    case loading
    case empty
    case editing(EditingDraftData)
}

// MARK: - Wrapper

@MainActor
@Observable
final class MealDraftViewModelWrapper {
    private let sharedVm: MealDraftViewModel

    var state: MealDraftState = .loading
    var didStartDraft: Bool = false
    var navigateToReview: Bool = false
    var committedMeal: MealData?
    var errorMessage: String?

    @ObservationIgnored private var stateTask: Task<Void, Never>?
    @ObservationIgnored private var effectTask: Task<Void, Never>?

    init() {
        self.sharedVm = KoinBridge.shared.getMealDraftViewModel()
        self.state = Self.mapState(sharedVm.uiState.value as? MealDraftUiState)

        stateTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<MealDraftUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                self?.state = Self.mapState(newState)
            }
        }

        effectTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<MealDraftEffect> = observeFlow(sharedVm.effects)
            for await effect in stream {
                self?.handleEffect(effect)
            }
        }
    }

    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
    }

    func onEvent(_ event: MealDraftEvent) {
        sharedVm.onEvent(event: event)
    }

    // MARK: - Private

    private func handleEffect(_ effect: MealDraftEffect) {
        if effect is MealDraftEffectStarted {
            didStartDraft = true
        } else if effect is MealDraftEffectNavigateToReview {
            navigateToReview = true
        } else if let committed = effect as? MealDraftEffectCommitted {
            committedMeal = MealData(
                id: committed.meal.id,
                name: committed.meal.name,
                date: committed.meal.date,
                createdAt: committed.meal.createdAt.description,
                entries: committed.meal.entries.map(KMPMappers.mealEntry)
            )
        } else if let showError = effect as? MealDraftEffectShowError {
            errorMessage = showError.message
        }
    }

    private static func mapState(_ kmpState: MealDraftUiState?) -> MealDraftState {
        guard let kmpState else { return .loading }

        if kmpState is MealDraftUiStateLoading {
            return .loading
        } else if kmpState is MealDraftUiStateEmpty {
            return .empty
        } else if let editing = kmpState as? MealDraftUiStateEditing {
            let items = editing.draft.items.map { item -> DraftItemData in
                if let resolved = item as? DraftItemResolved {
                    return DraftItemData(
                        id: resolved.id,
                        kind: .resolved(entry: KMPMappers.mealEntry(resolved.entry))
                    )
                } else if let photo = item as? DraftItemPendingPhoto {
                    return DraftItemData(id: photo.id, kind: .pendingPhoto(hint: photo.hint))
                } else if let text = item as? DraftItemPendingText {
                    return DraftItemData(id: text.id, kind: .pendingText(description: text.description_))
                }
                return DraftItemData(id: item.id, kind: .pendingText(description: ""))
            }
            let totals = editing.totals
            return .editing(EditingDraftData(
                id: editing.draft.id,
                name: editing.draft.name,
                items: items,
                resolvedCount: Int(editing.resolvedCount),
                pendingCount: Int(editing.pendingCount),
                totals: DraftTotalsData(
                    protein: totals.protein,
                    calories: totals.calories,
                    fat: totals.fat,
                    carbs: totals.carbs
                ),
                isProcessing: editing.isProcessing
            ))
        }
        return .loading
    }
}
