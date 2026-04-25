import SwiftUI
import shared

/// Swift-side representation of RootUiState.
enum RootState: Equatable {
    case loading
    case unauthenticated
    case authenticated(hasProfile: Bool)
}

@MainActor
@Observable
final class RootViewModelWrapper {
    private let sharedVm: RootViewModel

    var state: RootState = .loading

    nonisolated(unsafe) private var observationTask: Task<Void, Never>?

    init() {
        self.sharedVm = KoinHelper().getRootViewModel()
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

    private static func map(_ kmpState: RootUiState?) -> RootState {
        guard let kmpState else { return .loading }
        if kmpState is RootUiStateLoading {
            return .loading
        } else if kmpState is RootUiStateUnauthenticated {
            return .unauthenticated
        } else if let auth = kmpState as? RootUiStateAuthenticated {
            return .authenticated(hasProfile: auth.hasProfile)
        }
        return .loading
    }
}
