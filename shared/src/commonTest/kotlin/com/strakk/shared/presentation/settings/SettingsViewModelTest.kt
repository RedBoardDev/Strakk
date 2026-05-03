package com.strakk.shared.presentation.settings

import app.cash.turbine.test
import com.strakk.shared.domain.model.CheckIn
import com.strakk.shared.domain.model.CheckInInput
import com.strakk.shared.domain.model.CheckInListItem
import com.strakk.shared.domain.model.CheckInMeasurements
import com.strakk.shared.domain.model.CheckInPhoto
import com.strakk.shared.domain.model.CheckInSeriesPoint
import com.strakk.shared.domain.model.NutritionAverages
import com.strakk.shared.domain.model.NutritionGoals
import com.strakk.shared.domain.model.SubscriptionPlan
import com.strakk.shared.domain.model.SubscriptionState
import com.strakk.shared.domain.model.ActiveMealDraft
import com.strakk.shared.domain.model.DraftItem
import com.strakk.shared.domain.model.FrequentItem
import com.strakk.shared.domain.model.Meal
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.model.WaterEntry
import com.strakk.shared.domain.repository.CheckInRepository
import com.strakk.shared.domain.repository.MealDraftRepository
import com.strakk.shared.domain.repository.MealRepository
import com.strakk.shared.domain.repository.NutritionRepository
import app.cash.turbine.ReceiveTurbine
import com.strakk.shared.domain.usecase.GetCurrentUserEmailUseCase
import com.strakk.shared.domain.usecase.GetHevyApiKeyUseCase
import com.strakk.shared.domain.usecase.ObserveProfileUseCase
import com.strakk.shared.domain.usecase.ObserveSubscriptionStateUseCase
import com.strakk.shared.domain.usecase.SaveHevyApiKeyUseCase
import com.strakk.shared.domain.usecase.SignOutUseCase
import com.strakk.shared.domain.usecase.UpdateProfileUseCase
import com.strakk.shared.fixtures.FakeAuthRepository
import com.strakk.shared.fixtures.FakeProfileRepository
import com.strakk.shared.fixtures.FakeSubscriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var subscriptionRepository: FakeSubscriptionRepository
    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var authRepository: FakeAuthRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        subscriptionRepository = FakeSubscriptionRepository()
        profileRepository = FakeProfileRepository()
        authRepository = FakeAuthRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SettingsViewModel = SettingsViewModel(
        getCurrentUserEmail = GetCurrentUserEmailUseCase(authRepository),
        observeProfile = ObserveProfileUseCase(profileRepository),
        updateProfile = UpdateProfileUseCase(profileRepository),
        signOut = SignOutUseCase(
            authRepository = authRepository,
            nutritionRepository = FakeSettingsNutritionRepository(),
            profileRepository = profileRepository,
            mealRepository = FakeSettingsMealRepository(),
            checkInRepository = FakeCheckInRepository(),
            mealDraftRepository = FakeSettingsDraftRepository(),
        ),
        saveHevyApiKey = SaveHevyApiKeyUseCase(profileRepository),
        getHevyApiKey = GetHevyApiKeyUseCase(profileRepository),
        observeSubscriptionState = ObserveSubscriptionStateUseCase(subscriptionRepository),
    )

    // -------------------------------------------------------------------------
    // subscriptionDisplay mapping
    // -------------------------------------------------------------------------

    @Test
    fun `free subscription maps to SubscriptionDisplay Free`() = runTest {
        subscriptionRepository.emit(SubscriptionState.Free)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val ready = awaitReady()
            assertIs<SubscriptionDisplay.Free>(ready.subscriptionDisplay)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `expired subscription maps to SubscriptionDisplay Free`() = runTest {
        subscriptionRepository.emit(SubscriptionState.Expired)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val ready = awaitReady()
            assertIs<SubscriptionDisplay.Free>(ready.subscriptionDisplay)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `trial subscription maps to SubscriptionDisplay Trial with correct daysRemaining`() = runTest {
        val endsAt = Instant.fromEpochMilliseconds(
            kotlinx.datetime.Clock.System.now().toEpochMilliseconds() + 7L * 24 * 60 * 60 * 1000 + 60_000,
        )
        subscriptionRepository.emit(SubscriptionState.Trial(endsAt = endsAt))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val ready = awaitReady()
            val display = assertIs<SubscriptionDisplay.Trial>(ready.subscriptionDisplay)
            assertEquals(7, display.daysRemaining)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `trial subscription with past endsAt clamps daysRemaining to 0`() = runTest {
        val endsAt = Instant.parse("2020-01-01T00:00:00Z")
        subscriptionRepository.emit(SubscriptionState.Trial(endsAt = endsAt))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val ready = awaitReady()
            val display = assertIs<SubscriptionDisplay.Trial>(ready.subscriptionDisplay)
            assertEquals(0, display.daysRemaining)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `active monthly subscription maps to Active with Mensuel label`() = runTest {
        subscriptionRepository.emit(
            SubscriptionState.Active(
                plan = SubscriptionPlan.MONTHLY,
                expiresAt = Instant.parse("2026-06-03T00:00:00Z"),
            ),
        )
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val ready = awaitReady()
            val display = assertIs<SubscriptionDisplay.Active>(ready.subscriptionDisplay)
            assertEquals("Mensuel", display.planLabel)
            assertEquals("2026-06-03", display.expiresLabel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `active annual subscription maps to Active with Annuel label`() = runTest {
        subscriptionRepository.emit(
            SubscriptionState.Active(
                plan = SubscriptionPlan.ANNUAL,
                expiresAt = Instant.parse("2027-05-03T00:00:00Z"),
            ),
        )
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val ready = awaitReady()
            val display = assertIs<SubscriptionDisplay.Active>(ready.subscriptionDisplay)
            assertEquals("Annuel", display.planLabel)
            assertEquals("2027-05-03", display.expiresLabel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `payment failed subscription maps to SubscriptionDisplay PaymentFailed`() = runTest {
        subscriptionRepository.emit(SubscriptionState.PaymentFailed)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val ready = awaitReady()
            assertIs<SubscriptionDisplay.PaymentFailed>(ready.subscriptionDisplay)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -------------------------------------------------------------------------
    // One-shot effects
    // -------------------------------------------------------------------------

    @Test
    fun `OnUpgradeTapped emits NavigateToPaywall`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onEvent(SettingsEvent.OnUpgradeTapped)

            assertIs<SettingsEffect.NavigateToPaywall>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `OnManageSubscription emits ShowToast`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onEvent(SettingsEvent.OnManageSubscription)

            assertIs<SettingsEffect.ShowToast>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `OnRestorePurchase emits ShowToast`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onEvent(SettingsEvent.OnRestorePurchase)

            assertIs<SettingsEffect.ShowToast>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private suspend fun ReceiveTurbine<SettingsUiState>.awaitReady(): SettingsUiState.Ready {
    var state = awaitItem()
    while (state !is SettingsUiState.Ready) state = awaitItem()
    return state
}

// =============================================================================
// Minimal fakes scoped to SettingsViewModelTest
// =============================================================================

private class FakeCheckInRepository : CheckInRepository {
    override fun observeCheckIns(): Flow<List<CheckInListItem>> = emptyFlow()
    override fun observeCheckIn(id: String): Flow<CheckIn?> = emptyFlow()
    override fun observeCheckInSeries(): Flow<List<CheckInSeriesPoint>> = emptyFlow()
    override suspend fun createCheckIn(input: CheckInInput): CheckIn = error("Not used")
    override suspend fun updateCheckIn(id: String, input: CheckInInput): CheckIn = error("Not used")
    override suspend fun deleteCheckIn(id: String) = Unit
    override suspend fun uploadPhoto(checkinId: String, imageData: ByteArray, position: Int): CheckInPhoto = error("Not used")
    override suspend fun deletePhoto(photoId: String, storagePath: String) = Unit
    override suspend fun getPhotoUrl(storagePath: String): String = ""
    override suspend fun computeNutritionAverages(dates: List<String>): NutritionAverages = error("Not used")
    override suspend fun generateAiSummary(
        averages: NutritionAverages,
        goals: NutritionGoals,
        weightKg: Double?,
        feelingTags: List<String>,
        mentalFeeling: String,
        physicalFeeling: String,
    ): String = ""
    override suspend fun getPreviousMeasurements(weekLabel: String): CheckInMeasurements? = null
    override suspend fun checkExistingForWeek(weekLabel: String): String? = null
    override suspend fun clearCache() = Unit
}

private class FakeSettingsNutritionRepository : NutritionRepository {
    override fun observeMealsForDate(date: String): Flow<List<MealEntry>> = emptyFlow()
    override fun observeWaterEntriesForDate(date: String): Flow<List<WaterEntry>> = emptyFlow()
    override fun observeNutritionMutations(): Flow<Unit> = emptyFlow()
    override fun observeFrequentItems(limit: Int): Flow<List<FrequentItem>> = emptyFlow()
    override suspend fun addMeal(entry: MealEntry): MealEntry = entry
    override suspend fun deleteMeal(id: String) = Unit
    override suspend fun updateMealEntry(entry: MealEntry): MealEntry = entry
    override suspend fun addWater(logDate: String, amount: Int): WaterEntry = error("Not used")
    override suspend fun deleteWater(id: String) = Unit
    override suspend fun getActiveCalendarDays(monthStart: String, monthEnd: String): List<String> = emptyList()
    override fun clearCache() = Unit
}

private class FakeSettingsMealRepository : MealRepository {
    override fun observeMealsForDate(date: String): Flow<List<Meal>> = emptyFlow()
    override fun observeMeal(id: String): Flow<Meal?> = emptyFlow()
    override suspend fun createMeal(name: String, date: String): Meal = error("Not used")
    override suspend fun renameMeal(id: String, name: String) = Unit
    override suspend fun deleteMeal(id: String) = Unit
    override suspend fun commitMealDraft(
        draftId: String,
        name: String,
        date: String,
        entries: List<DraftItem.Resolved>,
        photoPathsByItemId: Map<String, String>,
    ): Meal = error("Not used")
    override suspend fun addEntryToMeal(mealId: String, entry: DraftItem.Resolved) = Unit
    override fun updateEntryInCache(entry: MealEntry) = Unit
    override fun clearCache() = Unit
    override suspend fun extractMealDraftBatch(
        draftId: String,
        photoItems: List<MealRepository.ExtractPhotoItem>,
        textItems: List<MealRepository.ExtractTextItem>,
    ): List<MealRepository.ExtractItemResult> = emptyList()
    override suspend fun analyzePhotoSingle(imageBase64: String, hint: String?, draftItemId: String): DraftItem.Resolved = error("Not used")
    override suspend fun analyzeTextSingle(description: String, draftItemId: String): DraftItem.Resolved = error("Not used")
    override suspend fun analyzePhotoForQuickAdd(imageBase64: String, hint: String?, logDate: String): MealEntry = error("Not used")
    override suspend fun analyzeTextForQuickAdd(description: String, logDate: String): MealEntry = error("Not used")
}

private class FakeSettingsDraftRepository : MealDraftRepository {
    override fun observeActiveDraft(): Flow<ActiveMealDraft?> = emptyFlow()
    override suspend fun createDraft(name: String, date: String): ActiveMealDraft = error("Not used")
    override suspend fun addItem(item: DraftItem) = Unit
    override suspend fun removeItem(itemId: String) = Unit
    override suspend fun rename(name: String) = Unit
    override suspend fun discard() = Unit
    override suspend fun markItemResolved(itemId: String, entry: MealEntry) = Unit
    override suspend fun recordUploadedPath(itemId: String, path: String) = Unit
}
