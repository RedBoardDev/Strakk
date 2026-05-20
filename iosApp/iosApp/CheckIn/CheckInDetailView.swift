import SwiftUI
import shared

// MARK: - CheckInDetailView

struct CheckInDetailView: View {
    @State private var vm: CheckInDetailViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    @State private var showDeleteAlert = false
    @State private var shareItems: [Any]?
    @State private var isGeneratingPdf = false
    @State private var showExportOptions = false
    @State private var exportOptions = PdfExportConfig()
    @State private var pendingCheckIn: CheckInData?

    let checkInId: String

    init(checkInId: String) {
        self.checkInId = checkInId
        _vm = State(initialValue: CheckInDetailViewModelWrapper(checkInId: checkInId))
    }

    var body: some View {
        ZStack {
            Color.strakkBackground.ignoresSafeArea()

            switch vm.state {
            case .loading:
                ProgressView()
                    .tint(Color.strakkPrimary)

            case let .ready(checkIn, delta, photoUrls, trainingStats, isLoadingTraining, pdfOptions):
                mainContent(
                    checkIn: checkIn,
                    delta: delta,
                    photoUrls: photoUrls,
                    trainingStats: trainingStats,
                    isLoadingTraining: isLoadingTraining,
                    pdfOptions: pdfOptions
                )
            }
        }
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Menu {
                    Button {
                        vm.onEvent(CheckInDetailEventOnEdit())
                    } label: {
                        Label("Edit", systemImage: "pencil")
                    }
                    Button(role: .destructive) {
                        showDeleteAlert = true
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .font(.title3)
                        .foregroundStyle(Color.strakkTextPrimary)
                }
                .accessibilityLabel(Text("Check-in options"))
            }
        }
        .alert("Delete this check-in?", isPresented: $showDeleteAlert) {
            Button("Delete", role: .destructive) {
                vm.onEvent(CheckInDetailEventOnConfirmDelete())
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This action cannot be undone.")
        }
        .alert("Error", isPresented: Binding(
            get: { vm.errorMessage != nil },
            set: { if !$0 { vm.errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { vm.errorMessage = nil }
        } message: {
            Text(vm.errorMessage ?? "")
        }
        .fullScreenCover(isPresented: Binding(
            get: { vm.navigateToWizardCheckInId != nil },
            set: { if !$0 { vm.navigateToWizardCheckInId = nil } }
        )) {
            CheckInWizardView(checkInId: checkInId)
        }
        .onChange(of: vm.navigateBack) { _, navigateBack in
            if navigateBack { dismiss() }
        }
    }

    // MARK: - Main content

    @ViewBuilder
    private func mainContent(
        checkIn: CheckInData,
        delta: CheckInDeltaData?,
        photoUrls: [String: String],
        trainingStats: WeeklyTrainingStatsData?,
        isLoadingTraining: Bool,
        pdfOptions: PdfExportConfig
    ) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: StrakkSpacing.xl) {
                headerSection(checkIn: checkIn)

                CheckInPhotosSection(photos: checkIn.photos, photoUrls: photoUrls)

                CheckInMeasurementsSection(checkIn: checkIn, delta: delta)

                CheckInFeelingsSection(
                    tags: checkIn.feelingTags,
                    mentalFeeling: checkIn.mentalFeeling,
                    physicalFeeling: checkIn.physicalFeeling
                )

                if let nutrition = checkIn.nutritionSummary {
                    CheckInNutritionSection(
                        nutrition: nutrition,
                        isRefreshing: vm.isRefreshingNutrition,
                        onRefresh: { vm.onEvent(CheckInDetailEventOnRefreshNutrition()) }
                    )
                } else {
                    CheckInEmptyNutritionSection()
                }

                CheckInTrainingSection(
                    stats: trainingStats,
                    isLoading: isLoadingTraining,
                    onRefresh: { vm.onEvent(CheckInDetailEventOnRefreshTraining()) }
                )

                actionButtons(checkIn: checkIn)
            }
            .padding(.horizontal, StrakkSpacing.lg)
            .padding(.vertical, StrakkSpacing.xl)
        }
        .sheet(isPresented: Binding(
            get: { shareItems != nil },
            set: { if !$0 { shareItems = nil } }
        )) {
            if let items = shareItems {
                ShareSheet(activityItems: items)
            }
        }
        .sheet(isPresented: $showExportOptions) { exportSheet }
    }

