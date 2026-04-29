package com.strakk.shared.domain.model

/**
 * Result of a successful export of a [WorkoutSession] to a Hevy routine.
 *
 * @property routineId Hevy-assigned ID of the created or updated routine.
 * @property routineTitle Title of the routine as stored in Hevy.
 * @property exercisesMatched Total number of exercises matched to existing Hevy exercise templates
 *   (algorithmic + AI combined).
 * @property exercisesCreated Number of new custom exercise templates created in Hevy.
 * @property exercisesMatchedByAlgo Number of exercises matched by the algorithmic phase (Phase 1).
 * @property exercisesMatchedByAi Number of exercises matched by the AI phase (Phase 2).
 */
data class HevyExportResult(
    val routineId: String,
    val routineTitle: String,
    val exercisesMatched: Int,
    val exercisesCreated: Int,
    val exercisesMatchedByAlgo: Int = 0,
    val exercisesMatchedByAi: Int = 0,
)
