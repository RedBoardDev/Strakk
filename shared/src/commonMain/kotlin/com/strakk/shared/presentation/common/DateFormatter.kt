package com.strakk.shared.presentation.common

import kotlinx.datetime.LocalDate

/**
 * Formats a [LocalDate] as a short label like "Sun, Apr 12".
 *
 * Uses English day/month abbreviations — adapt via expect/actual if the app
 * ever needs locale-aware formatting.
 */
fun formatDateLabel(date: LocalDate): String {
    val day = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "$day, $month ${date.dayOfMonth}"
}
