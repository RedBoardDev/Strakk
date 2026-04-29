import SwiftUI

// MARK: - ProgressSection — Hero protéines + ledger horizontal
//
// Card unique surface-1 :
//   - Gauche : ring protéines 96pt avec valeur centrale
//   - Séparateur vertical 1pt
//   - Droite : ledger 3 lignes (Calories / Glucides / Lipides) avec icône-case
//
// Aucun fond teinté primaire — l'accent vient du ring et du label.

struct ProgressSection: View {
    let summary: DailySummaryData

    private var proteinProgress: Double {
        guard let goal = summary.proteinGoal, goal > 0 else { return 0 }
        return min(summary.totalProtein / Double(goal), 2.0)
    }

    private var isProteinReached: Bool { proteinProgress >= 1.0 }

    private var ringColor: Color {
        isProteinReached ? Color.strakkSuccess : Color.strakkPrimary
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Header label "PROTÉINES" + check optionnel
            HStack {
                Text("PROTÉINES")
                    .font(.strakkOverline)
                    .foregroundStyle(Color.strakkPrimary)

                if isProteinReached {
                    Spacer()
                    Text("✓ Objectif atteint")
                        .font(.strakkCaptionBold)
                        .foregroundStyle(Color.strakkSuccess)
                }
            }

            HStack(alignment: .center, spacing: 20) {
                // Ring protéines (gauche)
                ProteinRing(
                    progress: min(proteinProgress, 1.0),
                    totalProtein: summary.totalProtein,
                    proteinGoal: summary.proteinGoal,
                    ringColor: ringColor
                )

                // Séparateur vertical
                Rectangle()
                    .fill(Color.strakkDivider)
                    .frame(width: 1)
                    .frame(maxHeight: .infinity)

                // Ledger 3 macros (droite)
                VStack(spacing: 0) {
                    LedgerRow(
                        icon: "flame.fill",
                        iconTint: Color.strakkPrimary,
                        label: "CALORIES",
                        value: "\(Int(summary.totalCalories))",
                        suffix: summary.calorieGoal.map { "/ \($0) kcal" } ?? "kcal"
                    )
                    Divider()
                        .overlay(Color.strakkDivider)
                        .padding(.vertical, 10)
                    LedgerRow(
                        icon: "leaf.fill",
                        iconTint: Color.strakkWater,
                        label: "GLUCIDES",
                        value: "\(Int(summary.totalCarbs))",
                        suffix: "g"
                    )
                    Divider()
                        .overlay(Color.strakkDivider)
                        .padding(.vertical, 10)
                    LedgerRow(
                        icon: "drop.fill",
                        iconTint: Color.strakkWarning,
                        label: "LIPIDES",
                        value: "\(Int(summary.totalFat))",
                        suffix: "g"
                    )
                }
                .frame(maxWidth: .infinity)
            }
        }
        .padding(16)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .accessibilityElement(children: .combine)
        .accessibilityLabel(accessibilitySummary)
    }

    private var accessibilitySummary: String {
        var parts: [String] = []
        parts.append("Protéines : \(Int(summary.totalProtein)) g")
        if let goal = summary.proteinGoal { parts.append("sur \(goal) g") }
        parts.append("Calories : \(Int(summary.totalCalories)) kcal")
        parts.append("Glucides : \(Int(summary.totalCarbs)) g")
        parts.append("Lipides : \(Int(summary.totalFat)) g")
        return parts.joined(separator: ". ")
    }
}

// MARK: - ProteinRing

private struct ProteinRing: View {
    let progress: Double
    let totalProtein: Double
    let proteinGoal: Int?
    let ringColor: Color

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color.strakkSurface3, lineWidth: 6)
                .frame(width: 96, height: 96)
            Circle()
                .trim(from: 0, to: progress)
                .stroke(
                    ringColor,
                    style: StrokeStyle(lineWidth: 6, lineCap: .round)
                )
                .frame(width: 96, height: 96)
                .rotationEffect(.degrees(-90))
                .animation(.easeOut(duration: 0.4), value: progress)

            VStack(spacing: 0) {
                Text("\(Int(totalProtein))")
                    .font(.system(size: 28, weight: .bold))
                    .monospacedDigit()
                    .contentTransition(.numericText())
                    .foregroundStyle(Color.strakkTextPrimary)
                Text(proteinGoal.map { "/ \($0) g" } ?? "g")
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextTertiary)
            }
        }
    }
}

// MARK: - LedgerRow (icon-case + label + value)

private struct LedgerRow: View {
    let icon: String
    let iconTint: Color
    let label: String
    let value: String
    let suffix: String

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            // Icon case
            ZStack {
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color.strakkSurface3)
                    .frame(width: 32, height: 32)
                Image(systemName: icon)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(iconTint)
            }

            Text(label)
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextSecondary)

            Spacer()

            HStack(alignment: .firstTextBaseline, spacing: 4) {
                Text(value)
                    .font(.strakkHeading3)
                    .fontWeight(.semibold)
                    .monospacedDigit()
                    .contentTransition(.numericText())
                    .foregroundStyle(Color.strakkTextPrimary)
                Text(suffix)
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextTertiary)
            }
        }
    }
}

// MARK: - Preview

#Preview("Mid-day") {
    ProgressSection(summary: DailySummaryData(
        totalProtein: 142,
        totalCalories: 1840,
        totalFat: 54,
        totalCarbs: 210,
        totalWater: 1500,
        proteinGoal: 160,
        calorieGoal: 2200,
        waterGoal: 2500
    ))
    .padding()
    .background(Color.strakkBackground)
}

#Preview("Empty") {
    ProgressSection(summary: DailySummaryData(
        totalProtein: 0,
        totalCalories: 0,
        totalFat: 0,
        totalCarbs: 0,
        totalWater: 0,
        proteinGoal: 105,
        calorieGoal: 2200,
        waterGoal: 2500
    ))
    .padding()
    .background(Color.strakkBackground)
}

#Preview("Reached") {
    ProgressSection(summary: DailySummaryData(
        totalProtein: 165,
        totalCalories: 2150,
        totalFat: 68,
        totalCarbs: 248,
        totalWater: 2300,
        proteinGoal: 160,
        calorieGoal: 2200,
        waterGoal: 2500
    ))
    .padding()
    .background(Color.strakkBackground)
}
