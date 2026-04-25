import SwiftUI
import shared

// MARK: - Swift-side data types

struct ManualEntryFormData: Equatable {
    let name: String
    let protein: String
    let calories: String
    let fat: String
    let carbs: String
    let quantity: String
    let isSubmitting: Bool
    let errorMessage: String?
    let isSubmittable: Bool
}

// MARK: - Wrapper

@MainActor
@Observable
final class ManualEntryViewModelWrapper {
    private let sharedVm: ManualEntryViewModel

    var formData: ManualEntryFormData = ManualEntryFormData(
        name: "", protein: "", calories: "", fat: "", carbs: "", quantity: "",
        isSubmitting: false, errorMessage: nil, isSubmittable: false
    )
    var submittedEntry: MealEntryData?
    var shouldDismiss: Bool = false
    var errorMessage: String?

    nonisolated(unsafe) private var stateTask: Task<Void, Never>?
    nonisolated(unsafe) private var effectTask: Task<Void, Never>?

    init() {
        self.sharedVm = KoinHelper().getManualEntryViewModel()
        self.formData = Self.mapState(sharedVm.uiState.value as? ManualEntryUiState)

        stateTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<ManualEntryUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                self?.formData = Self.mapState(newState)
            }
        }

        effectTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<ManualEntryEffect> = observeFlow(sharedVm.effects)
            for await effect in stream {
                self?.handleEffect(effect)
            }
        }
    }

    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
    }

    func onEvent(_ event: ManualEntryEvent) {
        sharedVm.onEvent(event: event)
    }

    // MARK: - Private

    private func handleEffect(_ effect: ManualEntryEffect) {
        if let submitted = effect as? ManualEntryEffectSubmitted {
            submittedEntry = TodayViewModelWrapper.mapEntry(submitted.entry)
            shouldDismiss = true
        } else if effect is ManualEntryEffectCancelled {
            shouldDismiss = true
        } else if let showError = effect as? ManualEntryEffectShowError {
            errorMessage = showError.message
        }
    }

    private static func mapState(_ kmpState: ManualEntryUiState?) -> ManualEntryFormData {
        guard let s = kmpState else {
            return ManualEntryFormData(
                name: "", protein: "", calories: "", fat: "", carbs: "", quantity: "",
                isSubmitting: false, errorMessage: nil, isSubmittable: false
            )
        }
        return ManualEntryFormData(
            name: s.name,
            protein: s.protein,
            calories: s.calories,
            fat: s.fat,
            carbs: s.carbs,
            quantity: s.quantity,
            isSubmitting: s.isSubmitting,
            errorMessage: s.errorMessage,
            isSubmittable: s.isSubmittable
        )
    }
}
