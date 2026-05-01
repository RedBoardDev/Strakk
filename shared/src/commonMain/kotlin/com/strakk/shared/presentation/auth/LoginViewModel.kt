package com.strakk.shared.presentation.auth

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.usecase.CheckProfileExistsUseCase
import com.strakk.shared.domain.usecase.ObserveProfileUseCase
import com.strakk.shared.domain.usecase.ResetPasswordUseCase
import com.strakk.shared.domain.usecase.SignInUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class LoginViewModel(
    private val signIn: SignInUseCase,
    private val resetPassword: ResetPasswordUseCase,
    private val observeProfile: ObserveProfileUseCase,
) : MviViewModel<LoginUiState, LoginEvent, LoginEffect>(LoginUiState()) {

    override fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.OnEmailChanged -> setState { copy(email = event.email, error = null) }
            is LoginEvent.OnPasswordChanged -> setState { copy(password = event.password, error = null) }
            is LoginEvent.OnLogin -> handleLogin()
            is LoginEvent.OnForgotPassword -> handleForgotPassword()
            is LoginEvent.OnNavigateToSignUp -> emit(LoginEffect.NavigateToSignUp)
        }
    }

    private fun handleLogin() {
        val state = uiState.value
        if (state.isLoading) return

        setState { copy(isLoading = true, error = null) }

        viewModelScope.launch {
            signIn(state.email, state.password)
                .onSuccess { resolveDestination() }
                .onFailure { error ->
                    setState {
                        copy(
                            isLoading = false,
                            error = mapLoginError(error),
                        )
                    }
                }
        }
    }

    private suspend fun resolveDestination() {
        val profile = observeProfile().firstOrNull()
        setState { copy(isLoading = false) }

        if (profile != null && profile.onboardingCompleted) {
            emit(LoginEffect.NavigateToHome)
        } else {
            emit(LoginEffect.NavigateToOnboarding)
        }
    }

    private fun handleForgotPassword() {
        val state = uiState.value
        val trimmedEmail = state.email.trim()

        if (trimmedEmail.isBlank()) {
            setState { copy(error = "Entre ton email d'abord.") }
            return
        }

        viewModelScope.launch {
            resetPassword(trimmedEmail)
                .onSuccess {
                    emit(
                        LoginEffect.ShowMessage(
                            "Si un compte existe avec cet email, tu recevras un lien de réinitialisation.",
                        ),
                    )
                }
                .onFailure {
                    emit(
                        LoginEffect.ShowMessage(
                            "Si un compte existe avec cet email, tu recevras un lien de réinitialisation.",
                        ),
                    )
                }
        }
    }

    private fun mapLoginError(error: Throwable): String {
        val message = error.message?.lowercase() ?: return "Une erreur est survenue. Réessaie."
        return when {
            "invalid" in message || "credentials" in message ->
                "Email ou mot de passe incorrect."
            "email not confirmed" in message ->
                "Confirme ton email avant de te connecter."
            "rate" in message || "too many" in message ->
                "Trop de tentatives. Réessaie dans quelques minutes."
            "network" in message || "connect" in message ->
                "Pas de connexion. Vérifie ton réseau."
            "validation" in message -> error.message ?: "Email invalide."
            else -> "Une erreur est survenue. Réessaie."
        }
    }
}
