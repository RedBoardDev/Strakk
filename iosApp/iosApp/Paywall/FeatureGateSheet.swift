import SwiftUI
import shared

// MARK: - FeatureGateSheet

struct FeatureGateSheet: View {
    let metadata: FeatureMetadata
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

                    Image(systemName: metadata.iconIos)
                        .font(.system(size: 32, weight: .medium))
                        .foregroundStyle(Color.strakkPrimary)
                }

                Spacer().frame(height: StrakkSpacing.xl)

                // PRO badge
                ProBadge()

                Spacer().frame(height: StrakkSpacing.sm)

                // Title
                Text(String(localized: String.LocalizationValue(metadata.titleKey)))
                    .font(.strakkHeading2)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, StrakkSpacing.xl)

                Spacer().frame(height: StrakkSpacing.xs)

                // Description
                Text(String(localized: String.LocalizationValue(metadata.descriptionKey)))
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
                    Text("Unlock with Pro")
                        .font(.strakkBodyBold)
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(Color.strakkPrimary, in: RoundedRectangle(cornerRadius: StrakkRadius.md))
                }
                .padding(.horizontal, StrakkSpacing.lg)
                .accessibilityLabel("Unlock this feature with Strakk Pro")

                Spacer().frame(height: StrakkSpacing.sm)

                // Dismiss
                Button {
                    onDismiss()
                } label: {
                    Text("Later")
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

// MARK: - ViewModifier for gated feature flow

private struct FeatureGateModifier: ViewModifier {
    @Binding var gatedFeature: Feature?
    @State private var showPaywall = false
    @State private var highlightedFeature: Feature?

    func body(content: Content) -> some View {
        content
            .sheet(isPresented: Binding(
                get: { gatedFeature != nil },
                set: { if !$0 { gatedFeature = nil } }
            )) {
                if let feature = gatedFeature {
                    let metadata = FeatureRegistry.companion.get(feature: feature)
                    FeatureGateSheet(
                        metadata: metadata,
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
    func featureGate(_ gatedFeature: Binding<Feature?>) -> some View {
        modifier(FeatureGateModifier(gatedFeature: gatedFeature))
    }
}
