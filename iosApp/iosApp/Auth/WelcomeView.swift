import SwiftUI

struct WelcomeView: View {
    let onContinueWithEmail: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            // Logo / App name block
            VStack(spacing: 12) {
                Text("Strakk")
                    .font(.strakkDisplay)
                    .foregroundStyle(Color.strakkTextPrimary)

                Text("Track your fitness journey")
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextSecondary)
            }

            Spacer()

            // CTA
            Button(action: onContinueWithEmail) {
                Text("Continue with email")
                    .font(.strakkBodyBold)
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color.strakkPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .accessibilityLabel("Continue with email")
            .padding(.horizontal, 20)
            .padding(.bottom, 48)
        }
    }
}
