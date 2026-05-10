import SwiftUI
import Charts
import shared

// MARK: - CheckInStatsView

struct CheckInStatsView: View {
    @State private var vm = CheckInStatsViewModelWrapper()

    var body: some View {
        ZStack {
            Color.strakkBackground.ignoresSafeArea()

            switch vm.state {
            case .loading:
                ProgressView()
                    .tint(Color.strakkPrimary)

            case .ready(
                let selectedPeriod,
                _,
                let filteredSeries,
                let weightTrend,
                let waistTrend,
                let regularity
            ):
                mainContent(
                    selectedPeriod: selectedPeriod,
                    filteredSeries: filteredSeries,
                    weightTrend: weightTrend,
                    waistTrend: waistTrend,
                    regularity: regularity
                )
            }
        }
        .navigationTitle("Trends")
        .navigationBarTitleDisplayMode(.large)
    }

    // MARK: - Main content

    @ViewBuilder
    private func mainContent(
        selectedPeriod: CheckInStatsPeriod,
        filteredSeries: [SeriesPointData],
        weightTrend: TrendInfoData?,
        waistTrend: TrendInfoData?,
        regularity: RegularityInfoData
    ) -> some View {
        let chartData = buildChartData(from: filteredSeries)
        ScrollView {
            VStack(alignment: .leading, spacing: StrakkSpacing.xl) {
                periodPicker(selectedPeriod: selectedPeriod)
                chartCards(chartData: chartData, weightTrend: weightTrend, waistTrend: waistTrend)
                if !chartData.hasAny { StatsNoDataView() }
                RegularityCardView(regularity: regularity)
            }
            .padding(.horizontal, StrakkSpacing.lg)
            .padding(.vertical, StrakkSpacing.xl)
        }
    }

    private struct ChartData {
        let weight: [(String, Double)]
        let waist: [(String, Double)]
        let arms: [(String, Double)]
        let thighs: [(String, Double)]
        let hips: [(String, Double)]

        var hasAny: Bool {
            weight.count >= 2 || waist.count >= 2 || arms.count >= 2
                || thighs.count >= 2 || hips.count >= 2
        }
    }

    private func buildChartData(from series: [SeriesPointData]) -> ChartData {
        let weight = series.compactMap { s -> (String, Double)? in
            guard let w = s.weight else { return nil }
            return (s.weekLabel, w)
        }
        let waist = series.compactMap { s -> (String, Double)? in
            guard let w = s.waist else { return nil }
            return (s.weekLabel, w)
        }
        let arms = series.compactMap { s -> (String, Double)? in
            guard let left = s.armLeft, let right = s.armRight else { return nil }
            return (s.weekLabel, (left + right) / 2)
        }
        let thighs = series.compactMap { s -> (String, Double)? in
            guard let left = s.thighLeft, let right = s.thighRight else { return nil }
            return (s.weekLabel, (left + right) / 2)
        }
        let hips = series.compactMap { s -> (String, Double)? in
            guard let h = s.hips else { return nil }
            return (s.weekLabel, h)
        }
        return ChartData(weight: weight, waist: waist, arms: arms, thighs: thighs, hips: hips)
    }

    @ViewBuilder
    private func chartCards(
        chartData: ChartData,
        weightTrend: TrendInfoData?,
        waistTrend: TrendInfoData?
    ) -> some View {
        if chartData.weight.count >= 2 {
            chartCard(
                title: "WEIGHT",
                unit: "kg",
                color: .strakkPrimary,
                points: chartData.weight,
                trend: weightTrend
            )
        }
        if chartData.waist.count >= 2 {
            chartCard(
                title: "WAIST",
                unit: "cm",
                color: .strakkWarning,
                points: chartData.waist,
                trend: waistTrend
            )
        }
        if chartData.arms.count >= 2 {
            chartCard(
                title: "ARMS (AVG.)",
                unit: "cm",
                color: .strakkSuccess,
                points: chartData.arms,
                trend: nil
            )
        }
        if chartData.thighs.count >= 2 {
            chartCard(
                title: "THIGHS (AVG.)",
                unit: "cm",
                color: .strakkPrimary,
                points: chartData.thighs,
                trend: nil
            )
        }
        if chartData.hips.count >= 2 {
            chartCard(
                title: "HIPS",
                unit: "cm",
                color: .strakkAccentBlue,
                points: chartData.hips,
                trend: nil
            )
        }
    }

    // MARK: - Period picker

    @ViewBuilder
    private func periodPicker(selectedPeriod: CheckInStatsPeriod) -> some View {
        Picker("Period", selection: Binding(
            get: { selectedPeriod },
            set: { period in
                switch period {
                case .fourWeeks:
                    vm.onEvent(CheckInStatsEventOnPeriodSelected(period: StatsPeriod.fourweeks))
                case .twelveWeeks:
                    vm.onEvent(CheckInStatsEventOnPeriodSelected(period: StatsPeriod.twelveweeks))
                case .all:
                    vm.onEvent(CheckInStatsEventOnPeriodSelected(period: StatsPeriod.all))
                }
            }
        )) {
            Text("4 wk").tag(CheckInStatsPeriod.fourWeeks)
            Text("12 wk").tag(CheckInStatsPeriod.twelveWeeks)
            Text("All").tag(CheckInStatsPeriod.all)
        }
        .pickerStyle(.segmented)
        .accessibilityLabel("Display period")
    }

