package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.CheckIn
import com.strakk.shared.domain.model.CheckInInput
import com.strakk.shared.domain.model.CheckInListItem
import com.strakk.shared.domain.model.CheckInMeasurements
import com.strakk.shared.domain.model.CheckInPhoto
import com.strakk.shared.domain.model.CheckInSeriesPoint
import com.strakk.shared.domain.model.NutritionAverages
import com.strakk.shared.domain.model.NutritionGoals
import com.strakk.shared.domain.repository.CheckInRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveCheckInQuickStatsUseCaseTest {

    @Test
    fun returnsNullWhenSeriesIsEmpty() = runTest {
        val useCase = ObserveCheckInQuickStatsUseCase(FakeCheckInRepository(emptyList()))

        assertNull(useCase().first())
    }

    @Test
    fun computesLatestValuesAndDeltasFromLatestTwoPoints() = runTest {
        val useCase = ObserveCheckInQuickStatsUseCase(
            FakeCheckInRepository(
                listOf(
                    point(weight = 78.0, armLeft = 35.0, armRight = 36.0, waist = 82.0),
                    point(weight = 80.0, armLeft = 34.0, armRight = 35.0, waist = 84.0),
                ),
            ),
        )

        val stats = useCase().first()

        assertEquals(78.0, stats?.lastWeight)
        assertEquals(-2.0, stats?.weightDelta)
        assertEquals(35.5, stats?.lastAvgArm)
        assertEquals(1.0, stats?.armDelta)
        assertEquals(82.0, stats?.lastWaist)
        assertEquals(-2.0, stats?.waistDelta)
    }

    @Test
    fun averagesSingleSidedArmMeasurements() = runTest {
        val useCase = ObserveCheckInQuickStatsUseCase(
            FakeCheckInRepository(
                listOf(
                    point(armLeft = 35.0, armRight = null),
                    point(armLeft = null, armRight = 34.0),
                ),
            ),
        )

        val stats = useCase().first()

        assertEquals(35.0, stats?.lastAvgArm)
        assertEquals(1.0, stats?.armDelta)
    }

    private fun point(
        weight: Double? = null,
        armLeft: Double? = null,
        armRight: Double? = null,
        waist: Double? = null,
    ) = CheckInSeriesPoint(
        weekLabel = "2026-W17",
        weight = weight,
        shoulders = null,
        chest = null,
        armLeft = armLeft,
        armRight = armRight,
        waist = waist,
        hips = null,
        thighLeft = null,
        thighRight = null,
    )

    private class FakeCheckInRepository(series: List<CheckInSeriesPoint>) : CheckInRepository {
        private val seriesFlow = MutableStateFlow(series)

        override fun observeCheckIns(): Flow<List<CheckInListItem>> = error("Unused")
        override fun observeCheckIn(id: String): Flow<CheckIn?> = error("Unused")
        override fun observeCheckInSeries(): Flow<List<CheckInSeriesPoint>> = seriesFlow
        override suspend fun createCheckIn(input: CheckInInput): CheckIn = error("Unused")
        override suspend fun updateCheckIn(id: String, input: CheckInInput): CheckIn = error("Unused")
        override suspend fun deleteCheckIn(id: String) = error("Unused")
        override suspend fun uploadPhoto(checkinId: String, imageData: ByteArray, position: Int): CheckInPhoto =
            error("Unused")

        override suspend fun deletePhoto(photoId: String, storagePath: String) = error("Unused")
        override suspend fun getPhotoUrl(storagePath: String): String = error("Unused")
        override suspend fun computeNutritionAverages(dates: List<String>): NutritionAverages = error("Unused")
        override suspend fun generateAiSummary(averages: NutritionAverages, goals: NutritionGoals): String =
            error("Unused")

        override suspend fun getPreviousMeasurements(weekLabel: String): CheckInMeasurements? = error("Unused")
        override suspend fun checkExistingForWeek(weekLabel: String): String? = error("Unused")
        override suspend fun clearCache() = Unit
    }
}
