import SwiftUI
import shared

// MARK: - Swift-side plan enum

enum SwiftSubscriptionPlan: Equatable {
    case annual
    case monthly
}

// MARK: - Swift-side state

struct PaywallData: Equatable {
    let features: [ProFeatureInfo]
    let highlightedFeature: ProFeature?
    let selectedPlan: SwiftSubscriptionPlan
    let isProcessing: Bool
    let isAlreadyPro: Bool
}

// MARK: - Wrapper

@MainActor
@Observable
final class PaywallViewModelWrapper {
    private let sharedVm: PaywallViewModel

    var paywallData: PaywallData
    var toastMessage: String?
    var shouldDismiss: Bool = false

    @ObservationIgnored private var stateTask: Task<Void, Never>?
    @ObservationIgnored private var effectTask: Task<Void, Never>?

    init(highlightedFeature: ProFeature? = nil) {
        self.sharedVm = KoinBridge.shared.getPaywallViewModel(highlightedFeature: highlightedFeature)
        // swiftlint:disable:next force_cast
        let initial = sharedVm.uiState.value as! PaywallUiState
        self.paywallData = Self.mapState(initial)

        stateTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<PaywallUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                self?.paywallData = Self.mapState(newState)
            }
        }

        effectTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<PaywallEffect> = observeFlow(sharedVm.effects)
            for await effect in stream {
                self?.handleEffect(effect)
            }
        }
    }

    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
    }

    func onEvent(_ event: PaywallEvent) {
        sharedVm.onEvent(event: event)
    }

    // MARK: - Effect handling

    private func handleEffect(_ effect: PaywallEffect) {
        if effect is PaywallEffectDismiss {
            shouldDismiss = true
        } else if let toast = effect as? PaywallEffectShowToast {
            toastMessage = toast.message
        }
    }

    // MARK: - Mapping

    private static func mapState(_ kmpState: PaywallUiState) -> PaywallData {
        let plan: SwiftSubscriptionPlan = kmpState.selectedPlan == .annual ? .annual : .monthly
        let features: [ProFeatureInfo] = kmpState.features.compactMap { $0 as? ProFeatureInfo }
        return PaywallData(
            features: features,
            highlightedFeature: kmpState.highlightedFeature,
            selectedPlan: plan,
            isProcessing: kmpState.isProcessing,
            isAlreadyPro: kmpState.isAlreadyPro
        )
    }
}
