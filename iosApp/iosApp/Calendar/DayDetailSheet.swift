import SwiftUI

struct DayDetailSheet: View {
    let detail: CalendarDayDetailData
    let onDismiss: () -> Void
    let onAddMeal: () -> Void

    var body: some View {
        NavigationStack {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        // MARK: Macros section
                        sectionHeader("NUTRITION")
                            .padding(.top, 20)

                        VStack(spacing: 8) {
                            macroRow(
                                label: "Protéines",
                                value: String(format: "%.0fg", detail.summary.totalProtein),
                                color: Color.strakkProtein,
                                current: detail.summary.totalProtein,
                                goal: detail.summary.proteinGoal.map { Double($0) }
                            )
                            macroRow(
                                label: "Calories",
                                value: String(format: "%.0f kcal", detail.summary.totalCalories),
                                color: Color.strakkCalories,
                                current: detail.summary.totalCalories,
                                goal: detail.summary.calorieGoal.map { Double($0) }
                            )
                            simpleRow(
                                label: "Lipides",
                                value: String(format: "%.0fg", detail.summary.totalFat)
                            )
                            simpleRow(
                                label: "Glucides",
                                value: String(format: "%.0fg", detail.summary.totalCarbs)
                            )
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 8)

                        // MARK: Water section
                        sectionHeader("EAU")
                            .padding(.top, 24)

                        macroRow(
                            label: "Consommation",
                            value: String(format: "%d mL", detail.summary.totalWater),
                            color: Color.strakkWater,
                            current: Double(detail.summary.totalWater),
                            goal: detail.summary.waterGoal.map { Double($0) }
                        )
                        .padding(.horizontal, 20)
                        .padding(.top, 8)

                        // MARK: Meals section
                        if !detail.meals.isEmpty {
                            sectionHeader("REPAS")
                                .padding(.top, 24)

                            VStack(spacing: 0) {
                                ForEach(Array(detail.meals.enumerated()), id: \.element.id) { idx, meal in
                                    VStack(spacing: 0) {
                                        if idx > 0 {
                                            Divider()
                                                .background(Color.strakkDivider)
                                                .padding(.leading, 16)
                                        }
                                        readOnlyMealRow(meal: meal)
                                    }
                                }
                            }
                            .background(Color.strakkSurface1)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .padding(.horizontal, 20)
                            .padding(.top, 8)
                        }

                        // MARK: Add meal button
                        Button(action: onAddMeal) {
                            HStack(spacing: 8) {
                                Image(systemName: "plus.circle")
                                    .font(.system(size: 16, weight: .semibold))
                                Text("Ajouter pour ce jour")
                                    .font(.strakkBodyBold)
                            }
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(Color.strakkPrimary)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        .accessibilityLabel("Ajouter un repas pour ce jour")
                        .padding(.horizontal, 20)
                        .padding(.top, 28)
                        .padding(.bottom, 32)
                    }
                }
            }
            .navigationTitle(formatDate(detail.date))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        onDismiss()
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 20))
                            .foregroundStyle(Color.strakkTextSecondary)
                    }
                    .accessibilityLabel("Fermer")
                }
            }
        }
    }

    // MARK: - Subviews

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.strakkOverline)
            .foregroundStyle(Color.strakkTextTertiary)
            .kerning(1.0)
            .padding(.horizontal, 20)
    }

    private func macroRow(
        label: String,
        value: String,
        color: Color,
        current: Double,
        goal: Double?
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(label)
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextPrimary)
                Spacer()
                Text(value)
                    .font(.strakkBodyBold)
                    .foregroundStyle(color)
                    .monospacedDigit()
                if let goal {
                    Text("/ \(Int(goal))")
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextTertiary)
                }
            }

            if let goal, goal > 0 {
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 4)
                            .fill(Color.strakkSurface2)
                            .frame(height: 6)

                        RoundedRectangle(cornerRadius: 4)
                            .fill(current >= goal ? Color.strakkSuccess : color)
                            .frame(width: min(geo.size.width * CGFloat(current / goal), geo.size.width), height: 6)
                            .animation(.easeOut(duration: 0.3), value: current)
                    }
                }
                .frame(height: 6)
            }
        }
        .padding(16)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func simpleRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextPrimary)
            Spacer()
            Text(value)
                .font(.strakkBodyBold)
                .foregroundStyle(Color.strakkTextSecondary)
                .monospacedDigit()
        }
        .padding(16)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func readOnlyMealRow(meal: MealEntryData) -> some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 3) {
                Text(meal.name ?? "Repas")
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .lineLimit(1)

                HStack(spacing: 4) {
                    Text(String(format: "%.0fg prot", meal.protein))
                        .foregroundStyle(Color.strakkPrimary)
                    Text("·")
                        .foregroundStyle(Color.strakkTextTertiary)
                    Text(String(format: "%.0f kcal", meal.calories))
                        .foregroundStyle(Color.strakkTextSecondary)
                }
                .font(.strakkCaption)
            }
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }

    // MARK: - Date formatting

    private func formatDate(_ dateString: String) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "fr_FR")

        guard let date = formatter.date(from: dateString) else { return dateString }

        let display = DateFormatter()
        display.dateFormat = "EEEE d MMMM"
        display.locale = Locale(identifier: "fr_FR")
        return display.string(from: date).capitalized
    }
}
