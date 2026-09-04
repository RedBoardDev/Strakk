import SwiftUI
import shared

struct MealDetailSheet: View {
    let meal: MealData
    let favorites: FavoritesSnapshotData
    let onEditEntry: (MealEntryData) -> Void
    let onDeleteEntry: (MealEntryData) -> Void
    let onDeleteMeal: () -> Void
    let onToggleFavorite: () -> Void
    let onToggleFavoriteFood: (MealEntryData) -> Void
    let onDismiss: () -> Void

    private var isFavorited: Bool { favorites.isMealFavorited(mealId: meal.id) }

    private func isEntryFavorited(_ entry: MealEntryData) -> Bool {
        favorites.isFoodFavorited(normalizedName: normalizeFoodName(entry.name))
    }

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
                StrakkCloseToolbarItem(action: onDismiss)
            }
        }
        .sheet(item: $selectedEntry) { entry in
            EntryDetailSheet(
                entry: entry,
                isFavorited: isEntryFavorited(entry),
                onEdit: {
                    selectedEntry = nil
                    onEditEntry(entry)
                },
                onDelete: {
                    onDeleteEntry(entry)
                    selectedEntry = nil
                },
                onToggleFavorite: { onToggleFavoriteFood(entry) },
                onDismiss: { selectedEntry = nil }
            )
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }

    // MARK: - Header

    private var header: some View {
        HStack(alignment: .top, spacing: StrakkSpacing.sm) {
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

            Spacer(minLength: 0)

            Button {
                HapticEngine.light()
                onToggleFavorite()
            } label: {
                Image(systemName: isFavorited ? "heart.fill" : "heart")
                    .font(.system(size: 20, weight: .medium))
                    .foregroundStyle(isFavorited ? Color.strakkPrimary : Color.strakkTextSecondary)
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(isFavorited ? "Remove favorite meal" : "Favorite meal")
        }
    }

    // MARK: - Total macros

    private var totalMacros: some View {
        var items: [MacroBreakdownItem] = [
            .init(
                label: String(localized: "Protein"),
                value: String(format: "%.0fg", meal.totalProtein),
                color: .strakkProtein
            ),
            .init(
                label: String(localized: "Calories"),
                value: String(format: "%.0f kcal", meal.totalCalories),
                color: .strakkCalories
            )
        ]
        if totalFat > 0 {
            items.append(.init(
                label: String(localized: "Fat"),
                value: String(format: "%.0fg", totalFat),
                color: .strakkAccentYellow
            ))
        }
        if totalCarbs > 0 {
            items.append(.init(
                label: String(localized: "Carbs"),
                value: String(format: "%.0fg", totalCarbs),
                color: .strakkAccentIndigo
            ))
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
                Label("Delete", systemImage: "trash")
            }
        }
        .contextMenu {
            Button { onEditEntry(entry) } label: {
                Label("Edit", systemImage: "pencil")
            }
            Button(role: .destructive) { onDeleteEntry(entry) } label: {
                Label("Delete", systemImage: "trash")
            }
        }
    }

    // MARK: - Delete button

    private var deleteButton: some View {
        Button(role: .destructive) {
            HapticEngine.medium()
            onDeleteMeal()
        } label: {
            HStack(spacing: StrakkSpacing.xs) {
                Image(systemName: "trash")
                    .font(.system(size: 14, weight: .semibold))
                Text("Delete meal")
                    .font(.strakkBodyBold)
            }
            .foregroundStyle(Color.strakkError)
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(Color.strakkSurface2)
            .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
        }
        .accessibilityLabel(Text("Delete meal"))
    }

    // MARK: - Computed

    private var totalFat: Double {
        meal.entries.compactMap(\.fat).reduce(0, +)
    }

    private var totalCarbs: Double {
        meal.entries.compactMap(\.carbs).reduce(0, +)
    }
}
