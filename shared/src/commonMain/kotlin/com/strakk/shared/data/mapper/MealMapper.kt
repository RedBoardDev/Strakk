package com.strakk.shared.data.mapper

import com.strakk.shared.data.dto.ActiveMealDraftDto
import com.strakk.shared.data.dto.AnalyzedEntryDto
import com.strakk.shared.data.dto.BreakdownItemDto
import com.strakk.shared.data.dto.DraftItemDto
import com.strakk.shared.data.dto.ExtractedItemDto
import com.strakk.shared.data.dto.FoodCatalogItemDto
import com.strakk.shared.data.dto.MealDto
import com.strakk.shared.data.dto.MealEntryDto
import com.strakk.shared.domain.model.ActiveMealDraft
import com.strakk.shared.domain.model.BreakdownItem
import com.strakk.shared.domain.model.DraftItem
import com.strakk.shared.domain.model.EntrySource
import com.strakk.shared.domain.model.FoodCatalogItem
import com.strakk.shared.domain.model.Meal
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.model.MealStatus
import com.strakk.shared.domain.model.toDbString
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

private val breakdownJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

// =============================================================================
// Meal + nested entries
// =============================================================================

internal fun MealDto.toDomain(): Meal = Meal(
    id = id,
    userId = userId,
    date = date,
    name = name,
    status = MealStatus.Processed,
    createdAt = Instant.parse(createdAt),
    entries = mealEntries
        .sortedBy { it.createdAt }
        .map { it.toDomain() },
)

internal fun MealEntryDto.toDomain(): MealEntry = MealEntry(
    id = id,
    logDate = logDate,
    name = name,
    protein = protein,
    calories = calories,
    fat = fat,
    carbs = carbs,
    source = EntrySource.fromString(source),
    createdAt = createdAt,
    mealId = mealId,
    quantity = quantity,
    breakdown = parseBreakdownJson(breakdownJson),
    photoPath = photoPath,
)

private fun parseBreakdownJson(raw: String?): List<BreakdownItem>? {
    if (raw.isNullOrBlank()) return null
    return runCatching {
        breakdownJson
            .decodeFromString<List<BreakdownItemDto>>(raw)
            .map { it.toDomain() }
    }.getOrNull()
}

internal fun List<BreakdownItem>.toJsonString(): String? {
    if (isEmpty()) return null
    return runCatching {
        breakdownJson.encodeToString(map { it.toDto() })
    }.getOrNull()
}

internal fun BreakdownItemDto.toDomain(): BreakdownItem = BreakdownItem(
    name = name,
    protein = proteinG,
    calories = caloriesKcal,
    fat = fatG,
    carbs = carbsG,
    quantity = quantity,
)

internal fun BreakdownItem.toDto(): BreakdownItemDto = BreakdownItemDto(
    name = name,
    proteinG = protein,
    caloriesKcal = calories,
    fatG = fat,
    carbsG = carbs,
    quantity = quantity,
)

// =============================================================================
// Food catalog
// =============================================================================

internal fun FoodCatalogItemDto.toDomain(): FoodCatalogItem = FoodCatalogItem(
    id = id,
    name = name,
    protein = protein,
    calories = calories,
    fat = fat,
    carbs = carbs,
    defaultPortionGrams = defaultPortionGrams,
)

// =============================================================================
// Analyzed entries (edge function responses)
// =============================================================================

/**
 * Converts a Claude response to a domain [MealEntry] ready to be persisted.
 *
 * @param id Local ID (usually the draft item ID) to assign to the resulting entry.
 * @param logDate Date the entry belongs to.
 * @param source Chosen by the caller according to context ([EntrySource.PhotoAi]
 *   for photo analyses, [EntrySource.TextAi] for text).
 * @param mealId Optional parent meal ID.
 */
internal fun AnalyzedEntryDto.toDomain(
    id: String,
    logDate: String,
    source: EntrySource,
    mealId: String? = null,
    createdAt: String = "",
): MealEntry = MealEntry(
    id = id,
    logDate = logDate,
    name = name,
    protein = proteinG,
    calories = caloriesKcal,
    fat = fatG,
    carbs = carbsG,
    source = source,
    createdAt = createdAt,
    mealId = mealId,
    quantity = quantity,
    breakdown = breakdown?.map { it.toDomain() }?.ifEmpty { null },
)

/**
 * Converts a batch extracted item to a Resolved draft item.
 *
 * @param logDate Draft's date.
 * @param defaultSource Fallback source when the caller has no better signal —
 *   typically [EntrySource.PhotoAi] since batches only carry pending IA items.
 */
internal fun ExtractedItemDto.toResolved(
    logDate: String,
    defaultSource: EntrySource,
): DraftItem.Resolved = DraftItem.Resolved(
    id = id,
    entry = entry.toDomain(
        id = id,
        logDate = logDate,
        source = defaultSource,
    ),
)

// =============================================================================
// Active draft (local persistence)
// =============================================================================

internal fun ActiveMealDraft.toDto(): ActiveMealDraftDto = ActiveMealDraftDto(
    id = id,
    date = date,
    name = name,
    createdAt = createdAt.toString(),
    items = items.map { it.toDto() },
    uploadedPaths = uploadedPaths,
)

internal fun ActiveMealDraftDto.toDomain(): ActiveMealDraft = ActiveMealDraft(
    id = id,
    date = date,
    name = name,
    createdAt = Instant.parse(createdAt),
    items = items.map { it.toDomain() },
    uploadedPaths = uploadedPaths,
)

internal fun DraftItem.toDto(): DraftItemDto = when (this) {
    is DraftItem.Resolved -> DraftItemDto(
        id = id,
        type = "resolved",
        entry = entry.toDto(),
    )
    is DraftItem.PendingPhoto -> DraftItemDto(
        id = id,
        type = "pending_photo",
        imageBase64 = imageBase64,
        hint = hint,
    )
    is DraftItem.PendingText -> DraftItemDto(
        id = id,
        type = "pending_text",
        description = description,
    )
}

internal fun DraftItemDto.toDomain(): DraftItem = when (type) {
    "resolved" -> DraftItem.Resolved(
        id = id,
        entry = requireNotNull(entry) { "resolved DraftItem missing entry" }.toDomain(),
    )
    "pending_photo" -> DraftItem.PendingPhoto(
        id = id,
        imageBase64 = requireNotNull(imageBase64) { "pending_photo DraftItem missing imageBase64" },
        hint = hint,
    )
    "pending_text" -> DraftItem.PendingText(
        id = id,
        description = requireNotNull(description) { "pending_text DraftItem missing description" },
    )
    else -> error("Unknown DraftItemDto.type: $type")
}

/**
 * Reverse mapper for persisting a [MealEntry] inside an [ActiveMealDraftDto].
 */
internal fun MealEntry.toDto(): MealEntryDto = MealEntryDto(
    id = id,
    userId = "", // not used locally; server fills on commit
    logDate = logDate,
    name = name,
    protein = protein,
    calories = calories,
    fat = fat,
    carbs = carbs,
    source = source.toDbString(),
    createdAt = createdAt,
    mealId = mealId,
    quantity = quantity,
    breakdownJson = breakdown?.toJsonString(),
    photoPath = null,
)
