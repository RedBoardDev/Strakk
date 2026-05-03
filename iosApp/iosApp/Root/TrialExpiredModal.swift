import SwiftUI

struct TrialExpiredModal: View {
    var onDiscoverOffers: () -> Void
    var onContinueFree: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.6)
                .ignoresSafeArea()

            VStack(alignment: .leading, spacing: 0) {
                Text("Ton essai se termine aujourd'hui")
                    .font(.strakkHeading2)
                    .foregroundStyle(Color.strakkTextPrimary)

                Spacer().frame(height: StrakkSpacing.md)

                Text("Continue avec Strakk Pro pour garder l'IA, la sync Santé et l'historique illimité.")
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextSecondary)
                    .fixedSize(horizontal: false, vertical: true)

                Spacer().frame(height: StrakkSpacing.lg)

                priceRow

                Spacer().frame(height: StrakkSpacing.xl)

                Button {
                    HapticEngine.light()
                    onDiscoverOffers()
                } label: {
                    Text("Découvrir les offres")
                        .font(.strakkBodyBold)
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(Color.strakkPrimary, in: RoundedRectangle(cornerRadius: StrakkRadius.md))
                }

                Spacer().frame(height: StrakkSpacing.md)

                Button {
                    onContinueFree()
                } label: {
                    Text("Continuer en gratuit")
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextSecondary)
                        .frame(maxWidth: .infinity)
                        .frame(height: 44)
                }
            }
            .padding(StrakkSpacing.xl)
            .background(Color.strakkSurface3, in: RoundedRectangle(cornerRadius: StrakkRadius.lg))
            .padding(.horizontal, 32)
        }
        .transition(.opacity)
    }

    private var priceRow: some View {
        HStack(spacing: StrakkSpacing.xxs) {
            Text("19,99 €/an · ")
                .font(.strakkBodyBold)
                .foregroundStyle(Color.strakkTextPrimary)
            + Text("1,67 €/mois")
                .font(.strakkBodyBold)
                .foregroundStyle(Color.strakkPrimary)
        }
    }
}
