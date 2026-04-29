import SwiftUI

// MARK: - WizardStepSummaryView

struct WizardStepSummaryView: View {
    let nutritionSummary: NutritionSummaryData?
    let nutritionLoading: Bool
    let weight: String
    let delta: CheckInDeltaData?
    let photoCount: Int
    let selectedTags: Set<String>
    let mentalFeeling: String
    let physicalFeeling: String

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: StrakkSpacing.xl) {
                // Nutrition card
                nutritionCard

                // Recap card
                recapCard

                Spacer().frame(height: StrakkSpacing.xl)
            }
            .padding(.horizontal, StrakkSpacing.lg)
            .padding(.vertical, StrakkSpacing.xl)
        }
    }

    // MARK: - Nutrition card

    private var nutritionCard: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.md) {
            Text("NUTRITION")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            VStack(alignment: .leading, spacing: StrakkSpacing.md) {
                if nutritionLoading {
                    HStack(spacing: StrakkSpacing.sm) {
                        ProgressView()
                            .tint(Color.strakkPrimary)
                        Text("Calcul en cours...")
                            .font(.strakkBody)
                            .foregroundStyle(Color.strakkTextSecondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.vertical, StrakkSpacing.lg)
                } else if let summary = nutritionSummary {
                    nutritionSummaryContent(summary: summary)
                } else {
                    Text("Aucune donnée nutritionnelle pour ces jours.")
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextTertiary)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.vertical, StrakkSpacing.lg)
                }
            }
            .padding(StrakkSpacing.md)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    @ViewBuilder
    private func nutritionSummaryContent(summary: NutritionSummaryData) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.md) {
            Text("Moy. journalière (\(summary.nutritionDays) jour\(summary.nutritionDays > 1 ? "s" : ""))")
                .font(.strakkCaptionBold)
                .foregroundStyle(Color.strakkTextSecondary)

            // Macros grid
            LazyVGrid(
                columns: Array(repeating: GridItem(.flexible()), count: 2),
                spacing: StrakkSpacing.sm
            ) {
                macroCell(label: "Protéines", value: "\(Int(summary.avgProtein))g", color: Color.strakkPrimary)
                macroCell(label: "Calories", value: "\(Int(summary.avgCalories))", color: Color.strakkAccentOrangeLight)
                macroCell(label: "Lipides", value: "\(Int(summary.avgFat))g", color: Color.strakkAccentYellow)
                macroCell(label: "Glucides", value: "\(Int(summary.avgCarbs))g", color: Color.strakkAccentIndigo)
                macroCell(label: "Eau", value: String(format: "%.1fL", Double(summary.avgWater) / 1000.0), color: Color.strakkWater)
            }

            // AI summary
            if let aiSummary = summary.aiSummary, !aiSummary.isEmpty {
                Text(aiSummary)
                    .font(.strakkBody)
                    .italic()
                    .foregroundStyle(Color.strakkTextSecondary)
                    .padding(.top, StrakkSpacing.xxs)
            }
        }
    }

    private func macroCell(label: String, value: String, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(value)
                .font(.strakkBodyBold)
                .foregroundStyle(color)
                .monospacedDigit()
            Text(label)
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(StrakkSpacing.sm)
        .background(Color.strakkSurface2)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    // MARK: - Recap card

    private var recapCard: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.md) {
            Text("RÉCAP")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            VStack(alignment: .leading, spacing: StrakkSpacing.md) {
                // Weight row
                if !weight.isEmpty {
                    recapRow(icon: "scalemass.fill", label: "Poids", value: weightWithDelta)
                }

                // Photo count
                recapRow(
                    icon: "photo.fill",
                    label: "Photos",
                    value: "\(photoCount)/3"
                )

                // Tags
                if !selectedTags.isEmpty {
                    tagChipsRow
                }

                if !mentalFeeling.isEmpty {
                    feelingPreview(title: "Mental", text: mentalFeeling)
                }

                if !physicalFeeling.isEmpty {
                    feelingPreview(title: "Physique", text: physicalFeeling)
                }
            }
            .padding(StrakkSpacing.md)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    // MARK: - Recap helpers

    private func recapRow(icon: String, label: String, value: String) -> some View {
        HStack(spacing: StrakkSpacing.xs) {
            Image(systemName: icon)
                .font(.system(size: 13))
                .foregroundStyle(Color.strakkTextTertiary)
                .frame(width: 16)

            Text(label)
                .font(.strakkCaptionBold)
                .foregroundStyle(Color.strakkTextSecondary)

            Spacer()

            Text(value)
                .font(.strakkBodyBold)
                .foregroundStyle(Color.strakkTextPrimary)
                .monospacedDigit()
        }
    }

    private var weightWithDelta: String {
        var result = "\(weight) kg"
        if let deltaWeight = delta?.weight {
            if deltaWeight > 0 {
                result += " (↑ +\(String(format: "%.1f", deltaWeight)))"
            } else if deltaWeight < 0 {
                result += " (↓ \(String(format: "%.1f", deltaWeight)))"
            }
        }
        return result
    }

    private func feelingPreview(title: String, text: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: StrakkSpacing.xs) {
                Image(systemName: "text.quote")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.strakkTextTertiary)
                Text(title)
                    .font(.strakkCaptionBold)
                    .foregroundStyle(Color.strakkTextSecondary)
            }
            Text(text)
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
                .lineLimit(3)
                .truncationMode(.tail)
        }
    }

    private var tagChipsRow: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            HStack(spacing: StrakkSpacing.xs) {
                Image(systemName: "tag.fill")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.strakkTextTertiary)
                Text("Ressentis")
                    .font(.strakkCaptionBold)
                    .foregroundStyle(Color.strakkTextSecondary)
            }

            FlowLayout(spacing: StrakkSpacing.xxs + 2) {
                ForEach(Array(selectedTags), id: \.self) { tag in
                    Text(feelingTagLabel(tag))
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextSecondary)
                        .padding(.horizontal, StrakkSpacing.xs)
                        .padding(.vertical, 3)
                        .background(Color.strakkSurface2)
                        .clipShape(Capsule())
                }
            }
        }
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
}

