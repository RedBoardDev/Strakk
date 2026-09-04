package com.strakk.shared.data.mapper

import com.strakk.shared.data.dto.FavoriteFoodDto
import com.strakk.shared.data.dto.FavoriteMealDto
import com.strakk.shared.data.dto.MealTemplateItemDto
import com.strakk.shared.data.dto.RecentFoodRowDto
import com.strakk.shared.data.dto.RecentMealRowDto
import com.strakk.shared.domain.model.FavoriteFood
import com.strakk.shared.domain.model.FavoriteMeal
import com.strakk.shared.domain.model.MealTemplateItem
import com.strakk.shared.domain.model.RecentMeal
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

internal object FavoritesMapper {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun FavoriteFoodDto.toDomain(): FavoriteFood = FavoriteFood(
        id = requireNotNull(id) { "favorite_foods row without id" },
        name = name,
        normalizedName = nameNormalized,
        protein = protein,
        calories = calories,
        fat = fat,
        carbs = carbs,
        quantity = quantity,
        foodCatalogId = foodCatalogId,
        createdAt = createdAt?.parseInstantOrNow() ?: nowInstant(),
    )

    fun FavoriteMealDto.toDomain(): FavoriteMeal = FavoriteMeal(
        id = requireNotNull(id) { "favorite_meals row without id" },
        name = name,
        items = decodeTemplateItems(itemsJson),
        sourceMealId = sourceMealId,
        createdAt = createdAt?.parseInstantOrNow() ?: nowInstant(),
    )

    fun RecentMealRowDto.toDomain(): RecentMeal = RecentMeal(
        mealId = mealId,
        name = name,
        items = decodeTemplateItems(items),
        createdAt = createdAt.parseInstantOrNow(),
    )

    fun RecentFoodRowDto.toTemplateItem(): MealTemplateItem = MealTemplateItem(
        name = name ?: nameNormalized,
        protein = protein,
        calories = calories,
        fat = fat,
        carbs = carbs,
        quantity = quantity,
    )

    fun MealTemplateItem.toDto(): MealTemplateItemDto = MealTemplateItemDto(
        name = name,
        protein = protein,
        calories = calories,
        fat = fat,
        carbs = carbs,
        quantity = quantity,
    )

    fun encodeItems(items: List<MealTemplateItem>): JsonElement = json.encodeToJsonElement(
        kotlinx.serialization.builtins.ListSerializer(MealTemplateItemDto.serializer()),
        items.map { it.toDto() },
    )

    private fun decodeTemplateItems(element: JsonElement): List<MealTemplateItem> {
        val array = element as? JsonArray ?: return emptyList()
        return array.map { node ->
            val dto = json.decodeFromJsonElement(MealTemplateItemDto.serializer(), node)
            MealTemplateItem(
                name = dto.name,
                protein = dto.protein,
                calories = dto.calories,
                fat = dto.fat,
                carbs = dto.carbs,
                quantity = dto.quantity,
            )
        }
    }

    private fun String.parseInstantOrNow(): Instant = runCatching { Instant.parse(this) }
        .getOrElse { nowInstant() }

    private fun nowInstant(): Instant = kotlinx.datetime.Clock.System.now()
}
