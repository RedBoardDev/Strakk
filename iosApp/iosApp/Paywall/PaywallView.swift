// swiftlint:disable type_body_length file_length
import SwiftUI
import shared

// MARK: - PaywallView

struct PaywallView: View {
    @State var viewModel: PaywallViewModelWrapper
    var onDismiss: () -> Void

    init(highlightedFeature: Feature? = nil, onDismiss: @escaping () -> Void) {
        self._viewModel = State(wrappedValue: PaywallViewModelWrapper(highlightedFeature: highlightedFeature))
        self.onDismiss = onDismiss
    }

    private var data: PaywallData { viewModel.paywallData }
    private var isAnnual: Bool { data.selectedPlan == .annual }

    var body: some View {
        ZStack(alignment: .bottom) {
            Color.strakkBackground.ignoresSafeArea()

            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    heroSection
                    featuresSection
                        .padding(.top, StrakkSpacing.xxl)
                    planCardsSection
                        .padding(.top, StrakkSpacing.xxl)
                    Spacer()
                        .frame(height: 200)
                }
                .padding(.horizontal, StrakkSpacing.lg)
            }

            stickyBottomBar
        }
        .overlay(alignment: .topTrailing) { closeButton }
        .onChange(of: viewModel.shouldDismiss) { _, shouldDismiss in
            if shouldDismiss { onDismiss() }
        }
        .alert(
            "Strakk Pro",
            isPresented: Binding(
                get: { viewModel.toastMessage != nil },
                set: { if !$0 { viewModel.toastMessage = nil } }
            )
        ) {
            Button("OK") { viewModel.toastMessage = nil }
        } message: {
            Text(viewModel.toastMessage ?? "")
        }
    }

    // MARK: - Close button

    private var closeButton: some View {
        Button {
            viewModel.onEvent(PaywallEventOnDismiss())
        } label: {
            Image(systemName: "xmark")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(Color.strakkTextTertiary)
                .frame(width: 30, height: 30)
                .background(Color.strakkSurface2, in: Circle())
        }
        .padding(.trailing, StrakkSpacing.lg)
        .padding(.top, StrakkSpacing.xxl)
        .accessibilityLabel("Close")
    }

    // MARK: - Hero

    private var heroSection: some View {
        VStack(spacing: 0) {
            Spacer().frame(height: 56)

            ZStack {
                Circle()
                    .fill(
                        RadialGradient(
                            colors: [
                                Color.strakkPrimary.opacity(0.20),
                                Color.strakkPrimary.opacity(0.05),
                                Color.clear
                            ],
                            center: .center,
                            startRadius: 0,
                            endRadius: 90
                        )
                    )
                    .frame(width: 180, height: 180)

                Circle()
                    .fill(Color.strakkSurface1)
                    .frame(width: 72, height: 72)

                Image(systemName: "sparkles")
                    .font(.system(size: 32, weight: .medium))
                    .foregroundStyle(Color.strakkPrimary)
            }

            Spacer().frame(height: StrakkSpacing.lg)

            ProBadge()

            Spacer().frame(height: StrakkSpacing.sm)

            Text("Unlock your full\ntracking potential")
                .font(.strakkHeading1)
                .foregroundStyle(Color.strakkTextPrimary)
                .multilineTextAlignment(.center)
                .lineSpacing(2)

            Spacer().frame(height: StrakkSpacing.xs)

            Text("The AI that understands your plate.")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
                .multilineTextAlignment(.center)

            Spacer().frame(height: StrakkSpacing.md)
        }
    }

    // MARK: - Features

    private var featuresSection: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("EVERYTHING IN PRO")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)
                .kerning(0.8)
                .padding(.bottom, StrakkSpacing.md)

            VStack(spacing: 6) {
                ForEach(data.features, id: \.feature) { metadata in
                    featureRow(
                        metadata: metadata,
                        highlighted: metadata.feature == data.highlightedFeature
                    )
                }
            }
        }
    }

    @ViewBuilder
    private func featureRow(metadata: FeatureMetadata, highlighted: Bool) -> some View {
        HStack(spacing: StrakkSpacing.sm) {
            Image(systemName: metadata.iconIos)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(highlighted ? Color.strakkPrimary : Color.strakkTextSecondary)
                .frame(width: 30, height: 30)
                .background(
                    highlighted ? Color.strakkPrimary.opacity(0.10) : Color.strakkSurface2,
                    in: RoundedRectangle(cornerRadius: 8)
                )

            VStack(alignment: .leading, spacing: 2) {
                Text(featureTitle(for: metadata.feature))
                    .font(highlighted ? .strakkBodyBold : .strakkBody)
                    .foregroundStyle(Color.strakkTextPrimary)

                if let quota = featureQuotaLabel(for: metadata.feature) {
                    Text(quota)
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextTertiary)
                }
            }

            Spacer()

            Image(systemName: "checkmark")
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(Color.strakkSuccess)
        }
        .padding(.horizontal, StrakkSpacing.sm)
        .padding(.vertical, 10)
        .background(
            highlighted ? Color.strakkSurface1 : Color.clear,
            in: RoundedRectangle(cornerRadius: 10)
        )
    }

    private func featureTitle(for feature: Feature) -> String {
        switch feature {
        case .aiPhotoAnalysis: return String(localized: "AI photo analysis")
        case .aiTextAnalysis: return String(localized: "AI text analysis")
        case .aiWeeklySummary: return String(localized: "AI weekly summary")
        case .healthSync: return String(localized: "Health sync")
        case .unlimitedHistory: return String(localized: "Unlimited history")
        case .photoComparison: return String(localized: "Photo comparison")
        case .hevyExport: return String(localized: "Hevy export")
        }
    }

    private func featureQuotaLabel(for feature: Feature) -> String? {
        switch feature {
        case .aiPhotoAnalysis: return String(localized: "100/month")
        case .aiTextAnalysis: return String(localized: "100/month")
        case .aiWeeklySummary: return String(localized: "5/month")
        case .hevyExport: return String(localized: "2/month")
        default: return nil
        }
    }

    // MARK: - Plan cards

    private var planCardsSection: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("CHOOSE YOUR PLAN")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)
                .kerning(0.8)
                .padding(.bottom, StrakkSpacing.md)

            HStack(spacing: StrakkSpacing.sm) {
                annualPlanCard
                monthlyPlanCard
            }
        }
    }

    private var annualPlanCard: some View {
        let selected = data.selectedPlan == .annual
        return Button {
            HapticEngine.light()
            viewModel.onEvent(PaywallEventOnPlanSelected(plan: .annual))
        } label: {
            VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
                HStack {
                    Text("Annual")
                        .font(.strakkCaptionBold)
                        .foregroundStyle(selected ? Color.strakkPrimary : Color.strakkTextSecondary)
                    Spacer()
                    Text("2 MONTHS FREE")
                        .font(.strakkOverline)
                        .foregroundStyle(Color.strakkSuccess)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 3)
                        .background(
                            Color.strakkSuccess.opacity(0.10),
                            in: RoundedRectangle(cornerRadius: 4)
                        )
                }

                HStack(alignment: .firstTextBaseline, spacing: 2) {
                    Text("19.99\u{00A0}€")
                        .font(.strakkHeading2)
                        .foregroundStyle(Color.strakkTextPrimary)
                    Text("/yr")
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextTertiary)
                }

                Text("i.e. 1.67\u{00A0}€/month")
                    .font(.strakkCaption)
                    .foregroundStyle(selected ? Color.strakkSuccess : Color.strakkTextTertiary)
            }
            .padding(StrakkSpacing.md)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: StrakkRadius.sm))
            .overlay(
                RoundedRectangle(cornerRadius: StrakkRadius.sm)
                    .strokeBorder(
                        selected ? Color.strakkPrimary : Color.strakkBorderFaint,
                        lineWidth: selected ? 1.5 : 1
                    )
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Annual plan, 19.99 euros per year")
    }

    private var monthlyPlanCard: some View {
        let selected = data.selectedPlan == .monthly
        return Button {
            HapticEngine.light()
            viewModel.onEvent(PaywallEventOnPlanSelected(plan: .monthly))
        } label: {
            VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
                Text("Monthly")
                    .font(.strakkCaptionBold)
                    .foregroundStyle(selected ? Color.strakkPrimary : Color.strakkTextSecondary)

                HStack(alignment: .firstTextBaseline, spacing: 2) {
                    Text("1.99\u{00A0}€")
                        .font(.strakkHeading2)
                        .foregroundStyle(Color.strakkTextPrimary)
                    Text("/mo")
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextTertiary)
                }

                Text("No commitment")
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextTertiary)
            }
            .padding(StrakkSpacing.md)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: StrakkRadius.sm))
            .overlay(
                RoundedRectangle(cornerRadius: StrakkRadius.sm)
                    .strokeBorder(
                        selected ? Color.strakkPrimary : Color.strakkBorderFaint,
                        lineWidth: selected ? 1.5 : 1
                    )
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Monthly plan, 1.99 euros per month")
    }

    // MARK: - Sticky bottom bar

    private var stickyBottomBar: some View {
        VStack(spacing: 0) {
            Rectangle()
                .fill(Color.strakkDivider)
                .frame(height: 0.5)

            VStack(spacing: StrakkSpacing.sm) {
                // Price recap above CTA
                if !data.isAlreadyPro {
                    Text(priceRecap)
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextSecondary)
                        .multilineTextAlignment(.center)
                }

                // CTA
                Button {
                    HapticEngine.medium()
                    viewModel.onEvent(PaywallEventOnSubscribeTapped())
                } label: {
                    Group {
                        if data.isProcessing {
                            ProgressView().tint(.white)
                        } else {
                            HStack(spacing: 6) {
                                Text(ctaLabel)
                                    .font(.strakkBodyBold)
                                Image(systemName: "chevron.right")
                                    .font(.system(size: 13, weight: .semibold))
                            }
                            .foregroundStyle(.white)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 54)
                    .background(
                        LinearGradient(
                            colors: data.isProcessing
                                ? [Color.strakkPrimary.opacity(0.5)]
                                : [Color.strakkPrimary, Color.strakkPrimaryLight],
                            startPoint: .leading,
                            endPoint: .trailing
                        ),
                        in: RoundedRectangle(cornerRadius: StrakkRadius.sm)
                    )
                }
                .disabled(data.isProcessing || data.isAlreadyPro)
                .accessibilityLabel("Subscribe to Strakk Pro")

                // Trust line
                HStack(spacing: 6) {
                    Image(systemName: "lock.shield.fill")
                        .font(.system(size: 10))
                    Text("No commitment · Cancel anytime")
                        .font(.strakkCaption)
                }
                .foregroundStyle(Color.strakkTextTertiary)

                // Restore
                Button {
                    viewModel.onEvent(PaywallEventOnRestoreTapped())
                } label: {
                    Text("Restore purchase")
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextTertiary)
                        .underline(color: Color.strakkTextTertiary.opacity(0.4))
                }
            }
            .padding(.horizontal, StrakkSpacing.lg)
            .padding(.top, StrakkSpacing.md)
            .padding(.bottom, StrakkSpacing.lg)
        }
        .background(
            Color.strakkBackground
                .opacity(0.95)
                .background(.ultraThinMaterial)
        )
        .ignoresSafeArea(edges: .bottom)
    }

    private var priceRecap: String {
        if isAnnual {
            return String(localized: "19.99\u{00A0}€/year · That's only 0.38\u{00A0}€/week")
        }
        return String(localized: "1.99\u{00A0}€/month · Cancel anytime")
    }

    private var ctaLabel: String {
        if data.isAlreadyPro { return String(localized: "Already Pro") }
        return String(localized: "Continue")
    }
}

// MARK: - Preview

#Preview {
    PaywallView(onDismiss: {})
}

#Preview("With highlight") {
    PaywallView(highlightedFeature: .aiPhotoAnalysis, onDismiss: {})
}
