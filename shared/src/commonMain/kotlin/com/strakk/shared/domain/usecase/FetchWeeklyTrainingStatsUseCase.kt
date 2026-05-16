package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.HevyWorkout
import com.strakk.shared.domain.model.WeeklyTrainingStats
import com.strakk.shared.domain.repository.ProfileRepository
import com.strakk.shared.domain.repository.WorkoutRepository

class FetchWeeklyTrainingStatsUseCase(
    private val workoutRepository: WorkoutRepository,
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(coveredDates: List<String>): Result<WeeklyTrainingStats?> {
        return try {
            val result = doFetch(coveredDates)
            Result.success(result)
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (e: Exception) {
            println("[FetchTraining] EXCEPTION: ${e::class.simpleName}: ${e.message}")
            println("[FetchTraining] cause: ${e.cause?.let { "${it::class.simpleName}: ${it.message}" }}")
            Result.failure(e)
        }
    }

    private suspend fun doFetch(coveredDates: List<String>): WeeklyTrainingStats? {
        if (coveredDates.isEmpty()) {
            println("[FetchTraining] coveredDates is empty, returning null")
            return null
        }

        println("[FetchTraining] checking Hevy API key...")
        val apiKey = profileRepository.getHevyApiKey()
        println("[FetchTraining] apiKey present: ${apiKey != null}")
        if (apiKey == null) return null

        val startDate = coveredDates.min()
        val endDate = coveredDates.max()
        println("[FetchTraining] fetching workouts $startDate..$endDate")
        val workouts = workoutRepository.fetchWorkoutsForDateRange(startDate, endDate)
        println("[FetchTraining] got ${workouts.size} workouts")
        if (workouts.isEmpty()) return null

        return computeStats(workouts)
    }

    private fun computeStats(workouts: List<HevyWorkout>): WeeklyTrainingStats {
        val muscleVolume = mutableMapOf<String, Double>()
        val allRpes = mutableListOf<Double>()

        for (workout in workouts) {
            for (exercise in workout.exercises) {
                val exerciseVolume = exercise.sets.sumOf { set ->
                    val w = set.weightKg ?: 0.0
                    val r = set.reps ?: 0
                    w * r
                }
                muscleVolume[exercise.muscleGroup] =
                    (muscleVolume[exercise.muscleGroup] ?: 0.0) + exerciseVolume

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
