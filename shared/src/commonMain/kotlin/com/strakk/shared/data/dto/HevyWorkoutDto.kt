package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSONB structure stored in the check_ins.training_stats column.
 */
@Serializable
internal data class TrainingStatsDto(
    @SerialName("total_sessions") val totalSessions: Int,
    @SerialName("total_duration_minutes") val totalDurationMinutes: Int,
    @SerialName("total_volume_kg") val totalVolumeKg: Double,
    @SerialName("avg_rpe") val avgRpe: Double? = null,
    @SerialName("muscle_group_volume") val muscleGroupVolume: Map<String, Double> = emptyMap(),
    val workouts: List<HevyWorkoutDto> = emptyList(),
)

@Serializable
internal data class FetchHevyWorkoutsRequestDto(
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
)

@Serializable
internal data class FetchHevyWorkoutsResponseDto(
    val workouts: List<HevyWorkoutDto>,
)

@Serializable
internal data class HevyWorkoutDto(
    val id: String,
    val title: String,
    val date: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("total_volume_kg") val totalVolumeKg: Double,
    val exercises: List<HevyWorkoutExerciseDto>,
)

@Serializable
internal data class HevyWorkoutExerciseDto(
    val name: String,
    @SerialName("muscle_group") val muscleGroup: String,
    @SerialName("superset_id") val supersetId: Int? = null,
    val sets: List<HevyWorkoutSetDto>,
)

@Serializable
internal data class HevyWorkoutSetDto(
    val type: String,
    @SerialName("weight_kg") val weightKg: Double? = null,
    val reps: Int? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    val rpe: Double? = null,
)
