import SwiftUI
import shared

enum RootState: Equatable {
    case loading
    case unauthenticated
    case authenticated(onboardingCompleted: Bool, showTrialExpiredModal: Bool)
}

@MainActor
@Observable
final class RootViewModelWrapper {
    private let sharedVm: RootViewModel

    var state: RootState = .loading
    var showPaywall: Bool = false

    @ObservationIgnored private var observationTask: Task<Void, Never>?

    init() {
        self.sharedVm = KoinBridge.shared.getRootViewModel()
        self.state = Self.map(sharedVm.uiState.value as? RootUiState)

        observationTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<RootUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                self?.state = Self.map(newState)
            }
        }
    }

    deinit {
        observationTask?.cancel()
    }

    func refreshProfile() {
        sharedVm.refreshProfile()
    }

    func dismissTrialModal() {
        sharedVm.dismissTrialModal()
    }

    private static func map(_ kmpState: RootUiState?) -> RootState {
        guard let kmpState else { return .loading }
        if kmpState is RootUiStateLoading {
            return .loading
        } else if kmpState is RootUiStateUnauthenticated {
            return .unauthenticated
        } else if let auth = kmpState as? RootUiStateAuthenticated {
            return .authenticated(
                onboardingCompleted: auth.onboardingCompleted,
                showTrialExpiredModal: auth.showTrialExpiredModal
            )
        }
        return .loading
    }
}
