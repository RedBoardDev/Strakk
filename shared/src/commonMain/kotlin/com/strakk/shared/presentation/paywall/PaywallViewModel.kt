package com.strakk.shared.presentation.paywall

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.model.ProFeature
import com.strakk.shared.domain.model.UserTier
import com.strakk.shared.domain.model.tier
import com.strakk.shared.domain.usecase.ObserveSubscriptionStateUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.launch

class PaywallViewModel(
    private val observeSubscriptionState: ObserveSubscriptionStateUseCase,
    highlightedFeature: ProFeature? = null,
) : MviViewModel<PaywallUiState, PaywallEvent, PaywallEffect>(
    PaywallUiState(highlightedFeature = highlightedFeature),
) {

    init {
        viewModelScope.launch {
            observeSubscriptionState().collect { state ->
                setState { copy(isAlreadyPro = state.tier == UserTier.PRO) }
            }
        }
    }

    override fun onEvent(event: PaywallEvent) = when (event) {
        is PaywallEvent.OnPlanSelected -> setState { copy(selectedPlan = event.plan) }
        PaywallEvent.OnSubscribeTapped -> handleSubscribe()
        PaywallEvent.OnRestoreTapped -> emit(PaywallEffect.ShowToast("Bientôt disponible"))
        PaywallEvent.OnDismiss -> emit(PaywallEffect.Dismiss)
    }

    private fun handleSubscribe() {
        // TODO: Integrate RevenueCat purchase flow
        emit(PaywallEffect.ShowToast("Bientôt disponible"))
    }
}
