package com.strakk.shared.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.model.AuthStatus
import com.strakk.shared.domain.model.SubscriptionState
import com.strakk.shared.domain.usecase.ObserveAuthStatusUseCase
import com.strakk.shared.domain.usecase.ObserveProfileUseCase
import com.strakk.shared.domain.usecase.ObserveSubscriptionStateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class RootViewModel(
    private val observeAuthStatus: ObserveAuthStatusUseCase,
    private val observeProfile: ObserveProfileUseCase,
    private val observeSubscriptionState: ObserveSubscriptionStateUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RootUiState>(RootUiState.Loading)
    val uiState: StateFlow<RootUiState> = _uiState.asStateFlow()

    init {
        observeSession()
    }

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

    fun dismissTrialModal() {
        val current = _uiState.value
        if (current is RootUiState.Authenticated) {
            _uiState.value = current.copy(showTrialExpiredModal = false)
        }
    }

    private suspend fun resolveAuthenticated(): RootUiState {
        val profile = observeProfile().firstOrNull()
        val completed = profile?.onboardingCompleted ?: false
        val subState = try {
            observeSubscriptionState().first()
        } catch (_: Exception) {
            SubscriptionState.Free
        }
        val trialExpired = subState is SubscriptionState.Trial &&
            subState.endsAt <= Clock.System.now()
        return RootUiState.Authenticated(
            onboardingCompleted = completed,
            showTrialExpiredModal = trialExpired,
        )
    }
}
