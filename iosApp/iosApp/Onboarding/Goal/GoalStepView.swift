import SwiftUI
import shared

struct GoalStepView: View {
    @Bindable var wrapper: OnboardingFlowViewModelWrapper

    private struct GoalOption {
        let goal: FitnessGoal
        let icon: String
        let title: LocalizedStringKey
    }

    private let options: [GoalOption] = [
        .init(goal: .loseFat, icon: "flame.fill", title: "Lose fat"),
        .init(goal: .gainMuscle, icon: "figure.strengthtraining.traditional", title: "Build muscle"),
        .init(goal: .maintain, icon: "equal.circle.fill", title: "Maintain"),
        .init(goal: .justTrack, icon: "chart.bar.fill", title: "Just track")
    ]

    var body: some View {
        VStack(spacing: 0) {
            stepHeader

            VStack(spacing: StrakkSpacing.sm) {
                ForEach(0..<options.count, id: \.self) { index in
                    let option = options[index]
                    SelectableCard(
                        icon: option.icon,
                        title: option.title,
                        isSelected: wrapper.state.fitnessGoal == option.goal
                    ) {
                        let newGoal: FitnessGoal? = wrapper.state.fitnessGoal == option.goal ? nil : option.goal
                        wrapper.send(OnboardingFlowEventOnFitnessGoalChanged(goal: newGoal))
                    }
                }
            }
            .padding(.horizontal, StrakkSpacing.xl)
            .padding(.top, StrakkSpacing.xl)

            Spacer()

            StrakkPrimaryButton(
                title: "Continue",
                action: { wrapper.send(OnboardingFlowEventOnContinue()) }
            )
            .padding(.horizontal, StrakkSpacing.xl)
            .padding(.bottom, StrakkSpacing.xxl)
        }
    }

    private var stepHeader: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
            OnboardingProgressBar(progress: wrapper.state.progressFraction)
                .padding(.horizontal, StrakkSpacing.xl)

            Text("What is your goal?")
                .font(.strakkHeading1)
                .foregroundStyle(Color.strakkTextPrimary)
                .padding(.horizontal, StrakkSpacing.xl)
                .padding(.top, StrakkSpacing.xl)
        }
    }
}
