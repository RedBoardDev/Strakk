package com.strakk.shared.data.repository

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.ktx.awaitRestore
import com.revenuecat.purchases.kmp.models.Offering
import com.strakk.shared.data.remote.CurrentUserIdProvider
import com.strakk.shared.domain.model.BillingResult
import com.strakk.shared.domain.model.PaywallOfferPrices
import com.strakk.shared.domain.model.SubscriptionPlan
import com.strakk.shared.domain.repository.BillingRepository
import com.revenuecat.purchases.kmp.models.Package as RevenueCatPackage

internal class BillingRepositoryImpl(
    private val userIdProvider: CurrentUserIdProvider,
) : BillingRepository {
    private var apiKey: String? = null
    private var configured = false

    override fun configure(apiKey: String) {
        this.apiKey = apiKey.trim().ifEmpty { null }
    }

    override suspend fun purchase(plan: SubscriptionPlan): BillingResult {
        return runCatching {
            ensureConfigured()
            val purchases = Purchases.sharedInstance
            val offering = purchases.awaitOfferings().current
                ?: return BillingResult.Error("No active offering available")
            val targetPackage = offering.packageFor(plan)
                ?: return BillingResult.Error("Plan package not available")

            purchases.awaitPurchase(targetPackage)
            BillingResult.Success
        }.getOrElse { mapError(it) }
    }

    override suspend fun restore(): BillingResult {
        return runCatching {
            ensureConfigured()
            Purchases.sharedInstance.awaitRestore()
            BillingResult.Success
        }.getOrElse { mapError(it) }
    }

    override suspend fun syncCustomerInfo() {
        runCatching {
            ensureConfigured()
            Purchases.sharedInstance.awaitCustomerInfo()
        }
    }

    override suspend fun loadPaywallOfferPrices(): PaywallOfferPrices {
        return runCatching {
            ensureConfigured()
            val offering = Purchases.sharedInstance.awaitOfferings().current
                ?: return PaywallOfferPrices()
            val monthlyPkg = offering.packageFor(SubscriptionPlan.MONTHLY)
            val annualPkg = offering.packageFor(SubscriptionPlan.ANNUAL)
            PaywallOfferPrices(
                monthlyFormatted = monthlyPkg?.storeProduct?.price?.formatted,
                annualFormatted = annualPkg?.storeProduct?.price?.formatted,
                annualPricePerMonthFormatted = annualPkg?.storeProduct?.pricePerMonth?.formatted,
            )
        }.getOrElse { PaywallOfferPrices() }
    }

    private fun mapError(throwable: Throwable): BillingResult {
        val message = throwable.message ?: "Billing operation failed"
        return if (message.lowercase().contains("cancel")) {
            BillingResult.Cancelled
        } else {
            BillingResult.Error(message)
        }
    }

    private fun ensureConfigured() {
        if (configured) return
        val key = apiKey ?: error("RevenueCat API key missing")
        if (Purchases.isConfigured) {
            configured = true
            return
        }
        val configBuilder = PurchasesConfiguration.Builder(key)
        val userId = runCatching { userIdProvider.currentOrThrow() }.getOrNull()
        if (!userId.isNullOrBlank()) {
            configBuilder.appUserId = userId
        }
        val config = configBuilder.build()
        Purchases.configure(config)
        configured = true
    }
}

private fun Offering.packageFor(plan: SubscriptionPlan): RevenueCatPackage? {
    val shortcut = when (plan) {
        SubscriptionPlan.MONTHLY -> monthly
        SubscriptionPlan.ANNUAL -> annual
    }
    if (shortcut != null) return shortcut
    return availablePackages.firstOrNull { pkg ->
        val id = pkg.identifier.lowercase()
        when (plan) {
            SubscriptionPlan.MONTHLY -> id.contains("monthly")
            SubscriptionPlan.ANNUAL -> id.contains("annual") || id.contains("yearly")
        }
    }
}
