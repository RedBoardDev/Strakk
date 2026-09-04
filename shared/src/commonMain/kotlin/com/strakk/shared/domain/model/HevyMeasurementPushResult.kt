package com.strakk.shared.domain.model

/**
 * Result of pushing a [CheckIn]'s body measurements to Hevy via the
 * `push-checkin-to-hevy` Edge Function.
 */
sealed interface HevyMeasurementPushResult {

    /**
     * The measurements were pushed successfully.
     *
     * @property date ISO date the measurements were recorded against in Hevy.
     */
    data class Pushed(val date: String) : HevyMeasurementPushResult

    /**
     * A Hevy body-measurement entry already existed for that date — nothing was pushed.
     * The client should ask the user to confirm, then retry with `overwrite = true`.
     *
     * @property date ISO date on which the conflicting entry exists.
     */
    data class Conflict(val date: String) : HevyMeasurementPushResult
}
