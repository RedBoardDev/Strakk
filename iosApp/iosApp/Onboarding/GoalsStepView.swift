import SwiftUI

struct GoalsStepView: View {
    let proteinGoal: String
    let calorieGoal: String
    let onProteinChanged: (String) -> Void
    let onCalorieChanged: (String) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 32) {
                // Header
                VStack(alignment: .leading, spacing: 12) {
                    Image(systemName: "figure.strengthtraining.traditional")
                        .font(.system(size: 48))
                        .foregroundStyle(Color.strakkTextSecondary)

                    Text("Set your daily goals")
                        .font(.strakkHeading1)
                        .foregroundStyle(Color.strakkTextPrimary)

                    Text("How much protein and calories do you aim for?")
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextSecondary)
                }

                // Fields
                VStack(spacing: 20) {
                    OnboardingFieldView(
                        label: "Protein",
                        placeholder: "150",
                        value: proteinGoal,
                        unit: "g/day",
                        onChange: onProteinChanged
                    )

                    OnboardingFieldView(
                        label: "Calories",
                        placeholder: "2200",
                        value: calorieGoal,
                        unit: "kcal/day",
                        onChange: onCalorieChanged
                    )
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 32)
            .padding(.bottom, 24)
        }
    }
}

struct OnboardingFieldView: View {
    let label: String
    let placeholder: String
    let value: String
    let unit: String
    let onChange: (String) -> Void

    @FocusState private var isFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.strakkCaptionBold)
                .foregroundStyle(Color.strakkTextSecondary)
                .textCase(.uppercase)
                .tracking(0.5)

            HStack {
                TextField(placeholder, text: Binding(
                    get: { value },
                    set: { onChange($0) }
                ))
                .keyboardType(.numberPad)
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextPrimary)
                .tint(Color.strakkPrimary)
                .focused($isFocused)

                Text(unit)
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextTertiary)
            }
            .frame(height: 48)
            .padding(.horizontal, 16)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(
                        isFocused ? Color.strakkPrimary : Color.strakkDivider,
                        lineWidth: 1
                    )
            )
        }
    }
}
