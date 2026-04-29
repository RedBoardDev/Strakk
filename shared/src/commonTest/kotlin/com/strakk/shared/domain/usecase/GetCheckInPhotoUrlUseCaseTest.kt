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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetCheckInPhotoUrlUseCaseTest {

    @Test
    fun returnsSignedUrlForStoragePath() = runTest {
        val repository = FakeCheckInRepository()
        val useCase = GetCheckInPhotoUrlUseCase(repository)

        val result = useCase("user/checkin/photo.jpg")

        assertEquals("signed:user/checkin/photo.jpg", result.getOrThrow())
        assertEquals("user/checkin/photo.jpg", repository.requestedPath)
    }

    private class FakeCheckInRepository : CheckInRepository {
        var requestedPath: String? = null

        override fun observeCheckIns(): Flow<List<CheckInListItem>> = error("Unused")
        override fun observeCheckIn(id: String): Flow<CheckIn?> = error("Unused")
        override fun observeCheckInSeries(): Flow<List<CheckInSeriesPoint>> = error("Unused")
        override suspend fun createCheckIn(input: CheckInInput): CheckIn = error("Unused")
        override suspend fun updateCheckIn(id: String, input: CheckInInput): CheckIn = error("Unused")
        override suspend fun deleteCheckIn(id: String) = error("Unused")
        override suspend fun uploadPhoto(checkinId: String, imageData: ByteArray, position: Int): CheckInPhoto =
            error("Unused")

        override suspend fun deletePhoto(photoId: String, storagePath: String) = error("Unused")
        override suspend fun getPhotoUrl(storagePath: String): String {
            requestedPath = storagePath
            return "signed:$storagePath"
        }

        override suspend fun computeNutritionAverages(dates: List<String>): NutritionAverages = error("Unused")
        override suspend fun generateAiSummary(averages: NutritionAverages, goals: NutritionGoals): String =
            error("Unused")

        override suspend fun getPreviousMeasurements(weekLabel: String): CheckInMeasurements? = error("Unused")
        override suspend fun checkExistingForWeek(weekLabel: String): String? = error("Unused")
        override suspend fun clearCache() = Unit
    }
}
