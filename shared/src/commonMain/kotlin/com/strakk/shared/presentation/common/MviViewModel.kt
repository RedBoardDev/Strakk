package com.strakk.shared.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base class for all ViewModels using the MVI pattern.
 *
 * Centralises the state / event / effect plumbing so concrete ViewModels
 * only declare their contract types and the [onEvent] reducer logic.
 *
 * Subclasses expose [uiState] (observed by the UI) and [effects] (one-shot
 * side effects such as navigation or error toasts), and use [setState] /
 * [emit] internally.
 */
abstract class MviViewModel<State : Any, Event : Any, Effect : Any>(
    initialState: State,
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects: Flow<Effect> = _effects.receiveAsFlow()

    /** Handle an event dispatched from the UI. */
    abstract fun onEvent(event: Event)

    /** Atomically update the current state using a reducer. */
    protected fun setState(reducer: State.() -> State) {
        _uiState.update(reducer)
    }

    /** Emit a one-shot side effect to the UI. Suspends briefly if the buffer is full. */
    protected fun emit(effect: Effect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
