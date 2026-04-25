package com.strakk.shared.domain.common

import kotlin.math.roundToInt

/**
 * Rounds a [Double] to one decimal place.
 *
 * Example: `3.456.roundTo1dp()` → `3.5`
 */
internal fun Double.roundTo1dp(): Double = (this * 10).roundToInt() / 10.0

/**
 * Rounds a [Double] to the nearest integer as a [Double].
 *
 * Example: `165.4.roundToIntAsDouble()` → `165.0`
 */
internal fun Double.roundToIntAsDouble(): Double = this.roundToInt().toDouble()
