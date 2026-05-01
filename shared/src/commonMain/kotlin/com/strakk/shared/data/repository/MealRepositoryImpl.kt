package com.strakk.shared.data.repository

import com.strakk.shared.data.dto.AnalyzeMealSingleRequestDto
import com.strakk.shared.data.dto.AnalyzedEntryDto
import com.strakk.shared.data.dto.ExtractDraftItemDto
import com.strakk.shared.data.dto.ExtractMealDraftRequestDto
import com.strakk.shared.data.dto.ExtractMealDraftResponseDto
import com.strakk.shared.data.dto.MealDto
import com.strakk.shared.data.dto.MealEntryDto
import com.strakk.shared.data.mapper.toDomain
import com.strakk.shared.data.mapper.toJsonString
import com.strakk.shared.data.mapper.toResolved
import com.strakk.shared.data.remote.CurrentUserIdProvider
import com.strakk.shared.domain.common.DomainError
import com.strakk.shared.domain.common.Logger
import com.strakk.shared.domain.model.DraftItem
import com.strakk.shared.domain.model.EntrySource
import com.strakk.shared.domain.model.Meal
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.model.toDbString
import com.strakk.shared.domain.repository.MealPhotoRepository
import com.strakk.shared.domain.repository.MealRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.call.body
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val LOG_TAG = "MealRepository"
private const val MEALS_EMBED_COLUMNS =
    "id,user_id,date,name,created_at," +
        "meal_entries(id,user_id,meal_id,log_date,name,protein,calories,fat,carbs," +
        "quantity,source,breakdown_json,photo_path,created_at)"

/**
 * Supabase-backed implementation of [MealRepository].
 *
 * Follows the reactive cache pattern established by [NutritionRepositoryImpl]:
 * - `observeMealsForDate` returns a [Flow] driven by an in-memory cache.
 * - The cache is lazily populated on first subscription per date.
 * - Mutations update both Supabase and the cache in one pass, so all observers
 *   see the change without re-fetching.
 */
