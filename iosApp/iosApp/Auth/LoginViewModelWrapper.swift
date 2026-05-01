import SwiftUI
import shared

@MainActor
@Observable
final class LoginViewModelWrapper {
    private let sharedVm: LoginViewModel

    var state: LoginUiState
    var infoMessage: String?

    // Navigation callbacks
    var onNavigateToHome: (() -> Void)?
    var onNavigateToOnboarding: (() -> Void)?
    var onNavigateToSignUp: (() -> Void)?

    @ObservationIgnored private var stateTask: Task<Void, Never>?
    @ObservationIgnored private var effectTask: Task<Void, Never>?

    init() {
        self.sharedVm = KoinBridge.shared.getLoginViewModel()
        // swiftlint:disable:next force_cast
        self.state = sharedVm.uiState.value as! LoginUiState

        stateTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<LoginUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                self?.state = newState
            }
        }

        effectTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<LoginEffect> = observeFlow(sharedVm.effects)
            for await effect in stream {
                self?.handleEffect(effect)
            }
        }
    }

    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
    }

    func send(_ event: LoginEvent) {
        sharedVm.onEvent(event: event)
    }

    private func handleEffect(_ effect: LoginEffect) {
        if effect is LoginEffectNavigateToHome {
            onNavigateToHome?()
        } else if effect is LoginEffectNavigateToOnboarding {
            onNavigateToOnboarding?()
        } else if effect is LoginEffectNavigateToSignUp {
            onNavigateToSignUp?()
        } else if let msg = effect as? LoginEffectShowMessage {
            infoMessage = msg.message
        }
    }
}