    // MARK: - Chart card

    @ViewBuilder
    private func chartCard(
        title: LocalizedStringKey,
        unit: String,
        color: Color,
        points: [(String, Double)],
        trend: TrendInfoData?
    ) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
            Text(title)
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
                Chart {
                    ForEach(Array(points.enumerated()), id: \.offset) { _, point in
                        LineMark(
                            x: .value("Week", abbreviatedLabel(point.0)),
                            y: .value("Value", point.1)
                        )
                        .foregroundStyle(color)
                        .interpolationMethod(.catmullRom)

                        PointMark(
                            x: .value("Week", abbreviatedLabel(point.0)),
                            y: .value("Value", point.1)
                        )
                        .foregroundStyle(color)
                        .symbolSize(30)
                    }
                }
                .chartXAxis {
                    AxisMarks(values: .automatic) { _ in
                        AxisValueLabel()
                            .font(.strakkCaption)
                            .foregroundStyle(Color.strakkTextTertiary)
                    }
                }
                .chartYAxis {
                    AxisMarks(position: .leading, values: .automatic(desiredCount: 4)) { _ in
                        AxisGridLine()
                            .foregroundStyle(Color.strakkDivider)
                        AxisValueLabel()
                            .font(.strakkCaption)
                            .foregroundStyle(Color.strakkTextTertiary)
                    }
                }
                .frame(height: 160)
                .chartBackground { _ in Color.clear }

                if let trend {
                    TrendLabelView(trend: trend, unit: unit)
                }
            }
            .padding(StrakkSpacing.md)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    // MARK: - Helpers

    private func abbreviatedLabel(_ weekLabel: String) -> String {
        // "2026-W17" → "W17"
        let parts = weekLabel.split(separator: "-W")
        if parts.count == 2, let week = parts.last {
            return String(localized: "W\(week)")
        }
        return weekLabel
    }
}

// MARK: - TrendLabelView

private struct TrendLabelView: View {
    let trend: TrendInfoData
    let unit: String

    var body: some View {
        let delta = trend.delta
        let sign = delta >= 0 ? "+" : ""
        let arrow = delta > 0 ? "↑" : (delta < 0 ? "↓" : "=")
        let value = String(format: "%.1f", delta)
        let trendText = String.localizedStringWithFormat(
            String(localized: "Trend: %@ %@%@ %@ over %@ wk"),
            arrow,
            sign,
            value,
            unit,
            String(trend.weeks)
        )
        HStack(spacing: StrakkSpacing.xxs) {
            Image(systemName: "arrow.trend.up")
                .font(.system(size: 12))
                .foregroundStyle(Color.strakkTextTertiary)
            Text(trendText)
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextSecondary)
        }
    }
}

// MARK: - StatsNoDataView

private struct StatsNoDataView: View {
    var body: some View {
        VStack(spacing: StrakkSpacing.sm) {
            Image(systemName: "chart.xyaxis.line")
                .font(.system(size: 36))
                .foregroundStyle(Color.strakkTextTertiary)
            Text("Not enough data to show trends.")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(StrakkSpacing.xxl)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - RegularityCardView

private struct RegularityCardView: View {
    let regularity: RegularityInfoData

    var body: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("CONSISTENCY")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
                HStack {
                    Text(weeksLabel)
                        .font(.strakkBodyBold)
                        .foregroundStyle(Color.strakkTextPrimary)
                    Spacer()
                    Text("\(regularity.percentage)%")
                        .font(.strakkHeading3)
                        .foregroundStyle(Color.strakkPrimary)
                }
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Capsule()
                            .fill(Color.strakkSurface2)
                            .frame(height: 8)
                        Capsule()
                            .fill(Color.strakkPrimary)
                            .frame(
                                width: geo.size.width * CGFloat(regularity.percentage) / 100,
                                height: 8
                            )
                    }
                }
                .frame(height: 8)
                .accessibilityLabel(accessibilityLabel)
            }
            .padding(StrakkSpacing.md)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    private var weeksLabel: String {
        String.localizedStringWithFormat(
            String(localized: "%@/%@ weeks"),
            String(regularity.checkInCount),
            String(regularity.totalWeeks)
        )
    }

    private var accessibilityLabel: String {
        String.localizedStringWithFormat(
            String(localized: "Consistency: %@%%"),
            String(regularity.percentage)
        )
    }
}

// MARK: - Preview

#Preview {
    NavigationStack {
        CheckInStatsView()
    }
}
