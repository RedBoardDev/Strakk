import SwiftUI
import shared

// MARK: - CheckInNutritionSection

struct CheckInNutritionSection: View {
    let nutrition: NutritionSummaryData

    var body: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("NUTRITION (\(nutrition.nutritionDays) jour\(nutrition.nutritionDays > 1 ? "s" : ""))")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            VStack(spacing: StrakkSpacing.sm) {
                LazyVGrid(
                    columns: Array(repeating: GridItem(.flexible(), spacing: StrakkSpacing.xs), count: 2),
                    spacing: StrakkSpacing.xs
                ) {
                    macroCell(label: "Calories", value: String(format: "%.0f kcal", nutrition.avgCalories), color: .strakkAccentOrange)
                    macroCell(label: "Protéines", value: String(format: "%.0f g", nutrition.avgProtein), color: .strakkPrimary)
                    macroCell(label: "Glucides", value: String(format: "%.0f g", nutrition.avgCarbs), color: .strakkAccentIndigo)
                    macroCell(label: "Lipides", value: String(format: "%.0f g", nutrition.avgFat), color: .strakkAccentYellow)
                }

                HStack {
                    Image(systemName: "drop.fill")
                        .foregroundStyle(Color.strakkWater)
                        .font(.system(size: 13))
                    Text("Eau : \(nutrition.avgWater) ml/j en moy.")
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextSecondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                if !nutrition.dailyData.isEmpty {
                    DailyNutritionTable(days: nutrition.dailyData)
                }

                if let summary = nutrition.aiSummary, !summary.isEmpty {
                    aiSummaryCard(summary)
                }
            }
            .padding(StrakkSpacing.md)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    @ViewBuilder
    private func macroCell(label: String, value: String, color: Color) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xxs) {
            Text(label)
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
            Text(value)
                .font(.strakkBodyBold)
                .foregroundStyle(color)
        }
        .padding(StrakkSpacing.sm)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.strakkSurface2)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    private func aiSummaryCard(_ summary: String) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xxs) {
            HStack(spacing: StrakkSpacing.xxs) {
                Image(systemName: "sparkles")
                    .font(.system(size: 11))
                    .foregroundStyle(Color.strakkPrimary)
                Text("AI Summary")
                    .font(.strakkOverline)
                    .foregroundStyle(Color.strakkPrimary)
            }
            Text(summary)
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
        }
        .padding(StrakkSpacing.sm)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.strakkAccentOrangeFaint)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .strokeBorder(Color.strakkAccentOrangeBorder, lineWidth: 1)
        )
    }
}

// MARK: - CheckInEmptyNutritionSection

struct CheckInEmptyNutritionSection: View {
    var body: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("NUTRITION")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            Text("Aucune donnée nutritionnelle")
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
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "fr_FR")
        return f
    }()

    private static let shortDateDisplayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "d MMM"
        f.locale = Locale(identifier: "fr_FR")
        return f
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
