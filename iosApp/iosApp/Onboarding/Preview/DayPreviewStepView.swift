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

            startButton
                .padding(.horizontal, StrakkSpacing.xl)
                .padding(.bottom, StrakkSpacing.xxl)
        }
    }

    private var stepHeader: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("Voilà à quoi ressemblera ta journée")
                .font(.strakkHeading1)
                .foregroundStyle(Color.strakkTextPrimary)
                .padding(.horizontal, StrakkSpacing.xl)
                .padding(.top, StrakkSpacing.xl)

            Text("Commence à tracker pour voir ta progression.")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
                .padding(.horizontal, StrakkSpacing.xl)
        }
    }

    private var macroGrid: some View {
        let s = wrapper.state
        let columns = [GridItem(.flexible()), GridItem(.flexible())]
        return LazyVGrid(columns: columns, spacing: StrakkSpacing.sm) {
            previewMacroCard(
                label: "Protéines",
                consumed: 0,
                goal: Int(s.proteinGoal),
                unit: "g",
                color: .strakkPrimary
            )
            previewMacroCard(
                label: "Calories",
                consumed: 0,
                goal: Int(s.calorieGoal),
                unit: "kcal",
                color: .strakkCalories
            )
            previewMacroCard(
                label: "Lipides",
                consumed: 0,
                goal: Int(s.fatGoal),
                unit: "g",
                color: .strakkAccentYellow
            )
            previewMacroCard(
                label: "Glucides",
                consumed: 0,
                goal: Int(s.carbGoal),
                unit: "g",
                color: .strakkAccentIndigo
            )
        }
    }

    private func previewMacroCard(label: String, consumed: Int, goal: Int, unit: String, color: Color) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text(label)
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextSecondary)

            HStack(alignment: .lastTextBaseline, spacing: 2) {
                Text("0")
                    .font(.strakkHeading2)
                    .foregroundStyle(Color.strakkTextPrimary)
                Text("/ \(goal)\(unit)")
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextTertiary)
            }

            GeometryReader { _ in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(Color.strakkSurface2)
                        .frame(height: 4)
                    Capsule()
                        .fill(color)
                        .frame(width: 0, height: 4)
                }
            }
            .frame(height: 4)
        }
        .padding(StrakkSpacing.md)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
    }

    private var waterBar: some View {
        let waterGoal = Int(wrapper.state.waterGoal)
        return VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            HStack {
                Image(systemName: "drop.fill")
                    .foregroundStyle(Color.strakkWater)
                Text("Eau")
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

    private var startButton: some View {
        Button {
            wrapper.send(OnboardingFlowEventOnContinue())
        } label: {
            Group {
                if wrapper.state.isSaving {
                    ProgressView()
                        .tint(.white)
                } else {
                    Text("Commencer à tracker")
                        .font(.strakkBodyBold)
                        .foregroundStyle(.white)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(wrapper.state.isSaving ? Color.strakkPrimary.opacity(0.6) : Color.strakkPrimary)
            .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
        }
        .disabled(wrapper.state.isSaving)
        .accessibilityLabel("Commencer à tracker")
    }
}