    // MARK: - Header

    @ViewBuilder
    private func headerSection(checkIn: CheckInData) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xxs) {
            Text(weekDisplayLabel(from: checkIn.weekLabel))
                .font(.strakkHeading1)
                .foregroundStyle(Color.strakkTextPrimary)

            if !checkIn.coveredDates.isEmpty {
                let count = checkIn.coveredDates.count
                Text("\(count) days covered")
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextSecondary)
            }
        }
    }

    // MARK: - Action buttons

    @ViewBuilder
    private func actionButtons(checkIn: CheckInData) -> some View {
        Button {
            pendingCheckIn = checkIn
            loadPersistedOptions()
            showExportOptions = true
        } label: {
            HStack(spacing: StrakkSpacing.xs) {
                if isGeneratingPdf {
                    ProgressView()
                        .tint(Color.strakkTextPrimary)
                        .scaleEffect(0.8)
                } else {
                    Image(systemName: "square.and.arrow.up")
                        .font(.system(size: 14, weight: .semibold))
                }
                Text("Share check-in")
                    .font(.strakkBodyBold)
            }
            .foregroundStyle(Color.strakkTextPrimary)
            .frame(maxWidth: .infinity)
            .frame(height: 48)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
        }
        .disabled(isGeneratingPdf)
        .accessibilityLabel(Text("Share check-in as PDF"))
    }

    private var exportSheet: some View {
        PdfExportOptionsSheet(
            options: $exportOptions,
            hasTrainingData: hasTrainingData,
            isGenerating: isGeneratingPdf,
            onExport: {
                guard !isGeneratingPdf else { return }
                saveExportOptions()
                showExportOptions = false
                isGeneratingPdf = true
                let checkInForExport = pendingCheckIn
                Task {
                    defer { isGeneratingPdf = false }
                    guard let pdfData = await vm.generatePdf(options: exportOptions) else { return }
                    let weekLabel = checkInForExport?.weekLabel ?? "checkin"
                    let tempUrl = FileManager.default.temporaryDirectory
                        .appendingPathComponent("CheckIn_\(weekLabel).pdf")
                    try? pdfData.write(to: tempUrl)
                    shareItems = [tempUrl]
                }
            },
            onCancel: { showExportOptions = false }
        )
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }

    private func loadPersistedOptions() {
        if case .ready(_, _, _, _, _, let pdfOptions) = vm.state {
            exportOptions = pdfOptions
        }
    }

    private func saveExportOptions() {
        let kmpOptions = PdfExportOptions(
            includePhotos: exportOptions.includePhotos,
            includeMeasurements: exportOptions.includeMeasurements,
            includeFeelings: exportOptions.includeFeelings,
            includeProtein: exportOptions.includeProtein,
            includeCalories: exportOptions.includeCalories,
            includeCarbs: exportOptions.includeCarbs,
            includeFat: exportOptions.includeFat,
            includeWater: exportOptions.includeWater,
            includeAverages: exportOptions.includeAverages,
            includeDailyData: exportOptions.includeDailyData,
            includeTrainingSummary: exportOptions.includeTrainingSummary,
            includeMuscleVolume: exportOptions.includeMuscleVolume,
            includeWorkoutDetails: exportOptions.includeWorkoutDetails
        )
        vm.onEvent(CheckInDetailEventOnUpdatePdfOptions(options: kmpOptions))
    }

    private var hasTrainingData: Bool {
        if case .ready(_, _, _, let training, _, _) = vm.state {
            return training != nil
        }
        return false
    }

    // MARK: - Helpers

    private func weekDisplayLabel(from weekLabel: String) -> String {
        let parts = weekLabel.split(separator: "-W")
        if parts.count == 2, let weekNumber = parts.last {
            return String(localized: "Week \(String(weekNumber))")
        }
        return weekLabel
    }
}

// MARK: - ShareSheet

private struct ShareSheet: UIViewControllerRepresentable {
    let activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

// MARK: - Preview

#Preview {
    NavigationStack {
        CheckInDetailView(checkInId: "preview-id")
    }
}
