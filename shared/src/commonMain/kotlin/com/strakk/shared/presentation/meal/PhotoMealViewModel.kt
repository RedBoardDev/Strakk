package com.strakk.shared.presentation.meal

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.common.Logger
import com.strakk.shared.domain.model.AiPrediction
import com.strakk.shared.domain.model.Feature
import com.strakk.shared.domain.model.FeatureAccess
import com.strakk.shared.domain.model.GroundedMealItem
import com.strakk.shared.domain.model.MacroValues
import com.strakk.shared.domain.model.StructuredQuantity
import com.strakk.shared.domain.model.UnitType
import com.strakk.shared.domain.model.toDbString
import com.strakk.shared.domain.usecase.AdjustGroundedItemUseCase
import com.strakk.shared.domain.usecase.CheckFeatureAccessUseCase
import com.strakk.shared.domain.usecase.CleanupOrphanPhotosUseCase
import com.strakk.shared.domain.usecase.SaveGroundedMealUseCase
import com.strakk.shared.domain.usecase.ScanMealUseCase
import com.strakk.shared.domain.usecase.SwapFoodMatchUseCase
import com.strakk.shared.domain.usecase.UploadMealPhotoUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random

private const val LOG_TAG = "PhotoMealVM"
private const val GRAMS_PER_100 = 100.0
private const val ANALYSIS_ERROR_MESSAGE = "We couldn't analyze this meal. Please try again."
private const val ANALYSIS_TEMPORARILY_UNAVAILABLE =
    "The AI scanner is temporarily unavailable. Please try again later."
private const val ANALYSIS_QUOTA_EXHAUSTED = "The AI scanner is temporarily busy. Please try again later."
private const val NO_FOODS_DETECTED = "No food items detected. Try a clearer photo or add manually."
private const val SAVE_ERROR = "Could not save the meal. Please try again."
private const val MEAL_NAME_MAX_LENGTH = 60

/**
 * Orchestrates the photo-to-meal analysis flow.
 *
 * The flow covers two entry paths:
 * - **Photo path**: upload → scan (identify + ground in one VPS call) → review → save.
 * - **Text path**: scan (text-only) → review → save.
 *
 * State stays in memory only — [com.strakk.shared.domain.model.GroundedMealItem]
 * items are never locally persisted before the explicit [PhotoMealEvent.Save].
 *
 * On failure or [PhotoMealEvent.Cancel], any uploaded photo paths are cleaned up
 * to avoid orphan storage objects.
 */