internal class MealRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val userIdProvider: CurrentUserIdProvider,
    private val photoRepository: MealPhotoRepository,
    private val logger: Logger,
) : MealRepository {

    // Date-keyed cache: date → list of meals for that date.
    private val mealsCache = MutableStateFlow<Map<String, List<Meal>>>(emptyMap())

    // Flat index by meal ID for O(1) lookup — kept in sync via [updateCaches].
    private val mealIndex = MutableStateFlow<Map<String, Meal>>(emptyMap())

    private val fetchedDates = mutableSetOf<String>()
    private val fetchMutex = Mutex()

    /** Updates both caches atomically. All mutations must go through this function. */
    private fun updateCaches(newByDate: Map<String, List<Meal>>) {
        mealsCache.value = newByDate
        mealIndex.value = newByDate.values.flatten().associateBy { it.id }
    }

    override fun observeMealsForDate(date: String): Flow<List<Meal>> =
        mealsCache.map { it[date] ?: emptyList() }
            .distinctUntilChanged()
            .onStart { ensureFetched(date) }

    /** O(1) lookup by meal ID via the flat [mealIndex]. */
    override fun observeMeal(id: String): Flow<Meal?> =
        mealIndex.map { it[id] }
            .distinctUntilChanged()

    private suspend fun ensureFetched(date: String) {
        val shouldFetch = fetchMutex.withLock { fetchedDates.add(date) }
        if (shouldFetch) {
            val meals = fetchMealsForDate(date)
            updateCaches(trimCache(mealsCache.value + (date to meals)))
        }
    }

    /**
     * Removes cache entries for dates older than 30 days to prevent unbounded growth.
     */
    private fun trimCache(cache: Map<String, List<Meal>>): Map<String, List<Meal>> {
        val cutoff = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .minus(kotlinx.datetime.DatePeriod(days = 30))
        return cache.filterKeys { dateKey ->
            runCatching { LocalDate.parse(dateKey) >= cutoff }.getOrDefault(true)
        }
    }

    private suspend fun fetchMealsForDate(date: String): List<Meal> =
        supabaseClient
            .from("meals")
            .select(Columns.raw(MEALS_EMBED_COLUMNS)) {
                filter { eq("date", date) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<MealDto>()
            .map { it.toDomain() }

    override suspend fun createMeal(name: String, date: String): Meal {
        val userId = userIdProvider.currentOrThrow()
        val payload = buildJsonObject {
            put("user_id", userId)
            put("date", date)
            put("name", name)
        }

        val dto = supabaseClient
            .from("meals")
            .insert(payload) { select(Columns.raw(MEALS_EMBED_COLUMNS)) }
            .decodeSingle<MealDto>()

        val meal = dto.toDomain()
        val current = mealsCache.value
        updateCaches(current + (date to ((current[date] ?: emptyList()) + meal)))
        return meal
    }

    override suspend fun renameMeal(id: String, name: String) {
        supabaseClient
            .from("meals")
            .update({ set("name", name) }) { filter { eq("id", id) } }

        updateCaches(
            mealsCache.value.mapValues { (_, meals) ->
                meals.map { if (it.id == id) it.copy(name = name) else it }
            },
        )
    }

    override suspend fun deleteMeal(id: String) {
        // Gather photo paths before the cascade wipes meal_entries.
        val photoPaths = mealIndex.value[id]
            ?.entries
            ?.mapNotNull { it.photoPath }
            ?: emptyList()

        supabaseClient
            .from("meals")
            .delete { filter { eq("id", id) } }

        updateCaches(
            mealsCache.value.mapValues { (_, meals) -> meals.filterNot { it.id == id } },
        )

        if (photoPaths.isNotEmpty()) {
            try {
                photoRepository.deletePhotos(photoPaths)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(LOG_TAG, "Failed to cleanup photos for meal $id", e)
            }
        }
    }

    override suspend fun commitMealDraft(
        draftId: String,
        name: String,
        date: String,
        entries: List<DraftItem.Resolved>,
        photoPathsByItemId: Map<String, String>,
    ): Meal {
        require(entries.isNotEmpty()) { "Cannot commit an empty meal" }

        val userId = userIdProvider.currentOrThrow()

        // 1) INSERT the meal row.
        val mealPayload = buildJsonObject {
            put("user_id", userId)
            put("date", date)
            put("name", name)
        }
        val mealBase = supabaseClient
            .from("meals")
            .insert(mealPayload) { select() }
            .decodeSingle<MealDto>()

        // 2) INSERT all entries with meal_id / photo_path set.
        // Compensating transaction: if entries insert fails, delete the orphan meal.
        val insertedEntries = try {
            val entriesPayload = buildJsonArray {
                entries.forEach { resolved ->
                    add(
                        buildEntryPayload(
                            userId = userId,
                            mealId = mealBase.id,
                            logDate = date,
                            entry = resolved.entry,
                            photoPath = photoPathsByItemId[resolved.id],
                        ),
                    )
                }
            }
            supabaseClient
                .from("meal_entries")
                .insert(entriesPayload) { select() }
                .decodeList<MealEntryDto>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(LOG_TAG, "Entries insert failed for meal ${mealBase.id}, rolling back meal", e)
            try {
                supabaseClient.from("meals").delete { filter { eq("id", mealBase.id) } }
            } catch (rollbackEx: CancellationException) {
                throw rollbackEx
            } catch (rollbackEx: Exception) {
                logger.e(LOG_TAG, "Rollback of meal ${mealBase.id} also failed", rollbackEx)
            }
            throw DomainError.DataError("Failed to save meal entries. The meal was not created.", e)
        }

        val meal = mealBase
            .copy(mealEntries = insertedEntries.sortedBy { it.createdAt })
            .toDomain()

        val current = mealsCache.value
        updateCaches(current + (date to ((current[date] ?: emptyList()) + meal)))
        return meal
    }

    override suspend fun addEntryToMeal(mealId: String, entry: DraftItem.Resolved) {
        val userId = userIdProvider.currentOrThrow()
        val existing = mealIndex.value[mealId]
            ?: throw DomainError.DataError("Meal $mealId not found in cache")

        val payload = buildEntryPayload(
            userId = userId,
            mealId = mealId,
            logDate = existing.date,
            entry = entry.entry,
            photoPath = null,
        )
        val inserted = supabaseClient
            .from("meal_entries")
            .insert(payload) { select() }
            .decodeSingle<MealEntryDto>()
            .toDomain()

        updateCaches(
            mealsCache.value.mapValues { (_, meals) ->
                meals.map { m ->
                    if (m.id == mealId) m.copy(entries = m.entries + inserted) else m
                }
            },
        )
    }

    override fun updateEntryInCache(entry: MealEntry) {
        val mealId = entry.mealId ?: return
        updateCaches(
            mealsCache.value.mapValues { (_, meals) ->
                meals.map { meal ->
                    if (meal.id == mealId) {
                        meal.copy(entries = meal.entries.map { e ->
                            if (e.id == entry.id) entry else e
                        })
                    } else meal
                }
            },
        )
    }

    override fun clearCache() {
        updateCaches(emptyMap())
        fetchedDates.clear()
    }

    // -------------------------------------------------------------------------
    // Edge function calls
    // -------------------------------------------------------------------------

    override suspend fun extractMealDraftBatch(
        draftId: String,
        photoItems: List<MealRepository.ExtractPhotoItem>,
        textItems: List<MealRepository.ExtractTextItem>,
    ): List<MealRepository.ExtractItemResult> {
        refreshSession()

        val request = ExtractMealDraftRequestDto(
            items = buildList {
                photoItems.forEach {
                    add(ExtractDraftItemDto.photo(it.id, it.photoPath, it.hint))
                }
                textItems.forEach {
                    add(ExtractDraftItemDto.text(it.id, it.description))
                }
            },
        )

        val response = try {
            supabaseClient.functions.invoke("extract-meal-draft", body = request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(LOG_TAG, "extract-meal-draft invoke failed", e)
            throw DomainError.DataError("Failed to analyze meal items", e)
        }

        if (response.status.value !in 200..299) {
            throw DomainError.DataError("Analysis service returned HTTP ${response.status.value}")
        }

        val decoded = try {
            response.body<ExtractMealDraftResponseDto>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw DomainError.DataError("Unexpected extract-meal-draft response", e)
        }

        val photoIds = photoItems.map { it.id }.toHashSet()
        val now = isoNow()

        val successes = decoded.items.map { extracted ->
            val defaultSource =
                if (photoIds.contains(extracted.id)) EntrySource.PhotoAi
                else EntrySource.TextAi
            MealRepository.ExtractItemResult(
                id = extracted.id,
                resolvedItem = DraftItem.Resolved(
                    id = extracted.id,
                    entry = extracted.entry.toDomain(
                        id = extracted.id,
                        logDate = "",
                        source = defaultSource,
                        createdAt = now,
                    ),
                ),
                error = null,
            )
        }

        val failures = decoded.failures.map { failure ->
            MealRepository.ExtractItemResult(
                id = failure.id,
                resolvedItem = null,
                error = failure.reason,
            )
        }

        return successes + failures
    }

    override suspend fun analyzePhotoSingle(
        imageBase64: String,
        hint: String?,
        draftItemId: String,
    ): DraftItem.Resolved {
        refreshSession()
        val request = AnalyzeMealSingleRequestDto.photo(imageBase64, hint)
        val dto = invokeAnalyzeSingle(request)
        return DraftItem.Resolved(
            id = draftItemId,
            entry = dto.toDomain(
                id = draftItemId,
                logDate = "",
                source = EntrySource.PhotoAi,
                createdAt = isoNow(),
            ),
        )
    }

    override suspend fun analyzeTextSingle(
        description: String,
        draftItemId: String,
    ): DraftItem.Resolved {
        refreshSession()
        val request = AnalyzeMealSingleRequestDto.text(description)
        val dto = invokeAnalyzeSingle(request)
        return DraftItem.Resolved(
            id = draftItemId,
            entry = dto.toDomain(
                id = draftItemId,
                logDate = "",
                source = EntrySource.TextAi,
                createdAt = isoNow(),
            ),
        )
    }

    override suspend fun analyzePhotoForQuickAdd(
        imageBase64: String,
        hint: String?,
        logDate: String,
    ): MealEntry {
        refreshSession()
        val request = AnalyzeMealSingleRequestDto.photo(imageBase64, hint)
        val dto = invokeAnalyzeSingle(request)
        return dto.toDomain(
            id = "",
            logDate = logDate,
            source = EntrySource.PhotoAi,
            createdAt = "",
        )
    }

    override suspend fun analyzeTextForQuickAdd(
        description: String,
        logDate: String,
    ): MealEntry {
        refreshSession()
        val request = AnalyzeMealSingleRequestDto.text(description)
        val dto = invokeAnalyzeSingle(request)
        return dto.toDomain(
            id = "",
            logDate = logDate,
            source = EntrySource.TextAi,
            createdAt = "",
        )
    }

    private suspend fun invokeAnalyzeSingle(request: AnalyzeMealSingleRequestDto): AnalyzedEntryDto {
        val response = try {
            supabaseClient.functions.invoke("analyze-meal-single", body = request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(LOG_TAG, "analyze-meal-single invoke failed", e)
            throw DomainError.DataError("Failed to analyze meal item", e)
        }

        if (response.status.value !in 200..299) {
            throw DomainError.DataError("Analysis service returned HTTP ${response.status.value}")
        }

        return try {
            response.body()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw DomainError.DataError("Unexpected analyze-meal-single response", e)
        }
    }

    private suspend fun refreshSession() {
        supabaseClient.auth.currentSessionOrNull()
            ?: throw DomainError.AuthError("No active session. Please sign in again.")
        try {
            supabaseClient.auth.refreshCurrentSession()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(LOG_TAG, "Session refresh failed", e)
            throw DomainError.AuthError(
                "Your session expired. Please sign out and sign in again.",
                e,
            )
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildEntryPayload(
        userId: String,
        mealId: String?,
        logDate: String,
        entry: MealEntry,
        photoPath: String?,
    ): JsonObject = buildJsonObject {
        put("user_id", userId)
        if (mealId != null) put("meal_id", mealId)
        put("log_date", logDate)
        entry.name?.let { put("name", it) }
        put("protein", entry.protein)
        put("calories", entry.calories)
        entry.fat?.let { put("fat", it) }
        entry.carbs?.let { put("carbs", it) }
        entry.quantity?.let { put("quantity", it) }
        put("source", entry.source.toDbString())
        entry.breakdown?.toJsonString()?.let { put("breakdown_json", it) }
        photoPath?.let { put("photo_path", it) }
    }

    private fun isoNow(): String = kotlinx.datetime.Clock.System.now().toString()
}
