package com.strakk.shared.domain.model

sealed interface QuotaStatus {
    data object Unlimited : QuotaStatus
    data object Blocked : QuotaStatus
    data class Limited(
        val used: Int,
        val limit: Int,
        val period: QuotaPeriod,
    ) : QuotaStatus {
        val remaining: Int get() = (limit - used).coerceAtLeast(0)
    }
}
