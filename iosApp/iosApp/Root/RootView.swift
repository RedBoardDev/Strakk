import SwiftUI

struct RootView: View {
    @Environment(RootViewModelWrapper.self) private var auth

    var body: some View {
        Group {
            switch auth.state {
            case .loading:
                ZStack {
                    Color.strakkBackground.ignoresSafeArea()
                    ProgressView()
                        .tint(.strakkPrimary)
                }

            case .unauthenticated:
                AuthFlowView()

            case .authenticated(let hasProfile):
                if hasProfile {
                    MainTabView()
                } else {
                    OnboardingView()
                }
            }
        }
        .animation(.easeInOut(duration: 0.25), value: auth.state)
    }
}
