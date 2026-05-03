import SwiftUI
import shared

// MARK: - Top 3 Pro features for trial confirmation

private let proOfferFeatures: [(symbol: String, title: String, description: String)] = [
    ("camera.viewfinder", "Photo intelligente", "Prends une photo, l'IA calcule tes macros."),
    ("text.bubble", "Texte intelligent", "Décris ton repas, l'IA fait le reste."),
    ("chart.bar.xaxis", "Bilan hebdo IA", "Un résumé personnalisé chaque semaine.")
]

// MARK: - ProOfferStepView

struct ProOfferStepView: View {
    var wrapper: OnboardingFlowViewModelWrapper

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Bonne nouvelle !")
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

    // MARK: - Headline with colored "7 jours"

    private var headlineText: some View {
        (Text("Ton essai Pro de ")
            .font(.strakkHeading1)
            .foregroundStyle(Color.strakkTextPrimary)
        + Text("7 jours")
            .font(.strakkHeading1)
            .foregroundStyle(Color.strakkPrimary)
        + Text(" est activé !")
            .font(.strakkHeading1)
            .foregroundStyle(Color.strakkTextPrimary))
            .fixedSize(horizontal: false, vertical: true)
    }

    // MARK: - Feature card

    private var featureCard: some View {
        VStack(spacing: 0) {
            ForEach(Array(proOfferFeatures.enumerated()), id: \.offset) { index, feature in
                HStack(alignment: .top, spacing: StrakkSpacing.sm) {
                    Image(systemName: feature.symbol)
                        .font(.system(size: 20))
                        .foregroundStyle(Color.strakkPrimary)
                        .frame(width: 28)

                    VStack(alignment: .leading, spacing: StrakkSpacing.xxs) {
                        Text(feature.title)
                            .font(.strakkBodyBold)
                            .foregroundStyle(Color.strakkTextPrimary)
                        Text(feature.description)
                            .font(.strakkCaption)
                            .foregroundStyle(Color.strakkTextSecondary)
                    }
                }
                .padding(.vertical, StrakkSpacing.sm)

                if index < proOfferFeatures.count - 1 {
                    Divider()
                        .background(Color.strakkDivider)
                }
            }
        }
        .padding(StrakkSpacing.md)
        .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: StrakkRadius.sm))
    }

    // MARK: - CTA section

    private var ctaSection: some View {
        VStack(spacing: StrakkSpacing.sm) {
            Button {
                HapticEngine.light()
                wrapper.send(OnboardingFlowEventOnStartFreeTrial())
            } label: {
                Text("C'est parti !")
                    .font(.strakkBodyBold)
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color.strakkPrimary, in: RoundedRectangle(cornerRadius: StrakkRadius.sm))
            }

            Text("Profite de toutes les fonctionnalités Pro pendant 7 jours.")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
                .multilineTextAlignment(.center)
        }
    }
}
