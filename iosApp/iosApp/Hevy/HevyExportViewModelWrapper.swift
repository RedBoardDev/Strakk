import SwiftUI
import shared

// MARK: - Swift-side data models

struct WorkoutSessionData: Equatable, Identifiable {
    let id: Int
    let name: String
    let exerciseCount: Int
    let sectionNames: [String]
}

struct ExportResultData: Equatable {
    let routineTitle: String
    let exercisesMatched: Int
    let exercisesCreated: Int
    let exercisesMatchedByAlgo: Int
    let exercisesMatchedByAi: Int
}

// MARK: - State enum

enum HevyExportState: Equatable {
    case idle
    case parsing
    case sessionList(programName: String, sessions: [WorkoutSessionData])
    case exporting(sessionName: String)
    case done(result: ExportResultData)
}

// MARK: - Wrapper

@MainActor
@Observable
final class HevyExportViewModelWrapper {
    private let sharedVm: HevyExportViewModel

    var state: HevyExportState = .idle
    var shouldDismiss: Bool = false
    var errorMessage: String?
    var requiresApiKey: Bool = false

    @ObservationIgnored private var stateTask: Task<Void, Never>?
    @ObservationIgnored private var effectTask: Task<Void, Never>?

    init() {
        self.sharedVm = KoinHelper().getHevyExportViewModel()

        stateTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<HevyExportUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                self?.state = Self.mapState(newState)
            }
        }

        effectTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<HevyExportEffect> = observeFlow(sharedVm.effects)
            for await effect in stream {
                self?.handleEffect(effect)
            }
        }
    }

    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
    }

    func onEvent(_ event: HevyExportEvent) {
        sharedVm.onEvent(event: event)
    }

    // MARK: - Private

    private func handleEffect(_ effect: HevyExportEffect) {
        if effect is HevyExportEffectExportSuccess {
            // State is already .done — no extra action needed
        } else if let showError = effect as? HevyExportEffectShowError {
            errorMessage = showError.message
        } else if effect is HevyExportEffectRequireApiKey {
            requiresApiKey = true
        } else if effect is HevyExportEffectDismiss {
            shouldDismiss = true
        }
    }

    private static func mapState(_ s: HevyExportUiState) -> HevyExportState {
        if s is HevyExportUiStateIdle {
            return .idle
        } else if s is HevyExportUiStateParsing {
            return .parsing
        } else if let sessionList = s as? HevyExportUiStateSessionList {
            let sessions: [WorkoutSessionData] = sessionList.sessions.enumerated().map { index, session in
                let exerciseCount = session.sections.reduce(0) { $0 + $1.exercises.count }
                let sectionNames = session.sections.map { $0.name }
                return WorkoutSessionData(
                    id: index,
                    name: session.name,
                    exerciseCount: exerciseCount,
                    sectionNames: sectionNames
                )
            }
            return .sessionList(programName: sessionList.programName, sessions: sessions)
        } else if let exporting = s as? HevyExportUiStateExporting {
            return .exporting(sessionName: exporting.sessionName)
        } else if let done = s as? HevyExportUiStateDone {
            let result = ExportResultData(
                routineTitle: done.result.routineTitle,
                exercisesMatched: Int(done.result.exercisesMatched),
                exercisesCreated: Int(done.result.exercisesCreated),
                exercisesMatchedByAlgo: Int(done.result.exercisesMatchedByAlgo),
                exercisesMatchedByAi: Int(done.result.exercisesMatchedByAi)
            )
            return .done(result: result)
        }
        return .idle
    }
}
