import SwiftUI
import shared

// MARK: - FeatureGateSheet

struct FeatureGateSheet: View {
    let featureInfo: ProFeatureInfo
    var onDiscoverPro: () -> Void
    var onDismiss: () -> Void

    var body: some View {
        ZStack {
            Color.strakkBackground.ignoresSafeArea()

            VStack(spacing: 0) {
                // Drag indicator area
                Spacer().frame(height: StrakkSpacing.lg)

                // Large icon with gradient glow
                ZStack {
                    Circle()
                        .fill(
                            RadialGradient(
                                colors: [Color.strakkPrimary.opacity(0.2), Color.clear],
                                center: .center,
                                startRadius: 0,
                                endRadius: 60
                            )
                        )
                        .frame(width: 120, height: 120)

                    Circle()
                        .fill(Color.strakkSurface2)
                        .frame(width: 72, height: 72)

                    Image(systemName: sfSymbol(for: featureInfo.feature))
                        .font(.system(size: 32, weight: .medium))
                        .foregroundStyle(Color.strakkPrimary)
                }

                Spacer().frame(height: StrakkSpacing.xl)

                // PRO badge
                ProBadge()

                Spacer().frame(height: StrakkSpacing.sm)

                // Title
                Text(featureInfo.title)
                    .font(.strakkHeading2)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, StrakkSpacing.xl)

                Spacer().frame(height: StrakkSpacing.xs)

                // Description
                Text(featureInfo.description_)
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, StrakkSpacing.xl)

                Spacer().frame(height: StrakkSpacing.xxl)

                // CTA button
                Button {
                    HapticEngine.light()
                    onDismiss()
                    onDiscoverPro()
                } label: {
                    Text("Débloquer avec Pro")
                        .font(.strakkBodyBold)
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(Color.strakkPrimary, in: RoundedRectangle(cornerRadius: StrakkRadius.md))
                }
                .padding(.horizontal, StrakkSpacing.lg)
                .accessibilityLabel("Débloquer cette fonctionnalité avec Strakk Pro")

                Spacer().frame(height: StrakkSpacing.sm)

                // Dismiss
                Button {
                    onDismiss()
                } label: {
                    Text("Plus tard")
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextTertiary)
                        .frame(height: 44)
                }

                Spacer().frame(height: StrakkSpacing.md)
            }
        }
        .presentationDetents([.height(420)])
        .presentationDragIndicator(.visible)
    }
}

// MARK: - SF Symbol helper (file-private)

private func sfSymbol(for feature: ProFeature) -> String {
    switch feature {
    case .aiPhotoAnalysis: return "camera.viewfinder"
    case .aiTextAnalysis: return "text.bubble"
    case .aiWeeklySummary: return "chart.bar.xaxis"
    case .healthSync: return "heart.circle"
    case .unlimitedHistory: return "clock.arrow.circlepath"
    case .photoComparison: return "photo.on.rectangle.angled"
    case .hevyExport: return "dumbbell.fill"
    default: return "star"
    }
}

// MARK: - ProFeature → ProFeatureInfo lookup

func proFeatureInfo(for feature: ProFeature) -> ProFeatureInfo? {
    ProFeatureInfoKt.allProFeatures().first { ($0 as? ProFeatureInfo)?.feature == feature } as? ProFeatureInfo
}

// MARK: - ViewModifier for gated feature flow

private struct FeatureGateModifier: ViewModifier {
    @Binding var gatedFeature: ProFeature?
    @State private var showPaywall = false
    @State private var highlightedFeature: ProFeature?

    func body(content: Content) -> some View {
        content
            .sheet(isPresented: Binding(
                get: { gatedFeature != nil },
                set: { if !$0 { gatedFeature = nil } }
            )) {
                if let feature = gatedFeature, let info = proFeatureInfo(for: feature) {
                    FeatureGateSheet(
                        featureInfo: info,
                        onDiscoverPro: {
                            highlightedFeature = feature
                            gatedFeature = nil
                            showPaywall = true
                        },
                        onDismiss: { gatedFeature = nil }
                    )
                }
            }
            .fullScreenCover(isPresented: $showPaywall) {
                PaywallView(
                    highlightedFeature: highlightedFeature,
                    onDismiss: { showPaywall = false }
                )
            }
    }
}

extension View {
    func featureGate(_ gatedFeature: Binding<ProFeature?>) -> some View {
        modifier(FeatureGateModifier(gatedFeature: gatedFeature))
    }
}
