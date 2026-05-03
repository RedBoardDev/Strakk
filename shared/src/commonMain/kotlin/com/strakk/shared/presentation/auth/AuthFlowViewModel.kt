package com.strakk.shared.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.usecase.SignInUseCase
import com.strakk.shared.domain.usecase.SignUpUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthFlowViewModel(
    private val signIn: SignInUseCase,
    private val signUp: SignUpUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthFlowUiState>(AuthFlowUiState.Welcome)
    val uiState: StateFlow<AuthFlowUiState> = _uiState.asStateFlow()

    fun onEvent(event: AuthFlowEvent) {
        when (event) {
            AuthFlowEvent.OnContinueWithEmail -> _uiState.value = AuthFlowUiState.SignIn()
            is AuthFlowEvent.OnEmailChanged -> updateEmail(event.email)
            is AuthFlowEvent.OnPasswordChanged -> updatePassword(event.password)
            AuthFlowEvent.OnSignIn -> performSignIn()
            AuthFlowEvent.OnSignUp -> performSignUp()
            AuthFlowEvent.OnSwitchToSignUp -> switchToSignUp()
            AuthFlowEvent.OnSwitchToSignIn -> switchToSignIn()
        }
    }

    private fun updateEmail(email: String) {
        when (val s = _uiState.value) {
            is AuthFlowUiState.SignIn -> _uiState.value = s.copy(email = email)
            is AuthFlowUiState.SignUp -> _uiState.value = s.copy(email = email)
            else -> Unit
        }
    }

    private fun updatePassword(password: String) {
        when (val s = _uiState.value) {
            is AuthFlowUiState.SignIn -> _uiState.value = s.copy(password = password)
            is AuthFlowUiState.SignUp -> _uiState.value = s.copy(password = password)
            else -> Unit
        }
    }

    private fun switchToSignUp() {
        val email = (_uiState.value as? AuthFlowUiState.SignIn)?.email.orEmpty()
        _uiState.value = AuthFlowUiState.SignUp(email = email)
    }

    private fun switchToSignIn() {
        val email = (_uiState.value as? AuthFlowUiState.SignUp)?.email.orEmpty()
        _uiState.value = AuthFlowUiState.SignIn(email = email)
    }

    private fun performSignUp() {
        val state = _uiState.value as? AuthFlowUiState.SignUp ?: return
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            val result = signUp(state.email, state.password)
            val current = _uiState.value as? AuthFlowUiState.SignUp ?: return@launch
            _uiState.value = current.copy(
                isLoading = false,
                error = result.exceptionOrNull()?.message,
            )
        }
    }

    private fun performSignIn() {
        val state = _uiState.value as? AuthFlowUiState.SignIn ?: return
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            val result = signIn(state.email, state.password)
            val current = _uiState.value as? AuthFlowUiState.SignIn ?: return@launch
            _uiState.value = current.copy(
                isLoading = false,
                error = result.exceptionOrNull()?.message,
            )
        }
    }
}
