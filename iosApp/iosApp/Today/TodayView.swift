import SwiftUI
import shared

// MARK: - Navigation destination

enum TodayDestination: Hashable {
    case mealDraft
    case addPicker(isDraft: Bool)
}

// MARK: - TodayView

struct TodayView: View {
    @State private var viewModel = TodayViewModelWrapper()
    @State private var draftViewModel = MealDraftViewModelWrapper()

    // Navigation
    @State private var navigationPath = NavigationPath()

    // Sheets
    @State private var showAddPickerSheet: Bool = false
    @State private var addPickerIsDraft: Bool = true
    @State private var showHevyExport: Bool = false

    // Expanded meal IDs
    @State private var expandedMealIds: Set<String> = []

    var body: some View {
        NavigationStack(path: $navigationPath) {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()

                switch viewModel.state {
                case .loading:
                    ProgressView()
                        .tint(Color.strakkPrimary)

                case .ready(let dateLabel, let summary, let timeline, let waterEntries, let activeDraft):
                    mainContent(
                        dateLabel: dateLabel,
                        summary: summary,
                        timeline: timeline,
                        waterEntries: waterEntries,
                        activeDraft: activeDraft
                    )
                    .safeAreaInset(edge: .bottom) {
                        if let draft = activeDraft {
                            floatingDraftBar(draft: draft)
                        } else {
                            stickyActionButtons()
                        }
                    }
                }
            }
            .navigationDestination(for: TodayDestination.self) { destination in
                switch destination {
                case .mealDraft:
                    MealDraftView(viewModel: draftViewModel, onAdd: {
                        addPickerIsDraft = true
                        showAddPickerSheet = true
                    })
                case .addPicker(let isDraft):
                    AddPickerSheet(
                        isDraftMode: isDraft,
                        draftViewModel: draftViewModel,
                        onDismiss: { navigationPath.removeLast() }
                    )
                }
            }
        }
        .sheet(isPresented: $showAddPickerSheet) {
            AddPickerSheet(
                isDraftMode: addPickerIsDraft,
                draftViewModel: draftViewModel,
                onDismiss: { showAddPickerSheet = false }
            )
        }
        .fullScreenCover(isPresented: $showHevyExport) {
            HevyExportFlow(onDismiss: { showHevyExport = false })
        }
        .alert("Erreur", isPresented: Binding(
            get: { viewModel.errorMessage != nil },
            set: { if !$0 { viewModel.errorMessage = nil } }
        )) {
            Button("OK") { viewModel.errorMessage = nil }
        } message: {
            Text(viewModel.errorMessage ?? "")
        }
        .onChange(of: draftViewModel.committedMeal) { _, meal in
            if meal != nil {
                navigationPath = NavigationPath()
                UINotificationFeedbackGenerator().notificationOccurred(.success)
            }
        }
    }

    // MARK: - Sticky action buttons

    @ViewBuilder
    private func stickyActionButtons() -> some View {
        HStack(spacing: 12) {
            // Repas button
            Button {
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                draftViewModel.onEvent(MealDraftEventStartDraft(initialName: nil, date: nil))
                navigationPath.append(TodayDestination.mealDraft)
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "fork.knife")
                        .font(.system(size: 16, weight: .semibold))
                    Text("Repas")
                        .font(.strakkBodyBold)
                }
                .foregroundStyle(Color.strakkTextPrimary)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(Color.strakkSurface2)
                .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .accessibilityLabel("Nouveau repas")

            // Rapide button
            Button {
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                addPickerIsDraft = false
                showAddPickerSheet = true
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "bolt.fill")
                        .font(.system(size: 16, weight: .semibold))
                    Text("Rapide")
                        .font(.strakkBodyBold)
                }
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(Color.strakkPrimary)
                .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .accessibilityLabel("Ajout rapide")
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 8)
        .padding(.top, 4)
        .background(Color.strakkBackground.opacity(0.95))
    }

    // MARK: - Main content

    @ViewBuilder
    private func mainContent(
        dateLabel: String,
        summary: DailySummaryData,
        timeline: [TimelineItemData],
        waterEntries: [WaterEntryData],
        activeDraft: ActiveDraftData?
    ) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                // 1. Header date
                HStack {
                    Text("Today")
                        .font(.strakkHeading1)
                        .foregroundStyle(Color.strakkTextPrimary)
                    Spacer()
                    Text(dateLabel)
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextSecondary)

                    Button {
                        showHevyExport = true
                    } label: {
                        Image(systemName: "dumbbell.fill")
                            .font(.system(size: 18))
                            .foregroundStyle(Color.strakkTextSecondary)
                            .frame(width: 44, height: 44)
                    }
                    .accessibilityLabel("Export Hevy")
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)

                // 2. Stats — 4 macro cards
                VStack(alignment: .leading, spacing: 10) {
                    Text("STATS")
                        .font(.strakkOverline)
                        .foregroundStyle(Color.strakkTextTertiary)

                    ProgressSection(summary: summary)
                }
                .padding(.horizontal, 20)
                .padding(.top, 20)

                // 3. Eau
                VStack(alignment: .leading, spacing: 10) {
                    Text("EAU")
                        .font(.strakkOverline)
                        .foregroundStyle(Color.strakkTextTertiary)

                    WaterRow(
                        summary: summary,
                        onAdd: { amount in
                            viewModel.onEvent(TodayEventOnAddWater(amount: Int32(amount)))
                        },
                        onRemove: { amount in
                            viewModel.onEvent(TodayEventOnRemoveWater(amount: Int32(amount)))
                        }
                    )
                }
                .padding(.horizontal, 20)
                .padding(.top, 24)

                // 4. Timeline
                VStack(alignment: .leading, spacing: 10) {
                    Text("TIMELINE")
                        .font(.strakkOverline)
                        .foregroundStyle(Color.strakkTextTertiary)

                    if timeline.isEmpty {
                        emptyTimelineView
                    } else {
                        VStack(spacing: 6) {
                            ForEach(timeline) { item in
                                timelineRow(item: item)
                            }
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 24)

                Spacer().frame(height: 120)
            }
        }
    }

    // MARK: - Timeline row

    @ViewBuilder
    private func timelineRow(item: TimelineItemData) -> some View {
        switch item {
        case .mealContainer(let meal):
            mealContainerRow(meal: meal)

        case .orphanEntry(let entry):
            orphanEntryRow(entry: entry)
        }
    }

    private func mealContainerRow(meal: MealData) -> some View {
        let isExpanded = expandedMealIds.contains(meal.id)

        return VStack(spacing: 0) {
            // Header row (always visible)
            Button {
                withAnimation(.easeInOut(duration: 0.2)) {
                    if isExpanded {
                        expandedMealIds.remove(meal.id)
                    } else {
                        expandedMealIds.insert(meal.id)
                    }
                }
            } label: {
                HStack(spacing: 10) {
                    // Time
                    Text(timeLabel(from: meal.createdAt))
                        .font(.strakkCaptionBold)
                        .foregroundStyle(Color.strakkTextTertiary)
                        .frame(width: 44, alignment: .leading)

                    // Name + sub-info
                    VStack(alignment: .leading, spacing: 2) {
                        Text(meal.name)
                            .font(.strakkBodyBold)
                            .foregroundStyle(Color.strakkTextPrimary)
                        Text("\(meal.entries.count) items")
                            .font(.strakkCaption)
                            .foregroundStyle(Color.strakkTextSecondary)
                    }

                    Spacer()

                    Text(String(format: "%.0f kcal", meal.totalCalories))
                        .font(.strakkCaptionBold)
                        .foregroundStyle(Color.strakkTextSecondary)
                        .monospacedDigit()

                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(Color.strakkTextTertiary)
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
            }
            .buttonStyle(.plain)

            // Expanded entries
            if isExpanded {
                VStack(spacing: 0) {
                    Divider()
                        .background(Color.strakkDivider)
                        .padding(.horizontal, 14)

                    ForEach(meal.entries) { entry in
                        HStack(spacing: 8) {
                            Spacer().frame(width: 44)
                            sourceIcon(for: entry.source)
                                .frame(width: 16)
                            Text(entry.name ?? "Item")
                                .font(.strakkBody)
                                .foregroundStyle(Color.strakkTextPrimary)
                                .lineLimit(1)
                            if let qty = entry.quantity {
                                Text(qty)
                                    .font(.strakkCaption)
                                    .foregroundStyle(Color.strakkTextTertiary)
                            }
                            Spacer()
                            Text(String(format: "%.0f kcal", entry.calories))
                                .font(.strakkCaption)
                                .foregroundStyle(Color.strakkTextSecondary)
                                .monospacedDigit()
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                    }
                }
            }
        }
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .contextMenu {
            Button {
                // Rename: open MealDraft screen in processed mode
            } label: {
                Label("Renommer", systemImage: "pencil")
            }
            Button(role: .destructive) {
                viewModel.onEvent(TodayEventOnDeleteMeal(mealId: meal.id))
            } label: {
                Label("Supprimer", systemImage: "trash")
            }
        }
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            Button(role: .destructive) {
                viewModel.onEvent(TodayEventOnDeleteMeal(mealId: meal.id))
            } label: {
                Label("Supprimer", systemImage: "trash")
            }
        }
        .accessibilityLabel("\(meal.name), \(meal.entries.count) items, \(Int(meal.totalCalories)) kcal")
    }

    private func orphanEntryRow(entry: MealEntryData) -> some View {
        HStack(spacing: 10) {
            Text(timeLabel(from: entry.createdAt))
                .font(.strakkCaptionBold)
                .foregroundStyle(Color.strakkTextTertiary)
                .frame(width: 44, alignment: .leading)

            sourceIcon(for: entry.source)
                .frame(width: 16)

            Text(entry.name ?? "Item")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextPrimary)
                .lineLimit(1)

            if let qty = entry.quantity {
                Text(qty)
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextTertiary)
            }

            Spacer()

            Text(String(format: "%.0f kcal", entry.calories))
                .font(.strakkCaptionBold)
                .foregroundStyle(Color.strakkTextSecondary)
                .monospacedDigit()
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            Button(role: .destructive) {
                viewModel.onEvent(TodayEventOnDeleteOrphanEntry(id: entry.id))
            } label: {
                Label("Supprimer", systemImage: "trash")
            }
        }
        .accessibilityLabel("\(entry.name ?? "Item"), \(Int(entry.calories)) kcal")
    }

    // MARK: - Floating draft bar

    private func floatingDraftBar(draft: ActiveDraftData) -> some View {
        let isEmpty = draft.resolvedCount == 0 && draft.pendingCount == 0

        return HStack(spacing: 12) {
            Button {
                navigationPath.append(TodayDestination.mealDraft)
            } label: {
                VStack(alignment: .leading, spacing: 2) {
                    Text(draft.name)
                        .font(.strakkBodyBold)
                        .foregroundStyle(.white)
                        .lineLimit(1)
                    Text(isEmpty ? "Aucun item · ajoute pour commencer" : draftSubtitle(draft: draft))
                        .font(.strakkCaption)
                        .foregroundStyle(.white.opacity(0.75))
                }
            }
            .buttonStyle(.plain)

            Spacer()

            Button {
                addPickerIsDraft = true
                showAddPickerSheet = true
            } label: {
                Text("+ Ajouter")
                    .font(.strakkCaptionBold)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(.white.opacity(0.2))
                    .clipShape(Capsule())
            }
            .accessibilityLabel("Ajouter un item au repas en cours")

            Button {
                if isEmpty {
                    draftViewModel.onEvent(MealDraftEventDiscard.shared)
                } else {
                    draftViewModel.onEvent(MealDraftEventProcess.shared)
                }
            } label: {
                Text(isEmpty ? "Annuler" : "Terminer")
                    .font(.strakkCaptionBold)
                    .foregroundStyle(isEmpty ? .white : Color.strakkPrimary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(isEmpty ? .white.opacity(0.2) : .white)
                    .clipShape(Capsule())
            }
            .accessibilityLabel(isEmpty ? "Annuler le repas vide" : "Terminer le repas")
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color.strakkPrimary)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal, 16)
        .padding(.bottom, 8)
        .shadow(color: .black.opacity(0.15), radius: 8, x: 0, y: -2)
    }

    // MARK: - Empty state

    private var emptyTimelineView: some View {
        VStack(spacing: 8) {
            Image(systemName: "fork.knife")
                .font(.system(size: 32))
                .foregroundStyle(Color.strakkTextTertiary)
            Text("Aucun item aujourd'hui")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
            Text("Utilisez les boutons ci-dessous pour commencer")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 40)
    }

    // MARK: - Helpers

    private func draftSubtitle(draft: ActiveDraftData) -> String {
        var parts: [String] = []
        if draft.resolvedCount > 0 {
            parts.append("\(draft.resolvedCount) item\(draft.resolvedCount > 1 ? "s" : "")")
        }
        if draft.pendingCount > 0 {
            parts.append("\(draft.pendingCount) en attente")
        }
        parts.append(String(format: "%.0f kcal", draft.totalCalories))
        return parts.joined(separator: " · ")
    }

    private func timeLabel(from isoString: String) -> String {
        let formats = ["yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss"]
        let df = DateFormatter()
        df.locale = Locale(identifier: "fr_FR")
        for fmt in formats {
            df.dateFormat = fmt
            if let date = df.date(from: isoString) {
                df.dateFormat = "HH:mm"
                return df.string(from: date)
            }
        }
        return ""
    }

    @ViewBuilder
    private func sourceIcon(for source: EntrySource) -> some View {
        switch source {
        case .photoai:
            Image(systemName: "camera.fill")
                .font(.system(size: 11))
                .foregroundStyle(Color.strakkTextTertiary)
        case .barcode:
            Image(systemName: "barcode.viewfinder")
                .font(.system(size: 11))
                .foregroundStyle(Color.strakkTextTertiary)
        case .manual:
            Image(systemName: "pencil")
                .font(.system(size: 11))
                .foregroundStyle(Color.strakkTextTertiary)
        case .textai:
            Image(systemName: "text.quote")
                .font(.system(size: 11))
                .foregroundStyle(Color.strakkTextTertiary)
        case .search, .frequent:
            Image(systemName: "magnifyingglass")
                .font(.system(size: 11))
                .foregroundStyle(Color.strakkTextTertiary)
        default:
            EmptyView()
        }
    }
}

// Color(hex:) is defined in Theme/StrakkColors.swift — do not redeclare here.

#Preview {
    TodayView()
}
