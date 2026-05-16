package com.strakk.shared.data.mapper

import com.strakk.shared.data.dto.HevyWorkoutDto
import com.strakk.shared.data.dto.HevyWorkoutExerciseDto
import com.strakk.shared.data.dto.HevyWorkoutSetDto
import com.strakk.shared.data.dto.TrainingStatsDto
import com.strakk.shared.domain.model.HevyWorkout
import com.strakk.shared.domain.model.HevyWorkoutExercise
import com.strakk.shared.domain.model.HevyWorkoutSet
import com.strakk.shared.domain.model.WeeklyTrainingStats

internal fun HevyWorkoutDto.toDomain(): HevyWorkout = HevyWorkout(
    id = id,
    title = title,
    date = date,
    durationMinutes = durationMinutes,
    totalVolumeKg = totalVolumeKg,
    exercises = exercises.map { it.toDomain() },
)

internal fun HevyWorkoutExerciseDto.toDomain(): HevyWorkoutExercise = HevyWorkoutExercise(
    name = name,
    muscleGroup = muscleGroup,
    supersetId = supersetId,
    sets = sets.map { it.toDomain() },
)

internal fun HevyWorkoutSetDto.toDomain(): HevyWorkoutSet = HevyWorkoutSet(
    type = type,
    weightKg = weightKg,
    reps = reps,
    durationSeconds = durationSeconds,
    rpe = rpe,
)

internal fun TrainingStatsDto.toDomain(): WeeklyTrainingStats = WeeklyTrainingStats(
    totalSessions = totalSessions,
    totalDurationMinutes = totalDurationMinutes,
    totalVolumeKg = totalVolumeKg,
    avgRpe = avgRpe,
    muscleGroupVolume = muscleGroupVolume,
    workouts = workouts.map { it.toDomain() },
)

internal fun WeeklyTrainingStats.toDto(): TrainingStatsDto = TrainingStatsDto(
    totalSessions = totalSessions,
    totalDurationMinutes = totalDurationMinutes,
    totalVolumeKg = totalVolumeKg,
    avgRpe = avgRpe,
    muscleGroupVolume = muscleGroupVolume,
    workouts = workouts.map { it.toDto() },
)

internal fun HevyWorkout.toDto(): HevyWorkoutDto = HevyWorkoutDto(
    id = id,
    title = title,
    date = date,
    durationMinutes = durationMinutes,
    totalVolumeKg = totalVolumeKg,
    exercises = exercises.map { it.toDto() },
)

internal fun HevyWorkoutExercise.toDto(): HevyWorkoutExerciseDto = HevyWorkoutExerciseDto(
    name = name,
    muscleGroup = muscleGroup,
    supersetId = supersetId,
    sets = sets.map { it.toDto() },
)

internal fun HevyWorkoutSet.toDto(): HevyWorkoutSetDto = HevyWorkoutSetDto(
    type = type,
    weightKg = weightKg,
    reps = reps,
    durationSeconds = durationSeconds,
    rpe = rpe,
)
