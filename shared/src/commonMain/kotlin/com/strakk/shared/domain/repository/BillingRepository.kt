package com.strakk.shared.domain.repository

import com.strakk.shared.domain.model.BillingResult
import com.strakk.shared.domain.model.PaywallOfferPrices
import com.strakk.shared.domain.model.SubscriptionPlan

interface BillingRepository {
    fun configure(apiKey: String)
    suspend fun purchase(plan: SubscriptionPlan): BillingResult
    suspend fun restore(): BillingResult
    suspend fun syncCustomerInfo()
    suspend fun loadPaywallOfferPrices(): PaywallOfferPrices
}
