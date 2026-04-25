import SwiftUI
import shared

// MARK: - TimelineCard
//
// Contient l'état vide OU la liste des items timeline,
// plus les deux CTAs (Repas / Rapide) toujours en bas.

struct TimelineCard: View {
    let timeline: [TimelineItemData]
    let expandedMealIds: Set<String>
    let onToggleMeal: (String) -> Void
    let onDeleteMeal: (String) -> Void
    let onDeleteEntry: (String) -> Void
    let onAddMeal: () -> Void
    let onQuickAdd: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            // MARK: Content zone (empty ou liste)
            if timeline.isEmpty {
                emptyState
                    .padding(.bottom, StrakkSpacing.lg)
            } else {
                timelineList
                    .padding(.bottom, StrakkSpacing.lg)
            }

            // MARK: CTAs — toujours présents
            ctaButtons
        }
        .padding(StrakkSpacing.xl)
        .background(
            LinearGradient(
                colors: [Color.strakkSurface1GradientTop, Color.strakkSurface1GradientBottom],
                startPoint: .top,
                endPoint: .bottom
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.xxl))
        .overlay(
            RoundedRectangle(cornerRadius: StrakkRadius.xxl)
                .strokeBorder(Color.strakkBorderSubtle, lineWidth: 1)
        )
    }

    // MARK: - Empty state

    private var emptyState: some View {
        VStack(spacing: 0) {
            // Icône circulaire
            ZStack {
                Circle()
                    .fill(Color.strakkSurface3)
                    .overlay(
                        Circle()
                            .strokeBorder(Color.strakkBorderFaint, lineWidth: 1)
                    )
                Image(systemName: "fork.knife")
                    .font(.system(size: 32, weight: .semibold))
                    .foregroundStyle(Color.strakkTextSecondary)
            }
            .frame(width: 88, height: 88)
            .accessibilityHidden(true)

            Spacer().frame(height: StrakkSpacing.md)

            Text("Aucun item aujourd'hui")
                .font(.strakkHeading1)
                .foregroundStyle(Color.strakkTextPrimary)
                .multilineTextAlignment(.center)

            Spacer().frame(height: StrakkSpacing.xs)

            Text("Utilisez les boutons ci-dessous pour commencer")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Timeline list

    private var timelineList: some View {
        VStack(spacing: StrakkSpacing.xs) {
            ForEach(timeline) { item in
                timelineRow(item: item)
            }
        }
    }

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
            Button {
                withAnimation(.easeInOut(duration: 0.2)) {
                    onToggleMeal(meal.id)
                }
            } label: {
                HStack(spacing: StrakkSpacing.xs) {
                    Text(timeLabel(from: meal.createdAt))
                        .font(.strakkCaptionBold)
                        .foregroundStyle(Color.strakkTextTertiary)
                        .frame(width: 44, alignment: .leading)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(meal.name)
                            .font(.strakkBodyBold)
                            .foregroundStyle(Color.strakkTextPrimary)
                        Text("\(meal.entries.count) item\(meal.entries.count > 1 ? "s" : "")")
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
                .padding(.horizontal, StrakkSpacing.md)
                .padding(.vertical, StrakkSpacing.sm)
            }
            .buttonStyle(.plain)

            if isExpanded {
                VStack(spacing: 0) {
                    Rectangle()
                        .fill(Color.strakkDivider)
                        .frame(height: 1)
                        .padding(.horizontal, StrakkSpacing.md)

                    ForEach(meal.entries) { entry in
                        HStack(spacing: StrakkSpacing.xs) {
                            Spacer().frame(width: 44)
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
                        .padding(.horizontal, StrakkSpacing.md)
                        .padding(.vertical, StrakkSpacing.xs)
                    }
                }
            }
        }
        .background(Color.strakkSurface2)
        .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
        .contextMenu {
            Button(role: .destructive) {
                onDeleteMeal(meal.id)
            } label: {
                Label("Supprimer", systemImage: "trash")
            }
        }
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            Button(role: .destructive) {
                onDeleteMeal(meal.id)
            } label: {
                Label("Supprimer", systemImage: "trash")
            }
        }
        .accessibilityLabel("\(meal.name), \(meal.entries.count) items, \(Int(meal.totalCalories)) kcal")
    }

    private func orphanEntryRow(entry: MealEntryData) -> some View {
        HStack(spacing: StrakkSpacing.xs) {
            Text(timeLabel(from: entry.createdAt))
                .font(.strakkCaptionBold)
                .foregroundStyle(Color.strakkTextTertiary)
                .frame(width: 44, alignment: .leading)

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
        .padding(.horizontal, StrakkSpacing.md)
        .padding(.vertical, StrakkSpacing.sm)
        .background(Color.strakkSurface2)
        .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            Button(role: .destructive) {
                onDeleteEntry(entry.id)
            } label: {
                Label("Supprimer", systemImage: "trash")
            }
        }
        .accessibilityLabel("\(entry.name ?? "Item"), \(Int(entry.calories)) kcal")
    }

    // MARK: - CTA buttons

    private var ctaButtons: some View {
        HStack(spacing: StrakkSpacing.md) {
            // Repas (secondaire)
            Button {
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                onAddMeal()
            } label: {
                HStack(spacing: StrakkSpacing.xs) {
                    Image(systemName: "fork.knife")
                        .font(.system(size: 18, weight: .semibold))
                    Text("Repas")
                        .font(.strakkHeading2)
                }
                .foregroundStyle(Color.strakkTextPrimary)
                .frame(maxWidth: .infinity)
                .frame(height: 64)
                .background(Color.strakkSurface2)
                .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.xl))
                .overlay(
                    RoundedRectangle(cornerRadius: StrakkRadius.xl)
                        .strokeBorder(Color.strakkBorderFaint, lineWidth: 1)
                )
            }
            .accessibilityLabel("Nouveau repas")

            // Rapide (primaire orange)
            Button {
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                onQuickAdd()
            } label: {
                HStack(spacing: StrakkSpacing.xs) {
                    Image(systemName: "bolt.fill")
                        .font(.system(size: 18, weight: .bold))
                    Text("Rapide")
                        .font(.strakkHeading2)
                        .fontWeight(.bold)
                }
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 64)
                .background(
                    LinearGradient(
                        colors: [Color.strakkAccentOrange, Color.strakkAccentOrangeLight],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.xl))
                .overlay(
                    RoundedRectangle(cornerRadius: StrakkRadius.xl)
                        .strokeBorder(.white.opacity(0.18), lineWidth: 1)
                )
                .shadow(color: Color.strakkAccentOrangeGlow, radius: 16, x: 0, y: 6)
            }
            .accessibilityLabel("Ajout rapide")
            .layoutPriority(0.6)
        }
    }

    // MARK: - Helpers

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
}
