import SwiftUI
import shared

struct DayPreviewStepView: View {
    @Bindable var wrapper: OnboardingFlowViewModelWrapper

    var body: some View {
        VStack(spacing: 0) {
            stepHeader

            ScrollView {
                VStack(spacing: StrakkSpacing.lg) {
                    macroGrid
                    waterBar
                }
                .padding(.horizontal, StrakkSpacing.xl)
                .padding(.top, StrakkSpacing.xl)
            }

            StrakkPrimaryButton(
                title: wrapper.state.isSaving ? "Saving…" : "Start tracking",
                action: { wrapper.send(OnboardingFlowEventOnContinue()) },
                isEnabled: !wrapper.state.isSaving
            )
            .padding(.horizontal, StrakkSpacing.xl)
            .padding(.bottom, StrakkSpacing.xxl)
            .accessibilityLabel(Text("Start tracking"))
        }
    }

    private var stepHeader: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("Here's what your day will look like")
                .font(.strakkHeading1)
                .foregroundStyle(Color.strakkTextPrimary)
                .padding(.horizontal, StrakkSpacing.xl)
                .padding(.top, StrakkSpacing.xl)

            Text("Start tracking to watch your progress.")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
                .padding(.horizontal, StrakkSpacing.xl)
        }
    }

    private var macroGrid: some View {
        let s = wrapper.state
        return MacroProgressGrid(
            totalProtein: 0,
            totalCalories: 0,
            totalFat: 0,
            totalCarbs: 0,
            proteinGoal: Int(s.proteinGoal),
            calorieGoal: Int(s.calorieGoal),
            fatGoal: Int(s.fatGoal),
            carbGoal: Int(s.carbGoal)
        )
    }

    private var waterBar: some View {
        let waterGoal = Int(wrapper.state.waterGoal)
        return VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            HStack {
                Image(systemName: "drop.fill")
                    .foregroundStyle(Color.strakkWater)
                Text("Water")
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextSecondary)
                Spacer()
                Text("0 / \(waterGoal) ml")
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextTertiary)
            }

            GeometryReader { _ in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(Color.strakkSurface2)
                        .frame(height: 8)
                    Capsule()
                        .fill(Color.strakkWater)
                        .frame(width: 0, height: 8)
                }
            }
            .frame(height: 8)
        }
        .padding(StrakkSpacing.md)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
    }
}
