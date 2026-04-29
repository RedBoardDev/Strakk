package com.strakk.shared.data.repository

import com.strakk.shared.data.dto.CheckInDto
import com.strakk.shared.data.dto.CheckInPhotoDto
import com.strakk.shared.data.dto.CheckInSummaryResponseDto
import com.strakk.shared.data.mapper.toDomain
import com.strakk.shared.data.mapper.toListItem
import com.strakk.shared.data.mapper.toMeasurements
import com.strakk.shared.data.mapper.toSeriesPoint
import com.strakk.shared.data.remote.CurrentUserIdProvider
import com.strakk.shared.domain.common.DomainError
import com.strakk.shared.domain.common.Logger
import com.strakk.shared.domain.model.CheckIn
import com.strakk.shared.domain.model.CheckInInput
import com.strakk.shared.domain.model.CheckInListItem
import com.strakk.shared.domain.model.CheckInMeasurements
import com.strakk.shared.domain.model.CheckInPhoto
import com.strakk.shared.domain.model.CheckInSeriesPoint
import com.strakk.shared.domain.model.DailyNutrition
import com.strakk.shared.domain.model.NutritionAverages
import com.strakk.shared.domain.model.NutritionGoals
import com.strakk.shared.domain.repository.CheckInRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.client.call.body
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val LOG_TAG = "CheckInRepository"
private const val BUCKET = "checkin-photos"
private const val TABLE = "checkins"
private const val PHOTOS_TABLE = "checkin_photos"
private const val CHECKIN_COLUMNS = "*, checkin_photos(*)"

