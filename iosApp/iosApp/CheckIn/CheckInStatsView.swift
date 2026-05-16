// swiftlint:disable file_length type_body_length
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
                let overview,
                let weightSeries,
                _,
                let training,
                let nutrition,
                let regularity
            ):
                mainContent(
                    selectedPeriod: selectedPeriod,
                    overview: overview,
                    weightSeries: weightSeries,
                    training: training,
                    nutrition: nutrition,
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
        overview: OverviewCardsData,
        weightSeries: [WeightPointData],
        training: TrainingStatsData?,
        nutrition: NutritionStatsData?,
        regularity: RegularityInfoData
    ) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: StrakkSpacing.xl) {
                periodPicker(selectedPeriod: selectedPeriod)
                overviewGrid(overview: overview)
                if weightSeries.count >= 2 {
                    weightChartSection(points: weightSeries)
                }
                if let training {
                    trainingSection(training: training)
                }
                if let nutrition {
                    nutritionSection(nutrition: nutrition)
                }
                consistencyCard(regularity: regularity)
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

    // MARK: - Overview 2×2 grid

    @ViewBuilder
    private func overviewGrid(overview: OverviewCardsData) -> some View {
        let columns = [
            GridItem(.flexible(), spacing: StrakkSpacing.sm),
            GridItem(.flexible(), spacing: StrakkSpacing.sm)
        ]
        LazyVGrid(columns: columns, spacing: StrakkSpacing.sm) {
            overviewWeightCard(overview: overview)
            overviewVolumeCard(overview: overview)
            overviewFrequencyCard(overview: overview)
            overviewNutritionCard(overview: overview)
        }
    }

    @ViewBuilder
    private func overviewWeightCard(overview: OverviewCardsData) -> some View {
        OverviewCard(
            icon: "scalemass.fill",
            iconColor: .strakkPrimary,
            label: "Weight"
        ) {
            if let kg = overview.weightKg {
                VStack(alignment: .leading, spacing: StrakkSpacing.xxs) {
                    Text(String(format: "%.1f kg", kg))
                        .font(.strakkHeading2)
                        .foregroundStyle(Color.strakkTextPrimary)
                        .monospacedDigit()
                    if let trend = overview.weightTrend {
                        weightTrendLabel(trend: trend)
                    }
                }
            } else {
                Text("—")
                    .font(.strakkHeading2)
                    .foregroundStyle(Color.strakkTextTertiary)
            }
        }
    }

    @ViewBuilder
    private func overviewVolumeCard(overview: OverviewCardsData) -> some View {
        OverviewCard(
            icon: "dumbbell.fill",
            iconColor: .strakkAccentIndigo,
            label: "Avg volume / wk"
        ) {
            if let vol = overview.avgWeeklyVolumeKg {
                Text(formatVolume(vol))
                    .font(.strakkHeading2)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .monospacedDigit()
            } else {
                Text("—")
                    .font(.strakkHeading2)
                    .foregroundStyle(Color.strakkTextTertiary)
            }
        }
    }

    @ViewBuilder
    private func overviewFrequencyCard(overview: OverviewCardsData) -> some View {
        OverviewCard(
            icon: "figure.strengthtraining.traditional",
            iconColor: .strakkSuccess,
            label: "Sessions / wk"
        ) {
            if let sessions = overview.avgSessionsPerWeek {
                Text(String(format: "%.1f", sessions))
                    .font(.strakkHeading2)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .monospacedDigit()
            } else {
                Text("—")
                    .font(.strakkHeading2)
                    .foregroundStyle(Color.strakkTextTertiary)
            }
        }
    }

    @ViewBuilder
    private func overviewNutritionCard(overview: OverviewCardsData) -> some View {
        OverviewCard(
            icon: "fork.knife",
            iconColor: .strakkWarning,
            label: "Nutrition"
        ) {
            if let compliance = overview.nutritionCompliancePct {
                HStack(alignment: .center, spacing: StrakkSpacing.xs) {
                    Text("\(compliance)%")
                        .font(.strakkHeading2)
                        .foregroundStyle(Color.strakkTextPrimary)
                        .monospacedDigit()
                    ComplianceGauge(percentage: compliance)
                }
            } else {
                Text("—")
                    .font(.strakkHeading2)
                    .foregroundStyle(Color.strakkTextTertiary)
            }
        }
    }

    @ViewBuilder
    private func weightTrendLabel(trend: TrendInfoData) -> some View {
        let isGain = trend.delta > 0
        let isNeutral = trend.delta == 0
        let color: Color = isNeutral ? .strakkTextTertiary : (isGain ? .strakkError : .strakkSuccess)
        let arrow = isNeutral ? "" : (isGain ? "↑" : "↓")
        let sign = trend.delta > 0 ? "+" : ""
        HStack(spacing: 2) {
            Text(arrow)
                .font(.strakkCaption)
                .foregroundStyle(color)
            Text(String(format: "%@%.1f kg", sign, trend.delta))
                .font(.strakkCaption)
                .foregroundStyle(color)
                .monospacedDigit()
        }
    }

    // MARK: - Weight chart

    @ViewBuilder
    private func weightChartSection(points: [WeightPointData]) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
            Text("WEIGHT")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            VStack(alignment: .leading, spacing: 0) {
                Chart {
                    ForEach(points) { point in
                        AreaMark(
                            x: .value("Week", abbreviatedLabel(point.weekLabel)),
                            y: .value("kg", point.weightKg)
                        )
                        .foregroundStyle(
                            LinearGradient(
                                colors: [Color.strakkAccentOrange.opacity(0.25), Color.clear],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .interpolationMethod(.catmullRom)

                        LineMark(
                            x: .value("Week", abbreviatedLabel(point.weekLabel)),
                            y: .value("kg", point.weightKg)
                        )
                        .foregroundStyle(Color.strakkAccentOrange)
                        .interpolationMethod(.catmullRom)
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
            }
            .padding(StrakkSpacing.md)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
        }
    }

    // MARK: - Training section

    @ViewBuilder
    private func trainingSection(training: TrainingStatsData) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
            Text("TRAINING")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            VStack(alignment: .leading, spacing: StrakkSpacing.md) {
                if training.volumeSeries.count >= 2 {
                    trainingVolumeChart(series: training.volumeSeries)
                }
                trainingSummaryRow(training: training)
            }
            .padding(StrakkSpacing.md)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
        }
    }

    @ViewBuilder
    private func trainingVolumeChart(series: [(weekLabel: String, volumeKg: Double)]) -> some View {
        Chart {
            ForEach(Array(series.enumerated()), id: \.offset) { _, point in
                AreaMark(
                    x: .value("Week", abbreviatedLabel(point.weekLabel)),
                    y: .value("Volume", point.volumeKg)
                )
                .foregroundStyle(
                    LinearGradient(
                        colors: [Color.strakkAccentIndigo.opacity(0.25), Color.clear],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
                .interpolationMethod(.catmullRom)

                LineMark(
                    x: .value("Week", abbreviatedLabel(point.weekLabel)),
                    y: .value("Volume", point.volumeKg)
                )
                .foregroundStyle(Color.strakkAccentIndigo)
                .interpolationMethod(.catmullRom)
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
            AxisMarks(position: .leading, values: .automatic(desiredCount: 3)) { _ in
                AxisGridLine()
                    .foregroundStyle(Color.strakkDivider)
                AxisValueLabel()
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextTertiary)
            }
        }
        .frame(height: 120)
        .chartBackground { _ in Color.clear }
    }

    @ViewBuilder
    private func trainingSummaryRow(training: TrainingStatsData) -> some View {
        HStack(spacing: 0) {
            StatInlineItem(
                value: "\(training.totalSessions)",
                label: "Sessions"
            )
            Divider()
                .frame(height: 32)
                .background(Color.strakkDivider)
            StatInlineItem(
                value: formatDuration(training.totalDurationMin),
                label: "Duration"
            )
            Divider()
                .frame(height: 32)
                .background(Color.strakkDivider)
            StatInlineItem(
                value: formatVolume(training.totalVolumeKg),
                label: "Volume"
            )
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Nutrition section

    @ViewBuilder
    private func nutritionSection(nutrition: NutritionStatsData) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
            Text("NUTRITION")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            VStack(alignment: .leading, spacing: StrakkSpacing.md) {
                ForEach(nutrition.macros) { macro in
                    MacroComplianceRow(macro: macro)
                }
                if let avgWater = nutrition.avgWater, let waterGoal = nutrition.waterGoal, waterGoal > 0 {
                    waterRow(avg: avgWater, goal: waterGoal)
                }
            }
            .padding(StrakkSpacing.md)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
        }
    }

    @ViewBuilder
    private func waterRow(avg: Int, goal: Int) -> some View {
        let fraction = min(Double(avg) / Double(goal), 1.0)
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            HStack {
                HStack(spacing: StrakkSpacing.xxs) {
                    Image(systemName: "drop.fill")
                        .font(.system(size: 12))
                        .foregroundStyle(Color.strakkWater)
                    Text("Water")
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextSecondary)
                }
                Spacer()
                Text(String(format: "%d / %d ml", avg, goal))
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextTertiary)
                    .monospacedDigit()
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(Color.strakkSurface2)
                        .frame(height: 6)
                    Capsule()
                        .fill(Color.strakkWater)
                        .frame(width: geo.size.width * fraction, height: 6)
                }
            }
            .frame(height: 6)
        }
    }

    // MARK: - Consistency card

    @ViewBuilder
    private func consistencyCard(regularity: RegularityInfoData) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("CONSISTENCY")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
                HStack {
                    Text(weeksLabel(regularity: regularity))
                        .font(.strakkBodyBold)
                        .foregroundStyle(Color.strakkTextPrimary)
                    Spacer()
                    Text("\(regularity.percentage)%")
                        .font(.strakkHeading3)
                        .foregroundStyle(Color.strakkPrimary)
                        .monospacedDigit()
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
                .accessibilityLabel(consistencyAccessibilityLabel(regularity: regularity))
            }
            .padding(StrakkSpacing.md)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
        }
    }

    // MARK: - Helpers

    private func abbreviatedLabel(_ weekLabel: String) -> String {
        let parts = weekLabel.split(separator: "-W")
        if parts.count == 2, let week = parts.last {
            return "W\(week)"
        }
        return weekLabel
    }

    private func formatVolume(_ kg: Double) -> String {
        if kg >= 1000 {
            return String(format: "%.1ft", kg / 1000)
        }
        return String(format: "%.0f kg", kg)
    }

    private func formatDuration(_ minutes: Int) -> String {
        let hours = minutes / 60
        let mins = minutes % 60
        if hours > 0 {
            return String(format: "%dh%02d", hours, mins)
        }
        return "\(minutes)m"
    }

    private func weeksLabel(regularity: RegularityInfoData) -> String {
        String.localizedStringWithFormat(
            String(localized: "%@/%@ weeks"),
            String(regularity.checkInCount),
            String(regularity.totalWeeks)
        )
    }

    private func consistencyAccessibilityLabel(regularity: RegularityInfoData) -> String {
        String.localizedStringWithFormat(
            String(localized: "Consistency: %@%%"),
            String(regularity.percentage)
        )
    }
}

