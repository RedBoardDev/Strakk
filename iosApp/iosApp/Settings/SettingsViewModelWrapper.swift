import SwiftUI
import shared

// MARK: - Subscription display state

enum SubscriptionDisplayData: Equatable {
    case free
    case trial(daysRemaining: Int, endsAtLabel: String)
    case active(
        plan: SwiftSubscriptionPlan,
        renewalLabel: String,
        canUpgradeToAnnual: Bool
    )
    case paymentFailed
}

enum DevSubscriptionOverrideData: String, CaseIterable, Identifiable {
    case server
    case free
    case trialSevenDays
    case trialOneDay
    case expired
    case proMonthly
    case proAnnual
    case paymentFailed

    var id: String { rawValue }

    var label: String {
        switch self {
        case .server: return "Backend"
        case .free: return "Free"
        case .trialSevenDays: return "Trial - 7 days"
        case .trialOneDay: return "Trial - 1 day"
        case .expired: return "Expired"
        case .proMonthly: return "Pro monthly"
        case .proAnnual: return "Pro annual"
        case .paymentFailed: return "Payment failed"
        }
    }

    var kmpValue: DevSubscriptionOverride {
        switch self {
        case .server: return .server
        case .free: return .free
        case .trialSevenDays: return .trial7Days
        case .trialOneDay: return .trial1Day
        case .expired: return .expired
        case .proMonthly: return .proMonthly
        case .proAnnual: return .proAnnual
        case .paymentFailed: return .paymentFailed
        }
    }
}

// MARK: - Swift-side data types

struct SettingsData: Equatable {
    let email: String?
    let proteinGoal: String
    let calorieGoal: String
    let fatGoal: String
    let carbGoal: String
    let waterGoal: String
    let hevyApiKey: String
    let subscriptionDisplay: SubscriptionDisplayData
    let devSubscriptionOverride: DevSubscriptionOverrideData
}

enum SettingsState: Equatable {
    case loading
    case ready(data: SettingsData)
}

// MARK: - Wrapper

@MainActor
@Observable
final class SettingsViewModelWrapper {
    private let sharedVm: SettingsViewModel

    var state: SettingsState = .loading
    var errorMessage: String?
    var toastMessage: String?
    var showPaywall: Bool = false
    var openManageSubscription: Bool = false

    @ObservationIgnored private var stateTask: Task<Void, Never>?
    @ObservationIgnored private var effectTask: Task<Void, Never>?

    init() {
        self.sharedVm = KoinBridge.shared.getSettingsViewModel()
        self.state = Self.mapState(sharedVm.uiState.value as? SettingsUiState)

        stateTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<SettingsUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                self?.state = Self.mapState(newState)
            }
        }

        effectTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<SettingsEffect> = observeFlow(sharedVm.effects)
            for await effect in stream {
                self?.handleEffect(effect)
            }
        }
    }

    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
    }

    func onEvent(_ event: SettingsEvent) {
        sharedVm.onEvent(event: event)
    }

    func setDevSubscriptionOverride(_ override: DevSubscriptionOverrideData) {
        sharedVm.onEvent(event: SettingsEventOnDevSubscriptionOverrideSelected(override: override.kmpValue))
    }

    private func handleEffect(_ effect: SettingsEffect) {
        if let showError = effect as? SettingsEffectShowError {
            errorMessage = showError.message
        } else if effect is SettingsEffectNavigateToPaywall {
            showPaywall = true
        } else if effect is SettingsEffectOpenManageSubscription {
            openManageSubscription = true
        } else if let toast = effect as? SettingsEffectShowToast {
            toastMessage = toast.message
        }
    }

    // MARK: - Mapping

    private static func mapState(_ kmpState: SettingsUiState?) -> SettingsState {
        guard let kmpState else { return .loading }

        if kmpState is SettingsUiStateLoading {
            return .loading
        } else if let ready = kmpState as? SettingsUiStateReady {
            let subDisplay: SubscriptionDisplayData = {
                let sub = ready.subscriptionDisplay
                if sub is SubscriptionDisplayFree {
                    return .free
                } else if let trial = sub as? SubscriptionDisplayTrial {
                    return .trial(
                        daysRemaining: Int(trial.daysRemaining),
                        endsAtLabel: trial.endsAtLabel
                    )
                } else if let active = sub as? SubscriptionDisplayActive {
                    let plan: SwiftSubscriptionPlan = active.plan == .annual ? .annual : .monthly
                    return .active(
                        plan: plan,
                        renewalLabel: active.renewalLabel,
                        canUpgradeToAnnual: active.canUpgradeToAnnual
                    )
                } else if sub is SubscriptionDisplayPaymentFailed {
                    return .paymentFailed
                }
                return .free
            }()

            return .ready(data: SettingsData(
                email: ready.email,
                proteinGoal: ready.proteinGoal,
                calorieGoal: ready.calorieGoal,
                fatGoal: ready.fatGoal,
                carbGoal: ready.carbGoal,
                waterGoal: ready.waterGoal,
                hevyApiKey: ready.hevyApiKey,
                subscriptionDisplay: subDisplay,
                devSubscriptionOverride: mapDevOverride(ready.devSubscriptionOverride)
            ))
        }
        return .loading
    }

    private static func mapDevOverride(_ override: DevSubscriptionOverride) -> DevSubscriptionOverrideData {
        switch override {
        case .server: return .server
        case .free: return .free
        case .trial7Days: return .trialSevenDays
        case .trial1Day: return .trialOneDay
        case .expired: return .expired
        case .proMonthly: return .proMonthly
        case .proAnnual: return .proAnnual
        case .paymentFailed: return .paymentFailed
        default: return .server
        }
    }
}
