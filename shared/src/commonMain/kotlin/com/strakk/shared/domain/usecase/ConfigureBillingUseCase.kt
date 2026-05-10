package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.repository.BillingRepository

class ConfigureBillingUseCase(
    private val billingRepository: BillingRepository,
) {
    operator fun invoke(apiKey: String) = billingRepository.configure(apiKey)
}
