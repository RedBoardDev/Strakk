package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.DevSubscriptionOverride
import com.strakk.shared.domain.repository.SubscriptionRepository

class SetDevSubscriptionOverrideUseCase(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(override: DevSubscriptionOverride) {
        repository.setDevOverride(override)
    }
}
