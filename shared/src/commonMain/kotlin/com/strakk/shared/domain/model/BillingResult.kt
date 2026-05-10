package com.strakk.shared.domain.model

sealed interface BillingResult {
    data object Success : BillingResult
    data object Cancelled : BillingResult
    data class Error(val message: String) : BillingResult
}
