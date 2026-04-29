package com.strakk.shared.presentation.meal

import app.cash.turbine.test
import com.strakk.shared.domain.common.ClockProvider
import com.strakk.shared.domain.model.DraftItem
import com.strakk.shared.domain.model.EntrySource
import com.strakk.shared.domain.model.FrequentItem
import com.strakk.shared.domain.model.Meal
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.model.WaterEntry
import com.strakk.shared.domain.repository.MealRepository
import com.strakk.shared.domain.repository.NutritionRepository
import com.strakk.shared.domain.usecase.BuildMealEntryUseCase
import com.strakk.shared.domain.usecase.QuickAddFromPhotoUseCase
import com.strakk.shared.domain.usecase.QuickAddFromTextUseCase
import com.strakk.shared.domain.usecase.QuickAddKnownEntryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import kotlin.test.assertFalse
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class QuickAddViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var nutritionRepository: FakeNutritionRepository

    private val clock = object : ClockProvider {
        override fun today(): LocalDate = LocalDate(2026, 4, 25)
        override fun now(): Instant = Instant.parse("2026-04-25T12:34:56Z")
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        nutritionRepository = FakeNutritionRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addKnownPersistsEntryAndEmitsCompleted() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onEvent(
                QuickAddEvent.AddKnown(
                    name = "Banane",
                    protein = 1.2,
                    calories = 90.0,
                    fat = null,
                    carbs = 20.0,
                    quantity = "100g",
                    source = EntrySource.Search,
                ),
            )

            val effect = assertIs<QuickAddEffect.Completed>(awaitItem())
            assertEquals("created-1", effect.entry.id)
            assertEquals("Banane", nutritionRepository.addedEntries.single().name)
            assertFalse(viewModel.uiState.value.isProcessing)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addFromTextTooShortEmitsValidationError() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onEvent(QuickAddEvent.AddFromText("no"))

            val effect = assertIs<QuickAddEffect.ShowError>(awaitItem())
            assertEquals("La description doit contenir entre 3 et 500 caractères.", effect.message)
            assertEquals(effect.message, viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isProcessing)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createViewModel(): QuickAddViewModel {
        val mealRepository = FakeMealRepository()
        return QuickAddViewModel(
            quickAddKnownEntry = QuickAddKnownEntryUseCase(
                nutritionRepository = nutritionRepository,
                buildMealEntry = BuildMealEntryUseCase(clock),
            ),
            quickAddFromText = QuickAddFromTextUseCase(
                mealRepository = mealRepository,
                nutritionRepository = nutritionRepository,
            ),
            quickAddFromPhoto = QuickAddFromPhotoUseCase(
                mealRepository = mealRepository,
                nutritionRepository = nutritionRepository,
            ),
        )
    }
}

private class FakeNutritionRepository : NutritionRepository {
    val addedEntries = mutableListOf<MealEntry>()

    override fun observeMealsForDate(date: String): Flow<List<MealEntry>> = emptyFlow()
    override fun observeWaterEntriesForDate(date: String): Flow<List<WaterEntry>> = emptyFlow()
    override fun observeNutritionMutations(): Flow<Unit> = emptyFlow()
    override fun observeFrequentItems(limit: Int): Flow<List<FrequentItem>> = emptyFlow()

    override suspend fun addMeal(entry: MealEntry): MealEntry {
        addedEntries += entry
        return entry.copy(id = "created-${addedEntries.size}")
    }

    override suspend fun deleteMeal(id: String) = Unit
    override suspend fun updateMealEntry(entry: MealEntry): MealEntry = entry
    override suspend fun addWater(logDate: String, amount: Int): WaterEntry =
        WaterEntry(id = "water-1", logDate = logDate, amount = amount, createdAt = "2026-04-25T12:34:56Z")

    override suspend fun deleteWater(id: String) = Unit
    override suspend fun getActiveCalendarDays(monthStart: String, monthEnd: String): List<String> = emptyList()
    override fun clearCache() = Unit
}

private class FakeMealRepository : MealRepository {
    override fun observeMealsForDate(date: String): Flow<List<Meal>> = emptyFlow()
    override fun observeMeal(id: String): Flow<Meal?> = emptyFlow()
    override suspend fun createMeal(name: String, date: String): Meal = unused()
    override suspend fun renameMeal(id: String, name: String) = Unit
    override suspend fun deleteMeal(id: String) = Unit
    override suspend fun commitMealDraft(
        draftId: String,
        name: String,
        date: String,
        entries: List<DraftItem.Resolved>,
        photoPathsByItemId: Map<String, String>,
    ): Meal = unused()

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
    ): DraftItem.Resolved = unused()

    override suspend fun analyzeTextSingle(
        description: String,
        draftItemId: String,
    ): DraftItem.Resolved = unused()

    override suspend fun analyzePhotoForQuickAdd(
        imageBase64: String,
        hint: String?,
        logDate: String,
    ): MealEntry = quickAddEntry(logDate = logDate, source = EntrySource.PhotoAi)

    override suspend fun analyzeTextForQuickAdd(
        description: String,
        logDate: String,
    ): MealEntry = quickAddEntry(logDate = logDate, source = EntrySource.TextAi)

    private fun quickAddEntry(logDate: String, source: EntrySource): MealEntry = MealEntry(
        id = "",
        logDate = logDate,
        name = "Analyzed",
        protein = 10.0,
        calories = 100.0,
        fat = null,
        carbs = null,
        source = source,
        createdAt = "2026-04-25T12:34:56Z",
    )

    private fun unused(): Nothing = error("Unused in QuickAddViewModelTest.")
}
