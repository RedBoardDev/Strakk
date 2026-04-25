package com.strakk.shared.data.repository

import com.russhwolf.settings.Settings
import com.strakk.shared.data.dto.ActiveMealDraftDto
import com.strakk.shared.data.mapper.toDomain
import com.strakk.shared.data.mapper.toDto
import com.strakk.shared.domain.common.Logger
import com.strakk.shared.domain.model.ActiveMealDraft
import com.strakk.shared.domain.model.DraftItem
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.repository.MealDraftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private const val LOG_TAG = "MealDraftRepository"
private const val ACTIVE_DRAFT_KEY = "active_meal_draft"

/**
 * Local-only implementation of [MealDraftRepository] backed by
 * `multiplatform-settings`.
 *
 * A single active draft is stored under the key [ACTIVE_DRAFT_KEY] as a JSON
 * string. The [cacheFlow] mirrors the persisted state and is re-emitted on
 * every mutation so ViewModels observe changes reactively.
 */
internal class MealDraftRepositoryImpl(
    private val settings: Settings,
    private val logger: Logger,
) : MealDraftRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }
    private val cacheFlow: MutableStateFlow<ActiveMealDraft?> =
        MutableStateFlow(loadPersisted())
    private val mutex = Mutex()

    override fun observeActiveDraft(): Flow<ActiveMealDraft?> = cacheFlow.asStateFlow()

    override suspend fun createDraft(name: String, date: String): ActiveMealDraft =
        mutex.withLock {
            val draft = ActiveMealDraft(
                id = generateId(),
                date = date,
                name = name,
                createdAt = Clock.System.now(),
                items = emptyList(),
                uploadedPaths = emptyMap(),
            )
            persist(draft)
            draft
        }

    override suspend fun addItem(item: DraftItem) = mutateOrThrow { current ->
        current.copy(items = current.items + item)
    }

    override suspend fun removeItem(itemId: String) = mutateOrThrow { current ->
        current.copy(
            items = current.items.filterNot { it.id == itemId },
            uploadedPaths = current.uploadedPaths - itemId,
        )
    }

    override suspend fun rename(name: String) = mutateOrThrow { current ->
        current.copy(name = name)
    }

    override suspend fun discard() = mutex.withLock {
        settings.remove(ACTIVE_DRAFT_KEY)
        cacheFlow.value = null
    }

    override suspend fun markItemResolved(itemId: String, entry: MealEntry) =
        mutateOrThrow { current ->
            val updatedItems = current.items.map { item ->
                if (item.id == itemId) DraftItem.Resolved(id = itemId, entry = entry)
                else item
            }
            current.copy(items = updatedItems)
        }

    override suspend fun recordUploadedPath(itemId: String, path: String) =
        mutateOrThrow { current ->
            current.copy(uploadedPaths = current.uploadedPaths + (itemId to path))
        }

    // -------------------------------------------------------------------------
    // Persistence helpers
    // -------------------------------------------------------------------------

    private suspend inline fun mutateOrThrow(transform: (ActiveMealDraft) -> ActiveMealDraft) {
        mutex.withLock {
            val current = cacheFlow.value
                ?: throw IllegalStateException("No active draft to mutate")
            persist(transform(current))
        }
    }

    private fun persist(draft: ActiveMealDraft) {
        val dto = draft.toDto()
        val encoded = json.encodeToString(ActiveMealDraftDto.serializer(), dto)
        settings.putString(ACTIVE_DRAFT_KEY, encoded)
        cacheFlow.value = draft
    }

    private fun loadPersisted(): ActiveMealDraft? {
        val raw = settings.getStringOrNull(ACTIVE_DRAFT_KEY) ?: return null
        return try {
            json.decodeFromString(ActiveMealDraftDto.serializer(), raw).toDomain()
        } catch (e: SerializationException) {
            logger.e(LOG_TAG, "Corrupt active_meal_draft, clearing", e)
            settings.remove(ACTIVE_DRAFT_KEY)
            null
        }
    }

    private fun generateId(): String {
        // Short, URL-safe, good enough for local uniqueness.
        // Draft IDs are never sent to Supabase as-is; server generates its own UUIDs on commit.
        val now = Clock.System.now().toEpochMilliseconds()
        val rand = (0..0xFFFF).random()
        return "draft-${now}-${rand.toString(16)}"
    }
}
