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

                Text("Ton coach nutrition & fitness, simplifié.")
                    .font(.strakkHeading2)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, StrakkSpacing.xl)
            }

            Spacer()

            VStack(spacing: StrakkSpacing.md) {
                Button {
                    wrapper.send(OnboardingFlowEventOnContinue())
                } label: {
                    Text("C'est parti")
                        .font(.strakkBodyBold)
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(Color.strakkPrimary)
                        .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
                }
                .accessibilityLabel("Commencer l'onboarding")

                Button {
                    wrapper.send(OnboardingFlowEventOnNavigateToLogin())
                } label: {
                    Text("Déjà un compte ?")
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextSecondary)
                        .underline()
                }
                .frame(height: 44)
                .accessibilityLabel("Se connecter à un compte existant")
            }
            .padding(.horizontal, StrakkSpacing.xl)
            .padding(.bottom, StrakkSpacing.xxl)
        }
    }
}
