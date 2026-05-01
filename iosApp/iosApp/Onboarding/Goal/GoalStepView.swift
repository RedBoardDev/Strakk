import SwiftUI
import shared

struct GoalStepView: View {
    @Bindable var wrapper: OnboardingFlowViewModelWrapper

    private struct GoalOption {
        let goal: FitnessGoal
        let icon: String
        let title: String
    }

    private let options: [GoalOption] = [
        .init(goal: .loseFat, icon: "flame.fill", title: "Perdre du gras"),
        .init(goal: .gainMuscle, icon: "figure.strengthtraining.traditional", title: "Prendre du muscle"),
        .init(goal: .maintain, icon: "equal.circle.fill", title: "Maintenir"),
        .init(goal: .justTrack, icon: "chart.bar.fill", title: "Juste tracker")
    ]

    var body: some View {
        VStack(spacing: 0) {
            stepHeader

            VStack(spacing: StrakkSpacing.sm) {
                ForEach(options, id: \.title) { option in
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

            continueButton
                .padding(.horizontal, StrakkSpacing.xl)
                .padding(.bottom, StrakkSpacing.xxl)
        }
    }

    private var stepHeader: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
            OnboardingProgressBar(progress: wrapper.state.progressFraction)
                .padding(.horizontal, StrakkSpacing.xl)

            Text("Quel est ton objectif ?")
                .font(.strakkHeading1)
                .foregroundStyle(Color.strakkTextPrimary)
                .padding(.horizontal, StrakkSpacing.xl)
                .padding(.top, StrakkSpacing.xl)
        }
    }

    private var continueButton: some View {
        Button {
            wrapper.send(OnboardingFlowEventOnContinue())
        } label: {
            Text("Continuer")
                .font(.strakkBodyBold)
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(Color.strakkPrimary)
                .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
        }
    }
}
