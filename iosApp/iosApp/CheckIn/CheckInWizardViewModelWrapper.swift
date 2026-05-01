import SwiftUI
import shared

// MARK: - Swift-side data types

enum CheckInWizardStep: Equatable {
    case dates
    case measurements
    case feelings
    case photos
    case summary
}

struct WeekOptionData: Identifiable, Equatable {
    var id: String { weekLabel }
    let weekLabel: String
    let displayLabel: String
    let startDate: String
    let endDate: String
}

struct DayOptionData: Identifiable, Equatable {
    var id: String { date }
    let date: String
    let displayLabel: String
    let selected: Bool
}

enum WizardPhotoData: Identifiable, Equatable {
    case remote(id: String, storagePath: String, signedUrl: String)
    case local(id: String, imageData: Data)

    var id: String {
        switch self {
        case .remote(let id, _, _): return id
        case .local(let id, _): return id
        }
    }

    static func == (lhs: WizardPhotoData, rhs: WizardPhotoData) -> Bool {
        lhs.id == rhs.id
    }
}

enum CheckInWizardState {
    case loading
    case ready(
        isEditMode: Bool,
        currentStep: CheckInWizardStep,
        weekLabel: String,
        availableWeeks: [WeekOptionData],
        coveredDates: Set<String>,
        weekDays: [DayOptionData],
        existingCheckInId: String?,
        weight: String,
        shoulders: String,
        chest: String,
        armLeft: String,
        armRight: String,
        waist: String,
        hips: String,
        thighLeft: String,
        thighRight: String,
        delta: CheckInDeltaData?,
        selectedTags: Set<String>,
        mentalFeeling: String,
        physicalFeeling: String,
        photos: [WizardPhotoData],
        nutritionSummary: NutritionSummaryData?,
        nutritionLoading: Bool,
        saving: Bool,
        canGoNext: Bool
    )
}

// MARK: - Wrapper

@MainActor
@Observable
final class CheckInWizardViewModelWrapper {
    private let sharedVm: CheckInWizardViewModel

    var state: CheckInWizardState = .loading
    /// Local image data keyed by photo ID (for photos added from Swift/camera)
    private var localPhotoData: [String: Data] = [:]
    var navigateBack = false
    var navigateToDetailId: String?
    var errorMessage: String?

    @ObservationIgnored private var stateTask: Task<Void, Never>?
    @ObservationIgnored private var effectTask: Task<Void, Never>?

