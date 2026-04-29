import SwiftUI
import shared

// MARK: - Swift-side data types

struct DailyNutritionData: Identifiable, Equatable {
    var id: String { date }
    let date: String
    let calories: Double
    let protein: Double
    let fat: Double
    let carbs: Double
    let waterMl: Int
}

struct CheckInPhotoData: Identifiable, Equatable {
    let id: String
    let storagePath: String
    let position: Int
}

struct CheckInData: Identifiable, Equatable {
    let id: String
    let weekLabel: String
    let coveredDates: [String]
    let weight: Double?
    let shoulders: Double?
    let chest: Double?
    let armLeft: Double?
    let armRight: Double?
    let waist: Double?
    let hips: Double?
    let thighLeft: Double?
    let thighRight: Double?
    let feelingTags: [String]
    let mentalFeeling: String?
    let physicalFeeling: String?
    let nutritionSummary: NutritionSummaryData?
    let photos: [CheckInPhotoData]
    let createdAt: String
    let updatedAt: String
}

enum CheckInDetailState {
    case loading
    case ready(checkIn: CheckInData, delta: CheckInDeltaData?, photoUrls: [String: String])
}

// MARK: - Wrapper

@MainActor
@Observable
final class CheckInDetailViewModelWrapper {
    private let sharedVm: CheckInDetailViewModel
    private let pdfUseCase: GenerateCheckInPdfUseCase
    private let checkInId: String

    var state: CheckInDetailState = .loading
    var navigateToWizardCheckInId: String?
    var navigateBack = false
    var errorMessage: String?

    @ObservationIgnored private var stateTask: Task<Void, Never>?
    @ObservationIgnored private var effectTask: Task<Void, Never>?

    init(checkInId: String) {
        self.checkInId = checkInId
        let koin = KoinHelper()
        self.sharedVm = koin.getCheckInDetailViewModel(checkInId: checkInId)
        self.pdfUseCase = koin.getGenerateCheckInPdfUseCase()

        stateTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<CheckInDetailUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                await MainActor.run {
                    self?.state = Self.mapState(newState)
                }
            }
        }

        effectTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<CheckInDetailEffect> = observeFlow(sharedVm.effects)
            for await effect in stream {
                await MainActor.run { self?.handleEffect(effect) }
            }
        }
    }

    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
    }

    func onEvent(_ event: CheckInDetailEvent) {
        sharedVm.onEvent(event: event)
    }

    func generatePdf(options: PdfExportConfig) async -> Data? {
        do {
            let kmpOptions = PdfExportOptions(
                includePhotos: options.includePhotos,
                includeMeasurements: options.includeMeasurements,
                includeFeelings: options.includeFeelings,
                includeProtein: options.includeProtein,
                includeCalories: options.includeCalories,
                includeCarbs: options.includeCarbs,
                includeFat: options.includeFat,
                includeWater: options.includeWater,
                includeAverages: options.includeAverages,
                includeDailyData: options.includeDailyData,
                includeAiSummary: options.includeAiSummary
            )
            let bytes = try await pdfUseCase.invoke(checkInId: checkInId, options: kmpOptions)
            return Data((0..<bytes.size).map { UInt8(bitPattern: bytes.get(index: $0)) })
        } catch {
            return nil
        }
    }

    // MARK: - Private

    private func handleEffect(_ effect: CheckInDetailEffect) {
        if let nav = effect as? CheckInDetailEffectNavigateToWizard {
            navigateToWizardCheckInId = nav.checkInId
        } else if effect is CheckInDetailEffectNavigateBack {
            navigateBack = true
        } else if let err = effect as? CheckInDetailEffectShowError {
            errorMessage = err.message
        }
    }

    private static func mapState(_ s: CheckInDetailUiState) -> CheckInDetailState {
        if s is CheckInDetailUiStateLoading { return .loading }
        guard let ready = s as? CheckInDetailUiStateReady else { return .loading }
        return .ready(
            checkIn: mapCheckIn(ready.checkIn),
            delta: ready.delta.map(mapDelta),
            photoUrls: ready.photoUrls
        )
    }

    private static func mapCheckIn(_ c: CheckIn) -> CheckInData {
        CheckInData(
            id: c.id,
            weekLabel: c.weekLabel,
            coveredDates: c.coveredDates,
            weight: c.weight?.doubleValue,
            shoulders: c.shoulders?.doubleValue,
            chest: c.chest?.doubleValue,
            armLeft: c.armLeft?.doubleValue,
            armRight: c.armRight?.doubleValue,
            waist: c.waist?.doubleValue,
            hips: c.hips?.doubleValue,
            thighLeft: c.thighLeft?.doubleValue,
            thighRight: c.thighRight?.doubleValue,
            feelingTags: c.feelingTags,
            mentalFeeling: c.mentalFeeling,
            physicalFeeling: c.physicalFeeling,
            nutritionSummary: c.nutritionSummary.map(mapNutrition),
            photos: c.photos.map { p in
                CheckInPhotoData(id: p.id, storagePath: p.storagePath, position: Int(p.position))
            },
            createdAt: c.createdAt,
            updatedAt: c.updatedAt
        )
    }

    private static func mapDelta(_ d: CheckInDelta) -> CheckInDeltaData {
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

    private static func mapNutrition(_ n: NutritionSummary) -> NutritionSummaryData {
        NutritionSummaryData(
            avgProtein: n.avgProtein,
            avgCalories: n.avgCalories,
            avgFat: n.avgFat,
            avgCarbs: n.avgCarbs,
            avgWater: Int(n.avgWater),
            nutritionDays: Int(n.nutritionDays),
            aiSummary: n.aiSummary,
            dailyData: n.dailyData.map { d in
                DailyNutritionData(
                    date: d.date,
                    calories: d.calories,
                    protein: d.protein,
                    fat: d.fat,
                    carbs: d.carbs,
                    waterMl: Int(d.waterMl)
                )
            }
        )
    }
}
