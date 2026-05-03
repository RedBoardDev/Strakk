package com.strakk.shared.presentation.auth

import app.cash.turbine.test
import com.strakk.shared.domain.model.AuthStatus
import com.strakk.shared.domain.usecase.ObserveAuthStatusUseCase
import com.strakk.shared.domain.usecase.ObserveProfileUseCase
import com.strakk.shared.fixtures.FakeAuthRepository
import com.strakk.shared.fixtures.FakeProfileRepository
import com.strakk.shared.fixtures.TestFixtures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RootViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var profileRepository: FakeProfileRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository()
        profileRepository = FakeProfileRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): RootViewModel = RootViewModel(
        observeAuthStatus = ObserveAuthStatusUseCase(authRepository),
        observeProfile = ObserveProfileUseCase(profileRepository),
    )

    @Test
    fun initialStateIsLoading() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertIs<RootUiState.Loading>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun whenUnauthenticatedStateBecomesUnauthenticated() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertIs<RootUiState.Loading>(awaitItem())

            authRepository.authStatusFlow.emit(AuthStatus.Unauthenticated)

            assertIs<RootUiState.Unauthenticated>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun whenAuthenticatedAndOnboardingCompletedIsTrue() = runTest {
        profileRepository.getProfileResult = TestFixtures.defaultUserProfile.copy(onboardingCompleted = true)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertIs<RootUiState.Loading>(awaitItem())

            authRepository.authStatusFlow.emit(AuthStatus.Authenticated(hasProfile = false))

            val state = awaitItem()
            assertIs<RootUiState.Authenticated>(state)
            assertTrue(state.onboardingCompleted, "Expected onboardingCompleted = true")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun whenAuthenticatedAndNoProfileOnboardingCompletedIsFalse() = runTest {
        profileRepository.getProfileResult = null
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertIs<RootUiState.Loading>(awaitItem())

            authRepository.authStatusFlow.emit(AuthStatus.Authenticated(hasProfile = false))

            val state = awaitItem()
            assertIs<RootUiState.Authenticated>(state)
            assertFalse(state.onboardingCompleted, "Expected onboardingCompleted = false")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun whenAuthenticatedAndOnboardingNotCompletedIsFalse() = runTest {
        // Default profile has onboardingCompleted = false
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertIs<RootUiState.Loading>(awaitItem())

            authRepository.authStatusFlow.emit(AuthStatus.Authenticated(hasProfile = false))

            val state = awaitItem()
            assertIs<RootUiState.Authenticated>(state)
            assertFalse(state.onboardingCompleted, "Expected onboardingCompleted = false")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