// MARK: - OverviewCard

private struct OverviewCard<Content: View>: View {
    let icon: String
    let iconColor: Color
    let label: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
            RoundedRectangle(cornerRadius: 8)
                .fill(iconColor.opacity(0.12))
                .frame(width: 36, height: 36)
                .overlay {
                    Image(systemName: icon)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(iconColor)
                }

            content

            Text(label)
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(StrakkSpacing.md)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
    }
}

// MARK: - ComplianceGauge

private struct ComplianceGauge: View {
    let percentage: Int

    private var fraction: Double { min(Double(percentage) / 100.0, 1.0) }
    private var color: Color { percentage >= 80 ? .strakkSuccess : (percentage >= 50 ? .strakkWarning : .strakkError) }

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color.strakkSurface2, lineWidth: 4)
            Circle()
                .trim(from: 0, to: fraction)
                .stroke(color, style: StrokeStyle(lineWidth: 4, lineCap: .round))
                .rotationEffect(.degrees(-90))
        }
        .frame(width: 28, height: 28)
        .animation(.easeOut(duration: 0.25), value: fraction)
    }
}

// MARK: - MacroComplianceRow

private struct MacroComplianceRow: View {
    let macro: MacroComplianceData

    private var accentColor: Color {
        switch macro.name.lowercased() {
        case "calories": return .strakkPrimaryLight
        case "protein": return .strakkPrimary
        case "carbs": return .strakkAccentIndigo
        case "fat": return .strakkAccentYellow
        default: return .strakkPrimary
        }
    }

    private var fraction: Double { min(Double(macro.percentage) / 100.0, 1.0) }

    var body: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            HStack {
                Text(macro.name)
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextSecondary)
                Spacer()
                Text("\(macro.percentage)%")
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextTertiary)
                    .monospacedDigit()
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(Color.strakkSurface2)
                        .frame(height: 6)
                    Capsule()
                        .fill(macro.percentage >= 100 ? Color.strakkSuccess : accentColor)
                        .frame(width: geo.size.width * fraction, height: 6)
                }
            }
            .frame(height: 6)
        }
    }
}

// MARK: - StatInlineItem

private struct StatInlineItem: View {
    let value: String
    let label: String

    var body: some View {
        VStack(spacing: StrakkSpacing.xxs) {
            Text(value)
                .font(.strakkBodyBold)
                .foregroundStyle(Color.strakkTextPrimary)
                .monospacedDigit()
            Text(label)
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Preview

#Preview {
    NavigationStack {
        CheckInStatsView()
    }
}