    init(checkInId: String?) {
        self.sharedVm = KoinBridge.shared.getCheckInWizardViewModel(checkInId: checkInId)

        stateTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<CheckInWizardUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                self?.updateState(newState)
            }
        }

        effectTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<CheckInWizardEffect> = observeFlow(sharedVm.effects)
            for await effect in stream {
                self?.handleEffect(effect)
            }
        }
    }

    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
    }

    func onEvent(_ event: CheckInWizardEvent) {
        sharedVm.onEvent(event: event)
    }

    func consumeNavigateBack() { navigateBack = false }
    func consumeNavigateToDetail() { navigateToDetailId = nil }

    /// Adds a photo from a Swift Data source.
    /// The Data → KotlinByteArray conversion is done off the main thread to avoid
    /// blocking the UI for large images (several MB is common for camera output).
    func addPhoto(imageData: Data) {
        pendingLocalPhotos.append(imageData)
        let sharedVm = self.sharedVm
        Task.detached(priority: .userInitiated) {
            let kotlinBytes = Self.makeKotlinByteArray(from: imageData)
            await MainActor.run {
                sharedVm.onEvent(event: CheckInWizardEventOnAddPhoto(imageData: kotlinBytes))
            }
        }
    }

    /// Converts Swift `Data` to `KotlinByteArray` off the main thread.
    private static func makeKotlinByteArray(from data: Data) -> KotlinByteArray {
        let kotlinBytes = KotlinByteArray(size: Int32(data.count))
        data.withUnsafeBytes { buffer in
            for (index, byte) in buffer.enumerated() {
                kotlinBytes.set(index: Int32(index), value: Int8(bitPattern: byte))
            }
        }
        return kotlinBytes
    }

    /// Queue of local photo Data, consumed in order as KMP state updates with new local photo IDs.
    private var pendingLocalPhotos: [Data] = []

    // MARK: - Private

    private func updateState(_ s: CheckInWizardUiState) {
        guard let ready = s as? CheckInWizardUiStateReady else {
            state = .loading
            return
        }
        // Assign pending local photo data to newly discovered local photo IDs
        for photo in ready.photos {
            if let local = photo as? WizardPhotoLocal,
               localPhotoData[local.id] == nil,
               !pendingLocalPhotos.isEmpty {
                localPhotoData[local.id] = pendingLocalPhotos.removeFirst()
            }
        }
        state = Self.mapState(ready, localData: localPhotoData)
    }

    private func handleEffect(_ effect: CheckInWizardEffect) {
        if effect is CheckInWizardEffectNavigateBack {
            navigateBack = true
        } else if let nav = effect as? CheckInWizardEffectNavigateToDetail {
            navigateToDetailId = nav.checkInId
        } else if let err = effect as? CheckInWizardEffectShowError {
            errorMessage = err.message
        }
    }

    private static func mapState(_ ready: CheckInWizardUiStateReady, localData: [String: Data]) -> CheckInWizardState {
        let step = mapStep(ready.currentStep)

        let availableWeeks = ready.availableWeeks.map { w in
            WeekOptionData(
                weekLabel: w.weekLabel,
                displayLabel: w.displayLabel,
                startDate: w.startDate,
                endDate: w.endDate
            )
        }

        let weekDays = ready.weekDays.map { d in
            DayOptionData(date: d.date, displayLabel: d.displayLabel, selected: d.selected)
        }

        let coveredDates = Set(ready.coveredDates)

        let photos = ready.photos.map { mapPhoto($0, localData: localData) }

        let delta = ready.delta.map { d in
            CheckInDeltaData(
                weight: d.weight?.doubleValue,
                shoulders: d.shoulders?.doubleValue,
                chest: d.chest?.doubleValue,
                armLeft: d.armLeft?.doubleValue,
                armRight: d.armRight?.doubleValue,
                waist: d.waist?.doubleValue,
                hips: d.hips?.doubleValue,
                thighLeft: d.thighLeft?.doubleValue,
                thighRight: d.thighRight?.doubleValue
            )
        }

        let nutrition = ready.nutritionSummary.map { n in
            NutritionSummaryData(
                avgProtein: n.avgProtein,
                avgCalories: n.avgCalories,
                avgFat: n.avgFat,
                avgCarbs: n.avgCarbs,
                avgWater: Int(n.avgWater),
                nutritionDays: Int(n.nutritionDays),
                aiSummary: n.aiSummary,
                dailyData: []
            )
        }

        return .ready(
            isEditMode: ready.isEditMode,
            currentStep: step,
            weekLabel: ready.weekLabel,
            availableWeeks: availableWeeks,
            coveredDates: coveredDates,
            weekDays: weekDays,
            existingCheckInId: ready.existingCheckInId,
            weight: ready.weight,
            shoulders: ready.shoulders,
            chest: ready.chest,
            armLeft: ready.armLeft,
            armRight: ready.armRight,
            waist: ready.waist,
            hips: ready.hips,
            thighLeft: ready.thighLeft,
            thighRight: ready.thighRight,
            delta: delta,
            selectedTags: Set(ready.selectedTags),
            mentalFeeling: ready.mentalFeeling,
            physicalFeeling: ready.physicalFeeling,
            photos: photos,
            nutritionSummary: nutrition,
            nutritionLoading: ready.nutritionLoading,
            saving: ready.saving,
            canGoNext: ready.canGoNext
        )
    }

    private static func mapStep(_ step: WizardStep) -> CheckInWizardStep {
        switch step {
        case WizardStep.dates: return .dates
        case WizardStep.measurements: return .measurements
        case WizardStep.feelings: return .feelings
        case WizardStep.photos: return .photos
        case WizardStep.summary: return .summary
        default: return .dates
        }
    }

    private static func mapPhoto(_ photo: any WizardPhoto, localData: [String: Data]) -> WizardPhotoData {
        if let remote = photo as? WizardPhotoRemote {
            return .remote(id: remote.id, storagePath: remote.storagePath, signedUrl: remote.signedUrl)
        } else if let local = photo as? WizardPhotoLocal {
            let data = localData[local.id] ?? Data()
            return .local(id: local.id, imageData: data)
        }
        return .local(id: photo.id, imageData: Data())
    }
}
