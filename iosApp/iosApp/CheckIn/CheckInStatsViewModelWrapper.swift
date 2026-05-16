import SwiftUI
import shared

// MARK: - Swift-side data types

enum CheckInStatsPeriod: Equatable {
    case fourWeeks
    case twelveWeeks
    case all
}

struct SeriesPointData: Identifiable, Equatable {
    var id: String { weekLabel }
    let weekLabel: String
    let weight: Double?
    let shoulders: Double?
    let chest: Double?
    let armLeft: Double?
    let armRight: Double?
    let waist: Double?
    let hips: Double?
    let thighLeft: Double?
    let thighRight: Double?
}

struct TrendInfoData: Equatable {
    let delta: Double
    let weeks: Int
}

struct OverviewCardsData: Equatable {
    let weightKg: Double?
    let weightTrend: TrendInfoData?
    let avgWeeklyVolumeKg: Double?
    let avgSessionsPerWeek: Double?
    let nutritionCompliancePct: Int?
}

struct WeightPointData: Identifiable, Equatable {
    var id: String { weekLabel }
    let weekLabel: String
    let weightKg: Double
}

struct BodySnapshotData: Equatable {
    let weight: Double?
    let shoulders: Double?
    let chest: Double?
    let armLeft: Double?
    let armRight: Double?
    let waist: Double?
    let hips: Double?
    let thighLeft: Double?
    let thighRight: Double?
    let weekLabel: String
}

struct TrainingStatsData: Equatable {
    let volumeSeries: [(weekLabel: String, volumeKg: Double)]
    let sessionsSeries: [(weekLabel: String, sessions: Int)]
    let totalVolumeKg: Double
    let totalSessions: Int
    let totalDurationMin: Int

    static func == (lhs: TrainingStatsData, rhs: TrainingStatsData) -> Bool {
        lhs.totalVolumeKg == rhs.totalVolumeKg
            && lhs.totalSessions == rhs.totalSessions
            && lhs.totalDurationMin == rhs.totalDurationMin
    }
}

struct MacroComplianceData: Identifiable, Equatable {
    var id: String { name }
    let name: String
    let avg: Double
    let goal: Double
    let percentage: Int
}

struct NutritionStatsData: Equatable {
    let macros: [MacroComplianceData]
    let avgWater: Int?
    let waterGoal: Int?
}

struct RegularityInfoData: Equatable {
    let checkInCount: Int
    let totalWeeks: Int
    let percentage: Int
}

enum CheckInStatsState {
    case loading
    case ready(
        selectedPeriod: CheckInStatsPeriod,
        allSeries: [SeriesPointData],
        overview: OverviewCardsData,
        weightSeries: [WeightPointData],
        bodySnapshot: BodySnapshotData?,
        training: TrainingStatsData?,
        nutrition: NutritionStatsData?,
        regularity: RegularityInfoData
    )
}

// MARK: - Wrapper

@MainActor
@Observable
final class CheckInStatsViewModelWrapper {
    private let sharedVm: CheckInStatsViewModel

    var state: CheckInStatsState = .loading

    @ObservationIgnored private var stateTask: Task<Void, Never>?