internal class CheckInRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val userIdProvider: CurrentUserIdProvider,
    private val logger: Logger,
) : CheckInRepository {

    private val cache = MutableStateFlow<List<CheckInDto>>(emptyList())
    private var fetched = false
    private val fetchMutex = Mutex()

    // -------------------------------------------------------------------------
    // Observation
    // -------------------------------------------------------------------------

    override fun observeCheckIns(): Flow<List<CheckInListItem>> =
        cache.map { list -> list.map { it.toListItem() } }
            .distinctUntilChanged()
            .onStart { ensureFetched() }

    override fun observeCheckIn(id: String): Flow<CheckIn?> =
        cache.map { list -> list.find { it.id == id }?.toDomain() }
            .distinctUntilChanged()
            .onStart { ensureFetched() }

    override fun observeCheckInSeries(): Flow<List<CheckInSeriesPoint>> =
        cache.map { list -> list.map { it.toSeriesPoint() } }
            .distinctUntilChanged()
            .onStart { ensureFetched() }

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    override suspend fun createCheckIn(input: CheckInInput): CheckIn {
        val userId = userIdProvider.currentOrThrow()
        val payload = buildCheckInPayload(userId, input)

        val created = supabaseClient
            .from(TABLE)
            .insert(payload) { select(Columns.raw(CHECKIN_COLUMNS)) }
            .decodeSingle<CheckInDto>()

        cache.value = listOf(created) + cache.value
        return created.toDomain()
    }

    override suspend fun updateCheckIn(id: String, input: CheckInInput): CheckIn {
        val payload = buildCheckInUpdatePayload(input)

        supabaseClient
            .from(TABLE)
            .update(payload) { filter { eq("id", id) } }

        val updated = supabaseClient
            .from(TABLE)
            .select(Columns.raw(CHECKIN_COLUMNS)) {
                filter { eq("id", id) }
            }
            .decodeSingle<CheckInDto>()

        cache.value = cache.value.map { if (it.id == id) updated else it }
        return updated.toDomain()
    }

    override suspend fun deleteCheckIn(id: String) {
        val photoPaths = cache.value.find { it.id == id }
            ?.checkinPhotos
            ?.map { it.storagePath }
            .orEmpty()

        supabaseClient.from(TABLE).delete { filter { eq("id", id) } }

        if (photoPaths.isNotEmpty()) {
            try {
                supabaseClient.storage.from(BUCKET).delete(photoPaths)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(LOG_TAG, "Failed to clean up ${photoPaths.size} photos from storage", e)
            }
        }

        cache.value = cache.value.filterNot { it.id == id }
    }

    // -------------------------------------------------------------------------
    // Photos
    // -------------------------------------------------------------------------

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun uploadPhoto(
        checkinId: String,
        imageData: ByteArray,
        position: Int,
    ): CheckInPhoto {
        val userId = userIdProvider.currentOrThrow()
        val path = "$userId/$checkinId/${Uuid.random()}.jpg"

        try {
            supabaseClient.storage.from(BUCKET).upload(path, imageData) { upsert = true }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw DomainError.DataError("Photo upload failed", e)
        }

        val photoPayload = buildJsonObject {
            put("checkin_id", checkinId)
            put("storage_path", path)
            put("position", position)
        }

        val photoDto = supabaseClient
            .from(PHOTOS_TABLE)
            .insert(photoPayload) { select() }
            .decodeSingle<CheckInPhotoDto>()

        cache.value = cache.value.map { dto ->
            if (dto.id == checkinId) {
                dto.copy(checkinPhotos = dto.checkinPhotos.orEmpty() + photoDto)
            } else {
                dto
            }
        }

        return photoDto.toDomain()
    }

    override suspend fun deletePhoto(photoId: String, storagePath: String) {
        try {
            supabaseClient.storage.from(BUCKET).delete(storagePath)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(LOG_TAG, "deletePhoto storage failed for $storagePath", e)
        }

        supabaseClient.from(PHOTOS_TABLE).delete { filter { eq("id", photoId) } }

        cache.value = cache.value.map { dto ->
            dto.copy(checkinPhotos = dto.checkinPhotos?.filterNot { it.id == photoId })
        }
    }

    override suspend fun getPhotoUrl(storagePath: String): String =
        supabaseClient.storage.from(BUCKET).createSignedUrl(storagePath, 3600.seconds)

    // -------------------------------------------------------------------------
    // Nutrition averages (client-side computation)
    // -------------------------------------------------------------------------

    override suspend fun computeNutritionAverages(dates: List<String>): NutritionAverages {
        if (dates.isEmpty()) return NutritionAverages(0.0, 0.0, 0.0, 0.0, 0, 0)

        val mealEntries = supabaseClient
            .from("meal_entries")
            .select {
                filter { isIn("log_date", dates) }
            }
            .decodeList<MealEntryLightDto>()

        val waterEntries = supabaseClient
            .from("water_entries")
            .select {
                filter { isIn("log_date", dates) }
            }
            .decodeList<WaterEntryLightDto>()

        data class DailyMacros(
            var protein: Double = 0.0,
            var calories: Double = 0.0,
            var fat: Double = 0.0,
            var carbs: Double = 0.0,
        )

        val macrosByDate = mutableMapOf<String, DailyMacros>()
        for (entry in mealEntries) {
            val daily = macrosByDate.getOrPut(entry.logDate) { DailyMacros() }
            daily.protein += entry.protein
            daily.calories += entry.calories
            daily.fat += entry.fat ?: 0.0
            daily.carbs += entry.carbs ?: 0.0
        }

        val waterByDate = mutableMapOf<String, Int>()
        for (entry in waterEntries) {
            waterByDate[entry.logDate] = (waterByDate[entry.logDate] ?: 0) + entry.amount
        }

        val allDates = (macrosByDate.keys + waterByDate.keys).toSet()
        if (allDates.isEmpty()) return NutritionAverages(0.0, 0.0, 0.0, 0.0, 0, 0)

        val count = allDates.size

        // Top foods: most frequent non-null, non-blank names (max 8)
        val foodFrequency = mutableMapOf<String, Int>()
        for (entry in mealEntries) {
            val n = entry.name?.trim()?.takeIf { it.isNotBlank() } ?: continue
            foodFrequency[n] = (foodFrequency[n] ?: 0) + 1
        }
        val topFoods = foodFrequency.entries
            .sortedByDescending { it.value }
            .take(8)
            .map { it.key }

        // Protein per day (Int, sorted chronologically)
        val proteinPerDay = allDates.sorted().map { date ->
            (macrosByDate[date]?.protein ?: 0.0).toInt()
        }

        // Days with any water logged
        val daysWithWater = waterByDate.count { it.value > 0 }

        // Daily breakdown sorted chronologically
        val dailyData = allDates.sorted().map { date ->
            val m = macrosByDate[date] ?: DailyMacros()
            DailyNutrition(
                date = date,
                calories = m.calories,
                protein = m.protein,
                fat = m.fat,
                carbs = m.carbs,
                waterMl = waterByDate[date] ?: 0,
            )
        }

        return NutritionAverages(
            avgProtein = macrosByDate.values.sumOf { it.protein } / count,
            avgCalories = macrosByDate.values.sumOf { it.calories } / count,
            avgFat = macrosByDate.values.sumOf { it.fat } / count,
            avgCarbs = macrosByDate.values.sumOf { it.carbs } / count,
            avgWater = waterByDate.values.sum() / count,
            nutritionDays = count,
            topFoods = topFoods,
            proteinPerDay = proteinPerDay,
            daysWithWater = daysWithWater,
            dailyData = dailyData,
        )
    }

    // -------------------------------------------------------------------------
    // AI Summary
    // -------------------------------------------------------------------------

    override suspend fun generateAiSummary(
        averages: NutritionAverages,
        goals: NutritionGoals,
        weightKg: Double?,
        feelingTags: List<String>,
        mentalFeeling: String,
        physicalFeeling: String,
    ): String {
        val requestBody = buildJsonObject {
            put("avg_protein", averages.avgProtein)
            put("avg_calories", averages.avgCalories)
            put("avg_fat", averages.avgFat)
            put("avg_carbs", averages.avgCarbs)
            put("avg_water", averages.avgWater)
            put("nutrition_days", averages.nutritionDays)
            put("top_foods", JsonArray(averages.topFoods.map { JsonPrimitive(it) }))
            put("protein_per_day", JsonArray(averages.proteinPerDay.map { JsonPrimitive(it) }))
            put("days_with_water", averages.daysWithWater)
            putJsonObject("goals") {
                goals.proteinGoal?.let { put("protein_goal", it) }
                goals.calorieGoal?.let { put("calorie_goal", it) }
                goals.waterGoal?.let { put("water_goal", it) }
            }
            weightKg?.let { put("weight_kg", it) }
            if (feelingTags.isNotEmpty()) {
                put("feeling_tags", JsonArray(feelingTags.map { JsonPrimitive(it) }))
            }
            if (mentalFeeling.isNotBlank()) put("mental_feeling", mentalFeeling)
            if (physicalFeeling.isNotBlank()) put("physical_feeling", physicalFeeling)
        }

        val response = try {
            supabaseClient.functions.invoke("generate-checkin-summary", body = requestBody)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw DomainError.DataError("Failed to generate AI summary", e)
        }

        if (response.status.value !in 200..299) {
            throw DomainError.DataError("AI summary service returned HTTP ${response.status.value}")
        }

        return try {
            response.body<CheckInSummaryResponseDto>().summary
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw DomainError.DataError("Unexpected AI summary response", e)
        }
    }

    // -------------------------------------------------------------------------
    // Previous measurements (for deltas)
    // -------------------------------------------------------------------------

    override suspend fun getPreviousMeasurements(weekLabel: String): CheckInMeasurements? {
        ensureFetched()
        return cache.value
            .filter { it.weekLabel < weekLabel }
            .maxByOrNull { it.weekLabel }
            ?.toMeasurements()
    }

    // -------------------------------------------------------------------------
    // Check existing
    // -------------------------------------------------------------------------

    override suspend fun checkExistingForWeek(weekLabel: String): String? {
        ensureFetched()
        return cache.value.find { it.weekLabel == weekLabel }?.id
    }

    // -------------------------------------------------------------------------
    // Cache
    // -------------------------------------------------------------------------

    override suspend fun clearCache() {
        fetchMutex.withLock {
            cache.value = emptyList()
            fetched = false
        }
    }

    private suspend fun ensureFetched() {
        fetchMutex.withLock {
            if (fetched) return@withLock
            val items = supabaseClient
                .from(TABLE)
                .select(Columns.raw(CHECKIN_COLUMNS)) {
                    order("week_label", Order.DESCENDING)
                }
                .decodeList<CheckInDto>()
            cache.value = items
            fetched = true
        }
    }

    // -------------------------------------------------------------------------
    // Payload builders
    // -------------------------------------------------------------------------

    private fun buildCheckInPayload(userId: String, input: CheckInInput) = buildJsonObject {
        put("user_id", userId)
        put("week_label", input.weekLabel)
        put("covered_dates", buildJsonArray { input.coveredDates.forEach { add(JsonPrimitive(it)) } })
        input.weight?.let { put("weight_kg", it) }
        input.shoulders?.let { put("shoulders_cm", it) }
        input.chest?.let { put("chest_cm", it) }
        input.armLeft?.let { put("arm_left_cm", it) }
        input.armRight?.let { put("arm_right_cm", it) }
        input.waist?.let { put("waist_cm", it) }
        input.hips?.let { put("hips_cm", it) }
        input.thighLeft?.let { put("thigh_left_cm", it) }
        input.thighRight?.let { put("thigh_right_cm", it) }
        put("feeling_tags", buildJsonArray { input.feelingTags.forEach { add(JsonPrimitive(it)) } })
        input.mentalFeeling?.let { put("mental_feeling", it) }
        input.physicalFeeling?.let { put("physical_feeling", it) }
        input.nutritionSummary?.let { ns ->
            put("avg_protein", ns.avgProtein)
            put("avg_calories", ns.avgCalories)
            put("avg_fat", ns.avgFat)
            put("avg_carbs", ns.avgCarbs)
            put("avg_water", ns.avgWater)
            put("nutrition_days", ns.nutritionDays)
            ns.aiSummary?.let { put("ai_summary", it) }
        }
    }

    private fun buildCheckInUpdatePayload(input: CheckInInput) = buildJsonObject {
        put("covered_dates", buildJsonArray { input.coveredDates.forEach { add(JsonPrimitive(it)) } })
        put("weight_kg", input.weight?.let { JsonPrimitive(it) } ?: JsonNull)
        put("shoulders_cm", input.shoulders?.let { JsonPrimitive(it) } ?: JsonNull)
        put("chest_cm", input.chest?.let { JsonPrimitive(it) } ?: JsonNull)
        put("arm_left_cm", input.armLeft?.let { JsonPrimitive(it) } ?: JsonNull)
        put("arm_right_cm", input.armRight?.let { JsonPrimitive(it) } ?: JsonNull)
        put("waist_cm", input.waist?.let { JsonPrimitive(it) } ?: JsonNull)
        put("hips_cm", input.hips?.let { JsonPrimitive(it) } ?: JsonNull)
        put("thigh_left_cm", input.thighLeft?.let { JsonPrimitive(it) } ?: JsonNull)
        put("thigh_right_cm", input.thighRight?.let { JsonPrimitive(it) } ?: JsonNull)
        put("feeling_tags", buildJsonArray { input.feelingTags.forEach { add(JsonPrimitive(it)) } })
        put("mental_feeling", input.mentalFeeling?.let { JsonPrimitive(it) } ?: JsonNull)
        put("physical_feeling", input.physicalFeeling?.let { JsonPrimitive(it) } ?: JsonNull)
        input.nutritionSummary?.let { ns ->
            put("avg_protein", ns.avgProtein)
            put("avg_calories", ns.avgCalories)
            put("avg_fat", ns.avgFat)
            put("avg_carbs", ns.avgCarbs)
            put("avg_water", ns.avgWater)
            put("nutrition_days", ns.nutritionDays)
            put("ai_summary", ns.aiSummary?.let { JsonPrimitive(it) } ?: JsonNull)
        }
    }
}

@Serializable
private data class MealEntryLightDto(
    @SerialName("log_date") val logDate: String,
    val name: String? = null,
    val protein: Double,
    val calories: Double,
    val fat: Double? = null,
    val carbs: Double? = null,
)

@Serializable
private data class WaterEntryLightDto(
    @SerialName("log_date") val logDate: String,
    val amount: Int,
)
