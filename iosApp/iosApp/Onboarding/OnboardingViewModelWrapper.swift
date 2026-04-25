import SwiftUI
import shared

@MainActor
@Observable
final class OnboardingViewModelWrapper {
    private let sharedVm: OnboardingViewModel

    var state: OnboardingUiState
    var errorMessage: String?
    var shouldNavigateToHome: Bool = false

    nonisolated(unsafe) private var stateTask: Task<Void, Never>?
    nonisolated(unsafe) private var effectTask: Task<Void, Never>?

    init() {
        self.sharedVm = KoinHelper().getOnboardingViewModel()
        self.state = (sharedVm.uiState.value as? OnboardingUiState) ?? OnboardingUiState(
            currentStep: 0,
            proteinGoal: "",
            calorieGoal: "",
            waterGoal: "",
            trackingReminderEnabled: false,
            trackingReminderTime: "17:00",
            checkinReminderEnabled: false,
            checkinReminderDay: 6,
            checkinReminderTime: "10:00",
            isSaving: false
        )

        stateTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<OnboardingUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                self?.state = newState
            }
        }

        effectTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<OnboardingEffect> = observeFlow(sharedVm.effects)
            for await effect in stream {
                self?.handleEffect(effect)
            }
        }
    }

    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
    }

    func onEvent(_ event: OnboardingEvent) {
        sharedVm.onEvent(event: event)
    }

    private func handleEffect(_ effect: OnboardingEffect) {
        if effect is OnboardingEffectNavigateToHome {
            shouldNavigateToHome = true
        } else if let showError = effect as? OnboardingEffectShowError {
            errorMessage = showError.message
        }
    }
}
