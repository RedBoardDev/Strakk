import SwiftUI
import shared

struct MealDetailSheet: View {
    let meal: MealData
    let onEditEntry: (MealEntryData) -> Void
    let onDeleteEntry: (MealEntryData) -> Void
    let onDeleteMeal: () -> Void
    let onDismiss: () -> Void

    @State private var selectedEntry: MealEntryData?

    var body: some View {
        NavigationStack {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        header
                            .padding(.horizontal, StrakkSpacing.lg)
                            .padding(.top, StrakkSpacing.lg)
                            .padding(.bottom, StrakkSpacing.xl)

                        totalMacros
                            .padding(.horizontal, StrakkSpacing.lg)
                            .padding(.bottom, StrakkSpacing.xl)

                        if !meal.entries.isEmpty {
                            Text("ITEMS")
                                .font(.strakkOverline)
                                .foregroundStyle(Color.strakkTextTertiary)
                                .kerning(1.0)
                                .padding(.horizontal, StrakkSpacing.lg)
                                .padding(.bottom, StrakkSpacing.xs)

                            entriesList
                                .padding(.horizontal, StrakkSpacing.lg)
                                .padding(.bottom, StrakkSpacing.xl)
                        }

                        deleteButton
                            .padding(.horizontal, StrakkSpacing.lg)
                            .padding(.bottom, StrakkSpacing.xxl)
                    }
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button { onDismiss() } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 20))
                            .foregroundStyle(Color.strakkTextSecondary)
                    }
                    .accessibilityLabel("Fermer")
                }
            }
        }
        .sheet(item: $selectedEntry) { entry in
            EntryDetailSheet(
                entry: entry,
                onEdit: {
                    selectedEntry = nil
                    onEditEntry(entry)
                },
                onDelete: {
                    onDeleteEntry(entry)
                    selectedEntry = nil
                },
                onDismiss: { selectedEntry = nil }
            )
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }

    // MARK: - Header

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(meal.name)
                .font(.strakkHeading2)
                .foregroundStyle(Color.strakkTextPrimary)

            HStack(spacing: 4) {
                Text(formatTimeLabel(from: meal.createdAt))
                Text("·")
                Text("\(meal.entries.count) item\(meal.entries.count > 1 ? "s" : "")")
            }
            .font(.strakkCaption)
            .foregroundStyle(Color.strakkTextSecondary)
        }
    }

    // MARK: - Total macros

    private var totalMacros: some View {
        var items: [MacroBreakdownItem] = [
            .init(label: "Protéines", value: String(format: "%.0fg", meal.totalProtein), color: .strakkProtein),
            .init(label: "Calories", value: String(format: "%.0f kcal", meal.totalCalories), color: .strakkCalories),
        ]
        if totalFat > 0 {
            items.append(.init(label: "Lipides", value: String(format: "%.0fg", totalFat), color: .strakkAccentYellow))
        }
        if totalCarbs > 0 {
            items.append(.init(label: "Glucides", value: String(format: "%.0fg", totalCarbs), color: .strakkAccentIndigo))
        }
        return MacroBreakdown(items: items)
    }


    // MARK: - Entries list

    private var entriesList: some View {
        VStack(spacing: 0) {
            ForEach(Array(meal.entries.enumerated()), id: \.element.id) { idx, entry in
                if idx > 0 {
                    Divider()
                        .background(Color.strakkDivider)
                        .padding(.leading, StrakkSpacing.md)
                }

                entryRow(entry: entry)
            }
        }
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
    }

    private func entryRow(entry: MealEntryData) -> some View {
        Button {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            selectedEntry = entry
        } label: {
            HStack(spacing: 0) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(entry.name ?? "Item")
                        .font(.strakkBodyBold)
                        .foregroundStyle(Color.strakkTextPrimary)
                        .lineLimit(1)

                    HStack(spacing: 0) {
                        Text(String(format: "%.0fg P", entry.protein))
                            .foregroundStyle(Color.strakkProtein)
                        Text(" · ").foregroundStyle(Color.strakkTextTertiary)
                        Text(String(format: "%.0f kcal", entry.calories))
                            .foregroundStyle(Color.strakkCalories)
                        if let qty = entry.quantity {
                            Text(" · ").foregroundStyle(Color.strakkTextTertiary)
                            Text(qty)
                                .foregroundStyle(Color.strakkTextTertiary)
                        }
                    }
                    .font(.strakkCaption)
                    .monospacedDigit()
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(Color.strakkTextTertiary)
            }
            .padding(.horizontal, StrakkSpacing.md)
            .padding(.vertical, StrakkSpacing.sm)
        }
        .buttonStyle(.plain)
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            Button(role: .destructive) {
                onDeleteEntry(entry)
            } label: {
                Label("Supprimer", systemImage: "trash")
            }
        }
        .contextMenu {
            Button { onEditEntry(entry) } label: {
                Label("Modifier", systemImage: "pencil")
            }
            Button(role: .destructive) { onDeleteEntry(entry) } label: {
                Label("Supprimer", systemImage: "trash")
            }
        }
    }

    // MARK: - Delete button

    private var deleteButton: some View {
        Button(role: .destructive) {
            onDeleteMeal()
        } label: {
            HStack(spacing: 6) {
                Image(systemName: "trash")
                    .font(.system(size: 14, weight: .semibold))
                Text("Supprimer le repas")
                    .font(.strakkBodyBold)
            }
            .foregroundStyle(Color.strakkError)
            .frame(maxWidth: .infinity)
            .frame(height: 48)
            .background(Color.strakkSurface2)
            .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
        }
        .accessibilityLabel("Supprimer le repas")
    }

    // MARK: - Computed

    private var totalFat: Double {
        meal.entries.compactMap(\.fat).reduce(0, +)
    }

    private var totalCarbs: Double {
        meal.entries.compactMap(\.carbs).reduce(0, +)
    }
}
