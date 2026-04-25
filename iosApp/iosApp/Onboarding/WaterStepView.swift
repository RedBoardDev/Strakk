import SwiftUI

struct WaterStepView: View {
    let waterGoal: String
    let onWaterChanged: (String) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 32) {
                // Header
                VStack(alignment: .leading, spacing: 12) {
                    Image(systemName: "drop.fill")
                        .font(.system(size: 48))
                        .foregroundStyle(Color.strakkWater)

                    Text("Stay hydrated")
                        .font(.strakkHeading1)
                        .foregroundStyle(Color.strakkTextPrimary)

                    Text("Set your daily water intake goal")
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextSecondary)
                }

                // Field
                OnboardingFieldView(
                    label: "Water",
                    placeholder: "2500",
                    value: waterGoal,
                    unit: "mL/day",
                    onChange: onWaterChanged
                )
            }
            .padding(.horizontal, 20)
            .padding(.top, 32)
            .padding(.bottom, 24)
        }
    }
}
