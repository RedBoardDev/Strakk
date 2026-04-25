package com.strakk.shared.domain.common

/**
 * Platform-agnostic logger interface for the shared module.
 *
 * Implementations are provided via expect/actual for each platform.
 */
interface Logger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

/**
 * Platform-specific [Logger] implementation.
 * Provided via expect/actual in each source set.
 */
expect fun createLogger(): Logger
