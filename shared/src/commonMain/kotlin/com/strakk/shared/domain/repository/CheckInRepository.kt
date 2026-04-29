package com.strakk.shared.domain.repository

import com.strakk.shared.domain.model.CheckIn
import com.strakk.shared.domain.model.CheckInInput
import com.strakk.shared.domain.model.CheckInListItem
import com.strakk.shared.domain.model.CheckInMeasurements
import com.strakk.shared.domain.model.CheckInPhoto
import com.strakk.shared.domain.model.CheckInSeriesPoint
import com.strakk.shared.domain.model.NutritionAverages
import com.strakk.shared.domain.model.NutritionGoals
import kotlinx.coroutines.flow.Flow

interface CheckInRepository {
    fun observeCheckIns(): Flow<List<CheckInListItem>>
    fun observeCheckIn(id: String): Flow<CheckIn?>
    fun observeCheckInSeries(): Flow<List<CheckInSeriesPoint>>
    suspend fun createCheckIn(input: CheckInInput): CheckIn
    suspend fun updateCheckIn(id: String, input: CheckInInput): CheckIn
    suspend fun deleteCheckIn(id: String)
    suspend fun uploadPhoto(checkinId: String, imageData: ByteArray, position: Int): CheckInPhoto
    suspend fun deletePhoto(photoId: String, storagePath: String)
    suspend fun getPhotoUrl(storagePath: String): String
    suspend fun computeNutritionAverages(dates: List<String>): NutritionAverages
    suspend fun generateAiSummary(
        averages: NutritionAverages,
        goals: NutritionGoals,
        weightKg: Double? = null,
        feelingTags: List<String> = emptyList(),
        mentalFeeling: String = "",
        physicalFeeling: String = "",
    ): String
    suspend fun getPreviousMeasurements(weekLabel: String): CheckInMeasurements?
    suspend fun checkExistingForWeek(weekLabel: String): String?
    suspend fun clearCache()
}
