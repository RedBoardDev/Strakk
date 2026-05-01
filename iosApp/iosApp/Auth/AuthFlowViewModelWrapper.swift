import SwiftUI
import shared

/// Swift-side representation of AuthFlowUiState.
enum AuthFlowState: Equatable {
    case welcome
    case signIn(email: String, password: String, isLoading: Bool, error: String?)
    case signUp(email: String, password: String, isLoading: Bool, error: String?)

    var discriminator: Int {
        switch self {
        case .welcome: return 0
        case .signIn: return 1
        case .signUp: return 2
        }
    }
}

@MainActor
@Observable
final class AuthFlowViewModelWrapper {
    private let sharedVm: AuthFlowViewModel

    var state: AuthFlowState = .welcome
    var errorMessage: String?

    @ObservationIgnored private var stateTask: Task<Void, Never>?
    @ObservationIgnored private var effectTask: Task<Void, Never>?

    init() {
        self.sharedVm = KoinBridge.shared.getAuthFlowViewModel()
        self.state = Self.mapState(sharedVm.uiState.value as? AuthFlowUiState)

        stateTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<AuthFlowUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                self?.state = Self.mapState(newState)
            }
        }

        effectTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<AuthFlowEffect> = observeFlow(sharedVm.effects)
            for await effect in stream {
                self?.handleEffect(effect)
            }
        }
    }

    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
    }

    func onEvent(_ event: AuthFlowEvent) {
        sharedVm.onEvent(event: event)
    }

    private func handleEffect(_ effect: AuthFlowEffect) {
        if let showError = effect as? AuthFlowEffectShowError {
            errorMessage = showError.message
        }
    }

    private static func mapState(_ kmpState: AuthFlowUiState?) -> AuthFlowState {
        guard let kmpState else { return .welcome }
        if kmpState is AuthFlowUiStateWelcome {
            return .welcome
        } else if let signIn = kmpState as? AuthFlowUiStateSignIn {
            return .signIn(
                email: signIn.email,
                password: signIn.password,
                isLoading: signIn.isLoading,
                error: signIn.error
            )
        } else if let signUp = kmpState as? AuthFlowUiStateSignUp {
            return .signUp(
                email: signUp.email,
                password: signUp.password,
                isLoading: signUp.isLoading,
                error: signUp.error
            )
        }
        return .welcome
    }
}
