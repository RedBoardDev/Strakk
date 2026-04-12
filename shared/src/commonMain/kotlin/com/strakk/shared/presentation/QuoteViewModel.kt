package com.strakk.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.model.Quote
import com.strakk.shared.domain.usecase.GetRandomQuoteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuoteViewModel(
    private val getRandomQuote: GetRandomQuoteUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuoteUiState>(QuoteUiState.Loading)
    val uiState: StateFlow<QuoteUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = QuoteUiState.Loading
            try {
                val quote = getRandomQuote()
                _uiState.value = QuoteUiState.Success(quote)
            } catch (e: Exception) {
                _uiState.value = QuoteUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun refresh() = load()

    /** Suspend accessor for iOS (without SKIE). */
    suspend fun fetchQuote(): Quote = getRandomQuote()
}

sealed interface QuoteUiState {
    data object Loading : QuoteUiState
    data class Success(val quote: Quote) : QuoteUiState
    data class Error(val message: String) : QuoteUiState
}
