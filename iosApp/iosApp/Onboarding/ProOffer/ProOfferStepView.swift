import SwiftUI
import shared

// MARK: - Top 3 Pro features for trial confirmation

private struct ProOfferFeature: Sendable {
    let symbol: String
    /// `LocalizedStringKey` is not `Sendable`; store the source string and wrap
    /// it at the call site via `Text(LocalizedStringKey(stringLiteral:))`.
    let titleKey: String
    let descriptionKey: String
}

private let proOfferFeatures: [ProOfferFeature] = [
    .init(
        symbol: "camera.viewfinder",
        titleKey: "Smart photo",
        descriptionKey: "Snap a meal, AI computes your macros."
    ),
    .init(
        symbol: "text.bubble",
        titleKey: "Smart text",
        descriptionKey: "Describe your meal, AI does the rest."
    ),
    .init(
        symbol: "chart.bar.xaxis",
        titleKey: "Weekly AI summary",
        descriptionKey: "A personalized recap every week."
    )
]

// MARK: - ProOfferStepView

struct ProOfferStepView: View {
    var wrapper: OnboardingFlowViewModelWrapper

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Good news!")
                .font(.strakkHeading2)
                .foregroundStyle(Color.strakkTextSecondary)
                .padding(.top, StrakkSpacing.xxl)

            headlineText
                .padding(.top, StrakkSpacing.xs)

            featureCard
                .padding(.top, StrakkSpacing.xl)

            Spacer()

            ctaSection
        }
        .padding(.horizontal, StrakkSpacing.lg)
        .padding(.bottom, StrakkSpacing.xl)
    }

    private var headlineText: some View {
        (Text("Your ")
            .font(.strakkHeading1)
            .foregroundStyle(Color.strakkTextPrimary)
        + Text("7-day")
            .font(.strakkHeading1)
            .foregroundStyle(Color.strakkPrimary)
        + Text(" Pro trial is live!")
            .font(.strakkHeading1)
            .foregroundStyle(Color.strakkTextPrimary))
            .fixedSize(horizontal: false, vertical: true)
    }

    private var featureCard: some View {
        VStack(spacing: 0) {
            ForEach(Array(proOfferFeatures.enumerated()), id: \.offset) { index, feature in
                HStack(alignment: .top, spacing: StrakkSpacing.sm) {
                    Image(systemName: feature.symbol)
                        .font(.system(size: 20))
                        .foregroundStyle(Color.strakkPrimary)
                        .frame(width: 28)

                    VStack(alignment: .leading, spacing: StrakkSpacing.xxs) {
                        Text(LocalizedStringKey(feature.titleKey))
                            .font(.strakkBodyBold)
                            .foregroundStyle(Color.strakkTextPrimary)
                        Text(LocalizedStringKey(feature.descriptionKey))
                            .font(.strakkCaption)
                            .foregroundStyle(Color.strakkTextSecondary)
                    }
                }
                .padding(.vertical, StrakkSpacing.sm)

                if index < proOfferFeatures.count - 1 {
                    Divider()
                        .background(Color.strakkDividerWeak)
                }
            }
        }
        .padding(StrakkSpacing.md)
        .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: StrakkRadius.sm))
    }

    private var ctaSection: some View {
        VStack(spacing: StrakkSpacing.sm) {
            StrakkPrimaryButton(
                title: "Let's go",
                action: { wrapper.send(OnboardingFlowEventOnStartFreeTrial()) }
            )

            Text("Enjoy every Pro feature for 7 days.")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
                .multilineTextAlignment(.center)
        }
    }
}
