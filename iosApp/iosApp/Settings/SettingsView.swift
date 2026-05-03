// swiftlint:disable file_length type_body_length
import SwiftUI
import shared

// MARK: - Focus field enum

private enum SettingsField: Hashable {
    case protein, calories, water
}

// MARK: - SettingsView

struct SettingsView: View {
    @State private var viewModel = SettingsViewModelWrapper()

    // Local goal text state
    @State private var proteinText: String = ""
    @State private var caloriesText: String = ""
    @State private var waterText: String = ""

    // Last values received from server — used to ignore saves triggered by initial load
    @State private var serverProtein: String = ""
    @State private var serverCalories: String = ""
    @State private var serverWater: String = ""
    @State private var serverHevyKey: String = ""

    // Hevy integration state
    @State private var hevyApiKeyText: String = ""

    @FocusState private var focusedField: SettingsField?

    @State private var didInitialize: Bool = false
    @State private var showSignOutAlert: Bool = false

    var body: some View {
        withAlerts
    }

    // MARK: - Alert layer

    private var withAlerts: some View {
        baseView
            .alert("Error", isPresented: errorAlertBinding) {
                Button("OK", role: .cancel) { viewModel.errorMessage = nil }
            } message: {
                Text(viewModel.errorMessage ?? "")
            }
            .alert("Sign out", isPresented: $showSignOutAlert) {
                Button("Cancel", role: .cancel) {}
                Button("Sign out", role: .destructive) {
                    viewModel.onEvent(SettingsEventOnSignOut())
                }
            } message: {
                Text("Are you sure you want to sign out?")
            }
            .alert("Strakk Pro", isPresented: Binding(
                get: { viewModel.toastMessage != nil },
                set: { if !$0 { viewModel.toastMessage = nil } }
            )) {
                Button("OK") { viewModel.toastMessage = nil }
            } message: {
                Text(viewModel.toastMessage ?? "")
            }
            .fullScreenCover(isPresented: $viewModel.showPaywall) {
                PaywallView(onDismiss: { viewModel.showPaywall = false })
            }
    }

    // MARK: - Base view

    private var baseView: some View {
        ZStack {
            Color.strakkBackground.ignoresSafeArea()

            switch viewModel.state {
            case .loading:
                loadingView
            case .ready(let data):
                readyView(data: data)
            }
        }
        .onAppear { handleStateChange(viewModel.state) }
        .onChange(of: viewModel.state) { _, newState in handleStateChange(newState) }
        .toolbar { keyboardToolbar }
    }

    // MARK: - Loading

    private var loadingView: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Settings")
                .font(.strakkHeading1)
                .foregroundStyle(Color.strakkTextPrimary)
                .padding(.horizontal, 20)
                .padding(.top, 16)

