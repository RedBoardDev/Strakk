package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.Feature
import com.strakk.shared.domain.model.FeatureAccess
import com.strakk.shared.domain.model.FeatureRegistry
import com.strakk.shared.domain.model.UserTier
import com.strakk.shared.domain.model.tier
import com.strakk.shared.domain.repository.FeatureLimitsRepository
import com.strakk.shared.domain.repository.FeatureUsageRepository
import com.strakk.shared.domain.repository.SubscriptionRepository

class CheckFeatureAccessUseCase(
    private val subscriptionRepository: SubscriptionRepository,
    private val featureLimitsRepository: FeatureLimitsRepository,
    private val featureUsageRepository: FeatureUsageRepository,
) {
    @Suppress("ReturnCount")
    suspend operator fun invoke(feature: Feature): FeatureAccess {
        val state = subscriptionRepository.getState()
        val isPro = state.tier == UserTier.PRO
        val metadata = FeatureRegistry.get(feature)
        val limits = featureLimitsRepository.getLimits(feature.key) ?: return FeatureAccess.Granted()

        if (limits.proOnly && !isPro) return FeatureAccess.ProRequired(feature, metadata)

        val quota = if (isPro) limits.quotaPro else limits.quotaFree
        if (quota == 0) return FeatureAccess.ProRequired(feature, metadata)
        if (quota == -1) return FeatureAccess.Granted()

        val since = periodStartInstant(limits.quotaPeriod)
        val used = featureUsageRepository.countUsage(feature.key, since)

        return if (used >= quota) {
            FeatureAccess.QuotaExhausted(feature, metadata, used, quota, limits.quotaPeriod)
        } else {
            FeatureAccess.Granted(remaining = quota - used)
        }
    }
}
