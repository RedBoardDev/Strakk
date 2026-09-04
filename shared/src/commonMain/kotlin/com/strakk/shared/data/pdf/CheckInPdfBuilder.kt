package com.strakk.shared.data.pdf

import com.strakk.shared.domain.model.CheckIn
import com.strakk.shared.domain.model.CheckInDelta
import com.strakk.shared.domain.model.CheckInSeriesPoint
import com.strakk.shared.domain.model.NutritionGoals
import com.strakk.shared.domain.model.PdfExportOptions
import com.strakk.shared.domain.model.WeeklyTrainingStats
import com.strakk.shared.domain.repository.CheckInRepository
import com.strakk.shared.domain.repository.ProfileRepository
import com.strakk.shared.domain.repository.WorkoutRepository
import com.strakk.shared.domain.service.CheckInPdfGenerator
import com.strakk.shared.domain.service.HtmlToPdfRenderer
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val BUCKET = "checkin-photos"
private const val WEIGHT_HISTORY_COUNT = 8

/** Series of one body measurement across the last N check-ins, oldest first. */
internal data class MeasurementSeries(
    val label: String,
    val unit: String,
    val values: List<Double?>, // null = not recorded that week
    val delta: Double?, // current − previous non-null (null if < 2 points)
    val weekLabels: List<String> = emptyList(), // abbreviated week labels e.g. "S20"
)

