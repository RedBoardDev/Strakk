package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.HevyWorkout
import com.strakk.shared.domain.model.WeeklyTrainingStats
import com.strakk.shared.domain.repository.CheckInRepository
import com.strakk.shared.domain.repository.ProfileRepository
import com.strakk.shared.domain.repository.WorkoutRepository

/**
 * Fetches training stats from Hevy for the given check-in's date range
 * and saves them to the check-in record for caching.
 *
 * Returns null if the user has no Hevy API key or no workouts in range.
 */
class RefreshTrainingStatsUseCase(
    private val workoutRepository: WorkoutRepository,
    private val profileRepository: ProfileRepository,
    private val checkInRepository: CheckInRepository,
) {
    suspend operator fun invoke(checkInId: String, coveredDates: List<String>): Result<WeeklyTrainingStats?> =
        runSuspendCatching {
            if (coveredDates.isEmpty()) return@runSuspendCatching null

            profileRepository.getHevyApiKey()
                ?: return@runSuspendCatching null

            val startDate = coveredDates.min()
            val endDate = coveredDates.max()
            val workouts = workoutRepository.fetchWorkoutsForDateRange(startDate, endDate)
            if (workouts.isEmpty()) return@runSuspendCatching null

            val stats = computeStats(workouts)
            checkInRepository.saveTrainingStats(checkInId, stats)
            stats
        }

    private fun computeStats(workouts: List<HevyWorkout>): WeeklyTrainingStats {
        val muscleVolume = mutableMapOf<String, Double>()
        val allRpes = mutableListOf<Double>()

        for (workout in workouts) {
            for (exercise in workout.exercises) {
                val vol = exercise.sets.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
                muscleVolume[exercise.muscleGroup] =
                    (muscleVolume[exercise.muscleGroup] ?: 0.0) + vol
                exercise.sets.mapNotNull { it.rpe }.let { allRpes.addAll(it) }
            }
        }

        return WeeklyTrainingStats(
            totalSessions = workouts.size,
            totalDurationMinutes = workouts.sumOf { it.durationMinutes },
            totalVolumeKg = workouts.sumOf { it.totalVolumeKg },
            avgRpe = if (allRpes.isNotEmpty()) allRpes.average() else null,
            muscleGroupVolume = muscleVolume,
            workouts = workouts,
        )
    }
}
