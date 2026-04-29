import SwiftUI
import shared

@MainActor
@Observable
final class QuickAddViewModelWrapper {
    private let sharedVm: QuickAddViewModel
    private let logDate: String?

    var isProcessing = false
    var errorMessage: String?
    var didComplete = false

    @ObservationIgnored private var stateTask: Task<Void, Never>?
    @ObservationIgnored private var effectTask: Task<Void, Never>?

    init(logDate: String? = nil) {
        self.logDate = logDate
        self.sharedVm = KoinHelper().getQuickAddViewModel()
        self.applyState(sharedVm.uiState.value as? QuickAddUiState)

        stateTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<QuickAddUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                self?.applyState(newState)
            }
        }

        effectTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<QuickAddEffect> = observeFlow(sharedVm.effects)
            for await effect in stream {
                self?.handleEffect(effect)
            }
        }
    }

    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
    }

    func addKnown(
        name: String,
        protein: Double,
        calories: Double,
        fat: Double?,
        carbs: Double?,
        quantity: String?,
        source: EntrySource
    ) {
        sharedVm.onEvent(event: QuickAddEventAddKnown(
            name: name,
            protein: protein,
            calories: calories,
            fat: asKotlinDouble(fat),
            carbs: asKotlinDouble(carbs),
            quantity: quantity,
            source: source,
            logDate: logDate
        ))
    }

    func addFromText(description: String) {
        sharedVm.onEvent(event: QuickAddEventAddFromText(description: description, logDate: logDate))
    }

    func addFromPhoto(imageBase64: String, hint: String?) {
        sharedVm.onEvent(event: QuickAddEventAddFromPhoto(imageBase64: imageBase64, hint: hint, logDate: logDate))
    }

    func clearError() {
        sharedVm.onEvent(event: QuickAddEventClearError.shared)
    }

    func consumeCompletion() {
        didComplete = false
    }

    private func applyState(_ state: QuickAddUiState?) {
        isProcessing = state?.isProcessing ?? false
        errorMessage = state?.errorMessage
    }

    private func handleEffect(_ effect: QuickAddEffect) {
        if effect is QuickAddEffectCompleted {
            didComplete = true
        } else if let error = effect as? QuickAddEffectShowError {
            errorMessage = error.message
        }
    }
}
