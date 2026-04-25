import SwiftUI
import shared

struct AuthFlowView: View {
    @State private var viewModel = AuthFlowViewModelWrapper()

    var body: some View {
        ZStack {
            Color.strakkBackground.ignoresSafeArea()

            Group {
                switch viewModel.state {
                case .welcome:
                    WelcomeView(onContinueWithEmail: {
                        viewModel.onEvent(AuthFlowEventOnContinueWithEmail())
                    })
                    .transition(.asymmetric(
                        insertion: .opacity,
                        removal: .move(edge: .leading).combined(with: .opacity)
                    ))

                case .signIn(let email, _, let isLoading, let error):
                    SignInView(
                        initialEmail: email,
                        isLoading: isLoading,
                        error: error,
                        onSignIn: { email, password in
                            viewModel.onEvent(AuthFlowEventOnEmailChanged(email: email))
                            viewModel.onEvent(AuthFlowEventOnPasswordChanged(password: password))
                            viewModel.onEvent(AuthFlowEventOnSignIn())
                        },
                        onSwitchToSignUp: { viewModel.onEvent(AuthFlowEventOnSwitchToSignUp()) }
                    )
                    .transition(.asymmetric(
                        insertion: .move(edge: .trailing).combined(with: .opacity),
                        removal: .move(edge: .leading).combined(with: .opacity)
                    ))

                case .signUp(let email, _, let isLoading, let error):
                    SignUpView(
                        initialEmail: email,
                        isLoading: isLoading,
                        error: error,
                        onSignUp: { email, password in
                            viewModel.onEvent(AuthFlowEventOnEmailChanged(email: email))
                            viewModel.onEvent(AuthFlowEventOnPasswordChanged(password: password))
                            viewModel.onEvent(AuthFlowEventOnSignUp())
                        },
                        onSwitchToSignIn: { viewModel.onEvent(AuthFlowEventOnSwitchToSignIn()) }
                    )
                    .transition(.asymmetric(
                        insertion: .move(edge: .trailing).combined(with: .opacity),
                        removal: .move(edge: .leading).combined(with: .opacity)
                    ))
                }
            }
            .animation(.easeInOut(duration: 0.3), value: viewModel.state.discriminator)
        }
        .alert("Error", isPresented: Binding(
            get: { viewModel.errorMessage != nil },
            set: { if !$0 { viewModel.errorMessage = nil } }
        )) {
            Button("OK") { viewModel.errorMessage = nil }
        } message: {
            Text(viewModel.errorMessage ?? "")
        }
    }
}
