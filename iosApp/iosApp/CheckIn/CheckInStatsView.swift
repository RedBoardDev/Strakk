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
        ScrollView {
            VStack(alignment: .leading, spacing: StrakkSpacing.xl) {
                // Period picker
                periodPicker(selectedPeriod: selectedPeriod)

                // Weight chart
                let weightPoints = filteredSeries.compactMap { s -> (String, Double)? in
                    guard let v = s.weight else { return nil }
                    return (s.weekLabel, v)
                }
                if weightPoints.count >= 2 {
                    chartCard(
                        title: "WEIGHT",
                        unit: "kg",
                        color: .strakkPrimary,
                        points: weightPoints,
                        trend: weightTrend
                    )
                }

                // Waist chart
                let waistPoints = filteredSeries.compactMap { s -> (String, Double)? in
                    guard let v = s.waist else { return nil }
                    return (s.weekLabel, v)
                }
                if waistPoints.count >= 2 {
                    chartCard(
                        title: "WAIST",
                        unit: "cm",
                        color: .strakkWarning,
                        points: waistPoints,
                        trend: waistTrend
                    )
                }

                // Arms chart (avg L+R)
                let armPoints = filteredSeries.compactMap { s -> (String, Double)? in
                    guard let l = s.armLeft, let r = s.armRight else { return nil }
                    return (s.weekLabel, (l + r) / 2)
                }
                if armPoints.count >= 2 {
                    chartCard(
                        title: "ARMS (AVG.)",
                        unit: "cm",
                        color: .strakkSuccess,
                        points: armPoints,
                        trend: nil
                    )
                }

                // Thighs chart (avg L+R)
                let thighPoints = filteredSeries.compactMap { s -> (String, Double)? in
                    guard let l = s.thighLeft, let r = s.thighRight else { return nil }
                    return (s.weekLabel, (l + r) / 2)
                }
                if thighPoints.count >= 2 {
                    chartCard(
                        title: "CUISSES (MOY.)",
                        unit: "cm",
                        color: .strakkPrimary,
                        points: thighPoints,
                        trend: nil
                    )
                }

                // Hips chart
                let hipPoints = filteredSeries.compactMap { s -> (String, Double)? in
                    guard let v = s.hips else { return nil }
                    return (s.weekLabel, v)
                }
                if hipPoints.count >= 2 {
                    chartCard(
                        title: "HANCHES",
                        unit: "cm",
                        color: .strakkAccentBlue,
                        points: hipPoints,
                        trend: nil
                    )
                }

                // No data message if nothing to show
                let hasAnyChart = weightPoints.count >= 2 || waistPoints.count >= 2
                    || armPoints.count >= 2 || thighPoints.count >= 2 || hipPoints.count >= 2
                if !hasAnyChart {
                    noDataView
                }

                // Regularity section
                regularityCard(regularity: regularity)
            }
            .padding(.horizontal, StrakkSpacing.lg)
            .padding(.vertical, StrakkSpacing.xl)
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
        title: String,
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
                    ForEach(Array(points.enumerated()), id: \.offset) { index, point in
                        LineMark(
                            x: .value("Semaine", abbreviatedLabel(point.0)),
                            y: .value("Valeur", point.1)
                        )
                        .foregroundStyle(color)
                        .interpolationMethod(.catmullRom)

                        PointMark(
                            x: .value("Semaine", abbreviatedLabel(point.0)),
                            y: .value("Valeur", point.1)
                        )
                        .foregroundStyle(color)
                        .symbolSize(30)
                    }
                }
                .chartXAxis {
                    AxisMarks(values: .automatic) { value in
                        AxisValueLabel()
                            .font(.strakkCaption)
                            .foregroundStyle(Color.strakkTextTertiary)
                    }
                }
                .chartYAxis {
                    AxisMarks(position: .leading, values: .automatic(desiredCount: 4)) { value in
                        AxisGridLine()
                            .foregroundStyle(Color.strakkDivider)
                        AxisValueLabel()
                            .font(.strakkCaption)
                            .foregroundStyle(Color.strakkTextTertiary)
                    }
                }
                .frame(height: 160)
                .chartBackground { _ in Color.clear }

                // Trend info
                if let trend {
                    trendLabel(trend: trend, unit: unit)
                }
            }
            .padding(StrakkSpacing.md)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    @ViewBuilder
    private func trendLabel(trend: TrendInfoData, unit: String) -> some View {
        let delta = trend.delta
        let sign = delta >= 0 ? "+" : ""
        let arrow = delta > 0 ? "↑" : (delta < 0 ? "↓" : "=")

        HStack(spacing: StrakkSpacing.xxs) {
            Image(systemName: "arrow.trend.up")
                .font(.system(size: 12))
                .foregroundStyle(Color.strakkTextTertiary)
            Text("Tendance : \(arrow) \(sign)\(String(format: "%.1f", delta)) \(unit) sur \(trend.weeks) sem.")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextSecondary)
        }
    }

    // MARK: - No data

    private var noDataView: some View {
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

    // MARK: - Regularity card

    @ViewBuilder
    private func regularityCard(regularity: RegularityInfoData) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("CONSISTENCY")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
                HStack {
                    Text("\(regularity.checkInCount)/\(regularity.totalWeeks) semaines")
                        .font(.strakkBodyBold)
                        .foregroundStyle(Color.strakkTextPrimary)
                    Spacer()
                    Text("\(regularity.percentage)%")
                        .font(.strakkHeading3)
                        .foregroundStyle(Color.strakkPrimary)
                }

                // Progress bar
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
                .accessibilityLabel("Consistency : \(regularity.percentage)%")
            }
            .padding(StrakkSpacing.md)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    // MARK: - Helpers

    private func abbreviatedLabel(_ weekLabel: String) -> String {
        // "2026-W17" → "S17"
        let parts = weekLabel.split(separator: "-W")
        if parts.count == 2, let week = parts.last {
            return "S\(week)"
        }
        return weekLabel
    }
}

// MARK: - Preview

#Preview {
    NavigationStack {
        CheckInStatsView()
    }
}
