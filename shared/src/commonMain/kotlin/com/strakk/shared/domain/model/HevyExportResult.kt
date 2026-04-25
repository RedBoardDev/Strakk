package com.strakk.shared.domain.model

/**
 * Result of a successful export of a [WorkoutSession] to a Hevy routine.
 *
 * @property routineId Hevy-assigned ID of the created or updated routine.
 * @property routineTitle Title of the routine as stored in Hevy.
 * @property exercisesMatched Number of exercises matched to existing Hevy exercise templates.
 * @property exercisesCreated Number of new exercise templates created in Hevy.
 */
data class HevyExportResult(
    val routineId: String,
    val routineTitle: String,
    val exercisesMatched: Int,
    val exercisesCreated: Int,
)
