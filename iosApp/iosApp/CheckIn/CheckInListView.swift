import SwiftUI
import shared

// MARK: - CheckInListView

struct CheckInListView: View {
    @State private var vm = CheckInListViewModelWrapper()

    var body: some View {
        NavigationStack {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()

                switch vm.state {
                case .loading:
                    ProgressView()
                        .tint(Color.strakkPrimary)

                case .ready(let checkIns, let quickStats):
                    mainContent(checkIns: checkIns, quickStats: quickStats)
                }
            }
            .navigationTitle("Check-ins")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        vm.onEvent(CheckInListEventOnCreateNew())
                    } label: {
                        Image(systemName: "plus.circle.fill")
                            .font(.title3)
                            .foregroundStyle(Color.strakkPrimary)
                    }
                    .accessibilityLabel("New check-in")
                }
            }
            .navigationDestination(item: $vm.navigateToDetailId) { id in
                CheckInDetailView(checkInId: id)
            }
            .navigationDestination(isPresented: $vm.navigateToStats) {
                CheckInStatsView()
            }
            .fullScreenCover(isPresented: $vm.navigateToWizard) {
                CheckInWizardView(checkInId: nil)
            }
            .featureGate($vm.gatedFeature)
        }
    }

    // MARK: - Main content

    @ViewBuilder
    private func mainContent(checkIns: [CheckInListItemData], quickStats: QuickStatsData?) -> some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: StrakkSpacing.xl) {
                // Quick stats
                if let stats = quickStats {
                    quickStatsSection(stats: stats)
                }

                // Check-in list
                checkInsSection(checkIns: checkIns)
            }
            .padding(.horizontal, StrakkSpacing.lg)
            .padding(.vertical, StrakkSpacing.xl)
        }
    }

    // MARK: - Quick stats section

    @ViewBuilder
    private func quickStatsSection(stats: QuickStatsData) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
            Text("QUICK STATS")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            HStack(spacing: StrakkSpacing.xs) {
                quickStatCard(
                    title: "Weight",
                    value: stats.lastWeight.map { String(format: "%.1f", $0) },
                    unit: "kg",
                    delta: stats.weightDelta
                )
                quickStatCard(
                    title: "Avg. arms",
                    value: stats.lastAvgArm.map { String(format: "%.1f", $0) },
                    unit: "cm",
                    delta: stats.armDelta
                )
                quickStatCard(
                    title: "Waist",
                    value: stats.lastWaist.map { String(format: "%.1f", $0) },
                    unit: "cm",
                    delta: stats.waistDelta
                )
            }

            Button {
                vm.onEvent(CheckInListEventOnOpenStats())
            } label: {
                HStack(spacing: StrakkSpacing.xs) {
                    Text("View detailed stats")
                        .font(.strakkCaptionBold)
                        .foregroundStyle(Color.strakkPrimary)
                    Image(systemName: "chevron.right")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(Color.strakkPrimary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, StrakkSpacing.xxs)
            }
            .accessibilityLabel("View detailed stats")
        }
    }

    @ViewBuilder
    private func quickStatCard(title: String, value: String?, unit: String, delta: Double?) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text(title)
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)
                .lineLimit(1)
                .minimumScaleFactor(0.8)

            if let value {
                HStack(alignment: .firstTextBaseline, spacing: 2) {
                    Text(value)
                        .font(.strakkHeading3)
                        .foregroundStyle(Color.strakkTextPrimary)
                    Text(unit)
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextSecondary)
                }
            } else {
                Text("—")
                    .font(.strakkHeading3)
                    .foregroundStyle(Color.strakkTextTertiary)
            }

            if let delta {
                deltaLabel(delta)
            } else {
                Text("—")
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextTertiary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(StrakkSpacing.sm)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    @ViewBuilder
    private func deltaLabel(_ delta: Double) -> some View {
        if delta == 0 {
            Text("=")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextSecondary)
        } else if delta > 0 {
            Text("↑ +\(String(format: "%.1f", delta))")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextSecondary)
        } else {
            Text("↓ \(String(format: "%.1f", delta))")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextSecondary)
        }
    }

    // MARK: - Check-ins section

    @ViewBuilder
    private func checkInsSection(checkIns: [CheckInListItemData]) -> some View {
        if checkIns.isEmpty {
            emptyState
        } else {
            VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
                Text("RECENT CHECK-INS")
                    .font(.strakkOverline)
                    .foregroundStyle(Color.strakkTextTertiary)

                ForEach(checkIns) { item in
                    checkInCard(item: item)
                }
            }
        }
    }

    @ViewBuilder
    private func checkInCard(item: CheckInListItemData) -> some View {
        Button {
            vm.onEvent(CheckInListEventOnOpenDetail(id: item.id))
        } label: {
            HStack(spacing: StrakkSpacing.sm) {
                VStack(alignment: .leading, spacing: StrakkSpacing.xxs) {
                    Text(weekDisplayLabel(from: item.weekLabel))
                        .font(.strakkBodyBold)
                        .foregroundStyle(Color.strakkTextPrimary)

                    HStack(spacing: StrakkSpacing.sm) {
                        if let weight = item.weight {
                            Label(String(format: "%.1f kg", weight), systemImage: "scalemass")
                                .font(.strakkCaption)
                                .foregroundStyle(Color.strakkTextSecondary)
                        }

                        Label("\(item.photoCount)", systemImage: "camera")
                            .font(.strakkCaption)
                            .foregroundStyle(Color.strakkTextSecondary)

                        if item.hasAiSummary {
                            Label("IA", systemImage: "sparkles")
                                .font(.strakkCaption)
                                .foregroundStyle(Color.strakkPrimary)
                        }
                    }
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(Color.strakkTextTertiary)
            }
            .padding(StrakkSpacing.md)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .accessibilityLabel(checkInAccessibilityLabel(for: item))
    }

    // MARK: - Empty state

    private var emptyState: some View {
        VStack(spacing: StrakkSpacing.md) {
            Image(systemName: "chart.line.uptrend.xyaxis")
                .font(.system(size: 48))
                .foregroundStyle(Color.strakkTextTertiary)

            Text("No check-ins yet")
                .font(.strakkHeading3)
                .foregroundStyle(Color.strakkTextPrimary)

            Text("Start tracking your progress by creating your first weekly check-in.")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
                .multilineTextAlignment(.center)

            Button {
                vm.onEvent(CheckInListEventOnCreateNew())
            } label: {
                Text("Get started")
                    .font(.strakkBodyBold)
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color.strakkPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .accessibilityLabel("Create my first check-in")
            .padding(.top, StrakkSpacing.xs)
        }
        .padding(.horizontal, StrakkSpacing.md)
        .frame(maxWidth: .infinity)
        .padding(.top, StrakkSpacing.xxxl)
    }

    // MARK: - Helpers

    private func weekDisplayLabel(from weekLabel: String) -> String {
        // "2026-W17" → "Semaine 17"
        let parts = weekLabel.split(separator: "-W")
        if parts.count == 2, let weekNumber = parts.last {
            return "Semaine \(weekNumber)"
        }
        return weekLabel
    }

    private func checkInAccessibilityLabel(for item: CheckInListItemData) -> String {
        let week = weekDisplayLabel(from: item.weekLabel)
        let weight = item.weight.map { ", \(String(format: "%.1f kg", $0))" } ?? ""
        return "\(week)\(weight)"
    }
}

// MARK: - Preview

#Preview {
    CheckInListView()
}
