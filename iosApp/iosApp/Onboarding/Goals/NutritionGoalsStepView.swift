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
                        .background(Color.strakkDividerWeak)
                        .padding(.horizontal, StrakkSpacing.xl)

                    steppersList
                        .padding(.horizontal, StrakkSpacing.xl)
                        .padding(.top, StrakkSpacing.md)
                }
            }

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
            Text("Your nutrition goals")
                .font(.strakkHeading1)
                .foregroundStyle(Color.strakkTextPrimary)
                .padding(.horizontal, StrakkSpacing.xl)
                .padding(.top, StrakkSpacing.xl)

            Text("Tweak them or let the AI compute them for you.")
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
                        Text("Try again or adjust manually")
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
        .accessibilityLabel(Text(aiButtonTitle(for: aiState)))
    }

    private func aiButtonTitle(for state: AiCalculationState) -> LocalizedStringKey {
        switch state {
        case .available: return "Calculate with AI"
        case .loading: return "Calculating…"
        case .completed: return "Goals calculated"
        case .failed: return "Calculation failed"
        default: return "Calculate with AI"
        }
    }

    private var steppersList: some View {
        let s = wrapper.state
        return VStack(spacing: 0) {
            StepperRow(
                label: "Protein",
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
                label: "Fat",
                value: Int(s.fatGoal),
                unit: "g",
                step: 5,
                range: 0...500,
                onDecrement: { wrapper.send(OnboardingFlowEventOnFatGoalChanged(value: Int32(s.fatGoal) - 5)) },
                onIncrement: { wrapper.send(OnboardingFlowEventOnFatGoalChanged(value: Int32(s.fatGoal) + 5)) }
            )
            dividerLine
            StepperRow(
                label: "Carbs",
                value: Int(s.carbGoal),
                unit: "g",
                step: 5,
                range: 0...700,
                onDecrement: { wrapper.send(OnboardingFlowEventOnCarbGoalChanged(value: Int32(s.carbGoal) - 5)) },
                onIncrement: { wrapper.send(OnboardingFlowEventOnCarbGoalChanged(value: Int32(s.carbGoal) + 5)) }
            )
            dividerLine
            StepperRow(
                label: "Water",
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
            .background(Color.strakkDividerWeak)
            .padding(.vertical, StrakkSpacing.xxs)
    }
}
