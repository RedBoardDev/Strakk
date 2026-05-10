package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.repository.SubscriptionRepository

class RefreshSubscriptionStateUseCase(
    private val subscriptionRepository: SubscriptionRepository,
) {
    suspend operator fun invoke() = subscriptionRepository.refreshState()
}
