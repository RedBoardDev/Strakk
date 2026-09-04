package com.strakk.shared.data.pdf

import com.strakk.shared.domain.model.CheckIn
import com.strakk.shared.domain.model.HevyWorkout
import com.strakk.shared.domain.model.NutritionGoals
import com.strakk.shared.domain.model.NutritionSummary
import com.strakk.shared.domain.model.PdfExportOptions
import com.strakk.shared.domain.model.WeeklyTrainingStats
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class CheckInHtmlTemplateTest {

    private val template = CheckInHtmlTemplate()

    @Test
    fun `workout details option renders compact Hevy links`() {
        val workout = HevyWorkout(
            id = "b459cba5-cd6d-463c-abd6-54f8eafcadcb",
            title = "Upper & Core",
            date = "2026-07-10",
            durationMinutes = 58,
            totalVolumeKg = 4_200.0,
            exercises = emptyList(),
        )

        val html = template.build(
            checkIn = checkIn(),
            delta = null,
            trainingStats = trainingStats(workout),
            photoDataBase64 = emptyMap(),
            options = minimalOptions(includeWorkoutDetails = true),
        )

        assertContains(html, "Séances Hevy")
        assertContains(html, "href=\"https://hevy.com/workout/${workout.id}\"")
        assertContains(html, "data-pdf-link-text=\"Ouvrir dans Hevy\"")
        assertContains(html, "Upper &amp; Core")
        assertFalse(html.contains("SUPERSET"))
    }

    @Test
    fun `invalid workout identifier is not rendered as a link`() {
        val workout = HevyWorkout(
            id = "\" onclick=\"alert(1)",
            title = "Unsafe",
            date = "2026-07-10",
            durationMinutes = 30,
            totalVolumeKg = 0.0,
            exercises = emptyList(),
        )

        val html = template.build(
            checkIn = checkIn(),
            delta = null,
            trainingStats = trainingStats(workout),
            photoDataBase64 = emptyMap(),
            options = minimalOptions(includeWorkoutDetails = true),
        )

        assertFalse(html.contains("onclick"))
        assertFalse(html.contains("Séances Hevy"))
    }

    @Test
    fun `nutrition renders averages and symmetric adherence side by side`() {
        val summary = NutritionSummary(
            avgProtein = 0.0,
            avgCalories = 1_200.0,
            avgFat = 0.0,
            avgCarbs = 0.0,
            avgWater = 0,
            nutritionDays = 7,
            aiSummary = null,
        )

        val html = template.build(
            checkIn = checkIn(nutritionSummary = summary),
            delta = null,
            trainingStats = null,
            photoDataBase64 = emptyMap(),
            options = minimalOptions(
                includeCalories = true,
                includeAverages = true,
            ),
            nutritionGoals = NutritionGoals(
                proteinGoal = null,
                calorieGoal = 1_000,
                fatGoal = null,
                carbGoal = null,
                waterGoal = null,
            ),
        )

        assertContains(html, "nutrition-overview")
        assertContains(html, "Moyennes sur 7 jours")
        assertContains(html, "Adhérence macros")
        assertContains(html, "width:80%")
    }

    @Test
    fun `measurement evolution uses separate absolute charts and explicit averages`() {
        val html = template.build(
            checkIn = checkIn(),
            delta = null,
            trainingStats = null,
            photoDataBase64 = emptyMap(),
            options = minimalOptions(includeMeasurements = true),
            measurementHistory = listOf(
                MeasurementSeries(
                    label = "Bras (moy. G/D)",
                    unit = "cm",
                    values = listOf(31.0, 31.5, 32.0),
                    delta = 0.5,
                    weekLabels = listOf("S27", "S28", "S29"),
                ),
            ),
        )

        assertContains(html, "measurement-grid")
        assertContains(html, "Bras (moy. G/D)")
        assertContains(html, "32 cm <small>+0.5 cm</small>")
        assertContains(html, ">32.5</text>")
        assertContains(html, ">30.5</text>")
    }

    @Test
    fun `statistics flow after feelings without a forced third page`() {
        val workout = HevyWorkout(
            id = "b459cba5-cd6d-463c-abd6-54f8eafcadcb",
            title = "Upper",
            date = "2026-07-10",
            durationMinutes = 58,
            totalVolumeKg = 4_200.0,
            exercises = emptyList(),
        )
        val html = template.build(
            checkIn = checkIn().copy(feelingTags = listOf("motivated")),
            delta = null,
            trainingStats = trainingStats(workout),
            photoDataBase64 = emptyMap(),
            options = minimalOptions().copy(
                includeFeelings = true,
                includeTrainingSummary = true,
            ),
        )

        val contentBetweenSections = html
            .substringAfter("Ressentis")
            .substringBefore("Statistiques")
        assertFalse(contentBetweenSections.contains("class=\"pb\""))
    }

    private fun checkIn(nutritionSummary: NutritionSummary? = null) = CheckIn(
        id = "check-in-id",
        weekLabel = "2026-W29",
        coveredDates = listOf("2026-07-13", "2026-07-14"),
        weight = null,
        shoulders = null,
        chest = null,
        armLeft = null,
        armRight = null,
        waist = null,
        hips = null,
        thighLeft = null,
        thighRight = null,
        feelingTags = emptyList(),
        mentalFeeling = null,
        physicalFeeling = null,
        nutritionSummary = nutritionSummary,
        photos = emptyList(),
        createdAt = "2026-07-14T10:00:00Z",
        updatedAt = "2026-07-14T10:00:00Z",
    )

    private fun trainingStats(workout: HevyWorkout) = WeeklyTrainingStats(
        totalSessions = 1,
        totalDurationMinutes = workout.durationMinutes,
        totalVolumeKg = workout.totalVolumeKg,
        avgRpe = null,
        muscleGroupVolume = emptyMap(),
        workouts = listOf(workout),
    )

    private fun minimalOptions(
        includeMeasurements: Boolean = false,
        includeCalories: Boolean = false,
        includeAverages: Boolean = false,
        includeWorkoutDetails: Boolean = false,
    ) = PdfExportOptions(
        includePhotos = false,
        includeMeasurements = includeMeasurements,
        includeFeelings = false,
        includeProtein = false,
        includeCalories = includeCalories,
        includeCarbs = false,
        includeFat = false,
        includeWater = false,
        includeAverages = includeAverages,
        includeDailyData = false,
        includeTrainingSummary = false,
        includeMuscleVolume = false,
        includeWorkoutDetails = includeWorkoutDetails,
    )
}
