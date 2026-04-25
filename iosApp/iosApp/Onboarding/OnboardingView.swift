import SwiftUI
import shared

struct OnboardingView: View {
    @State private var viewModel = OnboardingViewModelWrapper()
    @Environment(RootViewModelWrapper.self) private var auth

    var body: some View {
        ZStack {
            Color.strakkBackground.ignoresSafeArea()

            VStack(spacing: 0) {
                // Step indicator
                StepIndicatorView(currentStep: Int(viewModel.state.currentStep), totalSteps: 3)
                    .padding(.top, 24)
                    .padding(.horizontal, 20)

                // Step content
                ZStack {
                    if viewModel.state.currentStep == 0 {
                        GoalsStepView(
                            proteinGoal: viewModel.state.proteinGoal,
                            calorieGoal: viewModel.state.calorieGoal,
                            onProteinChanged: { viewModel.onEvent(OnboardingEventOnProteinGoalChanged(value: $0)) },
                            onCalorieChanged: { viewModel.onEvent(OnboardingEventOnCalorieGoalChanged(value: $0)) }
                        )
                        .transition(.asymmetric(
                            insertion: .move(edge: .trailing).combined(with: .opacity),
                            removal: .move(edge: .leading).combined(with: .opacity)
                        ))
                    } else if viewModel.state.currentStep == 1 {
                        WaterStepView(
                            waterGoal: viewModel.state.waterGoal,
                            onWaterChanged: { viewModel.onEvent(OnboardingEventOnWaterGoalChanged(value: $0)) }
                        )
                        .transition(.asymmetric(
                            insertion: .move(edge: .trailing).combined(with: .opacity),
                            removal: .move(edge: .leading).combined(with: .opacity)
                        ))
                    } else {
                        RemindersStepView(
                            trackingEnabled: viewModel.state.trackingReminderEnabled,
                            trackingTime: viewModel.state.trackingReminderTime,
                            checkinEnabled: viewModel.state.checkinReminderEnabled,
                            checkinDay: Int(viewModel.state.checkinReminderDay),
                            checkinTime: viewModel.state.checkinReminderTime,
                            onTrackingToggled: { viewModel.onEvent(OnboardingEventOnTrackingReminderToggled(enabled: $0)) },
                            onTrackingTimeChanged: { viewModel.onEvent(OnboardingEventOnTrackingReminderTimeChanged(time: $0)) },
                            onCheckinToggled: { viewModel.onEvent(OnboardingEventOnCheckinReminderToggled(enabled: $0)) },
                            onCheckinDayChanged: { viewModel.onEvent(OnboardingEventOnCheckinReminderDayChanged(day: Int32($0))) },
                            onCheckinTimeChanged: { viewModel.onEvent(OnboardingEventOnCheckinReminderTimeChanged(time: $0)) }
                        )
                        .transition(.asymmetric(
                            insertion: .move(edge: .trailing).combined(with: .opacity),
                            removal: .move(edge: .leading).combined(with: .opacity)
                        ))
                    }
                }
                .animation(.easeInOut(duration: 0.3), value: viewModel.state.currentStep)
                .frame(maxHeight: .infinity)

                // Bottom navigation
                VStack(spacing: 12) {
                    // Continue / Get started
                    Button(action: { viewModel.onEvent(OnboardingEventOnContinue()) }) {
                        ZStack {
                            if viewModel.state.isSaving {
                                ProgressView()
                                    .tint(.white)
                            } else {
                                Text(viewModel.state.isLastStep ? "Get started" : "Continue")
                                    .font(.strakkBodyBold)
                                    .foregroundStyle(.white)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(Color.strakkPrimary)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .disabled(viewModel.state.isSaving)
                    .accessibilityLabel(viewModel.state.isLastStep ? "Get started" : "Continue to next step")

                    // Back button (not on first step)
                    if !viewModel.state.isFirstStep {
                        Button(action: { viewModel.onEvent(OnboardingEventOnBack()) }) {
                            Text("Back")
                                .font(.strakkBody)
                                .foregroundStyle(Color.strakkPrimary)
                        }
                        .accessibilityLabel("Go back to previous step")
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 48)
            }
        }
        .alert("Error", isPresented: Binding(
            get: { viewModel.errorMessage != nil },
            set: { if !$0 { viewModel.errorMessage = nil } }
        )) {
            Button("OK") { viewModel.errorMessage = nil }
        } message: {
            Text(viewModel.errorMessage ?? "")
        }
        .onChange(of: viewModel.shouldNavigateToHome) { _, navigateToHome in
            if navigateToHome {
                auth.refreshProfile()
            }
        }
    }
}

// Step indicator component
private struct StepIndicatorView: View {
    let currentStep: Int
    let totalSteps: Int

    var body: some View {
        HStack(spacing: 8) {
            ForEach(0..<totalSteps, id: \.self) { index in
                Capsule()
                    .fill(index == currentStep ? Color.strakkPrimary : Color.strakkSurface2)
                    .frame(width: index == currentStep ? 24 : 8, height: 8)
                    .animation(.easeInOut(duration: 0.2), value: currentStep)
            }
        }
    }
}
