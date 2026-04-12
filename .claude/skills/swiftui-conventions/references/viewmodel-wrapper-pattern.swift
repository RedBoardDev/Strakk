import SwiftUI
import Shared // KMP shared module

// =============================================================================
// Pattern 1: Basic ViewModel wrapper — single StateFlow
// =============================================================================

/// Wraps a KMP ViewModel for SwiftUI consumption.
///
/// Key rules:
/// - `@Observable` (NOT `ObservableObject` / `@Published`)
/// - `@MainActor` on ALL wrappers
/// - Cancel observation tasks in `deinit`
/// - SKIE converts `StateFlow` to `AsyncSequence` automatically
@MainActor
@Observable
final class SessionListViewModelWrapper {
    private let sharedVm: SessionListViewModel

    var state: SessionListUiState

    private var observationTask: Task<Void, Never>?

    init(sharedVm: SessionListViewModel) {
        self.sharedVm = sharedVm
        self.state = sharedVm.uiState.value

        observationTask = Task { [weak self] in
            for await newState in sharedVm.uiState {
                self?.state = newState
            }
        }
    }

    deinit {
        observationTask?.cancel()
    }

    func onEvent(_ event: SessionListEvent) {
        sharedVm.onEvent(event: event)
    }
}

// =============================================================================
// Pattern 2: Wrapper with multiple state properties + effects
// =============================================================================

/// For screens with both state and one-shot effects (navigation, snackbar).
///
/// Effects are consumed via a separate async observation loop.
@MainActor
@Observable
final class CreateSessionViewModelWrapper {
    private let sharedVm: CreateSessionViewModel

    var state: CreateSessionUiState
    var snackbarMessage: String?
    var shouldNavigateBack: Bool = false

    private var stateTask: Task<Void, Never>?
    private var effectTask: Task<Void, Never>?

    init(sharedVm: CreateSessionViewModel) {
        self.sharedVm = sharedVm
        self.state = sharedVm.uiState.value

        stateTask = Task { [weak self] in
            for await newState in sharedVm.uiState {
                self?.state = newState
            }
        }

        effectTask = Task { [weak self] in
            for await effect in sharedVm.effects {
                self?.handleEffect(effect)
            }
        }
    }

    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
    }

    func onEvent(_ event: CreateSessionEvent) {
        sharedVm.onEvent(event: event)
    }

    private func handleEffect(_ effect: CreateSessionEffect) {
        switch onEnum(of: effect) {
        case .navigateBack:
            shouldNavigateBack = true
        case .showSnackbar(let data):
            snackbarMessage = data.message
        }
    }
}

// =============================================================================
// Pattern 3: onEnum(of:) for sealed interface exhaustive switch
// =============================================================================

/// SKIE generates Swift enums from Kotlin sealed interfaces.
/// Use `onEnum(of:)` for compile-time exhaustive matching.
///
/// This replaces manual type-casting and ensures you handle all cases.
struct SessionListContentView: View {
    let state: SessionListUiState

    var body: some View {
        switch onEnum(of: state) {
        case .loading:
            ProgressView("Loading sessions...")
        case .success(let data):
            SessionListView(sessions: data.sessions)
        case .error(let data):
            ContentUnavailableView(
                "Error",
                systemImage: "exclamationmark.triangle",
                description: Text(data.message)
            )
        }
    }
}

// =============================================================================
// Pattern 4: Consuming suspend functions from Swift
// =============================================================================

/// When calling KMP suspend functions from Swift, use `.task {}` for
/// structured concurrency. SKIE bridges suspend functions as async.
struct CreateSessionView: View {
    @State private var viewModel: CreateSessionViewModelWrapper

    init(sharedVm: CreateSessionViewModel) {
        _viewModel = State(
            initialValue: CreateSessionViewModelWrapper(sharedVm: sharedVm)
        )
    }

    var body: some View {
        Form {
            // ... form fields ...

            Button("Save") {
                viewModel.onEvent(.onSaveClick)
            }
        }
        // CORRECT: structured concurrency with .task
        .task {
            // If you need to call a suspend function directly:
            // await someKmpSuspendFunction()
        }
        .onChange(of: viewModel.shouldNavigateBack) { _, shouldNavigate in
            if shouldNavigate {
                // Handle navigation
            }
        }
    }
}

// =============================================================================
// Pattern 5: Error handling for KMP interop
// =============================================================================

/// KMP exceptions become NSError in Swift via Kotlin/Native.
/// Always wrap KMP calls in do/catch when they can throw.
@MainActor
@Observable
final class SafeViewModelWrapper {
    private let sharedVm: SessionListViewModel

    var state: SessionListUiState
    var errorMessage: String?

    private var observationTask: Task<Void, Never>?

    init(sharedVm: SessionListViewModel) {
        self.sharedVm = sharedVm
        self.state = sharedVm.uiState.value

        observationTask = Task { [weak self] in
            do {
                for try await newState in sharedVm.uiState {
                    self?.state = newState
                }
            } catch {
                self?.errorMessage = error.localizedDescription
            }
        }
    }

    deinit {
        observationTask?.cancel()
    }
}
