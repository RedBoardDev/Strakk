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

// MARK: - Training data types

struct HevyWorkoutSetData {
    let type: String
    let weightKg: Double?
    let reps: Int?
    let durationSeconds: Int?
    let rpe: Double?
}

struct HevyWorkoutExerciseData {
    let name: String
    let muscleGroup: String
    let sets: [HevyWorkoutSetData]
}

struct HevyWorkoutData: Identifiable {
    let id: String
    let title: String
    let date: String
    let durationMinutes: Int
    let totalVolumeKg: Double
    let exercises: [HevyWorkoutExerciseData]
}

struct WeeklyTrainingStatsData {
    let totalSessions: Int
    let totalDurationMinutes: Int
    let totalVolumeKg: Double
    let avgRpe: Double?
    let muscleGroupVolume: [String: Double]
    let workouts: [HevyWorkoutData]
}

enum CheckInDetailState {
    case loading
    case ready(
        checkIn: CheckInData,
        delta: CheckInDeltaData?,
        photoUrls: [String: String],
        trainingStats: WeeklyTrainingStatsData?,
        isLoadingTraining: Bool,
        pdfExportOptions: PdfExportConfig
    )
}

// MARK: - Wrapper

@MainActor
@Observable
final class CheckInDetailViewModelWrapper {
    private let sharedVm: CheckInDetailViewModel
    private let pdfUseCase: GenerateCheckInPdfUseCase
    private let checkInId: String

    var state: CheckInDetailState = .loading
    var isRefreshingNutrition: Bool = false
    var isPushingToHevy: Bool = false
    var navigateToWizardCheckInId: String?
    var navigateBack = false
    var errorMessage: String?
    var requiresHevyApiKey: Bool = false
    var hevyPushConflictDate: String?
    var hevyPushSuccessDate: String?

    @ObservationIgnored private var stateTask: Task<Void, Never>?
    @ObservationIgnored private var effectTask: Task<Void, Never>?

