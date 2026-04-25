package com.strakk.shared.domain.common

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Abstraction over clock access, enabling easy testing via fake implementations.
 */
interface ClockProvider {
    fun today(): LocalDate
    fun now(): Instant
}

/**
 * Production implementation using the system clock.
 */
class SystemClockProvider : ClockProvider {
    override fun today(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    override fun now(): Instant = Clock.System.now()
}