// MARK: - FlowLayout (local, non-private so visible to inner types)

private struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout Void) -> CGSize {
        let availableWidth = proposal.width ?? 0
        var height: CGFloat = 0
        var currentRowWidth: CGFloat = 0
        var currentRowHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if currentRowWidth + size.width > availableWidth, currentRowWidth > 0 {
                height += currentRowHeight + spacing
                currentRowWidth = 0
                currentRowHeight = 0
            }
            currentRowWidth += size.width + spacing
            currentRowHeight = max(currentRowHeight, size.height)
        }
        height += currentRowHeight
        return CGSize(width: availableWidth, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout Void) {
        var currentX = bounds.minX
        var currentY = bounds.minY
        var rowHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if currentX + size.width > bounds.maxX, currentX > bounds.minX {
                currentX = bounds.minX
                currentY += rowHeight + spacing
                rowHeight = 0
            }
            subview.place(at: CGPoint(x: currentX, y: currentY), proposal: ProposedViewSize(size))
            currentX += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}

// MARK: - Preview

#Preview {
    ZStack {
        Color.strakkBackground.ignoresSafeArea()
        WizardStepSummaryView(
            nutritionSummary: NutritionSummaryData(
                avgProtein: 145,
                avgCalories: 2100,
                avgFat: 72,
                avgCarbs: 210,
                avgWater: 2400,
                nutritionDays: 5,
                aiSummary: "Bonne semaine, apports protéiques dans la cible.",
                dailyData: []
            ),
            nutritionLoading: false,
            weight: "78.5",
            delta: CheckInDeltaData(
                weight: -0.5,
                shoulders: nil, chest: nil,
                armLeft: nil, armRight: nil,
                waist: nil, hips: nil,
                thighLeft: nil, thighRight: nil
            ),
            photoCount: 2,
            selectedTags: ["good_energy", "good_sleep"],
            mentalFeeling: "Bonne motivation dans l'ensemble.",
            physicalFeeling: "Énergie correcte, sommeil meilleur."
        )
    }
}
