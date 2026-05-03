package com.strakk.shared.presentation.today

import app.cash.turbine.test
import com.strakk.shared.domain.common.ClockProvider
import com.strakk.shared.domain.model.ActiveMealDraft
import com.strakk.shared.domain.model.DailySummary
import com.strakk.shared.domain.model.DraftItem
import com.strakk.shared.domain.model.Meal
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.model.SubscriptionPlan
import com.strakk.shared.domain.model.SubscriptionState
import com.strakk.shared.domain.model.WaterEntry
import com.strakk.shared.domain.repository.MealDraftRepository
import com.strakk.shared.domain.repository.MealRepository
import com.strakk.shared.domain.repository.NutritionRepository
import com.strakk.shared.domain.usecase.AddWaterUseCase
import com.strakk.shared.domain.usecase.DeleteMealContainerUseCase
import com.strakk.shared.domain.usecase.DeleteMealUseCase
import com.strakk.shared.domain.usecase.DeleteWaterUseCase
import com.strakk.shared.domain.usecase.ObserveActiveMealDraftUseCase
import com.strakk.shared.domain.usecase.ObserveDailySummaryUseCase
import com.strakk.shared.domain.usecase.ObserveMealContainersForDateUseCase
import com.strakk.shared.domain.usecase.ObserveMealsForDateUseCase
import com.strakk.shared.domain.usecase.ObserveSubscriptionStateUseCase
import com.strakk.shared.domain.usecase.ObserveWaterEntriesForDateUseCase
import com.strakk.shared.domain.usecase.RemoveLastWaterEntryUseCase
import com.strakk.shared.domain.usecase.UpdateMealEntryUseCase
import com.strakk.shared.fixtures.FakeProfileRepository
import com.strakk.shared.fixtures.FakeSubscriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var subscriptionRepository: FakeSubscriptionRepository
    private lateinit var nutritionRepository: FakeTodayNutritionRepository
    private lateinit var mealRepository: FakeTodayMealRepository
    private lateinit var draftRepository: FakeTodayDraftRepository
    private lateinit var profileRepository: FakeProfileRepository

    /** Fixed "now" anchored to a known date so trial countdown is deterministic. */
    private val fixedNow = Instant.parse("2026-05-03T12:00:00Z")
    private val fixedToday = LocalDate(2026, 5, 3)

    private val clock = object : ClockProvider {
        override fun today(): LocalDate = fixedToday
        override fun now(): Instant = fixedNow
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        subscriptionRepository = FakeSubscriptionRepository()
        nutritionRepository = FakeTodayNutritionRepository()
        mealRepository = FakeTodayMealRepository()
        draftRepository = FakeTodayDraftRepository()
        profileRepository = FakeProfileRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): TodayViewModel = TodayViewModel(
        observeDailySummary = ObserveDailySummaryUseCase(
            nutritionRepository = nutritionRepository,
            mealRepository = mealRepository,
            profileRepository = profileRepository,
        ),
        observeMeals = ObserveMealsForDateUseCase(nutritionRepository),
        observeMealContainers = ObserveMealContainersForDateUseCase(mealRepository),
        observeWaterEntries = ObserveWaterEntriesForDateUseCase(nutritionRepository),
        observeActiveDraft = ObserveActiveMealDraftUseCase(draftRepository),
        addWater = AddWaterUseCase(nutritionRepository),
        deleteWater = DeleteWaterUseCase(nutritionRepository),
        removeLastWaterEntry = RemoveLastWaterEntryUseCase(nutritionRepository),
        deleteOrphanEntry = DeleteMealUseCase(nutritionRepository),
        deleteMealContainer = DeleteMealContainerUseCase(mealRepository),
        updateEntry = UpdateMealEntryUseCase(nutritionRepository, mealRepository),
        observeSubscriptionState = ObserveSubscriptionStateUseCase(subscriptionRepository),
        clock = clock,
    )

    // -------------------------------------------------------------------------
    // Trial banner — subscription-driven tests
    // -------------------------------------------------------------------------

    @Test
    fun `trial with 2 days remaining shows ExpiringIn 2`() = runTest {
        // Trial ends in exactly 2 days from fixedNow
        val endsAt = Instant.parse("2026-05-05T12:00:00Z")
        subscriptionRepository.emit(SubscriptionState.Trial(endsAt = endsAt))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertIs<TodayUiState.Ready>(state)
            val banner = assertIs<TrialBannerState.ExpiringIn>(state.trialBanner)
            assertEquals(2, banner.daysRemaining)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `trial with 1 day remaining shows ExpiringIn 1`() = runTest {
        val endsAt = Instant.parse("2026-05-04T12:00:00Z")
        subscriptionRepository.emit(SubscriptionState.Trial(endsAt = endsAt))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertIs<TodayUiState.Ready>(state)
            val banner = assertIs<TrialBannerState.ExpiringIn>(state.trialBanner)
            assertEquals(1, banner.daysRemaining)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `trial with 5 days remaining shows no banner`() = runTest {
        val endsAt = Instant.parse("2026-05-08T12:00:00Z")
        subscriptionRepository.emit(SubscriptionState.Trial(endsAt = endsAt))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertIs<TodayUiState.Ready>(state)
            assertNull(state.trialBanner)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `free subscription shows no trial banner`() = runTest {
        subscriptionRepository.emit(SubscriptionState.Free)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertIs<TodayUiState.Ready>(state)
            assertNull(state.trialBanner)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `active subscription shows no trial banner`() = runTest {
        subscriptionRepository.emit(
            SubscriptionState.Active(
                plan = SubscriptionPlan.ANNUAL,
                expiresAt = Instant.parse("2027-01-01T00:00:00Z"),
            ),
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertIs<TodayUiState.Ready>(state)
            assertNull(state.trialBanner)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `subscription update to trial with 2 days updates banner`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertIs<TodayUiState.Ready>(initial)
            assertNull(initial.trialBanner)

            subscriptionRepository.emit(
                SubscriptionState.Trial(endsAt = Instant.parse("2026-05-05T12:00:00Z")),
            )

            val updated = awaitItem()
            assertIs<TodayUiState.Ready>(updated)
            val banner = assertIs<TrialBannerState.ExpiringIn>(updated.trialBanner)
            assertEquals(2, banner.daysRemaining)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -------------------------------------------------------------------------
    // OnTrialBannerTapped effect
    // -------------------------------------------------------------------------

    @Test
    fun `OnTrialBannerTapped emits NavigateToPaywall`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onEvent(TodayEvent.OnTrialBannerTapped)

            assertIs<TodayEffect.NavigateToPaywall>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

// =============================================================================
// Minimal fakes scoped to TodayViewModel tests
// =============================================================================

private class FakeTodayNutritionRepository : NutritionRepository {
    private val entriesFlow = MutableStateFlow<List<MealEntry>>(emptyList())
    private val waterFlow = MutableStateFlow<List<WaterEntry>>(emptyList())

    override fun observeMealsForDate(date: String): Flow<List<MealEntry>> = entriesFlow
    override fun observeWaterEntriesForDate(date: String): Flow<List<WaterEntry>> = waterFlow
    override fun observeNutritionMutations(): Flow<Unit> = emptyFlow()
    override fun observeFrequentItems(limit: Int): Flow<List<com.strakk.shared.domain.model.FrequentItem>> = emptyFlow()

    override suspend fun addMeal(entry: MealEntry): MealEntry = entry
    override suspend fun deleteMeal(id: String) = Unit
    override suspend fun updateMealEntry(entry: MealEntry): MealEntry = entry
    override suspend fun addWater(logDate: String, amount: Int): WaterEntry =
        WaterEntry(id = "w-1", logDate = logDate, amount = amount, createdAt = "2026-05-03T12:00:00Z")

    override suspend fun deleteWater(id: String) = Unit
    override suspend fun getActiveCalendarDays(monthStart: String, monthEnd: String): List<String> = emptyList()
    override fun clearCache() = Unit
}

private class FakeTodayMealRepository : MealRepository {
    private val mealsFlow = MutableStateFlow<List<Meal>>(emptyList())

    override fun observeMealsForDate(date: String): Flow<List<Meal>> = mealsFlow
    override fun observeMeal(id: String): Flow<Meal?> = emptyFlow()
    override suspend fun createMeal(name: String, date: String): Meal = notUsed()
    override suspend fun renameMeal(id: String, name: String) = Unit
    override suspend fun deleteMeal(id: String) = Unit
    override suspend fun commitMealDraft(
        draftId: String,
        name: String,
        date: String,
        entries: List<DraftItem.Resolved>,
        photoPathsByItemId: Map<String, String>,
    ): Meal = notUsed()

    override suspend fun addEntryToMeal(mealId: String, entry: DraftItem.Resolved) = Unit
    override fun updateEntryInCache(entry: MealEntry) = Unit
    override fun clearCache() = Unit
    override suspend fun extractMealDraftBatch(
        draftId: String,
        photoItems: List<MealRepository.ExtractPhotoItem>,
        textItems: List<MealRepository.ExtractTextItem>,
    ): List<MealRepository.ExtractItemResult> = emptyList()

    override suspend fun analyzePhotoSingle(
        imageBase64: String,
        hint: String?,
        draftItemId: String,
    ): DraftItem.Resolved = notUsed()

    override suspend fun analyzeTextSingle(
        description: String,
        draftItemId: String,
    ): DraftItem.Resolved = notUsed()

    override suspend fun analyzePhotoForQuickAdd(
        imageBase64: String,
        hint: String?,
        logDate: String,
    ): MealEntry = notUsed()

    override suspend fun analyzeTextForQuickAdd(
        description: String,
        logDate: String,
    ): MealEntry = notUsed()

    private fun notUsed(): Nothing = error("Not used in TodayViewModelTest")
}

private class FakeTodayDraftRepository : MealDraftRepository {
    private val draftFlow = MutableStateFlow<ActiveMealDraft?>(null)

    override fun observeActiveDraft(): Flow<ActiveMealDraft?> = draftFlow
    override suspend fun createDraft(name: String, date: String): ActiveMealDraft = error("Not used")
    override suspend fun addItem(item: DraftItem) = Unit
    override suspend fun removeItem(itemId: String) = Unit
    override suspend fun rename(name: String) = Unit
    override suspend fun discard() = Unit
    override suspend fun markItemResolved(itemId: String, entry: MealEntry) = Unit
    override suspend fun recordUploadedPath(itemId: String, path: String) = Unit
}
