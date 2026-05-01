import SwiftUI
import shared

// MARK: - Navigation destination

enum TodayDestination: Hashable {
    case mealDraft
    case addPicker(isDraft: Bool)
}

enum AddPickerMode: String, Identifiable {
    case draft
    case quickAdd

    var id: String { rawValue }
    var isDraft: Bool { self == .draft }
}

// MARK: - TodayView

struct TodayView: View {
    @State private var viewModel = TodayViewModelWrapper()
    @State private var draftViewModel = MealDraftViewModelWrapper()

    // Navigation
    @State private var navigationPath = NavigationPath()

    // Sheets
    @State private var addPickerMode: AddPickerMode?
    @State private var showHevyExport: Bool = false
    @State private var shouldOpenDraftAfterStart: Bool = false

    // Detail sheets
    @State private var selectedMeal: MealData?
    @State private var selectedEntry: MealEntryData?

    // Edit entry sheet
    @State private var editingEntry: MealEntryData?

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
                        addPickerMode = .draft
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
        .sheet(item: $addPickerMode) { mode in
            AddPickerSheet(
                isDraftMode: mode.isDraft,
                draftViewModel: draftViewModel,
                onDismiss: { addPickerMode = nil }
            )
        }
        .sheet(item: $selectedMeal) { meal in
            MealDetailSheet(
                meal: meal,
                onEditEntry: { entry in
                    selectedMeal = nil
                    editingEntry = entry
                },
                onDeleteEntry: { entry in
                    if let mealId = entry.mealId {
                        viewModel.onEvent(TodayEventOnDeleteMeal(mealId: mealId))
                    } else {
                        viewModel.onEvent(TodayEventOnDeleteOrphanEntry(id: entry.id))
                    }
                },
                onDeleteMeal: {
                    viewModel.onEvent(TodayEventOnDeleteMeal(mealId: meal.id))
                    selectedMeal = nil
                },
                onDismiss: { selectedMeal = nil }
            )
        }
        .sheet(item: $selectedEntry) { entry in
            EntryDetailSheet(
                entry: entry,
                onEdit: {
                    selectedEntry = nil
                    editingEntry = entry
                },
                onDelete: {
                    viewModel.onEvent(TodayEventOnDeleteOrphanEntry(id: entry.id))
                    selectedEntry = nil
                },
                onDismiss: { selectedEntry = nil }
            )
        }
        .sheet(item: $editingEntry) { entry in
            EditEntrySheet(
                entry: entry,
                onSave: { name, protein, calories, fat, carbs, quantity in
                    viewModel.onEvent(TodayEventOnUpdateEntry(
                        id: entry.id,
                        mealId: entry.mealId,
                        logDate: entry.logDate,
                        source: entry.source,
                        createdAt: entry.createdAt,
                        name: name,
                        protein: protein,
                        calories: calories,
                        fat: asKotlinDouble(fat),
                        carbs: asKotlinDouble(carbs),
                        quantity: quantity
                    ))
                    editingEntry = nil
                },
                onCancel: { editingEntry = nil }
            )
        }
        .fullScreenCover(isPresented: $showHevyExport) {
            HevyExportFlow(onDismiss: { showHevyExport = false })
        }
        .errorAlert(message: $viewModel.errorMessage)
        .onChange(of: draftViewModel.committedMeal) { _, meal in
            if meal != nil {
                navigationPath = NavigationPath()
                UINotificationFeedbackGenerator().notificationOccurred(.success)
            }
        }
        .onChange(of: draftViewModel.didStartDraft) { _, didStart in
            guard didStart, shouldOpenDraftAfterStart else { return }
            shouldOpenDraftAfterStart = false
            navigationPath.append(TodayDestination.mealDraft)
        }
    }

    // MARK: - Sticky action buttons

    @ViewBuilder
    private func stickyActionButtons() -> some View {
        HStack(spacing: 12) {
            // Repas button
            Button {
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                shouldOpenDraftAfterStart = true
                draftViewModel.didStartDraft = false
                draftViewModel.onEvent(MealDraftEventStartDraft(initialName: nil, date: nil))
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "fork.knife")
                        .font(.system(size: 16, weight: .semibold))
                    Text("Meal")
                        .font(.strakkBodyBold)
                }
                .foregroundStyle(Color.strakkTextPrimary)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(Color.strakkSurface2)
                .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .accessibilityLabel("New meal")

            // Quick button
            Button {
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                addPickerMode = .quickAdd
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "bolt.fill")
                        .font(.system(size: 16, weight: .semibold))
                    Text("Quick")
                        .font(.strakkBodyBold)
                }
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(Color.strakkPrimary)
                .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .accessibilityLabel("Quick add")
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

                // 3. Water
                VStack(alignment: .leading, spacing: 10) {
                    Text("WATER")
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
                        LazyVStack(spacing: 6) {
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
        Button {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            selectedMeal = meal
        } label: {
            HStack(spacing: 10) {
                Text(timeLabel(from: meal.createdAt))
                    .font(.strakkCaptionBold)
                    .foregroundStyle(Color.strakkTextTertiary)
                    .frame(width: 44, alignment: .leading)

                Image(systemName: "fork.knife")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.strakkTextSecondary)
                    .frame(width: 16)

                VStack(alignment: .leading, spacing: 2) {
                    Text(meal.name)
                        .font(.strakkHeading3)
                        .foregroundStyle(Color.strakkTextPrimary)
                        .lineLimit(1)
                    Text("\(meal.entries.count) item\(meal.entries.count > 1 ? "s" : "")")
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextSecondary)
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(Color.strakkTextTertiary)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
        }
        .buttonStyle(.plain)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .contextMenu {
            Button(role: .destructive) {
                viewModel.onEvent(TodayEventOnDeleteMeal(mealId: meal.id))
            } label: {
                Label("Delete", systemImage: "trash")
            }
        }
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            Button(role: .destructive) {
                viewModel.onEvent(TodayEventOnDeleteMeal(mealId: meal.id))
            } label: {
                Label("Delete", systemImage: "trash")
            }
        }
        .accessibilityLabel("\(meal.name), \(meal.entries.count) items")
    }

    private func orphanEntryRow(entry: MealEntryData) -> some View {
        Button {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            selectedEntry = entry
        } label: {
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
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
        }
        .buttonStyle(.plain)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            Button(role: .destructive) {
                viewModel.onEvent(TodayEventOnDeleteOrphanEntry(id: entry.id))
            } label: {
                Label("Delete", systemImage: "trash")
            }
        }
        .contextMenu {
            Button { editingEntry = entry } label: {
                Label("Edit", systemImage: "pencil")
            }
            Button(role: .destructive) {
                viewModel.onEvent(TodayEventOnDeleteOrphanEntry(id: entry.id))
            } label: {
                Label("Delete", systemImage: "trash")
            }
        }
        .accessibilityLabel(entry.name ?? "Item")
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
                    if !isEmpty {
                        Text(draftSubtitle(draft: draft))
                            .font(.strakkCaption)
                            .foregroundStyle(.white.opacity(0.75))
                    }
                }
            }
            .buttonStyle(.plain)

            Spacer()

            Button {
                addPickerMode = .draft
            } label: {
                Text("+ Add")
                    .font(.strakkCaptionBold)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(.white.opacity(0.2))
                    .clipShape(Capsule())
            }
            .accessibilityLabel("Add item to current meal")

            Button {
                if isEmpty {
                    draftViewModel.onEvent(MealDraftEventDiscard.shared)
                } else {
                    draftViewModel.onEvent(MealDraftEventProcess.shared)
                }
            } label: {
                Text(isEmpty ? "Cancel" : "Finish")
                    .font(.strakkCaptionBold)
                    .foregroundStyle(isEmpty ? .white : Color.strakkPrimary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(isEmpty ? .white.opacity(0.2) : .white)
                    .clipShape(Capsule())
            }
            .accessibilityLabel(isEmpty ? "Cancel empty meal" : "Finish meal")
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
        VStack(spacing: 20) {
            // Hairline + centered icon
            HStack(spacing: 14) {
                Rectangle()
                    .fill(
                        LinearGradient(
                            colors: [.clear, Color.strakkDivider],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .frame(height: 1)

                ZStack {
                    Circle()
                        .fill(Color.strakkSurface2)
                        .overlay(Circle().strokeBorder(Color.strakkDivider, lineWidth: 1))
                        .frame(width: 64, height: 64)
                    Image(systemName: "fork.knife")
                        .font(.system(size: 26, weight: .medium))
                        .foregroundStyle(Color.strakkTextSecondary)
                }

                Rectangle()
                    .fill(
                        LinearGradient(
                            colors: [Color.strakkDivider, .clear],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .frame(height: 1)
            }

            VStack(spacing: 5) {
                Text("No items today")
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkTextPrimary)
                Text("Use the buttons below to get started")
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextTertiary)
                    .multilineTextAlignment(.center)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 32)
    }

    // MARK: - Helpers

    private func draftSubtitle(draft: ActiveDraftData) -> String {
        var parts: [String] = []
        if draft.resolvedCount > 0 {
            parts.append("\(draft.resolvedCount) item\(draft.resolvedCount > 1 ? "s" : "")")
        }
        if draft.pendingCount > 0 {
            parts.append("\(draft.pendingCount) pending")
        }
        parts.append(String(format: "%.0f kcal", draft.totalCalories))
        return parts.joined(separator: " · ")
    }

    private func timeLabel(from isoString: String) -> String {
        formatTimeLabel(from: isoString)
    }

    @ViewBuilder
    private func sourceIcon(for source: EntrySource) -> some View {
        entrySourceIcon(for: source)
    }
}

// Color(hex:) is defined in Theme/StrakkColors.swift — do not redeclare here.

#Preview {
    TodayView()
}