    init() {
        self.sharedVm = KoinBridge.shared.getCheckInStatsViewModel()

        stateTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<CheckInStatsUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                self?.state = Self.mapState(newState)
            }
        }
    }

    deinit {
        stateTask?.cancel()
    }

    func onEvent(_ event: CheckInStatsEvent) {
        sharedVm.onEvent(event: event)
    }

    // MARK: - Private mapping

    private static func mapState(_ s: CheckInStatsUiState) -> CheckInStatsState {
        if s is CheckInStatsUiStateLoading { return .loading }
        guard let ready = s as? CheckInStatsUiStateReady else { return .loading }

        let period = mapPeriod(ready.selectedPeriod)
        let allSeries = ready.allSeries.map(mapSeriesPoint)
        let overview = mapOverview(ready.overview)
        let weightSeries = ready.weightSeries.map { point in
            WeightPointData(weekLabel: point.weekLabel, weightKg: point.weightKg)
        }
        let bodySnapshot = ready.bodySnapshot.map(mapBodySnapshot)
        let training = ready.training.map(mapTraining)
        let nutrition = ready.nutrition.map(mapNutrition)
        let regularity = mapRegularity(ready.regularity)

        return .ready(
            selectedPeriod: period,
            allSeries: allSeries,
            overview: overview,
            weightSeries: weightSeries,
            bodySnapshot: bodySnapshot,
            training: training,
            nutrition: nutrition,
            regularity: regularity
        )
    }

    private static func mapPeriod(_ period: StatsPeriod) -> CheckInStatsPeriod {
        switch period {
        case StatsPeriod.fourweeks: return .fourWeeks
        case StatsPeriod.twelveweeks: return .twelveWeeks
        case StatsPeriod.all: return .all
        default: return .fourWeeks
        }
    }

    private static func mapSeriesPoint(_ point: CheckInSeriesPoint) -> SeriesPointData {
        SeriesPointData(
            weekLabel: point.weekLabel,
            weight: point.weight?.doubleValue,
            shoulders: point.shoulders?.doubleValue,
            chest: point.chest?.doubleValue,
            armLeft: point.armLeft?.doubleValue,
            armRight: point.armRight?.doubleValue,
            waist: point.waist?.doubleValue,
            hips: point.hips?.doubleValue,
            thighLeft: point.thighLeft?.doubleValue,
            thighRight: point.thighRight?.doubleValue
        )
    }

    private static func mapOverview(_ overview: OverviewCards) -> OverviewCardsData {
        let weightTrend = overview.weightTrend.map { trend in
            TrendInfoData(delta: trend.delta, weeks: Int(trend.weeks))
        }
        return OverviewCardsData(
            weightKg: overview.weightKg?.doubleValue,
            weightTrend: weightTrend,
            avgWeeklyVolumeKg: overview.avgWeeklyVolumeKg?.doubleValue,
            avgSessionsPerWeek: overview.avgSessionsPerWeek?.doubleValue,
            nutritionCompliancePct: overview.nutritionCompliancePct.map { Int($0.int32Value) }
        )
    }

    private static func mapBodySnapshot(_ snapshot: BodySnapshot) -> BodySnapshotData {
        let ms = snapshot.measurements
        return BodySnapshotData(
            weight: ms.weight?.doubleValue,
            shoulders: ms.shoulders?.doubleValue,
            chest: ms.chest?.doubleValue,
            armLeft: ms.armLeft?.doubleValue,
            armRight: ms.armRight?.doubleValue,
            waist: ms.waist?.doubleValue,
            hips: ms.hips?.doubleValue,
            thighLeft: ms.thighLeft?.doubleValue,
            thighRight: ms.thighRight?.doubleValue,
            weekLabel: snapshot.weekLabel
        )
    }

    private static func mapTraining(_ training: TrainingStats) -> TrainingStatsData {
        let volumeSeries = training.volumeSeries.map { pair in
            (weekLabel: pair.first as? String ?? "", volumeKg: pair.second as? Double ?? 0.0)
        }
        let sessionsSeries = training.sessionsSeries.map { pair in
            (weekLabel: pair.first as? String ?? "", sessions: pair.second as? Int ?? 0)
        }
        return TrainingStatsData(
            volumeSeries: volumeSeries,
            sessionsSeries: sessionsSeries,
            totalVolumeKg: training.totalVolumeKg,
            totalSessions: Int(training.totalSessions),
            totalDurationMin: Int(training.totalDurationMin)
        )
    }

    private static func mapNutrition(_ nutrition: NutritionStats) -> NutritionStatsData {
        let macros = nutrition.macros.map { macro in
            MacroComplianceData(
                name: macro.name,
                avg: macro.avg,
                goal: macro.goal,
                percentage: Int(macro.percentage)
            )
        }
        return NutritionStatsData(
            macros: macros,
            avgWater: nutrition.avgWater.map { Int($0.int32Value) },
            waterGoal: nutrition.waterGoal.map { Int($0.int32Value) }
        )
    }

    private static func mapRegularity(_ regularity: RegularityInfo) -> RegularityInfoData {
        RegularityInfoData(
            checkInCount: Int(regularity.checkInCount),
            totalWeeks: Int(regularity.totalWeeks),
            percentage: Int(regularity.percentage)
        )
    }
}
