import SwiftUI

// MARK: - ProgressSection — 2×2 macro grid
//
// Four macro cards arranged in a LazyVGrid, each showing:
//   icon-case + label
//   consumed / goal value
//   progress bar (only when a goal is available)

struct ProgressSection: View {
    let summary: DailySummaryData

    var body: some View {
        LazyVGrid(
            columns: [GridItem(.flexible()), GridItem(.flexible())],
            spacing: StrakkSpacing.sm
        ) {
            MacroCard(
                icon: "dumbbell.fill",
                label: "Protéines",
                consumed: summary.totalProtein,
                goal: summary.proteinGoal,
                unit: "g",
                color: .strakkPrimary
            )
            MacroCard(
                icon: "flame.fill",
                label: "Calories",
                consumed: summary.totalCalories,
                goal: summary.calorieGoal,
                unit: "kcal",
                color: .strakkCalories
            )
            MacroCard(
                icon: "drop.fill",
                label: "Lipides",
                consumed: summary.totalFat,
                goal: nil,
                unit: "g",
                color: .strakkAccentYellow
            )
            MacroCard(
                icon: "leaf.fill",
                label: "Glucides",
                consumed: summary.totalCarbs,
                goal: nil,
                unit: "g",
                color: .strakkAccentIndigo
            )
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(accessibilitySummary)
    }

    private var accessibilitySummary: String {
        var parts: [String] = []
        parts.append("Protéines : \(Int(summary.totalProtein)) g")
        if let goal = summary.proteinGoal { parts.append("sur \(goal) g") }
        parts.append("Calories : \(Int(summary.totalCalories)) kcal")
        if let goal = summary.calorieGoal { parts.append("sur \(goal) kcal") }
        parts.append("Lipides : \(Int(summary.totalFat)) g")
        parts.append("Glucides : \(Int(summary.totalCarbs)) g")
        return parts.joined(separator: ". ")
    }
}

// MARK: - MacroCard

private struct MacroCard: View {
    let icon: String
    let label: String
    let consumed: Double
    let goal: Int?
    let unit: String
    let color: Color

    private var progress: Double {
        guard let goal, goal > 0 else { return 0 }
        return min(consumed / Double(goal), 1.0)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            // Icon + label row
            HStack(spacing: 8) {
                ZStack {
                    RoundedRectangle(cornerRadius: 7)
                        .fill(color.opacity(0.12))
                        .frame(width: 28, height: 28)
                    Image(systemName: icon)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(color)
                }
                Text(label)
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextSecondary)
                    .lineLimit(1)
            }

            // Value line
            HStack(alignment: .lastTextBaseline, spacing: 2) {
                Text("\(Int(consumed))")
                    .font(.strakkHeading2)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .monospacedDigit()
                    .contentTransition(.numericText())

                if let goal {
                    Text("/ \(goal)\(unit)")
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextTertiary)
                } else {
                    Text(unit)
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextTertiary)
                }
            }

            // Progress bar — always shown for uniform card height; fills to 0 when no goal
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(Color.strakkSurface2)
                        .frame(height: 4)
                    Capsule()
                        .fill(color)
                        .frame(width: progress * geo.size.width, height: 4)
                        .animation(.easeOut(duration: 0.4), value: progress)
                }
            }
            .frame(height: 4)
        }
        .padding(StrakkSpacing.md)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
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
