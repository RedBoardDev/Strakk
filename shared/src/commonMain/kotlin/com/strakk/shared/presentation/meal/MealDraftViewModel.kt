package com.strakk.shared.presentation.meal

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.common.ClockProvider
import com.strakk.shared.domain.common.DomainError
import com.strakk.shared.domain.model.DraftItem
import com.strakk.shared.domain.usecase.AddItemToDraftUseCase
import com.strakk.shared.domain.usecase.CommitMealDraftUseCase
import com.strakk.shared.domain.usecase.CreateMealDraftUseCase
import com.strakk.shared.domain.usecase.DiscardMealDraftUseCase
import com.strakk.shared.domain.usecase.ObserveActiveMealDraftUseCase
import com.strakk.shared.domain.usecase.ProcessMealDraftUseCase
import com.strakk.shared.domain.usecase.RemoveItemFromDraftUseCase
import com.strakk.shared.domain.usecase.RenameMealDraftUseCase
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
class MealDraftViewModel(
    private val observeActiveDraft: ObserveActiveMealDraftUseCase,
    private val createDraft: CreateMealDraftUseCase,
    private val addItem: AddItemToDraftUseCase,
    private val removeItem: RemoveItemFromDraftUseCase,
    private val renameDraft: RenameMealDraftUseCase,
    private val discardDraft: DiscardMealDraftUseCase,
    private val processDraft: ProcessMealDraftUseCase,
    private val commitDraft: CommitMealDraftUseCase,
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
        is MealDraftEvent.AddPendingPhoto -> handleAddItem(
            DraftItem.PendingPhoto(id = generateId(), imageBase64 = event.imageBase64, hint = event.hint),
        )
        is MealDraftEvent.AddPendingText -> handleAddItem(
            DraftItem.PendingText(id = generateId(), description = event.description),
        )
        MealDraftEvent.Discard -> handleDiscard()
        MealDraftEvent.Process -> handleProcess()
        MealDraftEvent.Commit -> handleCommit()
    }

    private fun handleStartDraft(event: MealDraftEvent.StartDraft) {
        viewModelScope.launch {
            val name = event.initialName ?: defaultDraftName()
            val date = event.date ?: clock.today().toString()
            createDraft(name, date).onFailure { emitError(it) }
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
        val message = when (throwable) {
            is DomainError -> throwable.message ?: "Une erreur est survenue."
            else -> throwable.message ?: "Une erreur est survenue."
        }
        emit(MealDraftEffect.ShowError(message))
    }

    private fun defaultDraftName(): String {
        val local = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
        val hh = local.hour.toString().padStart(2, '0')
        val mm = local.minute.toString().padStart(2, '0')
        return "Repas - ${hh}h${mm}"
    }

    private fun generateId(): String {
        val now = clock.now().toEpochMilliseconds()
        val rand = (0..0xFFFF).random()
        return "item-${now}-${rand.toString(16)}"
    }
}
