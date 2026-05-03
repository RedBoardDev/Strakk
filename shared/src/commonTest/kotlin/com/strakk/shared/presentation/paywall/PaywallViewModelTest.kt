package com.strakk.shared.presentation.paywall

import app.cash.turbine.test
import com.strakk.shared.domain.model.Feature
import com.strakk.shared.domain.model.FeatureRegistry
import com.strakk.shared.domain.model.SubscriptionPlan
import com.strakk.shared.domain.model.SubscriptionState
import com.strakk.shared.domain.usecase.ObserveSubscriptionStateUseCase
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

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        subscriptionRepository = FakeSubscriptionRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(highlightedFeature: Feature? = null): PaywallViewModel =
        PaywallViewModel(
            observeSubscriptionState = ObserveSubscriptionStateUseCase(subscriptionRepository),
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
    fun `initial state has isAlreadyPro false`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(false, state.isAlreadyPro)
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
    fun `OnRestoreTapped emits ShowToast effect`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onEvent(PaywallEvent.OnRestoreTapped)

            val effect = assertIs<PaywallEffect.ShowToast>(awaitItem())
            assertEquals("Bientôt disponible", effect.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `OnSubscribeTapped emits ShowToast effect`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onEvent(PaywallEvent.OnSubscribeTapped)

            val effect = assertIs<PaywallEffect.ShowToast>(awaitItem())
            assertEquals("Bientôt disponible", effect.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observing PRO subscription state sets isAlreadyPro to true`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial Free → isAlreadyPro = false

            subscriptionRepository.emit(
                SubscriptionState.Active(
                    plan = SubscriptionPlan.ANNUAL,
                    expiresAt = Instant.parse("2027-01-01T00:00:00Z"),
                ),
            )

            val updated = awaitItem()
            assertEquals(true, updated.isAlreadyPro)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observing Trial subscription state sets isAlreadyPro to true`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            subscriptionRepository.emit(
                SubscriptionState.Trial(endsAt = Instant.parse("2026-05-10T00:00:00Z")),
            )

            val updated = awaitItem()
            assertEquals(true, updated.isAlreadyPro)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observing Free subscription state keeps isAlreadyPro false`() = runTest {
        subscriptionRepository.emit(SubscriptionState.Free)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(false, state.isAlreadyPro)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observing Expired subscription state keeps isAlreadyPro false`() = runTest {
        subscriptionRepository.emit(SubscriptionState.Expired)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(false, state.isAlreadyPro)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
