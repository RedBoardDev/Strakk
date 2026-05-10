import SwiftUI

// MARK: - MacroCard — DESIGN.md §5 "Macro card".
//
// Used in BOTH Today and Calendar Day views to keep nutrition language
// consistent. Always renders the progress bar; when no goal is provided the
// bar fills to 0 and the unit appears without `/ goal`. When the goal is
// reached or exceeded, the fill switches to `success` (green).

struct MacroCard: View {
    let icon: String
    let label: LocalizedStringKey
    let consumed: Double
    let goal: Int?
    let unit: LocalizedStringKey
    let unitForFormatting: String
    let color: Color

    init(
        icon: String,
        label: LocalizedStringKey,
        consumed: Double,
        goal: Int?,
        unit: LocalizedStringKey,
        unitForFormatting: String,
        color: Color
    ) {
        self.icon = icon
        self.label = label
        self.consumed = consumed
        self.goal = goal
        self.unit = unit
        self.unitForFormatting = unitForFormatting
        self.color = color
    }

    private var progress: Double {
        guard let goal, goal > 0 else { return 0 }
        return min(consumed / Double(goal), 1.0)
    }

    private var hasReachedGoal: Bool {
        guard let goal, goal > 0 else { return false }
        return consumed >= Double(goal)
    }

    private var fillColor: Color {
        hasReachedGoal ? Color.strakkSuccess : color
    }

    var body: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            HStack(spacing: StrakkSpacing.xs) {
                ZStack {
                    RoundedRectangle(cornerRadius: 7, style: .continuous)
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

            HStack(alignment: .lastTextBaseline, spacing: 2) {
                Text("\(Int(consumed))")
                    .font(.strakkHeading2)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .monospacedDigit()
                    .contentTransition(.numericText())

                if let goal {
                    Text("/ \(goal)\(unitForFormatting)")
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextTertiary)
                } else {
                    Text(unit)
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextTertiary)
                }
            }

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(Color.strakkSurface2)
                        .frame(height: 4)
                    Capsule()
                        .fill(fillColor)
                        .frame(width: progress * geo.size.width, height: 4)
                        .animation(.easeOut(duration: 0.25), value: progress)
                        .animation(.easeOut(duration: 0.25), value: hasReachedGoal)
                }
            }
            .frame(height: 4)
        }
        .padding(StrakkSpacing.md)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm, style: .continuous))
    }
}

// MARK: - MacroProgressGrid — 2x2 macro layout shared by Today and Calendar.

struct MacroProgressGrid: View {
    let totalProtein: Double
    let totalCalories: Double
    let totalFat: Double
    let totalCarbs: Double
    let proteinGoal: Int?
    let calorieGoal: Int?
    let fatGoal: Int?
    let carbGoal: Int?

    init(
        totalProtein: Double,
        totalCalories: Double,
        totalFat: Double,
        totalCarbs: Double,
        proteinGoal: Int? = nil,
        calorieGoal: Int? = nil,
        fatGoal: Int? = nil,
        carbGoal: Int? = nil
    ) {
        self.totalProtein = totalProtein
        self.totalCalories = totalCalories
        self.totalFat = totalFat
        self.totalCarbs = totalCarbs
        self.proteinGoal = proteinGoal
        self.calorieGoal = calorieGoal
        self.fatGoal = fatGoal
        self.carbGoal = carbGoal
    }

    var body: some View {
        LazyVGrid(
            columns: [GridItem(.flexible()), GridItem(.flexible())],
            spacing: StrakkSpacing.sm
        ) {
            MacroCard(
                icon: "dumbbell.fill",
                label: "Protein",
                consumed: totalProtein,
                goal: proteinGoal,
                unit: "g",
                unitForFormatting: "g",
                color: .strakkPrimary
            )
            MacroCard(
                icon: "flame.fill",
                label: "Calories",
                consumed: totalCalories,
                goal: calorieGoal,
                unit: "kcal",
                unitForFormatting: "kcal",
                color: .strakkPrimaryLight
            )
            MacroCard(
                icon: "drop.fill",
                label: "Fat",
                consumed: totalFat,
                goal: fatGoal,
                unit: "g",
                unitForFormatting: "g",
                color: .strakkAccentYellow
            )
            MacroCard(
                icon: "leaf.fill",
                label: "Carbs",
                consumed: totalCarbs,
                goal: carbGoal,
                unit: "g",
                unitForFormatting: "g",
                color: .strakkAccentIndigo
            )
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(accessibilitySummary)
    }

    private var accessibilitySummary: String {
        var parts: [String] = []
        parts.append(String(localized: "Protein: \(Int(totalProtein)) g"))
        if let goal = proteinGoal {
            parts.append(String(localized: "Goal \(goal) g"))
        }
        parts.append(String(localized: "Calories: \(Int(totalCalories)) kcal"))
        if let goal = calorieGoal {
            parts.append(String(localized: "Goal \(goal) kcal"))
        }
        parts.append(String(localized: "Fat: \(Int(totalFat)) g"))
        if let goal = fatGoal {
            parts.append(String(localized: "Goal \(goal) g"))
        }
        parts.append(String(localized: "Carbs: \(Int(totalCarbs)) g"))
        if let goal = carbGoal {
            parts.append(String(localized: "Goal \(goal) g"))
        }
        return parts.joined(separator: ". ")
    }
}

#Preview("Macro grid") {
    MacroProgressGrid(
        totalProtein: 142,
        totalCalories: 1840,
        totalFat: 54,
        totalCarbs: 210,
        proteinGoal: 160,
        calorieGoal: 2200,
        fatGoal: 70,
        carbGoal: 240
    )
    .padding(StrakkSpacing.lg)
    .background(Color.strakkBackground)
}

#Preview("Macro grid - reached") {
    MacroProgressGrid(
        totalProtein: 165,
        totalCalories: 2210,
        totalFat: 75,
        totalCarbs: 245,
        proteinGoal: 160,
        calorieGoal: 2200,
        fatGoal: 70,
        carbGoal: 240
    )
    .padding(StrakkSpacing.lg)
    .background(Color.strakkBackground)
}
