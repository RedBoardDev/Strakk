package com.strakk.shared.data.repository

import com.strakk.shared.data.dto.MealEntryDto
import com.strakk.shared.data.dto.WaterEntryDto
import com.strakk.shared.data.mapper.toDomain
import com.strakk.shared.data.mapper.toJsonString
import com.strakk.shared.data.remote.CurrentUserIdProvider
import com.strakk.shared.domain.common.Logger
import com.strakk.shared.domain.model.FrequentItem
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.model.WaterEntry
import com.strakk.shared.domain.model.toDbString
import com.strakk.shared.domain.repository.NutritionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val LOG_TAG = "NutritionRepository"
private const val FREQUENT_ITEMS_FETCH_LIMIT = 200L

/**
 * Supabase-backed implementation of [NutritionRepository].
 *
 * Handles the timeline-wide stream of orphan [MealEntry] rows (quick-adds,
 * where `meal_id IS NULL`) and [WaterEntry] rows. Entries attached to a Meal
 * container are managed by [MealRepositoryImpl] and are NOT reflected here —
 * the UI combines both streams to build the Today timeline.
 */
internal class NutritionRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val userIdProvider: CurrentUserIdProvider,
    private val logger: Logger,
) : NutritionRepository {

    private val mealsCache = MutableStateFlow<Map<String, List<MealEntry>>>(emptyMap())
    private val waterCache = MutableStateFlow<Map<String, List<WaterEntry>>>(emptyMap())
    private val frequentItemsCache = MutableStateFlow<List<FrequentItem>?>(null)
    private val mealsFetchedDates = mutableSetOf<String>()
    private val waterFetchedDates = mutableSetOf<String>()
    private val fetchMutex = Mutex()
    private val mutationBus = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun observeMealsForDate(date: String): Flow<List<MealEntry>> =
        mealsCache.map { it[date] ?: emptyList() }
            .distinctUntilChanged()
            .onStart { ensureMealsFetched(date) }

    override fun observeWaterEntriesForDate(date: String): Flow<List<WaterEntry>> =
        waterCache.map { it[date] ?: emptyList() }
            .distinctUntilChanged()
            .onStart { ensureWaterFetched(date) }

    override fun observeNutritionMutations(): Flow<Unit> = mutationBus.asSharedFlow()

    override fun clearCache() {
        mealsCache.value = emptyMap()
        waterCache.value = emptyMap()
        frequentItemsCache.value = null
        mealsFetchedDates.clear()
        waterFetchedDates.clear()
    }

    private suspend fun ensureMealsFetched(date: String) {
        val shouldFetch = fetchMutex.withLock { mealsFetchedDates.add(date) }
        if (shouldFetch) {
            val entries = fetchOrphanMeals(date)
            mealsCache.update { cache ->
                trimCache(
                    cache + (date to mergeFetchedMealEntries(
                        cachedEntries = cache[date].orEmpty(),
                        fetchedEntries = entries,
                    )),
                )
            }
        }
    }

    private suspend fun ensureWaterFetched(date: String) {
        val shouldFetch = fetchMutex.withLock { waterFetchedDates.add(date) }
        if (shouldFetch) {
            val entries = fetchWaterEntries(date)
            waterCache.update { trimCache(it + (date to entries)) }
        }
    }

    /**
     * Removes cache entries for dates older than 30 days to prevent unbounded growth.
     */
    private fun <T> trimCache(cache: Map<String, T>): Map<String, T> {
        val cutoff = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .minus(DatePeriod(days = 30))
        return cache.filterKeys { dateKey ->
            runCatching { LocalDate.parse(dateKey) >= cutoff }.getOrDefault(true)
        }
    }

    private fun emitMutation() {
        mutationBus.tryEmit(Unit)
        frequentItemsCache.value = null
    }

    // -------------------------------------------------------------------------
    // Meal entries (orphans)
    // -------------------------------------------------------------------------

    private suspend fun fetchOrphanMeals(date: String): List<MealEntry> =
        supabaseClient
            .from("meal_entries")
            .select {
                filter {
                    eq("log_date", date)
                    exact("meal_id", null)
                }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<MealEntryDto>()
            .map { it.toDomain() }

    override suspend fun addMeal(entry: MealEntry): MealEntry {
        val userId = userIdProvider.currentOrThrow()
        val payload = buildJsonObject {
            put("user_id", userId)
            put("log_date", entry.logDate)
            entry.name?.let { put("name", it) }
            put("protein", entry.protein)
            put("calories", entry.calories)
            entry.fat?.let { put("fat", it) }
            entry.carbs?.let { put("carbs", it) }
            entry.quantity?.let { put("quantity", it) }
            put("source", entry.source.toDbString())
            entry.breakdown?.toJsonString()?.let { put("breakdown_json", it) }
            entry.photoPath?.let { put("photo_path", it) }
            // meal_id intentionally omitted (orphan quick-add)
        }

        val created = supabaseClient
            .from("meal_entries")
            .insert(payload) { select() }
            .decodeSingle<MealEntryDto>()
            .toDomain()

        mealsCache.update { cache ->
            val existing = cache[entry.logDate] ?: emptyList()
            cache + (entry.logDate to (existing + created))
        }
        emitMutation()
        return created
    }

    override suspend fun deleteMeal(id: String) {
        supabaseClient
            .from("meal_entries")
            .delete { filter { eq("id", id) } }
        mealsCache.update { byDate ->
            byDate.mapValues { (_, entries) -> entries.filterNot { it.id == id } }
        }
        emitMutation()
    }

    override suspend fun updateMealEntry(entry: MealEntry): MealEntry {
        supabaseClient
            .from("meal_entries")
            .update({
                set("name", entry.name)
                set("protein", entry.protein)
                set("calories", entry.calories)
                set("fat", entry.fat)
                set("carbs", entry.carbs)
                set("quantity", entry.quantity)
            }) { filter { eq("id", entry.id) } }

        mealsCache.update { byDate ->
            byDate.mapValues { (_, entries) ->
                entries.map { if (it.id == entry.id) entry else it }
            }
        }
        emitMutation()
        return entry
    }

    // -------------------------------------------------------------------------
    // Water entries
    // -------------------------------------------------------------------------

    private suspend fun fetchWaterEntries(date: String): List<WaterEntry> =
        supabaseClient
            .from("water_entries")
            .select {
                filter { eq("log_date", date) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<WaterEntryDto>()
            .map { it.toDomain() }

    override suspend fun addWater(logDate: String, amount: Int): WaterEntry {
        val userId = userIdProvider.currentOrThrow()

        val payload = buildJsonObject {
            put("user_id", userId)
            put("log_date", logDate)
            put("amount", amount)
        }

        val created = supabaseClient
            .from("water_entries")
            .insert(payload) { select() }
            .decodeSingle<WaterEntryDto>()
            .toDomain()

        waterCache.update {
            it + (logDate to ((it[logDate] ?: emptyList()) + created))
        }
        emitMutation()
        return created
    }

    override suspend fun deleteWater(id: String) {
        supabaseClient
            .from("water_entries")
            .delete { filter { eq("id", id) } }
        waterCache.update { byDate ->
            byDate.mapValues { (_, entries) -> entries.filterNot { it.id == id } }
        }
        emitMutation()
    }

    // -------------------------------------------------------------------------
    // Calendar
    // -------------------------------------------------------------------------

    override suspend fun getActiveCalendarDays(
        monthStart: String,
        monthEnd: String,
    ): List<String> {
        val mealDates = supabaseClient
            .from("meal_entries")
            .select {
                filter {
                    gte("log_date", monthStart)
                    lte("log_date", monthEnd)
                }
            }
            .decodeList<MealEntryDto>()
            .map { it.logDate }
            .toSet()

        val waterDates = supabaseClient
            .from("water_entries")
            .select {
                filter {
                    gte("log_date", monthStart)
                    lte("log_date", monthEnd)
                }
            }
            .decodeList<WaterEntryDto>()
            .map { it.logDate }
            .toSet()

        return (mealDates + waterDates).sorted()
    }

    // -------------------------------------------------------------------------
    // Frequent items
    // -------------------------------------------------------------------------

    override fun observeFrequentItems(limit: Int): Flow<List<FrequentItem>> = flow {
        frequentItemsCache.value?.let { emit(it) }

        val userId = try {
            userIdProvider.currentOrThrow()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emit(emptyList())
            return@flow
        }

        val rows = try {
            supabaseClient
                .from("meal_entries")
                .select {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                    range(0L, FREQUENT_ITEMS_FETCH_LIMIT - 1)
                }
                .decodeList<MealEntryDto>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(LOG_TAG, "Frequent items fetch failed", e)
            emit(frequentItemsCache.value ?: emptyList())
            return@flow
        }

        val now = Clock.System.now()
        val items = rows
            .groupBy { normalizeForDedup(it.name ?: "") }
            .mapNotNull { (normalizedName, entries) ->
                if (normalizedName.isBlank()) return@mapNotNull null
                val mostRecent = entries.first()
                val occurrences = entries.size
                val recencyBonus = computeRecencyBonus(mostRecent.createdAt, now)
                FrequentItem(
                    normalizedName = normalizedName,
                    name = mostRecent.name,
                    protein = mostRecent.protein,
                    calories = mostRecent.calories,
                    fat = mostRecent.fat,
                    carbs = mostRecent.carbs,
                    quantity = mostRecent.quantity,
                    occurrences = occurrences,
                ) to (occurrences * 2 + recencyBonus)
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }

        frequentItemsCache.value = items
        emit(items)
    }

    private fun normalizeForDedup(name: String): String =
        name.lowercase().trim().map { c ->
            when (c) {
                'à', 'â', 'ä' -> 'a'
                'é', 'è', 'ê', 'ë' -> 'e'
                'î', 'ï' -> 'i'
                'ô', 'ö' -> 'o'
                'ù', 'û', 'ü' -> 'u'
                'ç' -> 'c'
                else -> c
            }
        }.joinToString("")

    private fun computeRecencyBonus(createdAtStr: String, now: Instant): Double =
        runCatching {
            val createdAt = Instant.parse(createdAtStr)
            val daysAgo = (now - createdAt).inWholeDays.coerceAtLeast(0)
            when {
                daysAgo <= 1 -> 10.0
                daysAgo <= 7 -> 5.0
                daysAgo <= 30 -> 2.0
                else -> 0.0
            }
        }.getOrDefault(0.0)
}

internal fun mergeFetchedMealEntries(
    cachedEntries: List<MealEntry>,
    fetchedEntries: List<MealEntry>,
): List<MealEntry> {
    val fetchedIds = fetchedEntries.map { it.id }.toSet()
    return (fetchedEntries + cachedEntries.filterNot { it.id in fetchedIds })
        .sortedBy { it.createdAt }
}
