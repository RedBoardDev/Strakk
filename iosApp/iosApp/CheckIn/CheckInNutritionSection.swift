import SwiftUI
import shared

// MARK: - CheckInNutritionSection

struct CheckInNutritionSection: View {
    let nutrition: NutritionSummaryData
    var isRefreshing: Bool = false
    var onRefresh: (() -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            HStack(alignment: .center) {
                Text("NUTRITION (\(nutrition.nutritionDays) days)")
                    .font(.strakkOverline)
                    .foregroundStyle(Color.strakkTextTertiary)
                Spacer()
                if let onRefresh {
                    Button(action: onRefresh) {
                        if isRefreshing {
                            ProgressView()
                                .tint(Color.strakkTextTertiary)
                                .scaleEffect(0.75)
                        } else {
                            Image(systemName: "arrow.clockwise")
                                .font(.system(size: 12, weight: .medium))
                                .foregroundStyle(Color.strakkTextTertiary)
                        }
                    }
                    .disabled(isRefreshing)
                    .frame(width: 22, height: 22)
                    .accessibilityLabel(Text("Refresh nutrition"))
                }
            }

            VStack(spacing: StrakkSpacing.sm) {
                LazyVGrid(
                    columns: Array(repeating: GridItem(.flexible(), spacing: StrakkSpacing.xs), count: 2),
                    spacing: StrakkSpacing.xs
                ) {
                    macroCell(label: "Calories",
                              value: String(format: "%.0f kcal", nutrition.avgCalories),
                              color: .strakkAccentOrange,
                              progress: calorieScale)
                    macroCell(label: "Protein",
                              value: String(format: "%.0f g", nutrition.avgProtein),
                              color: .strakkPrimary,
                              progress: proteinScale)
                    macroCell(label: "Carbs",
                              value: String(format: "%.0f g", nutrition.avgCarbs),
                              color: .strakkAccentIndigo,
                              progress: carbsScale)
                    macroCell(label: "Fat",
                              value: String(format: "%.0f g", nutrition.avgFat),
                              color: .strakkAccentYellow,
                              progress: fatScale)
                }

                HStack {
                    Image(systemName: "drop.fill")
                        .foregroundStyle(Color.strakkWater)
                        .font(.system(size: 13))
                    Text("Water: \(nutrition.avgWater) ml/day avg")
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextSecondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                if !nutrition.dailyData.isEmpty {
                    DailyNutritionTable(days: nutrition.dailyData)
                }

            }
            .padding(StrakkSpacing.md)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    @ViewBuilder
    private func macroCell(label: LocalizedStringKey, value: String, color: Color, progress: Double) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xxs) {
            Text(label)
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
            Text(value)
                .font(.strakkBodyBold)
                .foregroundStyle(color)
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(Color.strakkSurface1)
                        .frame(height: 4)
                    Capsule()
                        .fill(color)
                        .frame(width: geo.size.width * min(progress, 1.0), height: 4)
                }
            }
            .frame(height: 4)
        }
        .padding(StrakkSpacing.sm)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.strakkSurface2)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    /// Relative progress of each macro compared to the highest value.
    private var maxCalories: Double { max(nutrition.avgCalories, 1) }
    private var calorieScale: Double { 1.0 }
    private var proteinScale: Double { (nutrition.avgProtein * 4) / maxCalories }
    private var carbsScale: Double { (nutrition.avgCarbs * 4) / maxCalories }
    private var fatScale: Double { (nutrition.avgFat * 9) / maxCalories }

}

// MARK: - CheckInEmptyNutritionSection

struct CheckInEmptyNutritionSection: View {
    var body: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("NUTRITION")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            Text("No nutrition data")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextTertiary)
                .padding(StrakkSpacing.md)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.strakkSurface1)
                .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }
}

// MARK: - DailyNutritionTable

struct DailyNutritionTable: View {
    let days: [DailyNutritionData]

    private static let shortDateInputFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        return formatter
    }()

    private static let shortDateDisplayFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "d MMM"
        formatter.locale = Locale.current
        return formatter
    }()

    private func formatShortDate(_ dateString: String) -> String {
        guard let date = Self.shortDateInputFormatter.date(from: dateString) else { return dateString }
        return Self.shortDateDisplayFormatter.string(from: date)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("PER DAY")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)
                .padding(.bottom, 8)

            VStack(spacing: 0) {
                HStack {
                    Text("Date")
                        .frame(width: 60, alignment: .leading)
                    Text("Cal.")
                        .frame(maxWidth: .infinity, alignment: .trailing)
                    Text("Prot.")
                        .frame(maxWidth: .infinity, alignment: .trailing)
                    Text("Carbs")
                        .frame(maxWidth: .infinity, alignment: .trailing)
                    Text("Fat")
                        .frame(maxWidth: .infinity, alignment: .trailing)
                    Text("Water")
                        .frame(maxWidth: .infinity, alignment: .trailing)
                }
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color.strakkSurface2)

                ForEach(Array(days.enumerated()), id: \.element.id) { idx, day in
                    HStack {
                        Text(formatShortDate(day.date))
                            .frame(width: 60, alignment: .leading)
                        Text(String(format: "%.0f", day.calories))
                            .frame(maxWidth: .infinity, alignment: .trailing)
                        Text(String(format: "%.0fg", day.protein))
                            .frame(maxWidth: .infinity, alignment: .trailing)
                        Text(String(format: "%.0fg", day.carbs))
                            .frame(maxWidth: .infinity, alignment: .trailing)
                        Text(String(format: "%.0fg", day.fat))
                            .frame(maxWidth: .infinity, alignment: .trailing)
                        Text(day.waterMl > 0 ? String(format: "%.1fL", Double(day.waterMl) / 1000.0) : "—")
                            .frame(maxWidth: .infinity, alignment: .trailing)
                    }
                    .font(.strakkCaption)
                    .foregroundStyle(idx % 2 == 0 ? Color.strakkTextPrimary : Color.strakkTextSecondary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(idx % 2 == 1 ? Color.strakkSurface2 : Color.clear)
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
    }
}
