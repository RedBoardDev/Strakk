package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.FeatureAccess
import com.strakk.shared.domain.model.ProFeature
import com.strakk.shared.domain.model.UserTier
import com.strakk.shared.domain.model.tier
import com.strakk.shared.domain.repository.SubscriptionRepository

class CheckFeatureAccessUseCase(
    private val subscriptionRepository: SubscriptionRepository,
) {
    suspend operator fun invoke(feature: ProFeature): FeatureAccess {
        val state = subscriptionRepository.getState()
        return if (state.tier == UserTier.PRO) {
            FeatureAccess.Granted
        } else {
            FeatureAccess.Gated(feature)
        }
    }
}
