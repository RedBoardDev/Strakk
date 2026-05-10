import SwiftUI
import shared

// MARK: - FrequentFoodRow

struct FrequentFoodRow: View {
    let item: FrequentItemData
    let isSelected: Bool
    @Binding var portionGrams: Double
    let isProcessing: Bool
    let onTap: () -> Void
    let onAdd: (_ protein: Double, _ calories: Double, _ fat: Double?, _ carbs: Double?, _ grams: Double) -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                Image(systemName: "clock")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.strakkTextTertiary)
                    .frame(width: 18)
                VStack(alignment: .leading, spacing: 2) {
                    Text(item.name ?? item.normalizedName)
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextPrimary)
                    Text(String(format: "%.0f kcal", item.calories))
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextSecondary)
                }
                Spacer()
                if let qty = item.quantity {
                    Text(qty)
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextTertiary)
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .contentShape(Rectangle())
            .onTapGesture { onTap() }

            if isSelected {
                PortionRowView(
                    name: item.name ?? item.normalizedName,
                    proteinPer100: item.protein,
                    caloriesPer100: item.calories,
                    fatPer100: item.fat,
                    carbsPer100: item.carbs,
                    portionGrams: $portionGrams,
                    isProcessing: isProcessing,
                    onAdd: onAdd
                )
                .transition(.opacity.combined(with: .move(edge: .top)))
            }

            Divider()
                .background(Color.strakkDivider)
                .padding(.leading, 50)
        }
    }
}

// MARK: - CatalogFoodRow

struct CatalogFoodRow: View {
    let item: FoodCatalogItemData
    let isSelected: Bool
    @Binding var portionGrams: Double
    let isProcessing: Bool
    let onTap: () -> Void
    let onAdd: (_ protein: Double, _ calories: Double, _ fat: Double?, _ carbs: Double?, _ grams: Double) -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: "book.closed")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.strakkTextTertiary)
                    .frame(width: 18)
                    .padding(.top, 2)
                catalogItemDetails
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .contentShape(Rectangle())
            .onTapGesture { onTap() }

            if isSelected {
                PortionRowView(
                    name: item.name,
                    proteinPer100: item.protein,
                    caloriesPer100: item.calories,
                    fatPer100: item.fat,
                    carbsPer100: item.carbs,
                    portionGrams: $portionGrams,
                    isProcessing: isProcessing,
                    onAdd: onAdd
                )
                .transition(.opacity.combined(with: .move(edge: .top)))
            }

            Divider()
                .background(Color.strakkDivider)
                .padding(.leading, 50)
        }
    }

    private var catalogItemDetails: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(alignment: .top, spacing: 8) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(item.name)
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextPrimary)
                        .lineLimit(2)
                    if let brand = item.brand, !brand.isEmpty {
                        Text(brand)
                            .font(.strakkCaption)
                            .foregroundStyle(Color.strakkTextSecondary)
                    }
                }
                Spacer(minLength: 0)
                if let grade = item.nutriscore?.first {
                    NutriscoreBadgeView(grade: grade)
                }
            }
            CatalogMacroLine(item: item)
            HStack(spacing: 4) {
                Image(systemName: "scalemass")
                    .font(.system(size: 10, weight: .medium))
                Text("values per 100 g")
                    .font(.strakkCaption)
            }
            .foregroundStyle(Color.strakkTextTertiary)
        }
    }
}

// MARK: - PortionRowView

struct PortionRowView: View {
    let name: String
    let proteinPer100: Double
    let caloriesPer100: Double
    let fatPer100: Double?
    let carbsPer100: Double?
    @Binding var portionGrams: Double
    let isProcessing: Bool
    let onAdd: (_ protein: Double, _ calories: Double, _ fat: Double?, _ carbs: Double?, _ grams: Double) -> Void

    var body: some View {
        HStack(spacing: 12) {
            portionStepper
            Spacer()
            addButton
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .background(Color.strakkSurface1)
    }

    private var portionStepper: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 8) {
                Button {
                    portionGrams = max(10, portionGrams - 10)
                } label: {
                    Image(systemName: "minus.circle.fill")
                        .font(.system(size: 22))
                        .foregroundStyle(Color.strakkSurface2)
                }
                Text(String(format: "%.0fg", portionGrams))
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .monospacedDigit()
                    .frame(minWidth: 48)
                Button {
                    portionGrams += 10
                } label: {
                    Image(systemName: "plus.circle.fill")
                        .font(.system(size: 22))
                        .foregroundStyle(Color.strakkPrimary)
                }
            }
            Text(String(
                format: "%.0f kcal · %.0fg prot",
                caloriesPer100 * portionGrams / 100,
                proteinPer100 * portionGrams / 100
            ))
            .font(.strakkCaption)
            .foregroundStyle(Color.strakkTextSecondary)
            .monospacedDigit()
        }
    }

    private var addButton: some View {
        Button {
            let grams = portionGrams
            onAdd(
                proteinPer100 * grams / 100,
                caloriesPer100 * grams / 100,
                fatPer100.map { $0 * grams / 100 },
                carbsPer100.map { $0 * grams / 100 },
                grams
            )
        } label: {
            HStack(spacing: 6) {
                if isProcessing {
                    ProgressView()
                        .tint(.white)
                        .scaleEffect(0.7)
                }
                Text(isProcessing ? "Adding..." : "Add")
                    .font(.strakkCaptionBold)
            }
            .foregroundStyle(.white)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(isProcessing ? Color.strakkSurface2 : Color.strakkPrimary)
            .clipShape(Capsule())
        }
        .disabled(isProcessing)
        .accessibilityLabel("Add \(name)")
    }
}

// MARK: - CatalogMacroLine

struct CatalogMacroLine: View {
    let item: FoodCatalogItemData

    var body: some View {
        HStack(spacing: 6) {
            Text(String(format: "%.0f kcal", item.calories))
                .foregroundStyle(Color.strakkCalories)
            Text("·").foregroundStyle(Color.strakkTextTertiary)
            Text(String(format: "%.0f g prot", item.protein))
                .foregroundStyle(Color.strakkProtein)
            if let fat = item.fat {
                Text("·").foregroundStyle(Color.strakkTextTertiary)
                Text(String(format: "%.0f g lip", fat))
                    .foregroundStyle(Color.strakkAccentYellow)
            }
            if let carbs = item.carbs {
                Text("·").foregroundStyle(Color.strakkTextTertiary)
                Text(String(format: "%.0f g gluc", carbs))
                    .foregroundStyle(Color.strakkAccentIndigo)
            }
        }
        .font(.strakkCaption)
        .monospacedDigit()
        .lineLimit(1)
        .minimumScaleFactor(0.85)
    }
}

// MARK: - NutriscoreBadgeView

struct NutriscoreBadgeView: View {
    let grade: Character

    var body: some View {
        let color: Color = {
            switch String(grade).lowercased() {
            case "a": return Color(red: 0.12, green: 0.56, blue: 0.24)
            case "b": return Color(red: 0.52, green: 0.73, blue: 0.18)
            case "c": return Color(red: 0.95, green: 0.76, blue: 0.20)
            case "d": return Color(red: 0.90, green: 0.49, blue: 0.13)
            case "e": return Color(red: 0.75, green: 0.22, blue: 0.17)
            default:  return Color.strakkTextTertiary
            }
        }()
        return Text(String(grade).uppercased())
            .font(.system(size: 11, weight: .bold))
            .foregroundStyle(.white)
            .frame(width: 22, height: 22)
            .background(color, in: RoundedRectangle(cornerRadius: 6))
    }
}
