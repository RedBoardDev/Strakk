import SwiftUI
import shared

// MARK: - Swift-side data types

struct CheckInListItemData: Identifiable, Equatable {
    let id: String
    let weekLabel: String
    let weight: Double?
    let photoCount: Int
    let hasAiSummary: Bool
    let createdAt: String
}

struct QuickStatsData: Equatable {
    let lastWeight: Double?
    let weightDelta: Double?
    let lastAvgArm: Double?
    let armDelta: Double?
    let lastWaist: Double?
    let waistDelta: Double?
}

enum CheckInListState {
    case loading
    case ready(checkIns: [CheckInListItemData], quickStats: QuickStatsData?)
}

// MARK: - Wrapper

@MainActor
@Observable
final class CheckInListViewModelWrapper {
    private let sharedVm: CheckInListViewModel

    var state: CheckInListState = .loading
    var navigateToDetailId: String?
    var navigateToWizard = false
    var navigateToStats = false
    var gatedFeature: Feature?

    @ObservationIgnored private var stateTask: Task<Void, Never>?
    @ObservationIgnored private var effectTask: Task<Void, Never>?

    init() {
        self.sharedVm = KoinBridge.shared.getCheckInListViewModel()

        stateTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<CheckInListUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                self?.state = Self.mapState(newState)
            }
        }

        effectTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<CheckInListEffect> = observeFlow(sharedVm.effects)
            for await effect in stream {
                self?.handleEffect(effect)
            }
        }
    }

    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
    }

    func onEvent(_ event: CheckInListEvent) {
        sharedVm.onEvent(event: event)
    }

    // MARK: - Private

    private func handleEffect(_ effect: CheckInListEffect) {
        if effect is CheckInListEffectNavigateToWizard {
            navigateToWizard = true
        } else if let detail = effect as? CheckInListEffectNavigateToDetail {
            navigateToDetailId = detail.id
        } else if effect is CheckInListEffectNavigateToStats {
            navigateToStats = true
        } else if let gated = effect as? CheckInListEffectFeatureGated {
            if let proRequired = gated.access as? FeatureAccessProRequired {
                gatedFeature = proRequired.feature
            } else if let quotaExhausted = gated.access as? FeatureAccessQuotaExhausted {
                gatedFeature = quotaExhausted.feature
            }
        }
    }

    private static func mapState(_ s: CheckInListUiState) -> CheckInListState {
        if s is CheckInListUiStateLoading { return .loading }
        guard let ready = s as? CheckInListUiStateReady else { return .loading }

        let items = ready.checkIns.map { item in
            CheckInListItemData(
                id: item.id,
                weekLabel: item.weekLabel,
                weight: item.weight?.doubleValue,
                photoCount: Int(item.photoCount),
                hasAiSummary: item.hasAiSummary,
                createdAt: item.createdAt
            )
        }

        let stats: QuickStatsData? = ready.quickStats.map { qs in
            QuickStatsData(
                lastWeight: qs.lastWeight?.doubleValue,
                weightDelta: qs.weightDelta?.doubleValue,
                lastAvgArm: qs.lastAvgArm?.doubleValue,
                armDelta: qs.armDelta?.doubleValue,
                lastWaist: qs.lastWaist?.doubleValue,
                waistDelta: qs.waistDelta?.doubleValue
            )
        }

        return .ready(checkIns: items, quickStats: stats)
    }
}