    init(checkInId: String) {
        self.checkInId = checkInId
        self.sharedVm = KoinBridge.shared.getCheckInDetailViewModel(checkInId: checkInId)
        self.pdfUseCase = KoinBridge.shared.getGenerateCheckInPdfUseCase()

        stateTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<CheckInDetailUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                self?.state = Self.mapState(newState)
                self?.isRefreshingNutrition = (newState as? CheckInDetailUiStateReady)?.isRefreshingNutrition == true
                self?.isPushingToHevy = (newState as? CheckInDetailUiStateReady)?.isPushingToHevy == true
            }
        }

        effectTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<CheckInDetailEffect> = observeFlow(sharedVm.effects)
            for await effect in stream {
                self?.handleEffect(effect)
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

    func consumeNavigateBack() { navigateBack = false }
    func consumeNavigateToWizard() { navigateToWizardCheckInId = nil }

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
                includeTrainingSummary: options.includeTrainingSummary,
                includeMuscleVolume: options.includeMuscleVolume,
                includeWorkoutDetails: options.includeWorkoutDetails
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
        } else if effect is CheckInDetailEffectRequireHevyApiKey {
            requiresHevyApiKey = true
        } else if let succeeded = effect as? CheckInDetailEffectHevyPushSucceeded {
            hevyPushSuccessDate = succeeded.date
        } else if let conflict = effect as? CheckInDetailEffectHevyPushConflict {
            hevyPushConflictDate = conflict.date
        }
    }

    private static func mapState(_ s: CheckInDetailUiState) -> CheckInDetailState {
        if s is CheckInDetailUiStateLoading { return .loading }
        guard let ready = s as? CheckInDetailUiStateReady else { return .loading }
        return .ready(
            checkIn: mapCheckIn(ready.checkIn),
            delta: ready.delta.map(mapDelta),
            photoUrls: ready.photoUrls,
            trainingStats: ready.trainingStats.map(mapTrainingStats),
            isLoadingTraining: ready.isLoadingTraining,
            pdfExportOptions: mapPdfOptions(ready.pdfExportOptions)
        )
    }

    private static func mapCheckIn(_ checkIn: CheckIn) -> CheckInData {
        CheckInData(
            id: checkIn.id,
            weekLabel: checkIn.weekLabel,
            coveredDates: checkIn.coveredDates,
            weight: checkIn.weight?.doubleValue,
            shoulders: checkIn.shoulders?.doubleValue,
            chest: checkIn.chest?.doubleValue,
            armLeft: checkIn.armLeft?.doubleValue,
            armRight: checkIn.armRight?.doubleValue,
            waist: checkIn.waist?.doubleValue,
            hips: checkIn.hips?.doubleValue,
            thighLeft: checkIn.thighLeft?.doubleValue,
            thighRight: checkIn.thighRight?.doubleValue,
            feelingTags: checkIn.feelingTags,
            mentalFeeling: checkIn.mentalFeeling,
            physicalFeeling: checkIn.physicalFeeling,
            nutritionSummary: checkIn.nutritionSummary.map(mapNutrition),
            photos: checkIn.photos.map { photo in
                CheckInPhotoData(id: photo.id, storagePath: photo.storagePath, position: Int(photo.position))
            },
            createdAt: checkIn.createdAt,
            updatedAt: checkIn.updatedAt
        )
    }

    private static func mapDelta(_ delta: CheckInDelta) -> CheckInDeltaData {
        CheckInDeltaData(
            weight: delta.weight?.doubleValue,
            shoulders: delta.shoulders?.doubleValue,
            chest: delta.chest?.doubleValue,
            armLeft: delta.armLeft?.doubleValue,
            armRight: delta.armRight?.doubleValue,
            waist: delta.waist?.doubleValue,
            hips: delta.hips?.doubleValue,
            thighLeft: delta.thighLeft?.doubleValue,
            thighRight: delta.thighRight?.doubleValue
        )
    }

    private static func mapTrainingStats(_ stats: WeeklyTrainingStats) -> WeeklyTrainingStatsData {
        WeeklyTrainingStatsData(
            totalSessions: Int(stats.totalSessions),
            totalDurationMinutes: Int(stats.totalDurationMinutes),
            totalVolumeKg: stats.totalVolumeKg,
            avgRpe: stats.avgRpe?.doubleValue,
            muscleGroupVolume: Dictionary(
                uniqueKeysWithValues: stats.muscleGroupVolume.map { ($0.key, $0.value.doubleValue) }
            ),
            workouts: stats.workouts.map { w in
                HevyWorkoutData(
                    id: w.id,
                    title: w.title,
                    date: w.date,
                    durationMinutes: Int(w.durationMinutes),
                    totalVolumeKg: w.totalVolumeKg,
                    exercises: w.exercises.map { ex in
                        HevyWorkoutExerciseData(
                            name: ex.name,
                            muscleGroup: ex.muscleGroup,
                            sets: ex.sets.map { s in
                                HevyWorkoutSetData(
                                    type: s.type,
                                    weightKg: s.weightKg?.doubleValue,
                                    reps: s.reps.map { Int($0.int32Value) },
                                    durationSeconds: s.durationSeconds.map { Int($0.int32Value) },
                                    rpe: s.rpe?.doubleValue
                                )
                            }
                        )
                    }
                )
            }
        )
    }

    private static func mapPdfOptions(_ options: PdfExportOptions) -> PdfExportConfig {
        PdfExportConfig(
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
            includeTrainingSummary: options.includeTrainingSummary,
            includeMuscleVolume: options.includeMuscleVolume,
            includeWorkoutDetails: options.includeWorkoutDetails
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
            dailyData: n.dailyData.map { daily in
                DailyNutritionData(
                    date: daily.date,
                    calories: daily.calories,
                    protein: daily.protein,
                    fat: daily.fat,
                    carbs: daily.carbs,
                    waterMl: Int(daily.waterMl)
                )
            }
        )
    }
}
