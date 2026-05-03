package com.strakk.shared.presentation.paywall

import com.strakk.shared.domain.model.ProFeature
import com.strakk.shared.domain.model.ProFeatureInfo
import com.strakk.shared.domain.model.SubscriptionPlan
import com.strakk.shared.domain.model.allProFeatures

data class PaywallUiState(
    val features: List<ProFeatureInfo> = allProFeatures(),
    val highlightedFeature: ProFeature? = null,
    val selectedPlan: SubscriptionPlan = SubscriptionPlan.ANNUAL,
    val isProcessing: Boolean = false,
    val isAlreadyPro: Boolean = false,
)

sealed interface PaywallEvent {
    data class OnPlanSelected(val plan: SubscriptionPlan) : PaywallEvent
    data object OnSubscribeTapped : PaywallEvent
    data object OnRestoreTapped : PaywallEvent
    data object OnDismiss : PaywallEvent
}

sealed interface PaywallEffect {
    data object Dismiss : PaywallEffect
    data class ShowToast(val message: String) : PaywallEffect
}
