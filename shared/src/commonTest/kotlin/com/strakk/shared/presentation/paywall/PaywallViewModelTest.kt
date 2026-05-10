package com.strakk.shared.presentation.paywall

import app.cash.turbine.test
import com.strakk.shared.domain.model.Feature
import com.strakk.shared.domain.model.BillingResult
import com.strakk.shared.domain.model.FeatureRegistry
import com.strakk.shared.domain.model.SubscriptionPlan
import com.strakk.shared.domain.model.SubscriptionState
import com.strakk.shared.domain.usecase.LoadPaywallOfferPricesUseCase
import com.strakk.shared.domain.usecase.ObserveSubscriptionStateUseCase
import com.strakk.shared.domain.usecase.PurchaseSubscriptionUseCase
import com.strakk.shared.domain.usecase.RefreshSubscriptionStateUseCase
import com.strakk.shared.domain.usecase.RestoreSubscriptionUseCase
import com.strakk.shared.fixtures.FakeBillingRepository
import com.strakk.shared.fixtures.FakeSubscriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class PaywallViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var subscriptionRepository: FakeSubscriptionRepository
    private lateinit var billingRepository: FakeBillingRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        subscriptionRepository = FakeSubscriptionRepository()
        billingRepository = FakeBillingRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(highlightedFeature: Feature? = null): PaywallViewModel =
        PaywallViewModel(
            observeSubscriptionState = ObserveSubscriptionStateUseCase(subscriptionRepository),
            purchaseSubscription = PurchaseSubscriptionUseCase(billingRepository),
            restoreSubscription = RestoreSubscriptionUseCase(billingRepository),
            refreshSubscriptionState = RefreshSubscriptionStateUseCase(subscriptionRepository),
            loadPaywallOfferPrices = LoadPaywallOfferPricesUseCase(billingRepository),
            highlightedFeature = highlightedFeature,
        )

    @Test
    fun `initial state has annual plan selected`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(SubscriptionPlan.ANNUAL, state.selectedPlan)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial state has no current plan`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.currentPlan)
            assertEquals(false, state.isTrial)
            assertEquals(false, state.selectedPlanIsCurrent)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial state has all 6 pro features`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(FeatureRegistry.all().size, state.features.size)
            assertEquals(Feature.entries.size, state.features.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `highlightedFeature is forwarded to initial state`() = runTest {
        val viewModel = createViewModel(highlightedFeature = Feature.AI_PHOTO_ANALYSIS)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(Feature.AI_PHOTO_ANALYSIS, state.highlightedFeature)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `no highlightedFeature gives null in initial state`() = runTest {
        val viewModel = createViewModel(highlightedFeature = null)

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.highlightedFeature)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `OnPlanSelected updates selectedPlan`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.onEvent(PaywallEvent.OnPlanSelected(SubscriptionPlan.MONTHLY))

            val updated = awaitItem()
            assertEquals(SubscriptionPlan.MONTHLY, updated.selectedPlan)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `OnDismiss emits Dismiss effect`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onEvent(PaywallEvent.OnDismiss)

            assertIs<PaywallEffect.Dismiss>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `OnRestoreTapped emits success toast`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onEvent(PaywallEvent.OnRestoreTapped)

            val effect = assertIs<PaywallEffect.ShowToast>(awaitItem())
            assertEquals("Purchases restored", effect.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `OnSubscribeTapped emits success and dismiss`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onEvent(PaywallEvent.OnSubscribeTapped)

            val effect = assertIs<PaywallEffect.ShowToast>(awaitItem())
            assertEquals("Subscription activated", effect.message)
            assertIs<PaywallEffect.Dismiss>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `OnSubscribeTapped with billing error emits error toast`() = runTest {
        billingRepository.purchaseResult = BillingResult.Error("Billing unavailable")
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onEvent(PaywallEvent.OnSubscribeTapped)
            val effect = assertIs<PaywallEffect.ShowToast>(awaitItem())
            assertEquals("Billing unavailable", effect.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observing annual subscription marks selected annual as current`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            subscriptionRepository.emit(
                SubscriptionState.Active(
                    plan = SubscriptionPlan.ANNUAL,
                    expiresAt = Instant.parse("2027-01-01T00:00:00Z"),
                ),
            )

            val updated = awaitItem()
            assertEquals(SubscriptionPlan.ANNUAL, updated.currentPlan)
            assertEquals(false, updated.isTrial)
            assertEquals(true, updated.selectedPlanIsCurrent)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observing monthly subscription allows annual selection as upgrade`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            subscriptionRepository.emit(
                SubscriptionState.Active(
                    plan = SubscriptionPlan.MONTHLY,
                    expiresAt = Instant.parse("2026-06-01T00:00:00Z"),
                ),
            )

            val updated = awaitItem()
            assertEquals(SubscriptionPlan.MONTHLY, updated.currentPlan)
            assertEquals(false, updated.selectedPlanIsCurrent)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observing trial subscription keeps plans selectable`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            subscriptionRepository.emit(
                SubscriptionState.Trial(endsAt = Instant.parse("2026-05-10T00:00:00Z")),
            )

            val updated = awaitItem()
            assertNull(updated.currentPlan)
            assertEquals(true, updated.isTrial)
            assertEquals(false, updated.selectedPlanIsCurrent)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observing Free subscription state keeps no current plan`() = runTest {
        subscriptionRepository.emit(SubscriptionState.Free)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.currentPlan)
            assertEquals(false, state.isTrial)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observing Expired subscription state keeps no current plan`() = runTest {
        subscriptionRepository.emit(SubscriptionState.Expired)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.currentPlan)
            assertEquals(false, state.isTrial)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
