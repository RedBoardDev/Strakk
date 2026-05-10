package com.strakk.shared.presentation.paywall

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.model.BillingResult
import com.strakk.shared.domain.model.Feature
import com.strakk.shared.domain.model.SubscriptionState
import com.strakk.shared.domain.usecase.LoadPaywallOfferPricesUseCase
import com.strakk.shared.domain.usecase.ObserveSubscriptionStateUseCase
import com.strakk.shared.domain.usecase.PurchaseSubscriptionUseCase
import com.strakk.shared.domain.usecase.RefreshSubscriptionStateUseCase
import com.strakk.shared.domain.usecase.RestoreSubscriptionUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.launch

class PaywallViewModel(
    private val observeSubscriptionState: ObserveSubscriptionStateUseCase,
    private val purchaseSubscription: PurchaseSubscriptionUseCase,
    private val restoreSubscription: RestoreSubscriptionUseCase,
    private val refreshSubscriptionState: RefreshSubscriptionStateUseCase,
    private val loadPaywallOfferPrices: LoadPaywallOfferPricesUseCase,
    highlightedFeature: Feature? = null,
) : MviViewModel<PaywallUiState, PaywallEvent, PaywallEffect>(
    PaywallUiState(highlightedFeature = highlightedFeature),
) {

    init {
        viewModelScope.launch {
            val prices = loadPaywallOfferPrices()
            setState { copy(offerPrices = prices) }
        }
        viewModelScope.launch {
            observeSubscriptionState().collect { state ->
                setState {
                    copy(
                        currentPlan = (state as? SubscriptionState.Active)?.plan,
                        isTrial = state is SubscriptionState.Trial,
                        selectedPlanIsCurrent = (state as? SubscriptionState.Active)?.plan == selectedPlan,
                        errorMessage = null,
                    )
                }
            }
        }
    }

    override fun onEvent(event: PaywallEvent) = when (event) {
        is PaywallEvent.OnPlanSelected -> setState {
            copy(
                selectedPlan = event.plan,
                selectedPlanIsCurrent = currentPlan == event.plan,
                errorMessage = null,
            )
        }
        PaywallEvent.OnSubscribeTapped -> handleSubscribe()
        PaywallEvent.OnRestoreTapped -> handleRestore()
        PaywallEvent.OnDismiss -> emit(PaywallEffect.Dismiss)
    }

    private fun handleSubscribe() {
        viewModelScope.launch {
            val selectedPlan = uiState.value.selectedPlan
            setState { copy(isProcessing = true, errorMessage = null) }
            when (val result = purchaseSubscription(selectedPlan)) {
                BillingResult.Success -> {
                    refreshSubscriptionState()
                    emit(PaywallEffect.ShowToast("Subscription activated"))
                    emit(PaywallEffect.Dismiss)
                }
                BillingResult.Cancelled -> emit(PaywallEffect.ShowToast("Purchase cancelled"))
                is BillingResult.Error -> {
                    setState { copy(errorMessage = result.message) }
                    emit(PaywallEffect.ShowToast(result.message))
                }
            }
            setState { copy(isProcessing = false) }
        }
    }

    private fun handleRestore() {
        viewModelScope.launch {
            setState { copy(isProcessing = true, errorMessage = null) }
            when (val result = restoreSubscription()) {
                BillingResult.Success -> {
                    refreshSubscriptionState()
                    emit(PaywallEffect.ShowToast("Purchases restored"))
                }
                BillingResult.Cancelled -> emit(PaywallEffect.ShowToast("Restore cancelled"))
                is BillingResult.Error -> {
                    setState { copy(errorMessage = result.message) }
                    emit(PaywallEffect.ShowToast(result.message))
                }
            }
            setState { copy(isProcessing = false) }
        }
    }
}
