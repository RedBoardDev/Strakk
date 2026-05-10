import SwiftUI
import shared

struct EntryDetailSheet: View {
    let entry: MealEntryData
    let onEdit: () -> Void
    let onDelete: () -> Void
    let onDismiss: () -> Void

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

                        macroBreakdown
                            .padding(.horizontal, StrakkSpacing.lg)
                            .padding(.bottom, StrakkSpacing.xl)

                        actions
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
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
    }

    // MARK: - Header

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(entry.name ?? "Item")
                .font(.strakkHeading2)
                .foregroundStyle(Color.strakkTextPrimary)
                .lineLimit(2)

            HStack(spacing: 6) {
                Text(formatTimeLabel(from: entry.createdAt))
                Text("·")
                entrySourceIcon(for: entry.source)
                Text(entrySourceLabel(for: entry.source))
            }
            .font(.strakkCaption)
            .foregroundStyle(Color.strakkTextSecondary)
        }
    }

    // MARK: - Macro breakdown

    private var macroBreakdown: some View {
        var items: [MacroBreakdownItem] = [
            .init(
                label: String(localized: "Protein"),
                value: String(format: "%.0fg", entry.protein),
                color: .strakkProtein
            ),
            .init(
                label: String(localized: "Calories"),
                value: String(format: "%.0f kcal", entry.calories),
                color: .strakkCalories
            )
        ]
        if let fat = entry.fat {
            items.append(.init(
                label: String(localized: "Fat"),
                value: String(format: "%.0fg", fat),
                color: .strakkAccentYellow
            ))
        }
        if let carbs = entry.carbs {
            items.append(.init(
                label: String(localized: "Carbs"),
                value: String(format: "%.0fg", carbs),
                color: .strakkAccentIndigo
            ))
        }
        if let qty = entry.quantity {
            items.append(.init(
                label: String(localized: "Quantity"),
                value: qty,
                color: .strakkTextSecondary
            ))
        }
        return MacroBreakdown(items: items)
    }

    // MARK: - Actions

    private var actions: some View {
        HStack(spacing: StrakkSpacing.sm) {
            StrakkSecondaryButton(
                title: "Edit",
                action: onEdit,
                icon: "pencil"
            )
            .accessibilityLabel(Text("Edit entry"))

            Button(role: .destructive) {
                HapticEngine.medium()
                onDelete()
            } label: {
                HStack(spacing: StrakkSpacing.xs) {
                    Image(systemName: "trash")
                        .font(.system(size: 14, weight: .semibold))
                    Text("Delete")
                        .font(.strakkBodyBold)
                }
                .foregroundStyle(Color.strakkError)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(Color.strakkSurface2)
                .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
            }
            .accessibilityLabel(Text("Delete entry"))
        }
    }

}
