package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.Feature
import com.strakk.shared.domain.model.QuotaPeriod
import com.strakk.shared.domain.model.QuotaStatus
import com.strakk.shared.domain.model.UserTier
import com.strakk.shared.domain.model.tier
import com.strakk.shared.domain.repository.FeatureLimitsRepository
import com.strakk.shared.domain.repository.FeatureUsageRepository
import com.strakk.shared.domain.repository.SubscriptionRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class GetFeatureQuotaStatusUseCase(
    private val featureLimitsRepository: FeatureLimitsRepository,
    private val featureUsageRepository: FeatureUsageRepository,
    private val subscriptionRepository: SubscriptionRepository,
) {
    suspend operator fun invoke(feature: Feature): QuotaStatus {
        val limits = featureLimitsRepository.getLimits(feature.key)
            ?: return QuotaStatus.Unlimited

        val state = subscriptionRepository.getState()
        val isPro = state.tier == UserTier.PRO
        val quota = if (isPro) limits.quotaPro else limits.quotaFree

        if (quota == -1) return QuotaStatus.Unlimited
        if (quota == 0) return QuotaStatus.Blocked

        val since = periodStartInstant(limits.quotaPeriod)
        val used = featureUsageRepository.countUsage(feature.key, since)

        return QuotaStatus.Limited(used = used, limit = quota, period = limits.quotaPeriod)
    }
}

internal fun periodStartInstant(period: QuotaPeriod): kotlinx.datetime.Instant {
    val tz = TimeZone.UTC
    val now = Clock.System.now()
    val localNow = now.toLocalDateTime(tz)

    val startDate = when (period) {
        QuotaPeriod.DAY -> localNow.date
        QuotaPeriod.WEEK -> {
            val dayOfWeek = localNow.date.dayOfWeek.ordinal // Monday=0
            localNow.date.minus(dayOfWeek, DateTimeUnit.DAY)
        }
        QuotaPeriod.MONTH -> kotlinx.datetime.LocalDate(localNow.year, localNow.monthNumber, 1)
    }

    return startDate.atStartOfDayIn(tz)
}
