package com.strakk.shared.presentation.settings

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.usecase.GetCurrentUserEmailUseCase
import com.strakk.shared.domain.usecase.GetHevyApiKeyUseCase
import com.strakk.shared.domain.usecase.ObserveProfileUseCase
import com.strakk.shared.domain.usecase.SaveHevyApiKeyUseCase
import com.strakk.shared.domain.usecase.SignOutUseCase
import com.strakk.shared.domain.usecase.UpdateProfileUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val DEBOUNCE_DELAY_MS = 500L

/**
 * Manages the Settings screen.
 *
 * Loads the user profile and email on init. Each editable field change updates
 * state immediately and triggers a debounced auto-save via [UpdateProfileUseCase].
 */
class SettingsViewModel(
    private val getCurrentUserEmail: GetCurrentUserEmailUseCase,
    private val observeProfile: ObserveProfileUseCase,
    private val updateProfile: UpdateProfileUseCase,
    private val signOut: SignOutUseCase,
    private val saveHevyApiKey: SaveHevyApiKeyUseCase,
    private val getHevyApiKey: GetHevyApiKeyUseCase,
) : MviViewModel<SettingsUiState, SettingsEvent, SettingsEffect>(SettingsUiState.Loading) {

    private var saveDebounceJob: Job? = null
    private var hevyKeyDebounceJob: Job? = null

    init {
        loadSettings()
    }

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.OnProteinGoalChanged -> {
                updateReady { copy(proteinGoal = event.value) }
                scheduleSave()
            }
            is SettingsEvent.OnCalorieGoalChanged -> {
                updateReady { copy(calorieGoal = event.value) }
                scheduleSave()
            }
            is SettingsEvent.OnWaterGoalChanged -> {
                updateReady { copy(waterGoal = event.value) }
                scheduleSave()
            }
            is SettingsEvent.OnHevyApiKeyChanged -> {
                updateReady { copy(hevyApiKey = event.value) }
                scheduleHevyKeySave()
            }
            SettingsEvent.OnSignOut -> viewModelScope.launch {
                signOut().onFailure { emitError(it) }
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val email = getCurrentUserEmail().getOrNull()
            val profile = observeProfile().first()
            val hevyKey = getHevyApiKey().getOrNull()

            setState {
                SettingsUiState.Ready(
                    email = email,
                    proteinGoal = profile?.proteinGoal?.toString() ?: "",
                    calorieGoal = profile?.calorieGoal?.toString() ?: "",
                    waterGoal = profile?.waterGoal?.toString() ?: "",
                    hevyApiKey = hevyKey ?: "",
                )
            }
        }
    }

    private inline fun updateReady(crossinline transform: SettingsUiState.Ready.() -> SettingsUiState.Ready) {
        setState { (this as? SettingsUiState.Ready)?.transform() ?: this }
    }

    private fun scheduleSave() {
        saveDebounceJob?.cancel()
        saveDebounceJob = viewModelScope.launch {
            delay(DEBOUNCE_DELAY_MS)
            performSave()
        }
    }

    private suspend fun performSave() {
        val state = uiState.value as? SettingsUiState.Ready ?: return

        updateProfile(
            proteinGoal = state.proteinGoal.toIntOrNull(),
            calorieGoal = state.calorieGoal.toIntOrNull(),
            waterGoal = state.waterGoal.toIntOrNull(),
        ).onFailure { emitError(it) }
    }

    private fun scheduleHevyKeySave() {
        hevyKeyDebounceJob?.cancel()
        hevyKeyDebounceJob = viewModelScope.launch {
            delay(DEBOUNCE_DELAY_MS)
            val state = uiState.value as? SettingsUiState.Ready ?: return@launch
            saveHevyApiKey(state.hevyApiKey).onFailure { emitError(it) }
        }
    }

    private fun emitError(throwable: Throwable) {
        emit(SettingsEffect.ShowError(throwable.message ?: "An error occurred"))
    }
}
