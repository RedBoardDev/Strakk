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
    @State var viewModel = TodayViewModelWrapper()
    @Environment(MealDraftViewModelWrapper.self) var draftViewModel

    // Navigation
    @State var navigationPath = NavigationPath()

    // Sheets
    @State var addPickerMode: AddPickerMode?
    @State var showHevyExport: Bool = false
    @State var shouldOpenDraftAfterStart: Bool = false

    // Detail sheets
    @State var selectedMeal: MealData?
    @State var selectedEntry: MealEntryData?

    // Edit entry sheet
    @State var editingEntry: MealEntryData?

    // Feature gating (fed by AddPickerSheet.onFeatureGated + CheckIn effect)
    @State var gatedFeature: ProFeature?

    var body: some View {
        NavigationStack(path: $navigationPath) {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()

                switch viewModel.state {
                case .loading:
                    ProgressView()
                        .tint(Color.strakkPrimary)

                case .ready(
                    let dateLabel,
                    let summary,
                    let timeline,
                    let waterEntries,
                    let activeDraft,
                    let trialBanner
                ):
                    mainContent(
                        dateLabel: dateLabel,
                        summary: summary,
                        timeline: timeline,
                        waterEntries: waterEntries,
                        activeDraft: activeDraft,
                        trialBanner: trialBanner
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
                        onDismiss: { navigationPath.removeLast() },
                        onFeatureGated: { gatedFeature = $0 }
                    )
                }
            }
        }
        .sheet(item: $addPickerMode) { mode in
            AddPickerSheet(
                isDraftMode: mode.isDraft,
                draftViewModel: draftViewModel,
                onDismiss: { addPickerMode = nil },
                onFeatureGated: { gatedFeature = $0 }
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
        .fullScreenCover(isPresented: $viewModel.showPaywall) {
            PaywallView(onDismiss: { viewModel.showPaywall = false })
        }
        .featureGate($gatedFeature)
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

    // stickyActionButtons() — TodayView+ActionBars.swift

    // MARK: - Main content

    @ViewBuilder
    // swiftlint:disable:next function_body_length
    private func mainContent(
        dateLabel: String,
        summary: DailySummaryData,
        timeline: [TimelineItemData],
        waterEntries: [WaterEntryData],
        activeDraft: ActiveDraftData?,
        trialBanner: TrialBannerData?
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
                        if KoinBridge.shared.isProUser() {
                            showHevyExport = true
                        } else {
                            gatedFeature = .hevyExport
                        }
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

                // 1b. Trial banner (between header and macro grid)
                if let banner = trialBanner {
                    trialBannerView(banner: banner)
                        .padding(.horizontal, 20)
                        .padding(.top, 12)
                }

                // 2. 4 macro cards
                ProgressSection(summary: summary)
                    .padding(.horizontal, 20)
                    .padding(.top, 20)

                // 3. Water
                WaterRow(
                    summary: summary,
                    onAdd: { amount in
                        viewModel.onEvent(TodayEventOnAddWater(amount: Int32(amount)))
                    },
                    onRemove: { amount in
                        viewModel.onEvent(TodayEventOnRemoveWater(amount: Int32(amount)))
                    }
                )
                .padding(.horizontal, 20)
                .padding(.top, 20)

                // 4. Timeline
                Group {
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
                .padding(.top, 32)

                Spacer().frame(height: 120)
            }
        }
    }

    // MARK: - Trial banner

    @ViewBuilder
    private func trialBannerView(banner: TrialBannerData) -> some View {
        let days: Int = {
            if case .expiringIn(let remaining) = banner { return remaining }
            return 0
        }()

        Button {
            viewModel.onEvent(TodayEventOnTrialBannerTapped())
        } label: {
            HStack(spacing: 12) {
                Image(systemName: "clock.fill")
                    .font(.system(size: 15))
                    .foregroundStyle(Color.strakkWarning)
                Text("Ton essai Pro expire dans \(days) jour\(days > 1 ? "s" : "")")
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextPrimary)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(Color.strakkTextSecondary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: StrakkRadius.sm))
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Ton essai Pro expire dans \(days) jours. Appuie pour découvrir les offres.")
    }

    // timeline rows, action bars, empty state — TodayView+Timeline.swift / TodayView+ActionBars.swift
}

// Color(hex:) is defined in Theme/StrakkColors.swift — do not redeclare here.

#Preview {
    TodayView()
        .environment(MealDraftViewModelWrapper())
}
