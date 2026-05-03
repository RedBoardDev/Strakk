import SwiftUI
import shared

struct OnboardingFlowView: View {
    @State private var wrapper = OnboardingFlowViewModelWrapper()
    @Environment(RootViewModelWrapper.self) private var rootWrapper

    var body: some View {
        ZStack {
            Color.strakkBackground.ignoresSafeArea()

            currentStepView
                .transition(.asymmetric(
                    insertion: .move(edge: .trailing).combined(with: .opacity),
                    removal: .move(edge: .leading).combined(with: .opacity)
                ))
                .animation(.easeInOut(duration: 0.25), value: wrapper.state.currentStep)
        }
        .task {
            wrapper.onNavigateToHome = { [rootWrapper] in
                rootWrapper.refreshProfile()
            }
        }
        .sheet(isPresented: $wrapper.showLogin) {
            LoginView(onDismiss: { wrapper.showLogin = false })
        }
        .alert("Erreur", isPresented: Binding(
            get: { wrapper.errorMessage != nil },
            set: { if !$0 { wrapper.errorMessage = nil } }
        )) {
            Button("OK") { wrapper.errorMessage = nil }
        } message: {
            Text(wrapper.errorMessage ?? "")
        }
    }

    @ViewBuilder
    private var currentStepView: some View {
        switch wrapper.state.currentStep {
        case .welcome:
            WelcomeStepView(wrapper: wrapper)
        case .weight:
            WeightStepView(wrapper: wrapper)
        case .bio:
            BioStepView(wrapper: wrapper)
        case .goal:
            GoalStepView(wrapper: wrapper)
        case .activityTraining:
            ActivityTrainingStepView(wrapper: wrapper)
        case .activityDaily:
            ActivityDailyStepView(wrapper: wrapper)
        case .signUp:
            SignUpStepView(wrapper: wrapper)
        case .nutritionGoals:
            NutritionGoalsStepView(wrapper: wrapper)
        case .dayPreview:
            DayPreviewStepView(wrapper: wrapper)
        case .proOffer:
            ProOfferStepView(wrapper: wrapper)
        default:
            EmptyView()
        }
    }
}
