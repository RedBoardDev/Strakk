package com.strakk.shared.presentation.meal

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.common.ClockProvider
import com.strakk.shared.domain.common.DomainError
import com.strakk.shared.domain.model.DraftItem
import com.strakk.shared.domain.model.Feature
import com.strakk.shared.domain.model.FeatureAccess
import com.strakk.shared.domain.model.MealEntryInput
import com.strakk.shared.domain.usecase.AddItemToDraftUseCase
import com.strakk.shared.domain.usecase.BuildMealEntryUseCase
import com.strakk.shared.domain.usecase.CheckFeatureAccessUseCase
import com.strakk.shared.domain.usecase.CommitMealDraftUseCase
import com.strakk.shared.domain.usecase.CreateMealDraftUseCase
import com.strakk.shared.domain.usecase.DiscardMealDraftUseCase
import com.strakk.shared.domain.usecase.ObserveActiveMealDraftUseCase
import com.strakk.shared.domain.usecase.ProcessMealDraftUseCase
import com.strakk.shared.domain.usecase.RemoveItemFromDraftUseCase
import com.strakk.shared.domain.usecase.RenameMealDraftUseCase
import com.strakk.shared.domain.usecase.UpdateDraftItemUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Orchestrates the active meal draft lifecycle.
 *
 * Observes [ObserveActiveMealDraftUseCase] to keep [uiState] synced with the
 * local persisted draft. The UI can start a new draft, add/remove items,
 * rename, discard, process (IA batch), and commit (server-side persistence).
 */
