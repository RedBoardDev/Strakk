package com.strakk.shared.data.repository

import com.strakk.shared.data.dto.FavoriteFoodDto
import com.strakk.shared.data.dto.FavoriteMealDto
import com.strakk.shared.data.dto.RecentFoodRowDto
import com.strakk.shared.data.dto.RecentMealRowDto
import com.strakk.shared.data.mapper.FavoritesMapper.encodeItems
import com.strakk.shared.data.mapper.FavoritesMapper.toDomain
import com.strakk.shared.data.mapper.FavoritesMapper.toTemplateItem
import com.strakk.shared.data.remote.CurrentUserIdProvider
import com.strakk.shared.domain.common.Logger
import com.strakk.shared.domain.model.FavoriteFood
import com.strakk.shared.domain.model.FavoriteMeal
import com.strakk.shared.domain.model.Meal
import com.strakk.shared.domain.model.MealTemplateItem
import com.strakk.shared.domain.model.RecentMeal
import com.strakk.shared.domain.repository.FavoritesRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val LOG_TAG = "FavoritesRepository"

@Suppress("TooManyFunctions")
internal class FavoritesRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val userIdProvider: CurrentUserIdProvider,
    private val logger: Logger,
) : FavoritesRepository {

    private val foodsCache = MutableStateFlow<List<FavoriteFood>?>(null)
    private val mealsCache = MutableStateFlow<List<FavoriteMeal>?>(null)
    private val foodsFetchMutex = Mutex()
    private val mealsFetchMutex = Mutex()

    override fun observeFavoriteFoods(): Flow<List<FavoriteFood>> = foodsFlow().onStart { ensureFoodsFetched() }

    override fun observeFavoriteMeals(): Flow<List<FavoriteMeal>> = mealsFlow().onStart { ensureMealsFetched() }

    override fun clearCache() {
        foodsCache.value = null
        mealsCache.value = null
    }

    // ---------- Favorite foods ----------

    @Suppress("LongParameterList")
    override suspend fun addFavoriteFood(
        name: String,
        protein: Double,
        calories: Double,
        fat: Double?,
        carbs: Double?,
        quantity: String?,
        foodCatalogId: Long?,
    ): FavoriteFood {
        val userId = userIdProvider.currentOrThrow()
        val trimmed = name.trim()
        val normalized = normalize(trimmed)

        val payload = buildJsonObject {
            put("user_id", userId)
            put("name", trimmed)
            put("name_normalized", normalized)
            put("protein", protein)
            put("calories", calories)
            fat?.let { put("fat", it) }
            carbs?.let { put("carbs", it) }
            quantity?.let { put("quantity", it) }
            foodCatalogId?.let { put("food_catalog_id", it) }
        }

        val created = supabaseClient
            .from("favorite_foods")
            .upsert(payload) {
                onConflict = "user_id,name_normalized"
                select()
            }
            .decodeSingle<FavoriteFoodDto>()
            .toDomain()

        foodsCache.value = (foodsCache.value.orEmpty().filterNot { it.normalizedName == normalized } + created)
            .sortedByDescending { it.createdAt }
        return created
    }

    override suspend fun removeFavoriteFoodByName(normalizedName: String) {
        supabaseClient
            .from("favorite_foods")
            .delete { filter { eq("name_normalized", normalizedName) } }
        foodsCache.value = foodsCache.value.orEmpty().filterNot { it.normalizedName == normalizedName }
    }

    // ---------- Favorite meals ----------

    override suspend fun addFavoriteMeal(meal: Meal): FavoriteMeal {
        val userId = userIdProvider.currentOrThrow()
        val items: List<MealTemplateItem> = meal.entries.map { entry ->
            MealTemplateItem(
                name = entry.name.orEmpty().ifBlank { "Item" },
                protein = entry.protein,
                calories = entry.calories,
                fat = entry.fat,
                carbs = entry.carbs,
                quantity = entry.quantity,
            )
        }

        val payload = buildJsonObject {
            put("user_id", userId)
            put("name", meal.name)
            put("items_json", encodeItems(items))
            put("source_meal_id", meal.id)
        }

        val created = supabaseClient
            .from("favorite_meals")
            .insert(payload) { select() }
            .decodeSingle<FavoriteMealDto>()
            .toDomain()

        mealsCache.value = (listOf(created) + mealsCache.value.orEmpty())
        return created
    }

    override suspend fun removeFavoriteMealBySourceId(sourceMealId: String) {
        supabaseClient
            .from("favorite_meals")
            .delete { filter { eq("source_meal_id", sourceMealId) } }
        mealsCache.value = mealsCache.value.orEmpty().filterNot { it.sourceMealId == sourceMealId }
    }

    override suspend fun removeFavoriteMealById(id: String) {
        supabaseClient
            .from("favorite_meals")
            .delete { filter { eq("id", id) } }
        mealsCache.value = mealsCache.value.orEmpty().filterNot { it.id == id }
    }

    // ---------- Recents ----------

    override suspend fun loadRecentMeals(daysWindow: Int, limit: Int): List<RecentMeal> {
        val params = buildJsonObject {
            put("days_window", daysWindow)
            put("max_rows", limit)
        }
        return try {
            supabaseClient.postgrest
                .rpc("recent_meals_v1", params)
                .decodeList<RecentMealRowDto>()
                .map { it.toDomain() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(LOG_TAG, "recent_meals_v1 failed", e)
            emptyList()
        }
    }

    override suspend fun loadRecentFoods(daysWindow: Int, limit: Int): List<MealTemplateItem> {
        val params = buildJsonObject {
            put("days_window", daysWindow)
            put("max_rows", limit)
        }
        return try {
            supabaseClient.postgrest
                .rpc("recent_foods_v1", params)
                .decodeList<RecentFoodRowDto>()
                .map { it.toTemplateItem() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(LOG_TAG, "recent_foods_v1 failed", e)
            emptyList()
        }
    }

    // ---------- internals ----------

    private fun foodsFlow(): Flow<List<FavoriteFood>> = kotlinx.coroutines.flow.flow {
        foodsCache.collect { value -> emit(value.orEmpty()) }
    }.distinctUntilChanged()

    private fun mealsFlow(): Flow<List<FavoriteMeal>> = kotlinx.coroutines.flow.flow {
        mealsCache.collect { value -> emit(value.orEmpty()) }
    }.distinctUntilChanged()

    private suspend fun ensureFoodsFetched() {
        val shouldFetch = foodsFetchMutex.withLock {
            val needed = foodsCache.value == null
            if (needed) foodsCache.value = emptyList()
            needed
        }
        if (!shouldFetch) return
        try {
            val rows = supabaseClient
                .from("favorite_foods")
                .select { order("created_at", Order.DESCENDING) }
                .decodeList<FavoriteFoodDto>()
                .map { it.toDomain() }
            foodsCache.value = rows
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(LOG_TAG, "favorite_foods fetch failed", e)
        }
    }

    private suspend fun ensureMealsFetched() {
        val shouldFetch = mealsFetchMutex.withLock {
            val needed = mealsCache.value == null
            if (needed) mealsCache.value = emptyList()
            needed
        }
        if (!shouldFetch) return
        try {
            val rows = supabaseClient
                .from("favorite_meals")
                .select { order("created_at", Order.DESCENDING) }
                .decodeList<FavoriteMealDto>()
                .map { it.toDomain() }
            mealsCache.value = rows
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(LOG_TAG, "favorite_meals fetch failed", e)
        }
    }

    private fun normalize(text: String): String = normalizeName(text)
}

private fun normalizeName(text: String): String = text.lowercase().map { ch ->
    when (ch) {
        'à', 'â', 'ä' -> 'a'
        'é', 'è', 'ê', 'ë' -> 'e'
        'î', 'ï' -> 'i'
        'ô', 'ö' -> 'o'
        'ù', 'û', 'ü' -> 'u'
        'ç' -> 'c'
        else -> ch
    }
}.joinToString("")
