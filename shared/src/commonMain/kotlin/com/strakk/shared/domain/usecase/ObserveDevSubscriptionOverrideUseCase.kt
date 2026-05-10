package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.DevSubscriptionOverride
import com.strakk.shared.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow

class ObserveDevSubscriptionOverrideUseCase(
    private val repository: SubscriptionRepository,
) {
    operator fun invoke(): Flow<DevSubscriptionOverride> = repository.observeDevOverride()
}
