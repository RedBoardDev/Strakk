package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.BillingResult
import com.strakk.shared.domain.model.SubscriptionPlan
import com.strakk.shared.domain.repository.BillingRepository

class PurchaseSubscriptionUseCase(
    private val billingRepository: BillingRepository,
) {
    suspend operator fun invoke(plan: SubscriptionPlan): BillingResult = billingRepository.purchase(plan)
}
