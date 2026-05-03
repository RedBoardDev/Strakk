import SwiftUI

struct RootView: View {
    @Environment(RootViewModelWrapper.self) private var auth

    private var showOnboarding: Bool {
        switch auth.state {
        case .unauthenticated: true
        case .authenticated(let completed, _): !completed
        default: false
        }
    }

    var body: some View {
        @Bindable var authBindable = auth

        Group {
            if auth.state == .loading {
                ZStack {
                    Color.strakkBackground.ignoresSafeArea()
                    ProgressView()
                        .tint(.strakkPrimary)
                }
            } else if showOnboarding {
                OnboardingFlowView()
            } else if case .authenticated(_, let showTrialExpiredModal) = auth.state {
                ZStack {
                    MainTabView()

                    if showTrialExpiredModal {
                        TrialExpiredModal(
                            onDiscoverOffers: {
                                auth.dismissTrialModal()
                                auth.showPaywall = true
                            },
                            onContinueFree: {
                                auth.dismissTrialModal()
                            }
                        )
                    }
                }
            }
        }
        .animation(.easeInOut(duration: 0.25), value: auth.state)
        .fullScreenCover(isPresented: $authBindable.showPaywall) {
            PaywallView(onDismiss: { auth.showPaywall = false })
        }
    }
}
