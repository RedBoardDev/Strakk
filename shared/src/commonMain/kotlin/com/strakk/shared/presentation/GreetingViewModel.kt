package com.strakk.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.model.Greeting
import com.strakk.shared.domain.usecase.GetGreetingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GreetingViewModel(
    private val getGreeting: GetGreetingUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GreetingUiState>(GreetingUiState.Loading)
    val uiState: StateFlow<GreetingUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = GreetingUiState.Loading
            try {
                val greeting = getGreeting()
                _uiState.value = GreetingUiState.Success(greeting)
            } catch (e: Exception) {
                _uiState.value = GreetingUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /** Suspend accessor for iOS (without SKIE, StateFlow is not observable from Swift). */
    suspend fun loadGreeting(): Greeting = getGreeting()
}

sealed interface GreetingUiState {
    data object Loading : GreetingUiState
    data class Success(val greeting: Greeting) : GreetingUiState
    data class Error(val message: String) : GreetingUiState
}
