import SwiftUI
import shared

// MARK: - SF Symbol mapping for ProFeature

private func sfSymbol(for feature: ProFeature) -> String {
    switch feature {
    case .aiPhotoAnalysis: return "camera.viewfinder"
    case .aiTextAnalysis: return "text.bubble"
    case .aiWeeklySummary: return "chart.bar.xaxis"
    case .healthSync: return "heart.circle"
    case .unlimitedHistory: return "clock.arrow.circlepath"
    case .photoComparison: return "photo.on.rectangle.angled"
    default: return "star"
    }
}

// MARK: - PaywallView

struct PaywallView: View {
    @State var viewModel: PaywallViewModelWrapper
    var onDismiss: () -> Void

    init(highlightedFeature: ProFeature? = nil, onDismiss: @escaping () -> Void) {
        self._viewModel = State(wrappedValue: PaywallViewModelWrapper(highlightedFeature: highlightedFeature))
        self.onDismiss = onDismiss
    }

    var body: some View {
        ZStack {
            Color.strakkBackground.ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    closeButton
                        .frame(maxWidth: .infinity, alignment: .trailing)
                        .padding(.horizontal, StrakkSpacing.lg)
                        .padding(.top, StrakkSpacing.lg)

                    overlineLabel
                        .padding(.horizontal, StrakkSpacing.lg)
                        .padding(.top, StrakkSpacing.xs)

                    headline
                        .padding(.horizontal, StrakkSpacing.lg)
                        .padding(.top, StrakkSpacing.xs)

                    subheadline
                        .padding(.horizontal, StrakkSpacing.lg)
                        .padding(.top, StrakkSpacing.xs)

                    featureList
                        .padding(.horizontal, StrakkSpacing.lg)
                        .padding(.top, StrakkSpacing.xl)

                    planToggle
                        .padding(.horizontal, StrakkSpacing.lg)
                        .padding(.top, StrakkSpacing.xl)

                    priceCard
                        .padding(.horizontal, StrakkSpacing.lg)
                        .padding(.top, StrakkSpacing.md)

                    ctaButton
                        .padding(.horizontal, StrakkSpacing.lg)
                        .padding(.top, StrakkSpacing.xl)

                    footerText
                        .padding(.horizontal, StrakkSpacing.lg)
                        .padding(.top, StrakkSpacing.md)

                    restoreButton
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, StrakkSpacing.lg)
                        .padding(.top, StrakkSpacing.md)
                        .padding(.bottom, StrakkSpacing.xxl)
                }
            }
        }
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
                .font(.system(size: 16, weight: .medium))
                .foregroundStyle(Color.strakkTextSecondary)
                .frame(width: 36, height: 36)
                .background(Color.strakkSurface1, in: Circle())
        }
        .accessibilityLabel("Fermer")
    }

    // MARK: - Overline / headline / subheadline

    private var overlineLabel: some View {
        Text("STRAKK PRO")
            .font(.strakkOverline)
            .foregroundStyle(Color.strakkPrimaryLight)
            .kerning(1.0)
    }

    private var headline: some View {
        Text("L'IA tracke tes repas pour toi.")
            .font(.strakkHeading1)
            .foregroundStyle(Color.strakkTextPrimary)
    }

    private var subheadline: some View {
        Text("Photographie ton assiette ou décris ton repas en une phrase. Strakk Pro fait le reste.")
            .font(.strakkBody)
            .foregroundStyle(Color.strakkTextSecondary)
            .fixedSize(horizontal: false, vertical: true)
    }

    // MARK: - Feature list

    private var featureList: some View {
        let data = viewModel.paywallData
        return VStack(spacing: StrakkSpacing.md) {
            ForEach(data.features, id: \.feature) { info in
                featureRow(info: info, highlighted: info.feature == data.highlightedFeature)
            }
        }
    }

    @ViewBuilder
    private func featureRow(info: ProFeatureInfo, highlighted: Bool) -> some View {
        HStack(alignment: .top, spacing: StrakkSpacing.md) {
            Image(systemName: sfSymbol(for: info.feature))
                .font(.system(size: 20))
                .foregroundStyle(Color.strakkPrimary)
                .frame(width: 28)

            VStack(alignment: .leading, spacing: StrakkSpacing.xxs) {
                Text(info.title)
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkTextPrimary)
                Text(info.description_)
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(StrakkSpacing.md)
        .background(highlighted ? Color.strakkSurface1 : Color.clear)
        .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
        .overlay(alignment: .leading) {
            if highlighted {
                Rectangle()
                    .fill(Color.strakkPrimary)
                    .frame(width: 3)
                    .clipShape(RoundedRectangle(cornerRadius: 2))
            }
        }
    }

    // MARK: - Plan toggle

    private var planToggle: some View {
        let data = viewModel.paywallData
        return HStack(spacing: 0) {
            planTab(
                label: "Mensuel",
                badge: nil,
                isSelected: data.selectedPlan == .monthly
            ) {
                viewModel.onEvent(PaywallEventOnPlanSelected(plan: .monthly))
            }
            planTab(
                label: "Annuel",
                badge: "POPULAIRE",
                isSelected: data.selectedPlan == .annual
            ) {
                viewModel.onEvent(PaywallEventOnPlanSelected(plan: .annual))
            }
        }
        .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: StrakkRadius.sm))
    }

    @ViewBuilder
    private func planTab(label: String, badge: String?, isSelected: Bool, onTap: @escaping () -> Void) -> some View {
        Button(action: onTap) {
            HStack(spacing: StrakkSpacing.xxs) {
                Text(label)
                    .font(.strakkBodyBold)
                    .foregroundStyle(isSelected ? .white : Color.strakkTextSecondary)
                if let badge {
                    Text(badge)
                        .font(.strakkOverline)
                        .foregroundStyle(isSelected ? Color.strakkPrimary : Color.strakkTextTertiary)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(
                            isSelected ? Color.strakkPrimary.opacity(0.15) : Color.strakkSurface2,
                            in: RoundedRectangle(cornerRadius: 4)
                        )
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 44)
            .background(
                isSelected ? Color.strakkPrimary : Color.clear,
                in: RoundedRectangle(cornerRadius: StrakkRadius.sm)
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label + (badge.map { ", \($0)" } ?? ""))
    }

    // MARK: - Price card

    private var priceCard: some View {
        let isAnnual = viewModel.paywallData.selectedPlan == .annual
        return VStack(alignment: .leading, spacing: StrakkSpacing.xxs) {
            Text(isAnnual ? "19,99 €/an" : "1,99 €/mois")
                .font(.strakkHeading2)
                .foregroundStyle(Color.strakkTextPrimary)
            Text(isAnnual ? "soit 1,67 €/mois · 2 mois offerts" : "Sans engagement")
                .font(.strakkBody)
                .foregroundStyle(isAnnual ? Color.strakkSuccess : Color.strakkTextSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(StrakkSpacing.md)
        .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: StrakkRadius.md))
    }

    // MARK: - CTA

    private var ctaButton: some View {
        let data = viewModel.paywallData
        return Button {
            HapticEngine.light()
            viewModel.onEvent(PaywallEventOnSubscribeTapped())
        } label: {
            Group {
                if data.isProcessing {
                    ProgressView()
                        .tint(.white)
                } else {
                    Text("Essayer 7 jours gratuit")
                        .font(.strakkBodyBold)
                        .foregroundStyle(.white)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(
                data.isProcessing ? Color.strakkPrimary.opacity(0.6) : Color.strakkPrimary,
                in: RoundedRectangle(cornerRadius: StrakkRadius.sm)
            )
        }
        .disabled(data.isProcessing || data.isAlreadyPro)
        .accessibilityLabel("Essayer Strakk Pro gratuitement pendant 7 jours")
    }

    // MARK: - Footer

    private var footerText: some View {
        VStack(spacing: StrakkSpacing.xxs) {
            Text("Annule à tout moment · Aucun engagement")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
            Text("Paiement sécurisé via App Store")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
        }
        .frame(maxWidth: .infinity)
        .multilineTextAlignment(.center)
    }

    private var restoreButton: some View {
        Button {
            viewModel.onEvent(PaywallEventOnRestoreTapped())
        } label: {
            Text("Restaurer un achat")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkPrimary)
        }
        .accessibilityLabel("Restaurer un achat précédent")
    }
}

#Preview {
    PaywallView(onDismiss: {})
}
