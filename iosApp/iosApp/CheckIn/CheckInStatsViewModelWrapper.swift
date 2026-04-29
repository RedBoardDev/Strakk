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

struct RegularityInfoData: Equatable {
    let checkInCount: Int
    let totalWeeks: Int
    let percentage: Int
}

enum CheckInStatsState {
    case loading
    case ready(
        selectedPeriod: CheckInStatsPeriod,
        series: [SeriesPointData],
        filteredSeries: [SeriesPointData],
        weightTrend: TrendInfoData?,
        waistTrend: TrendInfoData?,
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
        self.sharedVm = KoinHelper().getCheckInStatsViewModel()

        stateTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<CheckInStatsUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                await MainActor.run { self?.state = Self.mapState(newState) }
            }
        }
    }

    deinit {
        stateTask?.cancel()
    }

    func onEvent(_ event: CheckInStatsEvent) {
        sharedVm.onEvent(event: event)
    }

    // MARK: - Private

    private static func mapState(_ s: CheckInStatsUiState) -> CheckInStatsState {
        if s is CheckInStatsUiStateLoading { return .loading }
        guard let ready = s as? CheckInStatsUiStateReady else { return .loading }

        let period = mapPeriod(ready.selectedPeriod)
        let series = ready.series.map(mapPoint)
        let filteredSeries = ready.filteredSeries.map(mapPoint)
        let weightTrend = ready.weightTrend.map { t in TrendInfoData(delta: t.delta, weeks: Int(t.weeks)) }
        let waistTrend = ready.waistTrend.map { t in TrendInfoData(delta: t.delta, weeks: Int(t.weeks)) }
        let regularity = RegularityInfoData(
            checkInCount: Int(ready.regularity.checkInCount),
            totalWeeks: Int(ready.regularity.totalWeeks),
            percentage: Int(ready.regularity.percentage)
        )

        return .ready(
            selectedPeriod: period,
            series: series,
            filteredSeries: filteredSeries,
            weightTrend: weightTrend,
            waistTrend: waistTrend,
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

    private static func mapPoint(_ p: CheckInSeriesPoint) -> SeriesPointData {
        SeriesPointData(
            weekLabel: p.weekLabel,
            weight: p.weight?.doubleValue,
            shoulders: p.shoulders?.doubleValue,
            chest: p.chest?.doubleValue,
            armLeft: p.armLeft?.doubleValue,
            armRight: p.armRight?.doubleValue,
            waist: p.waist?.doubleValue,
            hips: p.hips?.doubleValue,
            thighLeft: p.thighLeft?.doubleValue,
            thighRight: p.thighRight?.doubleValue
        )
    }
}
