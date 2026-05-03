package com.strakk.shared.presentation.auth

import app.cash.turbine.test
import com.strakk.shared.domain.model.AuthStatus
import com.strakk.shared.domain.model.SubscriptionState
import com.strakk.shared.domain.model.UserProfile
import com.strakk.shared.domain.usecase.ObserveAuthStatusUseCase
import com.strakk.shared.domain.usecase.ObserveProfileUseCase
import com.strakk.shared.domain.usecase.ObserveSubscriptionStateUseCase
import com.strakk.shared.fixtures.FakeAuthRepository
import com.strakk.shared.fixtures.FakeProfileRepository
import com.strakk.shared.fixtures.FakeSubscriptionRepository
import com.strakk.shared.fixtures.TestFixtures
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RootViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var subscriptionRepository: FakeSubscriptionRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository()
        profileRepository = FakeProfileRepository()
        subscriptionRepository = FakeSubscriptionRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): RootViewModel = RootViewModel(
        observeAuthStatus = ObserveAuthStatusUseCase(authRepository),
        observeProfile = ObserveProfileUseCase(profileRepository),
        observeSubscriptionState = ObserveSubscriptionStateUseCase(subscriptionRepository),
    )

    @Test
    fun `initial state is Loading`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertIs<RootUiState.Loading>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unauthenticated status becomes Unauthenticated`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertIs<RootUiState.Loading>(awaitItem())

            authRepository.authStatusFlow.emit(AuthStatus.Unauthenticated)

            assertIs<RootUiState.Unauthenticated>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `authenticated with completed profile sets onboardingCompleted true`() = runTest {
        profileRepository.profileFlow.value = TestFixtures.defaultUserProfile.copy(
            onboardingCompleted = true,
        )
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertIs<RootUiState.Loading>(awaitItem())

            authRepository.authStatusFlow.emit(AuthStatus.Authenticated(hasProfile = true))

            val state = awaitItem()
            assertIs<RootUiState.Authenticated>(state)
            assertTrue(state.onboardingCompleted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `authenticated with no profile sets onboardingCompleted false`() = runTest {
        profileRepository.profileFlow.value = null
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertIs<RootUiState.Loading>(awaitItem())

            authRepository.authStatusFlow.emit(AuthStatus.Authenticated(hasProfile = false))

            val state = awaitItem()
            assertIs<RootUiState.Authenticated>(state)
            assertFalse(state.onboardingCompleted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `authenticated with incomplete onboarding sets onboardingCompleted false`() = runTest {
        profileRepository.profileFlow.value = TestFixtures.defaultUserProfile.copy(
            onboardingCompleted = false,
        )
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertIs<RootUiState.Loading>(awaitItem())

            authRepository.authStatusFlow.emit(AuthStatus.Authenticated(hasProfile = true))

            val state = awaitItem()
            assertIs<RootUiState.Authenticated>(state)
            assertFalse(state.onboardingCompleted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `expired trial shows trial expired modal`() = runTest {
        profileRepository.profileFlow.value = TestFixtures.defaultUserProfile
        subscriptionRepository.emit(
            SubscriptionState.Trial(endsAt = Instant.parse("2020-01-01T00:00:00Z")),
        )
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertIs<RootUiState.Loading>(awaitItem())

            authRepository.authStatusFlow.emit(AuthStatus.Authenticated(hasProfile = true))

            val state = awaitItem()
            assertIs<RootUiState.Authenticated>(state)
            assertTrue(state.showTrialExpiredModal)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `active trial does not show trial expired modal`() = runTest {
        profileRepository.profileFlow.value = TestFixtures.defaultUserProfile
        subscriptionRepository.emit(
            SubscriptionState.Trial(endsAt = Instant.parse("2099-01-01T00:00:00Z")),
        )
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertIs<RootUiState.Loading>(awaitItem())

            authRepository.authStatusFlow.emit(AuthStatus.Authenticated(hasProfile = true))

            val state = awaitItem()
            assertIs<RootUiState.Authenticated>(state)
            assertFalse(state.showTrialExpiredModal)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `free subscription does not show trial expired modal`() = runTest {
        profileRepository.profileFlow.value = TestFixtures.defaultUserProfile
        subscriptionRepository.emit(SubscriptionState.Free)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertIs<RootUiState.Loading>(awaitItem())

            authRepository.authStatusFlow.emit(AuthStatus.Authenticated(hasProfile = true))

            val state = awaitItem()
            assertIs<RootUiState.Authenticated>(state)
            assertFalse(state.showTrialExpiredModal)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissTrialModal sets showTrialExpiredModal to false`() = runTest {
        profileRepository.profileFlow.value = TestFixtures.defaultUserProfile
        subscriptionRepository.emit(
            SubscriptionState.Trial(endsAt = Instant.parse("2020-01-01T00:00:00Z")),
        )
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertIs<RootUiState.Loading>(awaitItem())

            authRepository.authStatusFlow.emit(AuthStatus.Authenticated(hasProfile = true))

            val authenticatedState = awaitItem()
            assertIs<RootUiState.Authenticated>(authenticatedState)
            assertTrue(authenticatedState.showTrialExpiredModal)

            viewModel.dismissTrialModal()

            val dismissed = awaitItem()
            assertIs<RootUiState.Authenticated>(dismissed)
            assertFalse(dismissed.showTrialExpiredModal)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
