import SwiftUI
import shared

struct NutritionGoalsStepView: View {
    @Bindable var wrapper: OnboardingFlowViewModelWrapper

    var body: some View {
        VStack(spacing: 0) {
            stepHeader

            ScrollView {
                VStack(spacing: 0) {
                    aiButton
                        .padding(.horizontal, StrakkSpacing.xl)
                        .padding(.top, StrakkSpacing.xl)
                        .padding(.bottom, StrakkSpacing.lg)

                    Divider()
                        .background(Color.strakkDivider)
                        .padding(.horizontal, StrakkSpacing.xl)

                    steppersList
                        .padding(.horizontal, StrakkSpacing.xl)
                        .padding(.top, StrakkSpacing.md)
                }
            }

            continueButton
                .padding(.horizontal, StrakkSpacing.xl)
                .padding(.bottom, StrakkSpacing.xxl)
        }
    }

    private var stepHeader: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
            Text("Tes objectifs nutritionnels")
                .font(.strakkHeading1)
                .foregroundStyle(Color.strakkTextPrimary)
                .padding(.horizontal, StrakkSpacing.xl)
                .padding(.top, StrakkSpacing.xl)

            Text("Ajuste ou laisse l'IA calculer pour toi.")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
                .padding(.horizontal, StrakkSpacing.xl)
        }
    }

    @ViewBuilder
    private var aiButton: some View {
        let aiState = wrapper.state.aiState
        Button {
            wrapper.send(OnboardingFlowEventOnCalculateWithAi())
        } label: {
            HStack(spacing: StrakkSpacing.sm) {
                Group {
                    if aiState == .loading {
                        ProgressView()
                            .tint(Color.strakkPrimary)
                            .frame(width: 20, height: 20)
                    } else if aiState == .completed {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundStyle(Color.strakkSuccess)
                    } else if aiState == .failed {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundStyle(Color.strakkError)
                    } else {
                        Image(systemName: "sparkles")
                            .foregroundStyle(Color.strakkPrimary)
                    }
                }
                .frame(width: 24, height: 24)

                VStack(alignment: .leading, spacing: 2) {
                    Text(aiButtonTitle(for: aiState))
                        .font(.strakkBodyBold)
                        .foregroundStyle(Color.strakkTextPrimary)
                    if aiState == .failed {
                        Text("Réessaie ou ajuste manuellement")
                            .font(.strakkCaption)
                            .foregroundStyle(Color.strakkTextSecondary)
                    }
                }

                Spacer()
            }
            .padding(StrakkSpacing.md)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
            .overlay(
                RoundedRectangle(cornerRadius: StrakkRadius.sm)
                    .strokeBorder(
                        aiState == .completed ? Color.strakkSuccess : Color.strakkPrimary.opacity(0.4),
                        lineWidth: 1
                    )
            )
        }
        .disabled(aiState == .loading || aiState == .completed)
        .accessibilityLabel(aiButtonTitle(for: aiState))
    }

    private func aiButtonTitle(for state: AiCalculationState) -> String {
        switch state {
        case .available: return "Calculer avec l'IA"
        case .loading: return "Calcul en cours..."
        case .completed: return "Objectifs calculés"
        case .failed: return "Calcul échoué"
        default: return "Calculer avec l'IA"
        }
    }

    private var steppersList: some View {
        let s = wrapper.state
        return VStack(spacing: 0) {
            StepperRow(
                label: "Protéines",
                value: Int(s.proteinGoal),
                unit: "g",
                step: 5,
                range: 0...500,
                onDecrement: { wrapper.send(OnboardingFlowEventOnProteinGoalChanged(value: Int32(s.proteinGoal) - 5)) },
                onIncrement: { wrapper.send(OnboardingFlowEventOnProteinGoalChanged(value: Int32(s.proteinGoal) + 5)) }
            )
            dividerLine
            StepperRow(
                label: "Calories",
                value: Int(s.calorieGoal),
                unit: "kcal",
                step: 50,
                range: 0...6000,
                onDecrement: {
                    wrapper.send(OnboardingFlowEventOnCalorieGoalChanged(value: Int32(s.calorieGoal) - 50))
                },
                onIncrement: {
                    wrapper.send(OnboardingFlowEventOnCalorieGoalChanged(value: Int32(s.calorieGoal) + 50))
                }
            )
            dividerLine
            StepperRow(
                label: "Lipides",
                value: Int(s.fatGoal),
                unit: "g",
                step: 5,
                range: 0...500,
                onDecrement: { wrapper.send(OnboardingFlowEventOnFatGoalChanged(value: Int32(s.fatGoal) - 5)) },
                onIncrement: { wrapper.send(OnboardingFlowEventOnFatGoalChanged(value: Int32(s.fatGoal) + 5)) }
            )
            dividerLine
            StepperRow(
                label: "Glucides",
                value: Int(s.carbGoal),
                unit: "g",
                step: 5,
                range: 0...700,
                onDecrement: { wrapper.send(OnboardingFlowEventOnCarbGoalChanged(value: Int32(s.carbGoal) - 5)) },
                onIncrement: { wrapper.send(OnboardingFlowEventOnCarbGoalChanged(value: Int32(s.carbGoal) + 5)) }
            )
            dividerLine
            StepperRow(
                label: "Eau",
                value: Int(s.waterGoal),
                unit: "ml",
                step: 100,
                range: 0...6000,
                onDecrement: { wrapper.send(OnboardingFlowEventOnWaterGoalChanged(value: Int32(s.waterGoal) - 100)) },
                onIncrement: { wrapper.send(OnboardingFlowEventOnWaterGoalChanged(value: Int32(s.waterGoal) + 100)) }
            )
        }
    }

    private var dividerLine: some View {
        Divider()
            .background(Color.strakkDivider)
            .padding(.vertical, StrakkSpacing.xxs)
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
