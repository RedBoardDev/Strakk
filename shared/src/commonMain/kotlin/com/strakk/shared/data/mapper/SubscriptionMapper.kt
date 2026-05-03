package com.strakk.shared.data.mapper

import com.strakk.shared.data.dto.SubscriptionDto
import com.strakk.shared.domain.model.SubscriptionPlan
import com.strakk.shared.domain.model.SubscriptionState
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal fun SubscriptionDto.toDomain(): SubscriptionState = when (status) {
    "trial" -> {
        val endsAt = trialEnd?.let { Instant.parse(it) } ?: Instant.DISTANT_PAST
        if (endsAt <= Clock.System.now()) {
            SubscriptionState.Expired
        } else {
            SubscriptionState.Trial(endsAt = endsAt)
        }
    }
    "active" -> {
        val expiresAt = currentPeriodEnd?.let { Instant.parse(it) } ?: Instant.DISTANT_PAST
        val domainPlan = when (plan) {
            "monthly" -> SubscriptionPlan.MONTHLY
            else -> SubscriptionPlan.ANNUAL
        }
        SubscriptionState.Active(plan = domainPlan, expiresAt = expiresAt)
    }
    "expired" -> SubscriptionState.Expired
    "payment_failed" -> SubscriptionState.PaymentFailed
    else -> SubscriptionState.Free
}
