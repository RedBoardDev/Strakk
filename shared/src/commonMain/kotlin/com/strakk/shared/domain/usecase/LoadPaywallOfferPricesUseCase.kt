package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.PaywallOfferPrices
import com.strakk.shared.domain.repository.BillingRepository

class LoadPaywallOfferPricesUseCase(
    private val billingRepository: BillingRepository,
) {
    suspend operator fun invoke(): PaywallOfferPrices = billingRepository.loadPaywallOfferPrices()
}
