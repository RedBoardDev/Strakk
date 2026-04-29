package com.strakk.shared.domain.model

data class CheckInQuickStats(
    val lastWeight: Double?,
    val weightDelta: Double?,
    val lastAvgArm: Double?,
    val armDelta: Double?,
    val lastWaist: Double?,
    val waistDelta: Double?,
)
