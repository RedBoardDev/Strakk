import SwiftUI
import shared

// MARK: - Swift-side data types

struct SettingsData: Equatable {
    let email: String?
    let proteinGoal: String
    let calorieGoal: String
    let waterGoal: String
    let trackingReminderEnabled: Bool
    let trackingReminderTime: String
    let checkinReminderEnabled: Bool
    let checkinReminderDay: Int
    let checkinReminderTime: String
    let hevyApiKey: String
}

enum SettingsState: Equatable {
    case loading
    case ready(data: SettingsData)
}

// MARK: - Wrapper

@MainActor
@Observable
final class SettingsViewModelWrapper {
    private let sharedVm: SettingsViewModel

    var state: SettingsState = .loading
    var errorMessage: String?

    nonisolated(unsafe) private var stateTask: Task<Void, Never>?
    nonisolated(unsafe) private var effectTask: Task<Void, Never>?

    init() {
        self.sharedVm = KoinHelper().getSettingsViewModel()
        self.state = Self.mapState(sharedVm.uiState.value as? SettingsUiState)

        stateTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<SettingsUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                self?.state = Self.mapState(newState)
            }
        }

        effectTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<SettingsEffect> = observeFlow(sharedVm.effects)
            for await effect in stream {
                self?.handleEffect(effect)
            }
        }
    }

    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
    }

    func onEvent(_ event: SettingsEvent) {
        sharedVm.onEvent(event: event)
    }

    private func handleEffect(_ effect: SettingsEffect) {
        if let showError = effect as? SettingsEffectShowError {
            errorMessage = showError.message
        }
    }

    // MARK: - Mapping

    private static func mapState(_ kmpState: SettingsUiState?) -> SettingsState {
        guard let kmpState else { return .loading }

        if kmpState is SettingsUiStateLoading {
            return .loading
        } else if let ready = kmpState as? SettingsUiStateReady {
            return .ready(data: SettingsData(
                email: ready.email,
                proteinGoal: ready.proteinGoal,
                calorieGoal: ready.calorieGoal,
                waterGoal: ready.waterGoal,
                trackingReminderEnabled: ready.trackingReminderEnabled,
                trackingReminderTime: ready.trackingReminderTime,
                checkinReminderEnabled: ready.checkinReminderEnabled,
                checkinReminderDay: Int(ready.checkinReminderDay),
                checkinReminderTime: ready.checkinReminderTime,
                hevyApiKey: ready.hevyApiKey
            ))
        }
        return .loading
    }
}
