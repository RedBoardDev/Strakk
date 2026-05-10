// swiftlint:disable type_body_length
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

    /// Fallback while RevenueCat offerings load or if a package is missing.
    private func formattedPriceOrPlaceholder(_ value: String?) -> String {
        guard let value, !value.isEmpty else {
            return String(localized: "Paywall price placeholder")
        }
        return value
    }

    var body: some View {
        ZStack {
            Color.strakkBackground.ignoresSafeArea()

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    compactHeaderSection
                    featuresSection
                        .padding(.top, StrakkSpacing.lg)
                }
                .padding(.horizontal, StrakkSpacing.lg)
                .padding(.bottom, StrakkSpacing.sm)
            }
            .scrollContentBackground(.hidden)
            .safeAreaInset(edge: .bottom, spacing: 0) {
                paywallBottomStack
            }
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
            ZStack {
                Circle()
                    .fill(Color.strakkSurface2)
                    .frame(width: 32, height: 32)
                Image(systemName: "xmark")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(Color.strakkTextTertiary)
            }
            .frame(width: 44, height: 44)
            .contentShape(Circle())
        }
        .padding(.trailing, StrakkSpacing.sm)
        .padding(.top, StrakkSpacing.xxl)
        .accessibilityLabel("Close")
    }

    // MARK: - Compact header (no hero chrome)

    private var compactHeaderSection: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
            Spacer()
                .frame(height: 8)

            Text(String(localized: "Unlock the full potential"))
                .font(.strakkHeading1)
                .foregroundStyle(Color.strakkTextPrimary)
                .fixedSize(horizontal: false, vertical: true)

            Text(
                String(
                    localized: "Smarter insights, cleaner tracking, and premium tools to help you stay consistent."
                )
            )
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: - Features

    private var featuresSection: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(String(localized: "EVERYTHING IN PRO"))
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
                .foregroundStyle(Color.strakkTextTertiary)
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
        default: return String(localized: "Feature")
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

    // MARK: - Fixed bottom: plans (not in scroll) + CTA

    private var paywallBottomStack: some View {
        VStack(spacing: 0) {
            Rectangle()
                .fill(Color.strakkDivider)
                .frame(height: 0.5)

            VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
                Text(String(localized: "CHOOSE YOUR PLAN"))
                    .font(.strakkOverline)
                    .foregroundStyle(Color.strakkTextTertiary)
                    .kerning(0.8)

                VStack(spacing: StrakkSpacing.xs) {
                    monthlyPlanCard
                    annualPlanCard
                }

                Button {
                    HapticEngine.medium()
                    viewModel.onEvent(PaywallEventOnSubscribeTapped())
                } label: {
                    Group {
                        if data.isProcessing {
                            ProgressView().tint(.white)
                        } else {
                            Text(ctaLabel)
                                .font(.strakkBodyBold)
                                .foregroundStyle(.white)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
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
                .disabled(data.isProcessing || data.selectedPlanIsCurrent)
                .accessibilityLabel("Subscribe to Strakk Pro")
            }
            .padding(.horizontal, StrakkSpacing.lg)
            .padding(.top, StrakkSpacing.md)
            .padding(.bottom, StrakkSpacing.lg)
        }
        .frame(maxWidth: .infinity)
        .background(Color.strakkBackground.ignoresSafeArea(edges: .bottom))
    }

    private var annualPlanCard: some View {
        let selected = data.selectedPlan == .annual
        return Button {
            HapticEngine.light()
            viewModel.onEvent(PaywallEventOnPlanSelected(plan: .annual))
        } label: {
            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .center) {
                    Text(String(localized: "Annual"))
                        .font(.strakkBodyBold)
                        .foregroundStyle(Color.strakkTextPrimary)
                    Spacer()
                    Text(String(localized: "2 MONTHS FREE"))
                        .font(.strakkOverline)
                        .foregroundStyle(Color.strakkPrimary)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(
                            Color.strakkPrimary.opacity(0.12),
                            in: RoundedRectangle(cornerRadius: 4)
                        )
                }

                HStack(alignment: .firstTextBaseline, spacing: 4) {
                    Text(formattedPriceOrPlaceholder(data.annualPriceFormatted))
                        .font(.strakkHeading3)
                        .foregroundStyle(Color.strakkTextPrimary)
                    Text(String(localized: "/year"))
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextTertiary)
                }

                if let perMonth = data.annualPricePerMonthFormatted, !perMonth.isEmpty {
                    Text(String(format: String(localized: "That's only %@ per month"), perMonth))
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextTertiary)
                }
            }
            .padding(StrakkSpacing.sm)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: StrakkRadius.sm))
            .overlay(
                RoundedRectangle(cornerRadius: StrakkRadius.sm)
                    .strokeBorder(
                        selected ? Color.strakkPrimary : Color.strakkBorderFaint,
                        lineWidth: selected ? 2 : 1
                    )
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel(
            String(
                format: String(localized: "Annual plan, %@ per year"),
                formattedPriceOrPlaceholder(data.annualPriceFormatted)
            )
        )
    }

    private var monthlyPlanCard: some View {
        let selected = data.selectedPlan == .monthly
        return Button {
            HapticEngine.light()
            viewModel.onEvent(PaywallEventOnPlanSelected(plan: .monthly))
        } label: {
            VStack(alignment: .leading, spacing: 6) {
                Text(String(localized: "Monthly access"))
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkTextPrimary)

                HStack(alignment: .firstTextBaseline, spacing: 4) {
                    Text(formattedPriceOrPlaceholder(data.monthlyPriceFormatted))
                        .font(.strakkHeading3)
                        .foregroundStyle(Color.strakkTextPrimary)
                    Text(String(localized: "/month"))
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextTertiary)
                }
            }
            .padding(StrakkSpacing.sm)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: StrakkRadius.sm))
            .overlay(
                RoundedRectangle(cornerRadius: StrakkRadius.sm)
                    .strokeBorder(
                        selected ? Color.strakkPrimary : Color.strakkBorderFaint,
                        lineWidth: selected ? 2 : 1
                    )
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel(
            String(
                format: String(localized: "Monthly plan, %@ per month"),
                formattedPriceOrPlaceholder(data.monthlyPriceFormatted)
            )
        )
    }

    private var ctaLabel: String {
        if data.selectedPlanIsCurrent { return String(localized: "Current plan") }
        return String(localized: "Unlock the full potential")
    }
}

// MARK: - Preview

#Preview {
    PaywallView(onDismiss: {})
}

#Preview("With highlight") {
    PaywallView(highlightedFeature: .aiPhotoAnalysis, onDismiss: {})
}
