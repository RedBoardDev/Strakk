package com.strakk.shared.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.model.AuthStatus
import com.strakk.shared.domain.usecase.CheckProfileExistsUseCase
import com.strakk.shared.domain.usecase.ObserveAuthStatusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Root ViewModel managing authentication state for the entire app.
 *
 * Observes the Supabase session via [ObserveAuthStatusUseCase] and enriches
 * [AuthStatus.Authenticated] by checking whether a profile exists.
 *
 * Pure state-observation VM — no events or effects, so it does not extend
 * [com.strakk.shared.presentation.common.MviViewModel].
 */
class RootViewModel(
    private val observeAuthStatus: ObserveAuthStatusUseCase,
    private val checkProfileExists: CheckProfileExistsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RootUiState>(RootUiState.Loading)
    val uiState: StateFlow<RootUiState> = _uiState.asStateFlow()

    init {
        observeSession()
    }

    /**
     * Re-evaluates the profile check for the current authenticated session.
     *
     * Call this after onboarding completes so the app shell transitions to
     * Home without waiting for a session change event.
     */
    fun refreshProfile() {
        viewModelScope.launch {
            if (_uiState.value is RootUiState.Authenticated) {
                _uiState.value = resolveAuthenticated()
            }
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            observeAuthStatus().collect { status ->
                _uiState.value = when (status) {
                    is AuthStatus.Loading -> RootUiState.Loading
                    is AuthStatus.Unauthenticated -> RootUiState.Unauthenticated
                    is AuthStatus.Authenticated -> resolveAuthenticated()
                }
            }
        }
    }

    /**
     * Returns an [RootUiState.Authenticated] with `hasProfile` set.
     * Falls back to `hasProfile = false` on errors, sending the user
     * to onboarding where profile creation will surface the real failure.
     */
    private suspend fun resolveAuthenticated(): RootUiState =
        RootUiState.Authenticated(hasProfile = checkProfileExists().getOrDefault(false))
}
