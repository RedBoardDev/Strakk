package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SubscriptionDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val status: String,
    val plan: String? = null,
    @SerialName("trial_end") val trialEnd: String? = null,
    @SerialName("current_period_end") val currentPeriodEnd: String? = null,
    @SerialName("revenuecat_customer_id") val revenuecatCustomerId: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)
