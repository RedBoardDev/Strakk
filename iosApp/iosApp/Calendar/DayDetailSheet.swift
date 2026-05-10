import SwiftUI
import shared

struct DayDetailSheet: View {
    let detail: CalendarDayDetailData
    let onDismiss: () -> Void
    let onAddMeal: () -> Void
    let onAddWater: (Int) -> Void
    let onRemoveWater: (Int) -> Void
    let onEditEntry: (MealEntryData, String, Double, Double, Double?, Double?, String?) -> Void
    let onDeleteEntry: (MealEntryData) -> Void
    let onDeleteMeal: (MealData) -> Void

    @State private var selectedMeal: MealData?
    @State private var selectedEntry: MealEntryData?
    @State private var editingEntry: MealEntryData?

    var body: some View {
        NavigationStack {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        SectionHeader(title: "NUTRITION")
                            .padding(.horizontal, StrakkSpacing.lg)
                            .padding(.top, StrakkSpacing.lg)

                        MacroProgressGrid(
                            totalProtein: detail.summary.totalProtein,
                            totalCalories: detail.summary.totalCalories,
                            totalFat: detail.summary.totalFat,
                            totalCarbs: detail.summary.totalCarbs,
                            proteinGoal: detail.summary.proteinGoal,
                            calorieGoal: detail.summary.calorieGoal,
                            fatGoal: detail.summary.fatGoal,
                            carbGoal: detail.summary.carbGoal
                        )
                        .padding(.horizontal, StrakkSpacing.lg)
                        .padding(.top, StrakkSpacing.xs)

                        SectionHeader(title: "WATER")
                            .padding(.horizontal, StrakkSpacing.lg)
                            .padding(.top, StrakkSpacing.xl)

                        WaterRow(
                            summary: detail.summary,
                            onAdd: { amount in onAddWater(amount) },
                            onRemove: { amount in onRemoveWater(amount) }
                        )
                        .padding(.horizontal, StrakkSpacing.lg)
                        .padding(.top, StrakkSpacing.xs)

                        if !detail.mealContainers.isEmpty || !detail.meals.isEmpty {
                            SectionHeader(title: "MEALS")
                                .padding(.horizontal, StrakkSpacing.lg)
                                .padding(.top, StrakkSpacing.xl)

                            VStack(spacing: 6) {
                                ForEach(detail.mealContainers) { meal in
                                    mealContainerRow(meal: meal)
                                }

                                ForEach(detail.meals) { entry in
                                    orphanEntryRow(entry: entry)
                                }
                            }
                            .padding(.horizontal, StrakkSpacing.lg)
                            .padding(.top, StrakkSpacing.xs)
                        }

                        StrakkPrimaryButton(
                            title: "Add for this day",
                            action: onAddMeal,
                            icon: "plus"
                        )
                        .padding(.horizontal, StrakkSpacing.lg)
                        .padding(.top, StrakkSpacing.xl)
                        .padding(.bottom, StrakkSpacing.xxl)
                    }
                }
            }
            .sheet(item: $selectedMeal) { meal in
                MealDetailSheet(
                    meal: meal,
                    onEditEntry: { entry in
                        selectedMeal = nil
                        editingEntry = entry
                    },
                    onDeleteEntry: { entry in
                        onDeleteEntry(entry)
                    },
                    onDeleteMeal: {
                        onDeleteMeal(meal)
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
                        onDeleteEntry(entry)
                        selectedEntry = nil
                    },
                    onDismiss: { selectedEntry = nil }
                )
            }
            .sheet(item: $editingEntry) { entry in
                EditEntrySheet(
                    entry: entry,
                    onSave: { name, protein, calories, fat, carbs, quantity in
                        onEditEntry(entry, name, protein, calories, fat, carbs, quantity)
                        editingEntry = nil
                    },
                    onCancel: { editingEntry = nil }
                )
            }
            .navigationTitle(formatDate(detail.date))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { StrakkCloseToolbarItem(action: onDismiss) }
        }
    }

    // MARK: - Timeline rows (same pattern as TodayView)

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
                onDeleteMeal(meal)
            } label: {
                Label("Delete", systemImage: "trash")
            }
        }
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            Button(role: .destructive) {
                onDeleteMeal(meal)
            } label: {
                Label("Delete", systemImage: "trash")
            }
        }
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
                onDeleteEntry(entry)
            } label: {
                Label("Delete", systemImage: "trash")
            }
        }
        .contextMenu {
            Button { editingEntry = entry } label: {
                Label("Edit", systemImage: "pencil")
            }
            Button(role: .destructive) {
                onDeleteEntry(entry)
            } label: {
                Label("Delete", systemImage: "trash")
            }
        }
    }

    // MARK: - Helpers

    @ViewBuilder
    private func sourceIcon(for source: EntrySource) -> some View {
        entrySourceIcon(for: source)
    }

    private func timeLabel(from isoString: String) -> String {
        formatTimeLabel(from: isoString)
    }

    private func formatDate(_ dateString: String) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale.current

        guard let date = formatter.date(from: dateString) else { return dateString }

        let display = DateFormatter()
        display.dateFormat = "EEEE d MMMM"
        display.locale = Locale.current
        return display.string(from: date).capitalized
    }
}
