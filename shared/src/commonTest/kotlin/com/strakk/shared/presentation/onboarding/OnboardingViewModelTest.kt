package com.strakk.shared.presentation.onboarding

import app.cash.turbine.test
import com.strakk.shared.domain.usecase.CreateProfileUseCase
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var profileRepository: FakeProfileRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = FakeProfileRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): OnboardingViewModel = OnboardingViewModel(
        createProfile = CreateProfileUseCase(profileRepository),
    )

    @Test
    fun initialStateHasStep0AndEmptyFields() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.currentStep)
            assertEquals("", state.proteinGoal)
            assertEquals("", state.calorieGoal)
            assertEquals("", state.waterGoal)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onProteinGoalChangedUpdatesState() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1)
            viewModel.onEvent(OnboardingEvent.OnProteinGoalChanged("150"))
            val state = awaitItem()
            assertEquals("150", state.proteinGoal)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onContinueStep0AdvancesToStep1() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1)
            viewModel.onEvent(OnboardingEvent.OnContinue)
            val state = awaitItem()
            assertEquals(1, state.currentStep)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onContinueStep1AdvancesToStep2() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(OnboardingEvent.OnContinue) // -> step 1

        viewModel.uiState.test {
            skipItems(1)
            viewModel.onEvent(OnboardingEvent.OnContinue) // -> step 2
            val state = awaitItem()
            assertEquals(2, state.currentStep)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onBackStep1ReturnsToStep0() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(OnboardingEvent.OnContinue) // -> step 1

        viewModel.uiState.test {
            skipItems(1)
            viewModel.onEvent(OnboardingEvent.OnBack)
            val state = awaitItem()
            assertEquals(0, state.currentStep)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onContinueLastStepCallsCreateProfileAndEmitsNavigateToHome() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(OnboardingEvent.OnProteinGoalChanged("150"))
        viewModel.onEvent(OnboardingEvent.OnCalorieGoalChanged("2000"))
        viewModel.onEvent(OnboardingEvent.OnContinue) // -> step 1
        viewModel.onEvent(OnboardingEvent.OnWaterGoalChanged("2500"))
        viewModel.onEvent(OnboardingEvent.OnContinue) // -> step 2

        viewModel.effects.test {
            viewModel.onEvent(OnboardingEvent.OnContinue) // submit

            assertIs<OnboardingEffect.NavigateToHome>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // Verify profile was created
        assertEquals(1, profileRepository.createProfileCalls.size)
        val data = profileRepository.createProfileCalls.first()
        assertEquals(150, data.proteinGoal)
        assertEquals(2000, data.calorieGoal)
        assertEquals(2500, data.waterGoal)
    }

    @Test
    fun createProfileFailureEmitsShowError() = runTest {
        profileRepository.shouldThrow = RuntimeException("Server error")
        val viewModel = createViewModel()
        viewModel.onEvent(OnboardingEvent.OnContinue) // -> step 1
        viewModel.onEvent(OnboardingEvent.OnContinue) // -> step 2

        viewModel.effects.test {
            viewModel.onEvent(OnboardingEvent.OnContinue) // submit

            val effect = awaitItem()
            assertIs<OnboardingEffect.ShowError>(effect)
            assertEquals("Server error", effect.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun createProfileFailureResetsSaving() = runTest {
        profileRepository.shouldThrow = RuntimeException("error")
        val viewModel = createViewModel()
        viewModel.onEvent(OnboardingEvent.OnContinue)
        viewModel.onEvent(OnboardingEvent.OnContinue)
        viewModel.onEvent(OnboardingEvent.OnContinue) // submit

        assertFalse(viewModel.uiState.value.isSaving)
    }
}
