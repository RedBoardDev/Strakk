package com.strakk.shared.presentation.auth

import app.cash.turbine.test
import com.strakk.shared.domain.usecase.SignInUseCase
import com.strakk.shared.domain.usecase.SignUpUseCase
import com.strakk.shared.fixtures.FakeAuthRepository
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
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AuthFlowViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var authRepository: FakeAuthRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AuthFlowViewModel = AuthFlowViewModel(
        signIn = SignInUseCase(authRepository),
        signUp = SignUpUseCase(authRepository),
    )

    @Test
    fun initialStateIsWelcome() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertIs<AuthFlowUiState.Welcome>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onContinueWithEmailTransitionsToSignIn() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertIs<AuthFlowUiState.Welcome>(awaitItem())

            viewModel.onEvent(AuthFlowEvent.OnContinueWithEmail)

            assertIs<AuthFlowUiState.SignIn>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onEmailChangedUpdatesEmailInSignInState() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(AuthFlowEvent.OnContinueWithEmail)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertIs<AuthFlowUiState.SignIn>(initial)

            viewModel.onEvent(AuthFlowEvent.OnEmailChanged(TestFixtures.VALID_EMAIL))

            val updated = awaitItem()
            assertIs<AuthFlowUiState.SignIn>(updated)
            assertEquals(TestFixtures.VALID_EMAIL, updated.email)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onPasswordChangedUpdatesPasswordInSignInState() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(AuthFlowEvent.OnContinueWithEmail)

        viewModel.uiState.test {
            skipItems(1)

            viewModel.onEvent(AuthFlowEvent.OnPasswordChanged(TestFixtures.VALID_PASSWORD))

            val updated = awaitItem()
            assertIs<AuthFlowUiState.SignIn>(updated)
            assertEquals(TestFixtures.VALID_PASSWORD, updated.password)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onSignInWithInvalidEmailShowsError() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(AuthFlowEvent.OnContinueWithEmail)
        viewModel.onEvent(AuthFlowEvent.OnEmailChanged(TestFixtures.EMAIL_NO_AT))
        viewModel.onEvent(AuthFlowEvent.OnPasswordChanged(TestFixtures.VALID_PASSWORD))

        viewModel.uiState.test {
            skipItems(1)

            viewModel.onEvent(AuthFlowEvent.OnSignIn)

            var state = awaitItem()
            if (state is AuthFlowUiState.SignIn && state.isLoading) {
                state = awaitItem()
            }
            assertIs<AuthFlowUiState.SignIn>(state)
            assertEquals(false, state.isLoading)
            assertEquals("Invalid email format", state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onSignInWithEmptyPasswordShowsError() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(AuthFlowEvent.OnContinueWithEmail)
        viewModel.onEvent(AuthFlowEvent.OnEmailChanged(TestFixtures.VALID_EMAIL))

        viewModel.uiState.test {
            skipItems(1)

            viewModel.onEvent(AuthFlowEvent.OnSignIn)

            var state = awaitItem()
            if (state is AuthFlowUiState.SignIn && state.isLoading) {
                state = awaitItem()
            }
            assertIs<AuthFlowUiState.SignIn>(state)
            assertEquals("Password is required", state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onSwitchToSignUpPreservesEmail() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertIs<AuthFlowUiState.Welcome>(awaitItem())

            viewModel.onEvent(AuthFlowEvent.OnContinueWithEmail)
            assertIs<AuthFlowUiState.SignIn>(awaitItem())

            viewModel.onEvent(AuthFlowEvent.OnEmailChanged(TestFixtures.VALID_EMAIL))
            assertIs<AuthFlowUiState.SignIn>(awaitItem())

            viewModel.onEvent(AuthFlowEvent.OnSwitchToSignUp)

            val signUpState = awaitItem()
            assertIs<AuthFlowUiState.SignUp>(signUpState)
            assertEquals(TestFixtures.VALID_EMAIL, signUpState.email)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onSwitchToSignInPreservesEmail() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertIs<AuthFlowUiState.Welcome>(awaitItem())

            viewModel.onEvent(AuthFlowEvent.OnContinueWithEmail)
            assertIs<AuthFlowUiState.SignIn>(awaitItem())

            viewModel.onEvent(AuthFlowEvent.OnSwitchToSignUp)
            assertIs<AuthFlowUiState.SignUp>(awaitItem())

            viewModel.onEvent(AuthFlowEvent.OnEmailChanged(TestFixtures.VALID_EMAIL))
            assertIs<AuthFlowUiState.SignUp>(awaitItem())

            viewModel.onEvent(AuthFlowEvent.OnSwitchToSignIn)

            val signInState = awaitItem()
            assertIs<AuthFlowUiState.SignIn>(signInState)
            assertEquals(TestFixtures.VALID_EMAIL, signInState.email)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onSignInSuccessDoesNotShowError() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(AuthFlowEvent.OnContinueWithEmail)
        viewModel.onEvent(AuthFlowEvent.OnEmailChanged(TestFixtures.VALID_EMAIL))
        viewModel.onEvent(AuthFlowEvent.OnPasswordChanged(TestFixtures.VALID_PASSWORD))

        viewModel.onEvent(AuthFlowEvent.OnSignIn)

        // StateFlow is conflated: isLoading=true → false happens in one coroutine frame,
        // so we read the final state directly rather than trying to capture the intermediate.
        val finalState = viewModel.uiState.value
        assertIs<AuthFlowUiState.SignIn>(finalState)
        assertNull(finalState.error)
    }
}
