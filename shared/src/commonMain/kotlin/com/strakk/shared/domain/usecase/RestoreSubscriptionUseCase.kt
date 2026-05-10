package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.BillingResult
import com.strakk.shared.domain.repository.BillingRepository

class RestoreSubscriptionUseCase(
    private val billingRepository: BillingRepository,
) {
    suspend operator fun invoke(): BillingResult = billingRepository.restore()
}
