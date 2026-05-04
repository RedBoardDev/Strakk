package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.CheckInsPage
import com.strakk.shared.domain.model.UserTier
import com.strakk.shared.domain.model.tier
import com.strakk.shared.domain.repository.CheckInRepository
import com.strakk.shared.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class ObserveCheckInsUseCase(
    private val checkInRepository: CheckInRepository,
    private val subscriptionRepository: SubscriptionRepository,
) {
    operator fun invoke(): Flow<CheckInsPage> = combine(
        checkInRepository.observeCheckIns(),
        subscriptionRepository.observeState(),
    ) { items, state ->
        if (state.tier == UserTier.PRO) {
            CheckInsPage(items, hiddenCount = 0)
        } else {
            val cutoff = cutoffDateString()
            val visible = items.filter { it.createdAt.take(DATE_PREFIX_LENGTH) >= cutoff }
            CheckInsPage(visible, hiddenCount = items.size - visible.size)
        }
    }

    companion object {
        private const val HISTORY_DAYS_FREE = 90
        private const val DATE_PREFIX_LENGTH = 10

        internal fun cutoffDateString(): String = Clock.System.now()
            .toLocalDateTime(TimeZone.UTC).date
            .minus(HISTORY_DAYS_FREE, DateTimeUnit.DAY)
            .toString()
    }
}