@Suppress("TooManyFunctions", "TooGenericExceptionCaught", "LongParameterList")
class PhotoMealViewModel(
    private val scanMeal: ScanMealUseCase,
    private val saveMeal: SaveGroundedMealUseCase,
    private val adjustGroundedItem: AdjustGroundedItemUseCase,
    private val swapFoodMatch: SwapFoodMatchUseCase,
    private val uploadMealPhoto: UploadMealPhotoUseCase,
    private val cleanupOrphanPhotos: CleanupOrphanPhotosUseCase,
    private val checkFeatureAccess: CheckFeatureAccessUseCase,
    private val logger: Logger,
) : MviViewModel<PhotoMealUiState, PhotoMealEvent, PhotoMealEffect>(PhotoMealUiState.Capturing) {

    /** Current target meal date in "yyyy-MM-dd" format. */
    private var currentDate: String = ""

    /** AiPrediction.photoIndex → Storage path map for grounding/save. */
    private var photoPathByPhotoIndex: Map<Int, String> = emptyMap()

    override fun onEvent(event: PhotoMealEvent) = when (event) {
        is PhotoMealEvent.StartAnalysis -> handleStartAnalysis(event)
        is PhotoMealEvent.StartTextAnalysis -> handleStartTextAnalysis(event)
        is PhotoMealEvent.AdjustQuantity -> handleAdjustQuantity(event)
        is PhotoMealEvent.ChangeCookingMethod -> handleChangeCookingMethod(event)
        is PhotoMealEvent.SwapFood -> handleSwapFood(event)
        is PhotoMealEvent.RemoveItem -> handleRemoveItem(event)
        is PhotoMealEvent.HideItem -> handleHideItem(event)
        is PhotoMealEvent.RestoreItem -> handleRestoreItem(event)
        is PhotoMealEvent.AddManualItem -> handleAddManualItem(event)
        is PhotoMealEvent.AddSearchItem -> handleAddSearchItem(event)
        is PhotoMealEvent.EditItemMacros -> handleEditItemMacros(event)
        PhotoMealEvent.Save -> handleSave()
        PhotoMealEvent.Cancel -> handleCancel()
    }

    // -------------------------------------------------------------------------
    // Photo analysis
    // -------------------------------------------------------------------------

    private fun handleStartAnalysis(event: PhotoMealEvent.StartAnalysis) {
        viewModelScope.launch {
            val access = checkFeatureAccess(Feature.AI_PHOTO_ANALYSIS)
            if (access !is FeatureAccess.Granted) {
                emit(PhotoMealEffect.FeatureGated(access))
                return@launch
            }

            currentDate = event.date
            setState { PhotoMealUiState.Identifying(hint = event.hint) }

            try {
                // Step 1 — upload photo to Storage.
                val itemId = Random.nextLong().toString()
                val storagePath = uploadMealPhoto(
                    draftId = event.date,
                    itemId = itemId,
                    base64 = event.imageBase64,
                ).getOrThrow()
                photoPathByPhotoIndex = mapOf(0 to storagePath)

                // Step 2 — scan: identify + ground in one VPS call.
                val result = scanMeal(
                    photoStoragePaths = listOf(storagePath),
                    hint = event.hint,
                ).getOrThrow()

                if (result.items.isEmpty()) {
                    cleanupPhotos()
                    setState { PhotoMealUiState.Capturing }
                    emit(PhotoMealEffect.ShowError(NO_FOODS_DETECTED))
                    return@launch
                }

                setState {
                    PhotoMealUiState.Reviewing(
                        items = result.items,
                        mealName = generateMealName(result.items),
                    )
                }
            } catch (e: CancellationException) {
                cleanupPhotos()
                throw e
            } catch (e: Exception) {
                logger.e(LOG_TAG, "Photo analysis failed", e)
                cleanupPhotos()
                setState { PhotoMealUiState.Capturing }
                emit(PhotoMealEffect.ShowError(e.toAnalysisMessage()))
            }
        }
    }

    // -------------------------------------------------------------------------
    // Text-only analysis
    // -------------------------------------------------------------------------

    private fun handleStartTextAnalysis(event: PhotoMealEvent.StartTextAnalysis) {
        viewModelScope.launch {
            val access = checkFeatureAccess(Feature.AI_TEXT_ANALYSIS)
            if (access !is FeatureAccess.Granted) {
                emit(PhotoMealEffect.FeatureGated(access))
                return@launch
            }

            currentDate = event.date
            photoPathByPhotoIndex = emptyMap()
            setState { PhotoMealUiState.Identifying(hint = event.description) }

            try {
                val result = scanMeal(
                    photoStoragePaths = emptyList(),
                    hint = event.description,
                    isTextOnly = true,
                ).getOrThrow()

                if (result.items.isEmpty()) {
                    setState { PhotoMealUiState.Capturing }
                    emit(PhotoMealEffect.ShowError(NO_FOODS_DETECTED))
                    return@launch
                }

                setState {
                    PhotoMealUiState.Reviewing(
                        items = result.items,
                        mealName = generateMealName(result.items),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(LOG_TAG, "Text analysis failed", e)
                setState { PhotoMealUiState.Capturing }
                emit(PhotoMealEffect.ShowError(e.toAnalysisMessage()))
            }
        }
    }

    // -------------------------------------------------------------------------
    // Review interactions (pure / local)
    // -------------------------------------------------------------------------

    private fun handleAdjustQuantity(event: PhotoMealEvent.AdjustQuantity) {
        val state = uiState.value as? PhotoMealUiState.Reviewing ?: return
        if (event.itemIndex !in state.items.indices) return
        val updated = adjustGroundedItem(state.items[event.itemIndex], newGrams = event.newGrams)
        setState {
            (this as PhotoMealUiState.Reviewing).copy(
                items = items.toMutableList().apply { set(event.itemIndex, updated) },
            )
        }
    }

    private fun handleChangeCookingMethod(event: PhotoMealEvent.ChangeCookingMethod) {
        val state = uiState.value as? PhotoMealUiState.Reviewing ?: return
        if (event.itemIndex !in state.items.indices) return
        val updated = adjustGroundedItem(state.items[event.itemIndex], newCookingMethod = event.method)
        setState {
            (this as PhotoMealUiState.Reviewing).copy(
                items = items.toMutableList().apply { set(event.itemIndex, updated) },
            )
        }
    }

    private fun handleSwapFood(event: PhotoMealEvent.SwapFood) {
        val state = uiState.value as? PhotoMealUiState.Reviewing ?: return
        if (event.itemIndex !in state.items.indices) return
        val updated = swapFoodMatch(state.items[event.itemIndex], event.newMatch)
        setState {
            (this as PhotoMealUiState.Reviewing).copy(
                items = items.toMutableList().apply { set(event.itemIndex, updated) },
            )
        }
    }

    private fun handleRemoveItem(event: PhotoMealEvent.RemoveItem) {
        val state = uiState.value as? PhotoMealUiState.Reviewing ?: return
        if (event.itemIndex !in state.items.indices) return
        val remaining = state.items.filterIndexed { i, _ -> i != event.itemIndex }
        if (remaining.isEmpty()) {
            // Last item removed → clean up uploaded photos and reset to start.
            viewModelScope.launch { cleanupPhotos() }
            setState { PhotoMealUiState.Capturing }
        } else {
            setState { (this as PhotoMealUiState.Reviewing).copy(items = remaining) }
        }
    }

    private fun handleHideItem(event: PhotoMealEvent.HideItem) {
        val state = uiState.value as? PhotoMealUiState.Reviewing ?: return
        if (event.itemIndex !in state.items.indices) return
        setState {
            (this as PhotoMealUiState.Reviewing).copy(
                items = items.toMutableList().apply {
                    set(event.itemIndex, get(event.itemIndex).copy(isHidden = true))
                },
            )
        }
    }

    private fun handleRestoreItem(event: PhotoMealEvent.RestoreItem) {
        val state = uiState.value as? PhotoMealUiState.Reviewing ?: return
        if (event.itemIndex !in state.items.indices) return
        setState {
            (this as PhotoMealUiState.Reviewing).copy(
                items = items.toMutableList().apply {
                    set(event.itemIndex, get(event.itemIndex).copy(isHidden = false))
                },
            )
        }
    }

    private fun handleAddManualItem(event: PhotoMealEvent.AddManualItem) {
        if (uiState.value !is PhotoMealUiState.Reviewing) return
        val newItem = GroundedMealItem(
            prediction = AiPrediction(photoIndex = 0, name = event.name, unit = "g", amount = event.grams),
            catalogMatch = null,
            catalogMatchId = null,
            groundingSource = null,
            similarity = 0.0,
            quantity = StructuredQuantity(
                grams = event.grams,
                displayLabel = "${event.grams.toInt()}g",
                unitType = UnitType.Grams,
            ),
            computedMacros = MacroValues(
                kcal = event.kcal,
                protein = event.protein,
                fat = event.fat,
                carbs = event.carbs,
            ),
            isGrounded = false,
            aiConfidence = 0.0,
            isManuallyAdded = true,
        )
        setState { (this as PhotoMealUiState.Reviewing).copy(items = items + newItem) }
    }

    private fun handleAddSearchItem(event: PhotoMealEvent.AddSearchItem) {
        if (uiState.value !is PhotoMealUiState.Reviewing) return
        val match = event.match
        val factor = event.grams / GRAMS_PER_100
        val newItem = GroundedMealItem(
            prediction = AiPrediction(photoIndex = 0, name = match.name, unit = "g", amount = event.grams),
            catalogMatch = match,
            catalogMatchId = match.id,
            groundingSource = match.source.toDbString(),
            similarity = 1.0,
            quantity = StructuredQuantity(
                grams = event.grams,
                displayLabel = "${event.grams.toInt()}g",
                unitType = UnitType.Grams,
            ),
            computedMacros = MacroValues(
                kcal = match.calories * factor,
                protein = match.protein * factor,
                fat = (match.fat ?: 0.0) * factor,
                carbs = (match.carbs ?: 0.0) * factor,
            ),
            isGrounded = true,
            aiConfidence = 1.0,
            isManuallyAdded = true,
        )
        setState { (this as PhotoMealUiState.Reviewing).copy(items = items + newItem) }
    }

    private fun handleEditItemMacros(event: PhotoMealEvent.EditItemMacros) {
        val state = uiState.value as? PhotoMealUiState.Reviewing ?: return
        if (event.itemIndex !in state.items.indices) return
        val item = state.items[event.itemIndex]
        val updated = item.copy(
            prediction = item.prediction.copy(name = event.name),
            quantity = item.quantity.copy(
                grams = event.grams,
                displayLabel = "${event.grams.toInt()}g",
            ),
            computedMacros = MacroValues(
                kcal = event.kcal,
                protein = event.protein,
                fat = event.fat,
                carbs = event.carbs,
            ),
        )
        setState {
            (this as PhotoMealUiState.Reviewing).copy(
                items = items.toMutableList().apply { set(event.itemIndex, updated) },
            )
        }
    }

    // -------------------------------------------------------------------------
    // Save
    // -------------------------------------------------------------------------

    private fun handleSave() {
        val state = uiState.value as? PhotoMealUiState.Reviewing ?: return
        if (state.isSaving) return

        val itemsToSave = state.items.filter { !it.isHidden }
        if (itemsToSave.isEmpty()) return

        setState { (this as PhotoMealUiState.Reviewing).copy(isSaving = true) }

        viewModelScope.launch {
            saveMeal(
                name = state.mealName,
                date = currentDate,
                items = itemsToSave,
                photoPathByPhotoIndex = photoPathByPhotoIndex,
            )
                .onSuccess { meal ->
                    // Photos are now owned by the meal — do not delete from Storage.
                    photoPathByPhotoIndex = emptyMap()
                    emit(PhotoMealEffect.MealSaved(meal))
                }
                .onFailure { throwable ->
                    logger.e(LOG_TAG, "Save meal failed", throwable)
                    setState { (this as PhotoMealUiState.Reviewing).copy(isSaving = false) }
                    emit(PhotoMealEffect.ShowError(SAVE_ERROR))
                }
        }
    }

    // -------------------------------------------------------------------------
    // Meal name generation
    // -------------------------------------------------------------------------

    private fun generateMealName(items: List<GroundedMealItem>): String = when (items.size) {
        0 -> "Photo meal"
        1 -> items[0].prediction.name.replaceFirstChar { it.uppercaseChar() }.take(MEAL_NAME_MAX_LENGTH)
        else -> {
            val first = items[0].prediction.name.replaceFirstChar { it.uppercaseChar() }
            val second = items[1].prediction.name
            "$first & $second".take(MEAL_NAME_MAX_LENGTH)
        }
    }

    // -------------------------------------------------------------------------
    // Cancel
    // -------------------------------------------------------------------------

    private fun handleCancel() {
        viewModelScope.launch {
            cleanupPhotos()
            setState { PhotoMealUiState.Capturing }
            emit(PhotoMealEffect.Cancelled)
        }
    }

    // -------------------------------------------------------------------------
    // Cleanup
    // -------------------------------------------------------------------------

    /**
     * Best-effort cleanup of uploaded photos that have not yet been attached to
     * a saved meal. Resets [photoPathByPhotoIndex] regardless of outcome.
     */
    private suspend fun cleanupPhotos() {
        val paths = photoPathByPhotoIndex.values.toList()
        photoPathByPhotoIndex = emptyMap()
        cleanupOrphanPhotos(paths)
    }

    private fun Throwable.toAnalysisMessage(): String = message
        ?.takeIf { it == ANALYSIS_QUOTA_EXHAUSTED || it == ANALYSIS_TEMPORARILY_UNAVAILABLE }
        ?: ANALYSIS_ERROR_MESSAGE
}
