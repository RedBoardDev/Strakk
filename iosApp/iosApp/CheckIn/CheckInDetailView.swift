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

            case .ready(let checkIn, let delta, let photoUrls):
                mainContent(checkIn: checkIn, delta: delta, photoUrls: photoUrls)
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
                        Label("Modifier", systemImage: "pencil")
                    }
                    Button(role: .destructive) {
                        showDeleteAlert = true
                    } label: {
                        Label("Supprimer", systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .font(.title3)
                        .foregroundStyle(Color.strakkTextPrimary)
                }
                .accessibilityLabel("Options du bilan")
            }
        }
        .alert("Supprimer ce bilan ?", isPresented: $showDeleteAlert) {
            Button("Supprimer", role: .destructive) {
                vm.onEvent(CheckInDetailEventOnConfirmDelete())
            }
            Button("Annuler", role: .cancel) {}
        } message: {
            Text("This action cannot be undone.")
        }
        .alert("Erreur", isPresented: Binding(
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
    private func mainContent(checkIn: CheckInData, delta: CheckInDeltaData?, photoUrls: [String: String]) -> some View {
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
                    CheckInNutritionSection(nutrition: nutrition)
                } else {
                    CheckInEmptyNutritionSection()
                }

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
        .sheet(isPresented: $showExportOptions) {
            PdfExportOptionsSheet(
                options: $exportOptions,
                isGenerating: isGeneratingPdf,
                onExport: {
                    guard !isGeneratingPdf else { return }
                    showExportOptions = false
                    isGeneratingPdf = true
                    let checkInForExport = pendingCheckIn
                    Task {
                        defer { isGeneratingPdf = false }
                        guard let pdfData = await vm.generatePdf(options: exportOptions) else { return }
                        let weekLabel = checkInForExport?.weekLabel ?? "bilan"
                        let tempUrl = FileManager.default.temporaryDirectory
                            .appendingPathComponent("Bilan_\(weekLabel).pdf")
                        try? pdfData.write(to: tempUrl)
                        shareItems = [tempUrl]
                    }
                },
                onCancel: { showExportOptions = false }
            )
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
        }
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
                Text("\(count) jour\(count > 1 ? "s" : "") couverts")
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
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .disabled(isGeneratingPdf)
        .accessibilityLabel("Partager le bilan en PDF")
    }

    // MARK: - Helpers

    private func weekDisplayLabel(from weekLabel: String) -> String {
        let parts = weekLabel.split(separator: "-W")
        if parts.count == 2, let weekNumber = parts.last {
            return "Semaine \(weekNumber)"
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
