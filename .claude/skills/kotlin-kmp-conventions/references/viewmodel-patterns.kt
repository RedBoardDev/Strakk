package com.strakk.shared.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.model.Session
import com.strakk.shared.domain.model.SessionId
import com.strakk.shared.domain.usecase.CreateSessionUseCase
import com.strakk.shared.domain.usecase.GetSessionsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// =============================================================================
// Pattern 1: StateFlow from use case with sealed interface UiState
// =============================================================================

/**
 * UiState as a sealed interface — NEVER sealed class.
 *
 * Each subtype is a data class (or data object for singletons).
 */
sealed interface SessionListUiState {
    data object Loading : SessionListUiState
    data class Success(
        val sessions: List<Session>,
    ) : SessionListUiState
    data class Error(
        val message: String,
    ) : SessionListUiState
}

/**
 * One-shot effects — navigation, snackbar, haptics.
 *
 * Consumed once via Channel, never replayed.
 */
sealed interface SessionListEffect {
    data class NavigateToDetail(
        val sessionId: SessionId,
    ) : SessionListEffect
    data class ShowSnackbar(
        val message: String,
    ) : SessionListEffect
    data object NavigateBack : SessionListEffect
}

/**
 * Events from the UI layer.
 */
sealed interface SessionListEvent {
    data class OnSessionClick(
        val sessionId: SessionId,
    ) : SessionListEvent
    data object OnRefresh : SessionListEvent
    data object OnBackClick : SessionListEvent
}

/**
 * ViewModel with reactive StateFlow from a use case.
 *
 * - State: derived from domain Flow via stateIn
 * - Effects: one-shot via Channel
 * - Events: handled via onEvent function
 */
class SessionListViewModel(
    getSessionsUseCase: GetSessionsUseCase,
) : ViewModel() {

    val uiState: StateFlow<SessionListUiState> = getSessionsUseCase()
        .map<List<Session>, SessionListUiState> { sessions ->
            SessionListUiState.Success(sessions = sessions)
        }
        .catch { throwable ->
            emit(SessionListUiState.Error(message = throwable.message.orEmpty()))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionListUiState.Loading,
        )

    private val _effects = Channel<SessionListEffect>(Channel.BUFFERED)
    val effects: Flow<SessionListEffect> = _effects.receiveAsFlow()

    fun onEvent(event: SessionListEvent) {
        when (event) {
            is SessionListEvent.OnSessionClick -> {
                _effects.trySend(
                    SessionListEffect.NavigateToDetail(sessionId = event.sessionId),
                )
            }
            is SessionListEvent.OnRefresh -> {
                // Re-subscription via stateIn handles refresh
            }
            is SessionListEvent.OnBackClick -> {
                _effects.trySend(SessionListEffect.NavigateBack)
            }
        }
    }
}

// =============================================================================
// Pattern 2: MutableStateFlow for form-like screens
// =============================================================================

sealed interface CreateSessionUiState {
    data object Idle : CreateSessionUiState
    data object Saving : CreateSessionUiState
    data class ValidationError(
        val message: String,
    ) : CreateSessionUiState
}

sealed interface CreateSessionEvent {
    data class OnNameChanged(
        val name: String,
    ) : CreateSessionEvent
    data object OnSaveClick : CreateSessionEvent
}

sealed interface CreateSessionEffect {
    data object NavigateBack : CreateSessionEffect
    data class ShowSnackbar(
        val message: String,
    ) : CreateSessionEffect
}

/**
 * ViewModel with MutableStateFlow for imperative state updates.
 *
 * Used when state is driven by user input, not a reactive data source.
 */
class CreateSessionViewModel(
    private val createSessionUseCase: CreateSessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateSessionUiState>(CreateSessionUiState.Idle)
    val uiState: StateFlow<CreateSessionUiState> = _uiState

    private val _effects = Channel<CreateSessionEffect>(Channel.BUFFERED)
    val effects: Flow<CreateSessionEffect> = _effects.receiveAsFlow()

    private var sessionName: String = ""

    fun onEvent(event: CreateSessionEvent) {
        when (event) {
            is CreateSessionEvent.OnNameChanged -> {
                sessionName = event.name
                _uiState.update { CreateSessionUiState.Idle }
            }
            is CreateSessionEvent.OnSaveClick -> saveSession()
        }
    }

    private fun saveSession() {
        viewModelScope.launch {
            _uiState.update { CreateSessionUiState.Saving }

            createSessionUseCase(
                name = sessionName,
                exercises = emptyList(),
            ).onSuccess {
                _effects.trySend(CreateSessionEffect.NavigateBack)
            }.onFailure { error ->
                _uiState.update {
                    CreateSessionUiState.ValidationError(
                        message = error.message.orEmpty(),
                    )
                }
            }
        }
    }
}
