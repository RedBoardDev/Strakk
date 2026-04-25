package com.strakk.shared.presentation.auth

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.usecase.SignInUseCase
import com.strakk.shared.domain.usecase.SignUpUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.launch

/**
 * Manages the Welcome → SignIn / SignUp auth flow.
 *
 * Validates input, calls sign-in / sign-up use cases, and surfaces errors.
 * Navigation after successful authentication is handled by [RootViewModel]
 * observing the session status.
 */
class AuthFlowViewModel(
    private val signIn: SignInUseCase,
    private val signUp: SignUpUseCase,
) : MviViewModel<AuthFlowUiState, AuthFlowEvent, AuthFlowEffect>(AuthFlowUiState.Welcome) {

    override fun onEvent(event: AuthFlowEvent) {
        when (event) {
            is AuthFlowEvent.OnContinueWithEmail -> setState { AuthFlowUiState.SignIn() }
            is AuthFlowEvent.OnEmailChanged -> updateField { it.withEmail(event.email) }
            is AuthFlowEvent.OnPasswordChanged -> updateField { it.withPassword(event.password) }
            is AuthFlowEvent.OnSignIn -> performSignIn()
            is AuthFlowEvent.OnSignUp -> performSignUp()
            is AuthFlowEvent.OnSwitchToSignUp ->
                setState { AuthFlowUiState.SignUp(email = currentEmail()) }
            is AuthFlowEvent.OnSwitchToSignIn ->
                setState { AuthFlowUiState.SignIn(email = currentEmail()) }
        }
    }

    private fun performSignIn() {
        val state = uiState.value as? AuthFlowUiState.SignIn ?: return
        if (state.isLoading) return

        setState { state.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            signIn(state.email, state.password)
                .onSuccess {
                    // Navigation is driven by RootViewModel observing session status.
                    setState { state.copy(isLoading = false) }
                }
                .onFailure { error ->
                    setState {
                        state.copy(
                            isLoading = false,
                            error = error.message ?: "Sign in failed",
                        )
                    }
                }
        }
    }

    private fun performSignUp() {
        val state = uiState.value as? AuthFlowUiState.SignUp ?: return
        if (state.isLoading) return

        setState { state.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            signUp(state.email, state.password)
                .onSuccess {
                    setState { state.copy(isLoading = false) }
                }
                .onFailure { error ->
                    setState {
                        state.copy(
                            isLoading = false,
                            error = error.message ?: "Sign up failed",
                        )
                    }
                }
        }
    }

    private fun currentEmail(): String = when (val state = uiState.value) {
        is AuthFlowUiState.SignIn -> state.email
        is AuthFlowUiState.SignUp -> state.email
        AuthFlowUiState.Welcome -> ""
    }

    private inline fun updateField(crossinline transform: (AuthFlowUiState) -> AuthFlowUiState) {
        setState { transform(this) }
    }

    private fun AuthFlowUiState.withEmail(email: String): AuthFlowUiState = when (this) {
        is AuthFlowUiState.SignIn -> copy(email = email, error = null)
        is AuthFlowUiState.SignUp -> copy(email = email, error = null)
        AuthFlowUiState.Welcome -> this
    }

    private fun AuthFlowUiState.withPassword(password: String): AuthFlowUiState = when (this) {
        is AuthFlowUiState.SignIn -> copy(password = password, error = null)
        is AuthFlowUiState.SignUp -> copy(password = password, error = null)
        AuthFlowUiState.Welcome -> this
    }
}
