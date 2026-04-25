package com.strakk.shared.domain.model

/**
 * A full workout program parsed from a PDF.
 *
 * @property programName Human-readable name of the program.
 * @property sessions Ordered list of training sessions.
 */
data class WorkoutProgram(
    val programName: String,
    val sessions: List<WorkoutSession>,
)

/**
 * A single training session within a [WorkoutProgram].
 *
 * @property name Session label (e.g. "Day A – Push").
 * @property sections Exercise sections / muscle groups within the session.
 */
data class WorkoutSession(
    val name: String,
    val sections: List<ExerciseSection>,
)

/**
 * A named section grouping exercises within a [WorkoutSession].
 *
 * @property name Section label (e.g. "Chest", "Warm-up").
 * @property exercises Ordered list of exercises in this section.
 */
data class ExerciseSection(
    val name: String,
    val exercises: List<ProgramExercise>,
)

/**
 * A single exercise entry inside an [ExerciseSection].
 *
 * @property name Exercise name (e.g. "Bench Press").
 * @property sets Number of sets.
 * @property reps Rep scheme as text — may be "12", "10-12", "30s", etc.
 * @property weightKg Optional prescribed weight in kg.
 * @property restSeconds Rest period between sets in seconds.
 * @property notes Optional coaching notes or cues.
 * @property supersetGroup Optional superset group identifier. Exercises sharing
 *   the same non-null value are supersetted together.
 * @property exerciseType Hevy exercise type identifier — one of:
 *   `weight_reps`, `bodyweight_reps`, `duration`, `reps_only`.
 * @property equipmentCategory Equipment category (e.g. "barbell", "dumbbell").
 * @property muscleGroup Primary muscle group targeted.
 */
data class ProgramExercise(
    val name: String,
    val sets: Int,
    val reps: String,
    val weightKg: Double?,
    val restSeconds: Int,
    val notes: String?,
    val supersetGroup: Int?,
    val exerciseType: String,
    val equipmentCategory: String,
    val muscleGroup: String,
)
