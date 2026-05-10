package com.strakk.shared.presentation.settings

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.model.BillingResult
import com.strakk.shared.domain.model.SubscriptionPlan
import com.strakk.shared.domain.model.SubscriptionState
import com.strakk.shared.domain.usecase.GetCurrentUserEmailUseCase
import com.strakk.shared.domain.usecase.GetHevyApiKeyUseCase
import com.strakk.shared.domain.usecase.ObserveDevSubscriptionOverrideUseCase
import com.strakk.shared.domain.usecase.ObserveProfileUseCase
import com.strakk.shared.domain.usecase.ObserveSubscriptionStateUseCase
import com.strakk.shared.domain.usecase.RefreshSubscriptionStateUseCase
import com.strakk.shared.domain.usecase.RestoreSubscriptionUseCase
import com.strakk.shared.domain.usecase.SaveHevyApiKeyUseCase
import com.strakk.shared.domain.usecase.SetDevSubscriptionOverrideUseCase
import com.strakk.shared.domain.usecase.SignOutUseCase
import com.strakk.shared.domain.usecase.UpdateProfileUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

private const val DEBOUNCE_DELAY_MS = 500L
private const val ISO_DATE_PREFIX_LENGTH = 10

/**
 * Manages the Settings screen.
 *
 * Loads the user profile and email on init. Each editable field change updates
 * state immediately and triggers a debounced auto-save via [UpdateProfileUseCase].
 */
@Suppress("LongParameterList")
class SettingsViewModel(
    private val getCurrentUserEmail: GetCurrentUserEmailUseCase,
    private val observeProfile: ObserveProfileUseCase,
    private val updateProfile: UpdateProfileUseCase,
    private val signOut: SignOutUseCase,
    private val saveHevyApiKey: SaveHevyApiKeyUseCase,
    private val getHevyApiKey: GetHevyApiKeyUseCase,
    private val observeSubscriptionState: ObserveSubscriptionStateUseCase,
    private val observeDevSubscriptionOverride: ObserveDevSubscriptionOverrideUseCase,
    private val setDevSubscriptionOverride: SetDevSubscriptionOverrideUseCase,
    private val restoreSubscription: RestoreSubscriptionUseCase,
    private val refreshSubscriptionState: RefreshSubscriptionStateUseCase,
) : MviViewModel<SettingsUiState, SettingsEvent, SettingsEffect>(SettingsUiState.Loading) {

    private var saveDebounceJob: Job? = null
    private var hevyKeyDebounceJob: Job? = null

    init {
        loadSettings()
    }

    @Suppress("CyclomaticComplexMethod")
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
            is SettingsEvent.OnFatGoalChanged -> {
                updateReady { copy(fatGoal = event.value) }
                scheduleSave()
            }
            is SettingsEvent.OnCarbGoalChanged -> {
                updateReady { copy(carbGoal = event.value) }
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
            SettingsEvent.OnUpgradeTapped -> emit(SettingsEffect.NavigateToPaywall)
            SettingsEvent.OnManageSubscription -> emit(SettingsEffect.OpenManageSubscription)
            SettingsEvent.OnRestorePurchase -> viewModelScope.launch {
                when (val result = restoreSubscription()) {
                    is BillingResult.Success -> {
                        refreshSubscriptionState()
                        emit(SettingsEffect.ShowToast("Purchases restored"))
                    }
                    is BillingResult.Cancelled -> {
                        emit(SettingsEffect.ShowToast("Restore cancelled"))
                    }
                    is BillingResult.Error -> {
                        emit(SettingsEffect.ShowToast(result.message))
                    }
                }
            }
            is SettingsEvent.OnDevSubscriptionOverrideSelected -> viewModelScope.launch {
                setDevSubscriptionOverride(event.override)
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
                    fatGoal = profile?.fatGoal?.toString() ?: "",
                    carbGoal = profile?.carbGoal?.toString() ?: "",
                    waterGoal = profile?.waterGoal?.toString() ?: "",
                    hevyApiKey = hevyKey ?: "",
                )
            }
        }
        observeSubscription()
        observeDevOverride()
    }

    private fun observeSubscription() {
        viewModelScope.launch {
            observeSubscriptionState().collect { sub ->
                val display = mapSubscriptionDisplay(sub)
                updateReady { copy(subscriptionDisplay = display) }
            }
        }
    }

    private fun observeDevOverride() {
        viewModelScope.launch {
            observeDevSubscriptionOverride().collect { override ->
                updateReady { copy(devSubscriptionOverride = override) }
            }
        }
    }

    private fun mapSubscriptionDisplay(state: SubscriptionState): SubscriptionDisplay = when (state) {
        is SubscriptionState.Free, is SubscriptionState.Expired -> SubscriptionDisplay.Free
        is SubscriptionState.Trial -> {
            val days = (state.endsAt - Clock.System.now()).inWholeDays.toInt()
            SubscriptionDisplay.Trial(
                daysRemaining = maxOf(days, 0),
                endsAtLabel = state.endsAt.toString().take(ISO_DATE_PREFIX_LENGTH),
            )
        }
        is SubscriptionState.Active -> {
            SubscriptionDisplay.Active(
                plan = state.plan,
                renewalLabel = state.expiresAt.toString().take(ISO_DATE_PREFIX_LENGTH),
                canUpgradeToAnnual = state.plan == SubscriptionPlan.MONTHLY,
            )
        }
        is SubscriptionState.PaymentFailed -> SubscriptionDisplay.PaymentFailed
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
            fatGoal = state.fatGoal.toIntOrNull(),
            carbGoal = state.carbGoal.toIntOrNull(),
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
