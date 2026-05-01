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
                OnboardingFlowView()

            case .authenticated(let onboardingCompleted):
                if onboardingCompleted {
                    MainTabView()
                } else {
                    OnboardingFlowView()
                }
            }
        }
        .animation(.easeInOut(duration: 0.25), value: auth.state)
    }
}