@Suppress("LongParameterList")
class MealDraftViewModel(
    private val observeActiveDraft: ObserveActiveMealDraftUseCase,
    private val createDraft: CreateMealDraftUseCase,
    private val addItem: AddItemToDraftUseCase,
    private val removeItem: RemoveItemFromDraftUseCase,
    private val renameDraft: RenameMealDraftUseCase,
    private val discardDraft: DiscardMealDraftUseCase,
    private val processDraft: ProcessMealDraftUseCase,
    private val commitDraft: CommitMealDraftUseCase,
    private val updateDraftItem: UpdateDraftItemUseCase,
    private val buildMealEntry: BuildMealEntryUseCase,
    private val checkFeatureAccess: CheckFeatureAccessUseCase,
    private val clock: ClockProvider,
) : MviViewModel<MealDraftUiState, MealDraftEvent, MealDraftEffect>(MealDraftUiState.Loading) {

    init {
        observeActiveDraft()
            .onEach { draft ->
                setState {
                    when {
                        draft == null -> MealDraftUiState.Empty
                        this is MealDraftUiState.Editing -> copy(draft = draft)
                        else -> MealDraftUiState.Editing(draft = draft)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: MealDraftEvent) = when (event) {
        is MealDraftEvent.StartDraft -> handleStartDraft(event)
        is MealDraftEvent.Rename -> handleRename(event.name)
        is MealDraftEvent.RemoveItem -> handleRemoveItem(event.itemId)
        is MealDraftEvent.AddResolvedItem -> handleAddItem(event.item)
        is MealDraftEvent.AddPendingPhoto -> handleGatedAdd(Feature.AI_PHOTO_ANALYSIS) {
            handleAddItem(DraftItem.PendingPhoto(id = generateId(), imageBase64 = event.imageBase64, hint = event.hint))
        }
        is MealDraftEvent.AddPendingText -> handleGatedAdd(Feature.AI_TEXT_ANALYSIS) {
            handleAddItem(DraftItem.PendingText(id = generateId(), description = event.description))
        }
        is MealDraftEvent.AddManualItem -> handleAddKnownItem(event)
        is MealDraftEvent.UpdateResolvedItem -> handleUpdateResolvedItem(event)
        MealDraftEvent.Discard -> handleDiscard()
        MealDraftEvent.Process -> handleProcess()
        MealDraftEvent.Commit -> handleCommit()
    }

    private fun handleGatedAdd(feature: Feature, onGranted: () -> Unit) {
        viewModelScope.launch {
            when (val access = checkFeatureAccess(feature)) {
                is FeatureAccess.Granted -> onGranted()
                else -> emit(MealDraftEffect.FeatureGated(access))
            }
        }
    }

    private fun handleStartDraft(event: MealDraftEvent.StartDraft) {
        viewModelScope.launch {
            val name = event.initialName ?: defaultDraftName()
            val date = event.date ?: clock.today().toString()
            createDraft(name, date)
                .onSuccess { emit(MealDraftEffect.Started) }
                .onFailure { emitError(it) }
        }
    }

    private fun handleRename(name: String) {
        viewModelScope.launch { renameDraft(name).onFailure { emitError(it) } }
    }

    private fun handleRemoveItem(itemId: String) {
        viewModelScope.launch { removeItem(itemId).onFailure { emitError(it) } }
    }

    private fun handleAddItem(item: DraftItem) {
        viewModelScope.launch { addItem(item).onFailure { emitError(it) } }
    }

    private fun handleAddKnownItem(event: MealDraftEvent.AddManualItem) {
        val itemId = generateId()
        val date = (uiState.value as? MealDraftUiState.Editing)?.draft?.date
        handleAddItem(
            DraftItem.Resolved(
                id = itemId,
                entry = buildMealEntry(
                    MealEntryInput.Known(
                        name = event.name,
                        protein = event.protein,
                        calories = event.calories,
                        fat = event.fat,
                        carbs = event.carbs,
                        quantity = event.quantity,
                        source = event.source,
                        logDate = date,
                    ),
                    localId = itemId,
                ),
            ),
        )
    }

    private fun handleUpdateResolvedItem(event: MealDraftEvent.UpdateResolvedItem) {
        val state = uiState.value as? MealDraftUiState.Editing ?: return
        viewModelScope.launch {
            val entry = buildMealEntry(
                MealEntryInput.Known(
                    name = event.name,
                    protein = event.protein,
                    calories = event.calories,
                    fat = event.fat,
                    carbs = event.carbs,
                    quantity = event.quantity,
                    source = event.source,
                    logDate = state.draft.date,
                ),
                localId = event.itemId,
            ).copy(createdAt = event.createdAt)
            updateDraftItem(event.itemId, entry).onFailure { emitError(it) }
        }
    }

    private fun handleDiscard() {
        viewModelScope.launch {
            discardDraft()
                .onSuccess { emit(MealDraftEffect.Discarded) }
                .onFailure { emitError(it) }
        }
    }

    private fun handleProcess() {
        val state = uiState.value as? MealDraftUiState.Editing ?: return
        if (state.isProcessing) return

        setState {
            if (this is MealDraftUiState.Editing) copy(isProcessing = true) else this
        }
        viewModelScope.launch {
            processDraft()
                .onSuccess { emit(MealDraftEffect.NavigateToReview) }
                .onFailure { emitError(it) }
            setState {
                if (this is MealDraftUiState.Editing) copy(isProcessing = false) else this
            }
        }
    }

    private fun handleCommit() {
        viewModelScope.launch {
            commitDraft()
                .onSuccess { meal -> emit(MealDraftEffect.Committed(meal)) }
                .onFailure { emitError(it) }
        }
    }

    private fun emitError(throwable: Throwable) {
        val message = throwable.message ?: "An error occurred"
        emit(MealDraftEffect.ShowError(message))
    }

    private fun defaultDraftName(): String {
        val local = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
        val hh = local.hour.toString().padStart(2, '0')
        val mm = local.minute.toString().padStart(2, '0')
        return "Meal - ${hh}h${mm}"
    }

    private fun generateId(): String {
        // 128-bit randomness (16 bytes) encoded as hex — collision-safe for local item IDs.
        val bytes = kotlin.random.Random.nextBytes(16)
        val hex = bytes.joinToString("") { it.toInt().and(0xFF).toString(16).padStart(2, '0') }
        return "item-$hex"
    }
}
