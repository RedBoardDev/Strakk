import SwiftUI
import shared

@MainActor
@Observable
final class OnboardingFlowViewModelWrapper {
    private let sharedVm: OnboardingFlowViewModel

    var state: OnboardingFlowUiState
    var showLogin: Bool = false
    var errorMessage: String?

    @ObservationIgnored private var stateTask: Task<Void, Never>?
    @ObservationIgnored private var effectTask: Task<Void, Never>?

    // Callback injected by RootView for navigation to home
    var onNavigateToHome: (() -> Void)?

    init() {
        self.sharedVm = KoinBridge.shared.getOnboardingFlowViewModel()
        // swiftlint:disable:next force_cast
        self.state = sharedVm.uiState.value as! OnboardingFlowUiState

        stateTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<OnboardingFlowUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                self?.state = newState
            }
        }

        effectTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<OnboardingFlowEffect> = observeFlow(sharedVm.effects)
            for await effect in stream {
                self?.handleEffect(effect)
            }
        }
    }

    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
    }

    func send(_ event: OnboardingFlowEvent) {
        sharedVm.onEvent(event: event)
    }

    private func handleEffect(_ effect: OnboardingFlowEffect) {
        if effect is OnboardingFlowEffectNavigateToHome {
            onNavigateToHome?()
        } else if effect is OnboardingFlowEffectNavigateToLogin {
            showLogin = true
        } else if let showError = effect as? OnboardingFlowEffectShowError {
            errorMessage = showError.message
        }
    }
}