internal class CheckInPdfBuilderImpl(
    private val checkInRepository: CheckInRepository,
    private val workoutRepository: WorkoutRepository,
    private val profileRepository: ProfileRepository,
    private val supabaseClient: SupabaseClient,
    private val htmlRenderer: HtmlToPdfRenderer,
) : CheckInPdfGenerator {

    private val template = CheckInHtmlTemplate()

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun generate(checkInId: String, options: PdfExportOptions): ByteArray {
        val checkIn = checkInRepository.observeCheckIn(checkInId).firstOrNull()
            ?: error("Check-in $checkInId not found")

        val enrichedCheckIn = enrichWithNutrition(checkIn, options)
        val delta = loadDelta(enrichedCheckIn)
        val photoDataBase64 = if (options.includePhotos) downloadPhotosAsBase64(enrichedCheckIn) else emptyMap()
        val trainingStats = if (options.includeTraining) {
            enrichedCheckIn.trainingStats ?: loadTrainingStats(enrichedCheckIn)
        } else {
            null
        }

        val weightHistory = if (options.includeMeasurements) {
            loadWeightHistory(enrichedCheckIn.weekLabel)
        } else {
            emptyList()
        }
        val measurementHistory = if (options.includeMeasurements) {
            loadMeasurementsHistory(enrichedCheckIn.weekLabel)
        } else {
            emptyList()
        }
        val nutritionGoals = loadNutritionGoals()

        val html = template.build(
            checkIn = enrichedCheckIn,
            delta = delta,
            trainingStats = trainingStats,
            photoDataBase64 = photoDataBase64,
            options = options,
            weightHistory = weightHistory,
            nutritionGoals = nutritionGoals,
            measurementHistory = measurementHistory,
        )

        return htmlRenderer.render(html)
    }

    private suspend fun enrichWithNutrition(checkIn: CheckIn, options: PdfExportOptions): CheckIn {
        if (!options.includeDailyData || checkIn.nutritionSummary == null || checkIn.coveredDates.isEmpty()) {
            return checkIn
        }
        return try {
            val averages = checkInRepository.computeNutritionAverages(checkIn.coveredDates)
            checkIn.copy(nutritionSummary = checkIn.nutritionSummary.copy(dailyData = averages.dailyData))
        } catch (_: Exception) {
            checkIn
        }
    }

    private suspend fun loadDelta(checkIn: CheckIn): CheckInDelta? {
        return try {
            val previous = checkInRepository.getPreviousMeasurements(checkIn.weekLabel)
                ?: return null
            CheckInDelta(
                weight = delta(checkIn.weight, previous.weight),
                shoulders = delta(checkIn.shoulders, previous.shoulders),
                chest = delta(checkIn.chest, previous.chest),
                armLeft = delta(checkIn.armLeft, previous.armLeft),
                armRight = delta(checkIn.armRight, previous.armRight),
                waist = delta(checkIn.waist, previous.waist),
                hips = delta(checkIn.hips, previous.hips),
                thighLeft = delta(checkIn.thighLeft, previous.thighLeft),
                thighRight = delta(checkIn.thighRight, previous.thighRight),
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun delta(current: Double?, previous: Double?): Double? {
        if (current == null || previous == null) return null
        return current - previous
    }

    @Suppress("NestedBlockDepth")
    private suspend fun loadTrainingStats(checkIn: CheckIn): WeeklyTrainingStats? {
        if (checkIn.coveredDates.isEmpty()) return null
        return try {
            val startDate = checkIn.coveredDates.min()
            val endDate = checkIn.coveredDates.max()
            val workouts = workoutRepository.fetchWorkoutsForDateRange(startDate, endDate)
            if (workouts.isEmpty()) return null

            val muscleVolume = mutableMapOf<String, Double>()
            val allRpes = mutableListOf<Double>()
            for (workout in workouts) {
                for (exercise in workout.exercises) {
                    val vol = exercise.sets.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
                    muscleVolume[exercise.muscleGroup] = (muscleVolume[exercise.muscleGroup] ?: 0.0) + vol
                    exercise.sets.mapNotNull { it.rpe }.let { allRpes.addAll(it) }
                }
            }

            WeeklyTrainingStats(
                totalSessions = workouts.size,
                totalDurationMinutes = workouts.sumOf { it.durationMinutes },
                totalVolumeKg = workouts.sumOf { it.totalVolumeKg },
                avgRpe = if (allRpes.isNotEmpty()) allRpes.average() else null,
                muscleGroupVolume = muscleVolume,
                workouts = workouts,
            )
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun loadWeightHistory(throughWeekLabel: String): List<Pair<String, Double>> {
        return try {
            checkInRepository.observeCheckInSeries().firstOrNull()
                ?.historyThroughWeek(throughWeekLabel, WEIGHT_HISTORY_COUNT)
                ?.filter { it.weight != null }
                ?.map { it.weekLabel to it.weight!! }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun loadMeasurementsHistory(throughWeekLabel: String): List<MeasurementSeries> {
        val points = try {
            checkInRepository.observeCheckInSeries().firstOrNull()
                ?.historyThroughWeek(throughWeekLabel, WEIGHT_HISTORY_COUNT)
                ?: return emptyList()
        } catch (_: Exception) {
            return emptyList()
        }

        // "2026-W20" → "S20"
        val weekLabels = points.map { "S" + it.weekLabel.substringAfter("-W", it.weekLabel) }

        fun avg(a: Double?, b: Double?): Double? = listOfNotNull(a, b)
            .let { if (it.isEmpty()) null else it.average() }

        fun series(label: String, unit: String, get: (CheckInSeriesPoint) -> Double?): MeasurementSeries {
            val values = points.map(get)
            val current = values.lastOrNull { it != null }
            val previous = values.dropLast(1).lastOrNull { it != null }
            return MeasurementSeries(
                label = label,
                unit = unit,
                values = values,
                delta = if (current != null && previous != null) current - previous else null,
                weekLabels = weekLabels,
            )
        }

        return listOf(
            series("Taille", "cm") { it.waist },
            series("Hanches", "cm") { it.hips },
            series("Épaules", "cm") { it.shoulders },
            series("Buste", "cm") { it.chest },
            series("Bras (moy. G/D)", "cm") { avg(it.armLeft, it.armRight) },
            series("Cuisses (moy. G/D)", "cm") { avg(it.thighLeft, it.thighRight) },
        ).filter { ms -> ms.values.any { it != null } }
    }

    private suspend fun loadNutritionGoals(): NutritionGoals? = try {
        val profile = profileRepository.getProfile() ?: return null
        NutritionGoals(
            proteinGoal = profile.proteinGoal,
            calorieGoal = profile.calorieGoal,
            fatGoal = profile.fatGoal,
            carbGoal = profile.carbGoal,
            waterGoal = profile.waterGoal,
        )
    } catch (_: Exception) {
        null
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun downloadPhotosAsBase64(checkIn: CheckIn): Map<String, String> = coroutineScope {
            checkIn.photos.map { photo ->
                async {
                    try {
                        val bytes = supabaseClient.storage
                            .from(BUCKET)
                            .downloadAuthenticated(photo.storagePath)
                        photo.id to Base64.encode(bytes)
                    } catch (_: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull().toMap()
        }
}
