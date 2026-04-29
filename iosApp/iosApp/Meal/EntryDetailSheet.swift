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
            .init(label: "Protéines", value: String(format: "%.0fg", entry.protein), color: .strakkProtein),
            .init(label: "Calories", value: String(format: "%.0f kcal", entry.calories), color: .strakkCalories),
        ]
        if let fat = entry.fat {
            items.append(.init(label: "Lipides", value: String(format: "%.0fg", fat), color: .strakkAccentYellow))
        }
        if let carbs = entry.carbs {
            items.append(.init(label: "Glucides", value: String(format: "%.0fg", carbs), color: .strakkAccentIndigo))
        }
        if let qty = entry.quantity {
            items.append(.init(label: "Quantité", value: qty, color: .strakkTextSecondary))
        }
        return MacroBreakdown(items: items)
    }


    // MARK: - Actions

    private var actions: some View {
        HStack(spacing: StrakkSpacing.sm) {
            Button {
                onEdit()
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "pencil")
                        .font(.system(size: 14, weight: .semibold))
                    Text("Modifier")
                        .font(.strakkBodyBold)
                }
                .foregroundStyle(Color.strakkTextPrimary)
                .frame(maxWidth: .infinity)
                .frame(height: 48)
                .background(Color.strakkSurface2)
                .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
            }
            .accessibilityLabel("Modifier l'entrée")

            Button(role: .destructive) {
                onDelete()
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "trash")
                        .font(.system(size: 14, weight: .semibold))
                    Text("Supprimer")
                        .font(.strakkBodyBold)
                }
                .foregroundStyle(Color.strakkError)
                .frame(maxWidth: .infinity)
                .frame(height: 48)
                .background(Color.strakkSurface2)
                .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
            }
            .accessibilityLabel("Supprimer l'entrée")
        }
    }

}
