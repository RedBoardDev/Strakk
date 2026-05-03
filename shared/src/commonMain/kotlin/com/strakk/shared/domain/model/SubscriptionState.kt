package com.strakk.shared.domain.model

import kotlinx.datetime.Instant

sealed interface SubscriptionState {
    data object Free : SubscriptionState
    data class Trial(val endsAt: Instant) : SubscriptionState
    data class Active(val plan: SubscriptionPlan, val expiresAt: Instant) : SubscriptionState
    data object Expired : SubscriptionState
    data object PaymentFailed : SubscriptionState
}

val SubscriptionState.tier: UserTier
    get() = when (this) {
        is SubscriptionState.Free, is SubscriptionState.Expired -> UserTier.FREE
        is SubscriptionState.Trial, is SubscriptionState.Active, is SubscriptionState.PaymentFailed -> UserTier.PRO
    }
