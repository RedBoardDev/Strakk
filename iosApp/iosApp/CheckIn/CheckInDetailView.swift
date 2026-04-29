import SwiftUI
import shared

// MARK: - CheckInDetailView

struct CheckInDetailView: View {
    @State private var vm: CheckInDetailViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    @State private var showDeleteAlert = false
    @State private var showWizard = false
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
            Text("Cette action est irréversible.")
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
                // Header
                headerSection(checkIn: checkIn)

                photosSection(photos: checkIn.photos, photoUrls: photoUrls)

                measurementsSection(checkIn: checkIn, delta: delta)

                feelingsSection(
                    tags: checkIn.feelingTags,
                    mentalFeeling: checkIn.mentalFeeling,
                    physicalFeeling: checkIn.physicalFeeling
                )

                if let nutrition = checkIn.nutritionSummary {
                    nutritionSection(nutrition: nutrition)
                } else {
                    emptyNutritionSection
                }

                // Action buttons
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

    // MARK: - Photos

    @ViewBuilder
    private func photosSection(photos: [CheckInPhotoData], photoUrls: [String: String]) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("PHOTOS")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            if photos.isEmpty {
                emptyValue("Aucune photo")
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: StrakkSpacing.sm) {
                        ForEach(photos.sorted(by: { $0.position < $1.position })) { photo in
                            photoThumbnail(photo: photo, signedUrl: photoUrls[photo.id])
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func photoThumbnail(photo: CheckInPhotoData, signedUrl: String?) -> some View {
        ZStack {
            Color.strakkSurface2

            if let urlString = signedUrl, let url = URL(string: urlString) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    case .failure:
                        Image(systemName: "camera.fill")
                            .font(.system(size: 24))
                            .foregroundStyle(Color.strakkTextTertiary)
                    case .empty:
                        ProgressView()
                            .tint(Color.strakkPrimary)
                    @unknown default:
                        EmptyView()
                    }
                }
            } else {
                Image(systemName: "camera.fill")
                    .font(.system(size: 24))
                    .foregroundStyle(Color.strakkTextTertiary)
            }
        }
        .frame(width: 120, height: 160)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .accessibilityLabel("Photo \(photo.position + 1)")
    }

    // MARK: - Measurements

    @ViewBuilder
    private func measurementsSection(checkIn: CheckInData, delta: CheckInDeltaData?) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("MESURES")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            VStack(spacing: 0) {
                let rows: [(String, Double?, String, Double?)] = [
                    ("Poids", checkIn.weight, "kg", delta?.weight),
                    ("Épaules", checkIn.shoulders, "cm", delta?.shoulders),
                    ("Poitrine", checkIn.chest, "cm", delta?.chest),
                    ("Bras gauche", checkIn.armLeft, "cm", delta?.armLeft),
                    ("Bras droit", checkIn.armRight, "cm", delta?.armRight),
                    ("Taille", checkIn.waist, "cm", delta?.waist),
                    ("Hanches", checkIn.hips, "cm", delta?.hips),
                    ("Cuisse gauche", checkIn.thighLeft, "cm", delta?.thighLeft),
                    ("Cuisse droite", checkIn.thighRight, "cm", delta?.thighRight)
                ]

                ForEach(Array(rows.enumerated()), id: \.offset) { index, row in
                    measurementRow(label: row.0, value: row.1, unit: row.2, delta: row.3)
                    if index < rows.count - 1 {
                        Divider().background(Color.strakkDivider)
                    }
                }
            }
            .padding(StrakkSpacing.md)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    @ViewBuilder
    private func measurementRow(label: String, value: Double?, unit: String, delta: Double?) -> some View {
        HStack(spacing: StrakkSpacing.xs) {
            Text(label)
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextPrimary)
                .frame(maxWidth: .infinity, alignment: .leading)

            if let delta {
                deltaView(delta)
            }

            if let value {
                Text("\(String(format: "%.1f", value)) \(unit)")
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .monospacedDigit()
            } else {
                Text("—")
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkTextTertiary)
            }
        }
        .frame(minHeight: 44)
    }

    @ViewBuilder
    private func deltaView(_ delta: Double) -> some View {
        if delta == 0 {
            Text("=")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
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

    // MARK: - Feelings

    private static let positiveFeelingIds: Set<String> = [
        "energy_stable", "good_energy", "motivated", "disciplined", "good_sleep",
        "good_recovery", "strong_training", "good_mood", "focused", "light_body", "good_digestion"
    ]

    private static let negativeFeelingIds: Set<String> = [
        "low_energy", "tired", "poor_sleep", "stress", "low_motivation", "heavy_body",
        "sore", "joint_discomfort", "digestion_discomfort", "bloating", "hungry",
        "irritability", "low_mood"
    ]

    @ViewBuilder
    private func feelingsSection(tags: [String], mentalFeeling: String?, physicalFeeling: String?) -> some View {
        let positiveTags = tags.filter { Self.positiveFeelingIds.contains($0) }
        let negativeTags = tags.filter { Self.negativeFeelingIds.contains($0) }
        let hasMental = mentalFeeling.map { !$0.isEmpty } ?? false
        let hasPhysical = physicalFeeling.map { !$0.isEmpty } ?? false
        let isEmpty = tags.isEmpty && !hasMental && !hasPhysical

        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("RESSENTIS")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
                if isEmpty {
                    emptyValue("Aucun ressenti renseigné")
                } else {
                    // Positive tags
                    if !positiveTags.isEmpty {
                        VStack(alignment: .leading, spacing: StrakkSpacing.xxs) {
                            Text("Sensations positives")
                                .font(.strakkOverline)
                                .foregroundStyle(Color.strakkTextTertiary)
                            FlowLayout(spacing: StrakkSpacing.xs) {
                                ForEach(positiveTags, id: \.self) { tag in
                                    Text(feelingTagLabel(tag))
                                        .font(.strakkCaptionBold)
                                        .foregroundStyle(.white)
                                        .padding(.horizontal, StrakkSpacing.sm)
                                        .padding(.vertical, StrakkSpacing.xxs)
                                        .background(Color.strakkSuccess)
                                        .clipShape(Capsule())
                                }
                            }
                        }
                    }

                    // Negative tags
                    if !negativeTags.isEmpty {
                        VStack(alignment: .leading, spacing: StrakkSpacing.xxs) {
                            Text("Sensations négatives")
                                .font(.strakkOverline)
                                .foregroundStyle(Color.strakkTextTertiary)
                            FlowLayout(spacing: StrakkSpacing.xs) {
                                ForEach(negativeTags, id: \.self) { tag in
                                    Text(feelingTagLabel(tag))
                                        .font(.strakkCaptionBold)
                                        .foregroundStyle(.white)
                                        .padding(.horizontal, StrakkSpacing.sm)
                                        .padding(.vertical, StrakkSpacing.xxs)
                                        .background(Color.strakkError)
                                        .clipShape(Capsule())
                                }
                            }
                        }
                    }

                    // Mental feeling card
                    if hasMental, let mentalFeeling {
                        feelingCard(header: "RESSENTI MENTAL", text: mentalFeeling)
                    }

                    // Physical feeling card
                    if hasPhysical, let physicalFeeling {
                        feelingCard(header: "RESSENTI PHYSIQUE", text: physicalFeeling)
                    }
                }
            }
            .padding(StrakkSpacing.md)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    private func feelingCard(header: String, text: String) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xxs) {
            Text(header)
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)
            Text(text)
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
        }
        .padding(StrakkSpacing.sm)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.strakkSurface2)
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    private func feelingTagLabel(_ slug: String) -> String {
        [
            "energy_stable": "Énergie stable",
            "good_energy": "Bonne énergie",
            "motivated": "Motivation",
            "disciplined": "Régularité",
            "good_sleep": "Bien dormi",
            "good_recovery": "Bonne récup",
            "strong_training": "Séances solides",
            "good_mood": "Bonne humeur",
            "focused": "Mental clair",
            "light_body": "Corps léger",
            "good_digestion": "Bonne digestion",
            "low_energy": "Peu d'énergie",
            "tired": "Fatigue",
            "poor_sleep": "Mal dormi",
            "stress": "Stress",
            "low_motivation": "Motivation basse",
            "heavy_body": "Corps lourd",
            "sore": "Courbatures",
            "joint_discomfort": "Gêne articulaire",
            "digestion_discomfort": "Digestion difficile",
            "bloating": "Ballonnements",
            "hungry": "Faim marquée",
            "irritability": "Irritabilité",
            "low_mood": "Moral bas",
        ][slug] ?? slug
    }

    // MARK: - Nutrition

    private var emptyNutritionSection: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("NUTRITION")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            emptyValue("Aucune donnée nutritionnelle")
                .padding(StrakkSpacing.md)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.strakkSurface1)
                .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    @ViewBuilder
    private func nutritionSection(nutrition: NutritionSummaryData) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("NUTRITION (\(nutrition.nutritionDays) jour\(nutrition.nutritionDays > 1 ? "s" : ""))")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            VStack(spacing: StrakkSpacing.sm) {
                // Macros grid
                LazyVGrid(
                    columns: Array(repeating: GridItem(.flexible(), spacing: StrakkSpacing.xs), count: 2),
                    spacing: StrakkSpacing.xs
                ) {
                    macroCell(label: "Calories", value: String(format: "%.0f kcal", nutrition.avgCalories), color: .strakkAccentOrange)
                    macroCell(label: "Protéines", value: String(format: "%.0f g", nutrition.avgProtein), color: .strakkPrimary)
                    macroCell(label: "Glucides", value: String(format: "%.0f g", nutrition.avgCarbs), color: .strakkAccentIndigo)
                    macroCell(label: "Lipides", value: String(format: "%.0f g", nutrition.avgFat), color: .strakkAccentYellow)
                }

                // Water
                HStack {
                    Image(systemName: "drop.fill")
                        .foregroundStyle(Color.strakkWater)
                        .font(.system(size: 13))
                    Text("Eau : \(nutrition.avgWater) ml/j en moy.")
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextSecondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                // Daily data
                if !nutrition.dailyData.isEmpty {
                    dailyNutritionSection(days: nutrition.dailyData)
                }

                // AI summary
                if let summary = nutrition.aiSummary, !summary.isEmpty {
                    VStack(alignment: .leading, spacing: StrakkSpacing.xxs) {
                        HStack(spacing: StrakkSpacing.xxs) {
                            Image(systemName: "sparkles")
                                .font(.system(size: 11))
                                .foregroundStyle(Color.strakkPrimary)
                            Text("Résumé IA")
                                .font(.strakkOverline)
                                .foregroundStyle(Color.strakkPrimary)
                        }
                        Text(summary)
                            .font(.strakkBody)
                            .foregroundStyle(Color.strakkTextSecondary)
                    }
                    .padding(StrakkSpacing.sm)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.strakkAccentOrangeFaint)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .strokeBorder(Color.strakkAccentOrangeBorder, lineWidth: 1)
                    )
                }
            }
            .padding(StrakkSpacing.md)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    private func emptyValue(_ text: String) -> some View {
        Text(text)
            .font(.strakkBody)
            .foregroundStyle(Color.strakkTextTertiary)
    }

    @ViewBuilder
    private func macroCell(label: String, value: String, color: Color) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xxs) {
            Text(label)
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
            Text(value)
                .font(.strakkBodyBold)
                .foregroundStyle(color)
        }
        .padding(StrakkSpacing.sm)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.strakkSurface2)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    @ViewBuilder
    private func dailyNutritionSection(days: [DailyNutritionData]) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("PAR JOUR")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)
                .padding(.bottom, 8)

            VStack(spacing: 0) {
                // Header row
                HStack {
                    Text("Date")
                        .frame(width: 60, alignment: .leading)
                    Text("Cal.")
                        .frame(maxWidth: .infinity, alignment: .trailing)
                    Text("Prot.")
                        .frame(maxWidth: .infinity, alignment: .trailing)
                    Text("Gluc.")
                        .frame(maxWidth: .infinity, alignment: .trailing)
                    Text("Lip.")
                        .frame(maxWidth: .infinity, alignment: .trailing)
                    Text("Eau")
                        .frame(maxWidth: .infinity, alignment: .trailing)
                }
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color.strakkSurface2)

                ForEach(Array(days.enumerated()), id: \.element.id) { idx, day in
                    HStack {
                        Text(formatShortDate(day.date))
                            .frame(width: 60, alignment: .leading)
                        Text(String(format: "%.0f", day.calories))
                            .frame(maxWidth: .infinity, alignment: .trailing)
                        Text(String(format: "%.0fg", day.protein))
                            .frame(maxWidth: .infinity, alignment: .trailing)
                        Text(String(format: "%.0fg", day.carbs))
                            .frame(maxWidth: .infinity, alignment: .trailing)
                        Text(String(format: "%.0fg", day.fat))
                            .frame(maxWidth: .infinity, alignment: .trailing)
                        Text(day.waterMl > 0 ? String(format: "%.1fL", Double(day.waterMl) / 1000.0) : "—")
                            .frame(maxWidth: .infinity, alignment: .trailing)
                    }
                    .font(.strakkCaption)
                    .foregroundStyle(idx % 2 == 0 ? Color.strakkTextPrimary : Color.strakkTextSecondary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(idx % 2 == 1 ? Color.strakkSurface2 : Color.clear)
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
    }

    private func formatShortDate(_ dateString: String) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "fr_FR")
        guard let date = formatter.date(from: dateString) else { return dateString }
        let display = DateFormatter()
        display.dateFormat = "d MMM"
        display.locale = Locale(identifier: "fr_FR")
        return display.string(from: date)
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
                Text("Partager le bilan")
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

// MARK: - FlowLayout helper (wrapping HStack)

private struct FlowLayout: Layout {
    var spacing: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? 0
        var height: CGFloat = 0
        var x: CGFloat = 0
        var rowHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > width, x > 0 {
                height += rowHeight + spacing
                x = 0
                rowHeight = 0
            }
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
        height += rowHeight
        return CGSize(width: width, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var rowHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > bounds.maxX, x > bounds.minX {
                y += rowHeight + spacing
                x = bounds.minX
                rowHeight = 0
            }
            subview.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}

// MARK: - Preview

#Preview {
    NavigationStack {
        CheckInDetailView(checkInId: "preview-id")
    }
}
