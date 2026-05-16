package com.strakk.shared.domain.model

data class HevyWorkoutSet(
    val type: String,
    val weightKg: Double?,
    val reps: Int?,
    val durationSeconds: Int?,
    val rpe: Double?,
)

data class HevyWorkoutExercise(
    val name: String,
    val muscleGroup: String,
    val supersetId: Int? = null,
    val sets: List<HevyWorkoutSet>,
)

data class HevyWorkout(
    val id: String,
    val title: String,
    val date: String,
    val durationMinutes: Int,
    val totalVolumeKg: Double,
    val exercises: List<HevyWorkoutExercise>,
)

data class WeeklyTrainingStats(
    val totalSessions: Int,
    val totalDurationMinutes: Int,
    val totalVolumeKg: Double,
    val avgRpe: Double?,
    val muscleGroupVolume: Map<String, Double>,
    val workouts: List<HevyWorkout>,
)
