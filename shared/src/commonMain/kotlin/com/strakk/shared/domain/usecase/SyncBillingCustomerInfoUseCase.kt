package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.repository.BillingRepository

class SyncBillingCustomerInfoUseCase(
    private val billingRepository: BillingRepository,
) {
    suspend operator fun invoke() = billingRepository.syncCustomerInfo()
}
