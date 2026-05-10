import SwiftUI
import shared

struct TrialExpiredModal: View {
    var onDiscoverOffers: () -> Void
    var onContinueFree: () -> Void

    /// Default while store prices are unavailable.
    private static let priceDash = "\u{2014}"

    @State private var annualSegment: String = Self.priceDash
    @State private var perMonthSegment: String?

    var body: some View {
        ZStack {
            Color.black.opacity(0.6)
                .ignoresSafeArea()

            VStack(alignment: .leading, spacing: 0) {
                Text(String(localized: "trial_expired_title"))
                    .font(.strakkHeading2)
                    .foregroundStyle(Color.strakkTextPrimary)

                Spacer().frame(height: StrakkSpacing.md)

                Text(String(localized: "trial_expired_body"))
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextSecondary)
                    .fixedSize(horizontal: false, vertical: true)

                Spacer().frame(height: StrakkSpacing.lg)

                priceRow

                Spacer().frame(height: StrakkSpacing.xl)

                StrakkPrimaryButton(
                    title: "trial_expired_cta",
                    action: onDiscoverOffers
                )

                Spacer().frame(height: StrakkSpacing.md)

                Button {
                    onContinueFree()
                } label: {
                    Text(String(localized: "trial_expired_free"))
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
        .task {
            await loadPricesFromStore()
        }
    }

    @ViewBuilder
    private var priceRow: some View {
        if let perMonth = perMonthSegment {
            HStack(spacing: 0) {
                Text(annualSegment)
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkTextPrimary)
                Text(" · ")
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkTextPrimary)
                Text(perMonth)
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkPrimary)
            }
        } else {
            Text(annualSegment)
                .font(.strakkBodyBold)
                .foregroundStyle(Color.strakkTextPrimary)
        }
    }

    private func loadPricesFromStore() async {
        let prices: PaywallOfferPrices
        do {
            prices = try await KoinBridge.shared.getLoadPaywallOfferPricesUseCase().invoke()
        } catch {
            return
        }
        let annual = prices.annualFormatted?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let perMo = prices.annualPricePerMonthFormatted?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !annual.isEmpty, !perMo.isEmpty {
            annualSegment = annual
            perMonthSegment = perMo
        } else if !annual.isEmpty {
            annualSegment = annual
            perMonthSegment = nil
        }
    }
}
