package com.strakk.shared.presentation.paywall

import com.strakk.shared.domain.model.Feature
import com.strakk.shared.domain.model.FeatureMetadata
import com.strakk.shared.domain.model.FeatureRegistry
import com.strakk.shared.domain.model.PaywallOfferPrices
import com.strakk.shared.domain.model.SubscriptionPlan

data class PaywallUiState(
    val features: List<FeatureMetadata> = FeatureRegistry.all(),
    val highlightedFeature: Feature? = null,
    val selectedPlan: SubscriptionPlan = SubscriptionPlan.ANNUAL,
    val isProcessing: Boolean = false,
    val currentPlan: SubscriptionPlan? = null,
    val isTrial: Boolean = false,
    val selectedPlanIsCurrent: Boolean = false,
    val errorMessage: String? = null,
    val offerPrices: PaywallOfferPrices = PaywallOfferPrices(),
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