            Spacer()
            ProgressView()
                .tint(Color.strakkPrimary)
                .frame(maxWidth: .infinity)
            Spacer()
        }
    }

    // MARK: - Ready

    private func readyView(data: SettingsData) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                // Header
                Text("Settings")
                    .font(.strakkHeading1)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .padding(.horizontal, 20)
                    .padding(.top, 16)

                Spacer().frame(height: 24)

                accountSection(email: data.email)

                Spacer().frame(height: 24)

                proSection(display: data.subscriptionDisplay)

                Spacer().frame(height: 24)

                goalsSection

                Spacer().frame(height: 24)

                hevySection

                Spacer().frame(height: 24)

                dataSourcesSection

                Spacer().frame(height: 32)

                signOutButton

                Spacer().frame(height: 32 + 49)
            }
        }
        .scrollDismissesKeyboard(.interactively)
        .simultaneousGesture(TapGesture().onEnded { _ in focusedField = nil })
        .onChange(of: proteinText) { _, newValue in
            guard didInitialize, newValue != serverProtein else { return }
            serverProtein = newValue
            viewModel.onEvent(SettingsEventOnProteinGoalChanged(value: newValue))
        }
        .onChange(of: caloriesText) { _, newValue in
            guard didInitialize, newValue != serverCalories else { return }
            serverCalories = newValue
            viewModel.onEvent(SettingsEventOnCalorieGoalChanged(value: newValue))
        }
        .onChange(of: waterText) { _, newValue in
            guard didInitialize, newValue != serverWater else { return }
            serverWater = newValue
            viewModel.onEvent(SettingsEventOnWaterGoalChanged(value: newValue))
        }
        .onChange(of: hevyApiKeyText) { _, newValue in
            // Do NOT update serverHevyKey here — the KMP ViewModel handles debounce
            // internally via scheduleHevyKeySave (500 ms). serverHevyKey only changes
            // on the initial server load so re-loading doesn't re-trigger a save.
            guard didInitialize, newValue != serverHevyKey else { return }
            viewModel.onEvent(SettingsEventOnHevyApiKeyChanged(value: newValue))
        }
    }

    // MARK: - Account section

    private func accountSection(email: String?) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("ACCOUNT")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)
                .kerning(1.0)
                .padding(.horizontal, 20)

            VStack(spacing: 0) {
                HStack {
                    Image(systemName: "envelope")
                        .font(.system(size: 13))
                        .foregroundStyle(Color.strakkTextTertiary)
                    Spacer().frame(width: 4)
                    Text("Email")
                        .font(.strakkCaptionBold)
                        .foregroundStyle(Color.strakkTextSecondary)
                    Spacer()
                    Text(email ?? "—")
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextPrimary)
                        .lineLimit(1)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
            }
            .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: 12))
            .padding(.horizontal, 20)
        }
    }

    // MARK: - Goals section

    private var goalsSection: some View {
        let anyFocused = focusedField != nil
        return VStack(alignment: .leading, spacing: 8) {
            Text("DAILY GOALS")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)
                .kerning(1.0)
                .padding(.horizontal, 20)

            VStack(spacing: 0) {
                goalRow(
                    color: .strakkProtein,
                    label: "Protein",
                    placeholder: "150",
                    unit: "g",
                    text: $proteinText,
                    field: .protein
                )

                Divider()
                    .background(Color.strakkDivider)
                    .padding(.leading, 16)

                goalRow(
                    color: .strakkCalories,
                    label: "Calories",
                    placeholder: "2200",
                    unit: "kcal",
                    text: $caloriesText,
                    field: .calories
                )

                Divider()
                    .background(Color.strakkDivider)
                    .padding(.leading, 16)

                goalRow(
                    color: .strakkWater,
                    label: "Water",
                    placeholder: "2000",
                    unit: "mL",
                    text: $waterText,
                    field: .water
                )
            }
            .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .strokeBorder(
                        anyFocused ? Color.strakkPrimary.opacity(0.5) : Color.clear,
                        lineWidth: 1
                    )
                    .animation(.easeInOut(duration: 0.15), value: anyFocused)
            )
            .padding(.horizontal, 20)
        }
    }

    @ViewBuilder
    private func goalRow(
        color: Color,
        label: String,
        placeholder: String,
        unit: String,
        text: Binding<String>,
        field: SettingsField
    ) -> some View {
        HStack {
            Circle()
                .fill(color)
                .frame(width: 8, height: 8)
            Spacer().frame(width: 8)
            Text(label)
                .font(.strakkBodyBold)
                .foregroundStyle(Color.strakkTextPrimary)
            Spacer()
            TextField(placeholder, text: text)
                .font(.strakkHeading3)
                .monospacedDigit()
                .foregroundStyle(Color.strakkTextPrimary)
                .tint(Color.strakkPrimary)
                .keyboardType(.numberPad)
                .multilineTextAlignment(.trailing)
                .focused($focusedField, equals: field)
                .frame(maxWidth: 80)
            Text(unit)
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
    }

    // MARK: - Strakk Pro section

    @ViewBuilder
    private func proSection(display: SubscriptionDisplayData) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("STRAKK PRO")
                    .font(.strakkOverline)
                    .foregroundStyle(Color.strakkTextTertiary)
                    .kerning(1.0)

                Spacer()

                if display != .free {
                    Text("PRO")
                        .font(.strakkCaptionBold)
                        .foregroundStyle(Color.strakkPrimary)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.strakkPrimary.opacity(0.15), in: RoundedRectangle(cornerRadius: 6))
                }
            }
            .padding(.horizontal, 20)

            switch display {
            case .free:
                proFreeCard
            case .trial(let daysRemaining):
                proTrialCard(daysRemaining: daysRemaining)
            case .active(let planLabel, let expiresLabel):
                proActiveCard(planLabel: planLabel, expiresLabel: expiresLabel)
            case .paymentFailed:
                proPaymentFailedCard
            }
        }
    }

    private var proFreeCard: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
            Text("L'IA tracke tes repas pour toi.")
                .font(.strakkBodyBold)
                .foregroundStyle(Color.strakkTextPrimary)
            Text("Photo, texte, bilan hebdo.")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextSecondary)

            Button {
                HapticEngine.light()
                viewModel.onEvent(SettingsEventOnUpgradeTapped())
            } label: {
                Text("Passer à Pro")
                    .font(.strakkBodyBold)
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color.strakkPrimary, in: RoundedRectangle(cornerRadius: 12))
            }

            Button {
                viewModel.onEvent(SettingsEventOnRestorePurchase())
            } label: {
                Text("Restaurer un achat")
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkPrimary)
            }
            .frame(maxWidth: .infinity)
        }
        .padding(16)
        .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal, 20)
    }

    private func proTrialCard(daysRemaining: Int) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("Essai gratuit")
                .font(.strakkBodyBold)
                .foregroundStyle(Color.strakkTextPrimary)
            Text("Expire dans \(daysRemaining) jour\(daysRemaining > 1 ? "s" : "")")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkWarning)

            Button {
                viewModel.onEvent(SettingsEventOnManageSubscription())
            } label: {
                Text("Gérer l'abonnement")
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color.strakkSurface2, in: RoundedRectangle(cornerRadius: 12))
            }
        }
        .padding(16)
        .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal, 20)
    }

    private func proActiveCard(planLabel: String, expiresLabel: String) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            HStack(spacing: StrakkSpacing.xs) {
                Text(planLabel)
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkTextPrimary)
                Circle()
                    .fill(Color.strakkSuccess)
                    .frame(width: 8, height: 8)
                Text("Actif")
                    .font(.strakkCaptionBold)
                    .foregroundStyle(Color.strakkSuccess)
            }

            Text("Se renouvelle le \(expiresLabel)")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextSecondary)

            Button {
                viewModel.onEvent(SettingsEventOnManageSubscription())
            } label: {
                Text("Gérer l'abonnement")
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color.strakkSurface2, in: RoundedRectangle(cornerRadius: 12))
            }

            Button {
                viewModel.onEvent(SettingsEventOnRestorePurchase())
            } label: {
                Text("Restaurer un achat")
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkPrimary)
            }
            .frame(maxWidth: .infinity)
        }
        .padding(16)
        .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal, 20)
    }

    private var proPaymentFailedCard: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("Problème de paiement")
                .font(.strakkBodyBold)
                .foregroundStyle(Color.strakkError)
            Text("Mets à jour ton moyen de paiement pour conserver l'accès Pro.")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextSecondary)

            Button {
                viewModel.onEvent(SettingsEventOnManageSubscription())
            } label: {
                Text("Régler le problème")
                    .font(.strakkBodyBold)
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color.strakkError, in: RoundedRectangle(cornerRadius: 12))
            }
        }
        .padding(16)
        .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: 12))
        .overlay(alignment: .leading) {
            Rectangle()
                .fill(Color.strakkError)
                .frame(width: 3)
                .clipShape(RoundedRectangle(cornerRadius: 2))
        }
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal, 20)
    }

    // MARK: - Hevy section

    private var hevySection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("HEVY INTEGRATION")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)
                .kerning(1.0)
                .padding(.horizontal, 20)

            VStack(spacing: 0) {
                HStack {
                    Image(systemName: "dumbbell.fill")
                        .font(.system(size: 13))
                        .foregroundStyle(Color.strakkTextTertiary)
                    Spacer().frame(width: 8)
                    Text("API Key")
                        .font(.strakkBodyBold)
                        .foregroundStyle(Color.strakkTextPrimary)
                    Spacer()
                    SecureField("Paste Hevy API key", text: $hevyApiKeyText)
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextPrimary)
                        .multilineTextAlignment(.trailing)
                        .frame(maxWidth: 200)
                        .tint(Color.strakkPrimary)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
            }
            .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: 12))
            .padding(.horizontal, 20)
        }
    }

    // MARK: - Data Sources (ODbL attribution)

    private var dataSourcesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("DATA SOURCES")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)
                .padding(.horizontal, 20)

            VStack(alignment: .leading, spacing: 8) {
                Text("Food data provided by:")
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextSecondary)
                Text("• Open Food Facts (ODbL)")
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextSecondary)
                Text("• CIQUAL 2020 — ANSES")
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextSecondary)
            }
            .padding(.horizontal, 20)
        }
    }

    // MARK: - Sign Out button

    private var signOutButton: some View {
        Button {
            HapticEngine.light()
            showSignOutAlert = true
        } label: {
            Text("Sign out")
                .font(.strakkBodyBold)
                .foregroundStyle(Color.strakkError)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: 12))
        }
        .padding(.horizontal, 20)
        .accessibilityLabel("Sign out of Strakk")
    }

    // MARK: - Keyboard toolbar

    @ToolbarContentBuilder
    private var keyboardToolbar: some ToolbarContent {
        ToolbarItemGroup(placement: .keyboard) {
            Button {
                navigateFocus(direction: .previous)
            } label: {
                Image(systemName: "chevron.up")
            }
            .disabled(focusedField == .protein)

            Button {
                navigateFocus(direction: .next)
            } label: {
                Image(systemName: "chevron.down")
            }
            .disabled(focusedField == .water)

            Spacer()
        }
    }

    // MARK: - Helpers

    private var errorAlertBinding: Binding<Bool> {
        Binding(
            get: { viewModel.errorMessage != nil },
            set: { if !$0 { viewModel.errorMessage = nil } }
        )
    }

    private func handleStateChange(_ newState: SettingsState) {
        guard !didInitialize else { return }
        guard case .ready(let data) = newState else { return }
        serverProtein = data.proteinGoal
        serverCalories = data.calorieGoal
        serverWater = data.waterGoal
        serverHevyKey = data.hevyApiKey
        proteinText = data.proteinGoal
        caloriesText = data.calorieGoal
        waterText = data.waterGoal
        hevyApiKeyText = data.hevyApiKey
        didInitialize = true
    }

    private func navigateFocus(direction: FocusDirection) {
        let order: [SettingsField] = [.protein, .calories, .water]
        guard let current = focusedField,
              let idx = order.firstIndex(of: current) else { return }
        if direction == .previous, idx > 0 { focusedField = order[idx - 1] }
        if direction == .next, idx < order.count - 1 { focusedField = order[idx + 1] }
    }
}

private enum FocusDirection { case previous, next }
