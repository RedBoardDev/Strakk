import SwiftUI
import shared

struct WelcomeStepView: View {
    @Bindable var wrapper: OnboardingFlowViewModelWrapper

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            VStack(spacing: StrakkSpacing.md) {
                Text("Strakk")
                    .font(.strakkDisplayHero)
                    .foregroundStyle(Color.strakkPrimary)

                Text("Your nutrition & fitness coach, made simple.")
                    .font(.strakkHeading2)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, StrakkSpacing.xl)
            }

            Spacer()

            VStack(spacing: StrakkSpacing.md) {
                StrakkPrimaryButton(
                    title: "Get started",
                    action: { wrapper.send(OnboardingFlowEventOnContinue()) }
                )
                .accessibilityLabel(Text("Start onboarding"))

                Button {
                    wrapper.send(OnboardingFlowEventOnNavigateToLogin())
                } label: {
                    Text("Already have an account?")
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextSecondary)
                        .underline()
                }
                .frame(height: 44)
                .accessibilityLabel(Text("Sign in to an existing account"))
            }
            .padding(.horizontal, StrakkSpacing.xl)
            .padding(.bottom, StrakkSpacing.xxl)
        }
    }
}
