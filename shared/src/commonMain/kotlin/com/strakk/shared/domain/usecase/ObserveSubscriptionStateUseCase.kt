package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.SubscriptionState
import com.strakk.shared.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow

class ObserveSubscriptionStateUseCase(
    private val subscriptionRepository: SubscriptionRepository,
) {
    operator fun invoke(): Flow<SubscriptionState> = subscriptionRepository.observeState()
}
