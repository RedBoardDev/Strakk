import SwiftUI
import shared

// MARK: - Swift-side data types

enum PhotoMealState: Equatable {
    case capturing
    case identifying(hint: String?)
    case grounding(identifiedNames: [String])
    case reviewing(ReviewingData)
}

struct ReviewingData: Equatable {
    let items: [GroundedItemData]
    let mealName: String
    let isSaving: Bool
    let totalKcal: Double
    let totalProtein: Double
    let totalFat: Double
    let totalCarbs: Double
    let groundedCount: Int
    let hiddenCount: Int

    var hasVisibleItems: Bool { items.contains { !$0.isHidden } }
}

struct GroundedItemData: Identifiable, Equatable {
    /// Index in the list at construction time (may shift after removals).
    let id: Int
    let name: String
    let unit: String
    let amount: Double
    let matchName: String?
    let matchSource: String?
    let isGrounded: Bool
    let aiConfidence: Double
    let kcal: Double
    let protein: Double
    let fat: Double
    let carbs: Double
    let grams: Double
    let cookingMethod: String?
    let isHidden: Bool
    let isManuallyAdded: Bool

    /// Stable identity across index shifts when items are removed. Used by
    /// SwiftUI's `ForEach` to preserve card state (`isExpanded`, `currentGrams`).
    var stableKey: String { "\(name)|\(unit)|\(amount)" }
}

// MARK: - Wrapper

@MainActor
@Observable
final class PhotoMealViewModelWrapper {
    private let sharedVm: PhotoMealViewModel

    var state: PhotoMealState = .capturing
    var savedMeal: MealData?
    var errorMessage: String?
    var gatedFeature: Feature?
    var didCancel: Bool = false

    @ObservationIgnored private var stateTask: Task<Void, Never>?
    @ObservationIgnored private var effectTask: Task<Void, Never>?

    init() {
        self.sharedVm = KoinBridge.shared.getPhotoMealViewModel()
        self.state = Self.mapState(sharedVm.uiState.value as? PhotoMealUiState)

        stateTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<PhotoMealUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                self?.state = Self.mapState(newState)
            }
        }

        effectTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<PhotoMealEffect> = observeFlow(sharedVm.effects)
            for await effect in stream {
                self?.handleEffect(effect)
            }
        }
    }

    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
    }

    func onEvent(_ event: PhotoMealEvent) {
        sharedVm.onEvent(event: event)
    }

    // MARK: - Private

    private func handleEffect(_ effect: PhotoMealEffect) {
        if let saved = effect as? PhotoMealEffectMealSaved {
            savedMeal = KMPMappers.meal(saved.meal)
        } else if effect is PhotoMealEffectCancelled {
            didCancel = true
        } else if let showError = effect as? PhotoMealEffectShowError {
            errorMessage = showError.message
        } else if let gated = effect as? PhotoMealEffectFeatureGated {
            if let proRequired = gated.access as? FeatureAccessProRequired {
                gatedFeature = proRequired.feature
            } else if let quotaExhausted = gated.access as? FeatureAccessQuotaExhausted {
                gatedFeature = quotaExhausted.feature
            }
        }
    }

    private static func mapState(_ kmpState: PhotoMealUiState?) -> PhotoMealState {
        guard let kmpState else { return .capturing }

        if kmpState is PhotoMealUiStateCapturing {
            return .capturing
        } else if let identifying = kmpState as? PhotoMealUiStateIdentifying {
            return .identifying(hint: identifying.hint)
        } else if let grounding = kmpState as? PhotoMealUiStateGrounding {
            return .grounding(identifiedNames: grounding.identifiedNames)
        } else if let reviewing = kmpState as? PhotoMealUiStateReviewing {
            let totals = reviewing.totals
            let items = reviewing.items.enumerated().map { index, item -> GroundedItemData in
                GroundedItemData(
                    id: index,
                    name: item.prediction.name,
                    unit: item.prediction.unit,
                    amount: item.prediction.amount,
                    matchName: item.catalogMatch?.name,
                    matchSource: item.groundingSource,
                    isGrounded: item.isGrounded,
                    aiConfidence: item.aiConfidence,
                    kcal: item.computedMacros.kcal,
                    protein: item.computedMacros.protein,
                    fat: item.computedMacros.fat,
                    carbs: item.computedMacros.carbs,
                    grams: item.quantity.grams,
                    cookingMethod: nil,
                    isHidden: item.isHidden,
                    isManuallyAdded: item.isManuallyAdded
                )
            }
            return .reviewing(ReviewingData(
                items: items,
                mealName: reviewing.mealName,
                isSaving: reviewing.isSaving,
                totalKcal: totals.kcal,
                totalProtein: totals.protein,
                totalFat: totals.fat,
                totalCarbs: totals.carbs,
                groundedCount: Int(reviewing.groundedCount),
                hiddenCount: Int(reviewing.hiddenCount)
            ))
        }
        return .capturing
    }
}
