package com.strakk.shared.domain.model

/**
 * Store-localized price strings from RevenueCat ([BillingRepository.loadPaywallOfferPrices]).
 * Built from [com.revenuecat.purchases.kmp.models.StoreProduct] (`price`, `pricePerMonth`) on each
 * offering package — same localized formatting as checkout ([docs](https://revenuecat.github.io/purchases-kmp/models/com.revenuecat.purchases.kmp.models/-store-product/index.html)).
 * Values are null until offerings load or if the package is missing.
 */
data class PaywallOfferPrices(
    val monthlyFormatted: String? = null,
    val annualFormatted: String? = null,
    val annualPricePerMonthFormatted: String? = null,
)
