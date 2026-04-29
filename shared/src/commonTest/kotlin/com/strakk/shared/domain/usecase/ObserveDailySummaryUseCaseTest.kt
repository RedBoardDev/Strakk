package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.DailySummary
import com.strakk.shared.domain.model.EntrySource
import com.strakk.shared.domain.model.Meal
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.model.MealStatus
import com.strakk.shared.domain.model.UserProfile
import com.strakk.shared.domain.model.WaterEntry
import com.strakk.shared.domain.repository.MealRepository
import com.strakk.shared.domain.repository.NutritionRepository
import com.strakk.shared.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveDailySummaryUseCaseTest {

    @Test
    fun summaryIncludesOrphanEntriesAndMealContainerEntries() = runTest {
        val nutritionRepository = FakeNutritionRepository(
            meals = listOf(entry(id = "orphan", protein = 10.0, calories = 100.0)),
            water = listOf(
                WaterEntry(
                    id = "water",
                    logDate = "2026-04-25",
                    amount = 500,
                    createdAt = "2026-04-25T12:00:00Z",
                ),
            ),
        )
        val mealRepository = FakeMealRepository(
            meals = listOf(
                meal(
                    entries = listOf(
                        entry(id = "meal-entry-1", protein = 20.0, calories = 220.0, fat = 7.0, carbs = 12.0),
                        entry(id = "meal-entry-2", protein = 5.0, calories = 80.0, fat = 1.0, carbs = 10.0),
                    ),
                ),
            ),
        )
        val profileRepository = FakeProfileRepository()

        val summary = ObserveDailySummaryUseCase(
            nutritionRepository = nutritionRepository,
            mealRepository = mealRepository,
            profileRepository = profileRepository,
        )("2026-04-25").first()

        assertEquals(
            DailySummary(
                totalProtein = 35.0,
                totalCalories = 400.0,
                totalFat = 8.0,
                totalCarbs = 22.0,
                totalWater = 500,
                proteinGoal = 150,
                calorieGoal = 2200,
                waterGoal = 2500,
            ),
            summary,
        )
    }

    private fun entry(
        id: String,
        protein: Double,
        calories: Double,
        fat: Double? = null,
        carbs: Double? = null,
    ) = MealEntry(
        id = id,
        logDate = "2026-04-25",
        name = id,
        protein = protein,
        calories = calories,
        fat = fat,
        carbs = carbs,
        source = EntrySource.Manual,
        createdAt = "2026-04-25T12:00:00Z",
    )

    private fun meal(entries: List<MealEntry>) = Meal(
        id = "meal",
        userId = "user",
        date = "2026-04-25",
        name = "Déjeuner",
        status = MealStatus.Processed,
        createdAt = Instant.parse("2026-04-25T12:00:00Z"),
        entries = entries,
    )
}

private class FakeNutritionRepository(
    meals: List<MealEntry>,
    water: List<WaterEntry>,
) : NutritionRepository {
    private val mealsFlow = MutableStateFlow(meals)
    private val waterFlow = MutableStateFlow(water)

    override fun observeMealsForDate(date: String): Flow<List<MealEntry>> = mealsFlow
    override fun observeWaterEntriesForDate(date: String): Flow<List<WaterEntry>> = waterFlow
    override fun observeNutritionMutations(): Flow<Unit> = MutableStateFlow(Unit)
    override fun observeFrequentItems(limit: Int) = MutableStateFlow(emptyList<com.strakk.shared.domain.model.FrequentItem>())
    override suspend fun addMeal(entry: MealEntry): MealEntry = entry
    override suspend fun deleteMeal(id: String) = Unit
    override suspend fun updateMealEntry(entry: MealEntry): MealEntry = entry
    override suspend fun addWater(logDate: String, amount: Int): WaterEntry =
        WaterEntry("water", logDate, amount, "2026-04-25T12:00:00Z")
    override suspend fun deleteWater(id: String) = Unit
    override suspend fun getActiveCalendarDays(monthStart: String, monthEnd: String): List<String> = emptyList()
    override fun clearCache() = Unit
}

private class FakeMealRepository(
    meals: List<Meal>,
) : MealRepository {
    private val mealsFlow = MutableStateFlow(meals)

    override fun observeMealsForDate(date: String): Flow<List<Meal>> = mealsFlow
    override fun observeMeal(id: String): Flow<Meal?> = MutableStateFlow(mealsFlow.value.firstOrNull { it.id == id })
    override suspend fun createMeal(name: String, date: String): Meal = error("Not used")
    override suspend fun renameMeal(id: String, name: String) = Unit
    override suspend fun deleteMeal(id: String) = Unit
    override suspend fun commitMealDraft(
        draftId: String,
        name: String,
        date: String,
        entries: List<com.strakk.shared.domain.model.DraftItem.Resolved>,
        photoPathsByItemId: Map<String, String>,
    ): Meal = error("Not used")
    override suspend fun addEntryToMeal(
        mealId: String,
        entry: com.strakk.shared.domain.model.DraftItem.Resolved,
    ) = Unit
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
    ): com.strakk.shared.domain.model.DraftItem.Resolved = error("Not used")
    override suspend fun analyzeTextSingle(
        description: String,
        draftItemId: String,
    ): com.strakk.shared.domain.model.DraftItem.Resolved = error("Not used")
    override suspend fun analyzePhotoForQuickAdd(
        imageBase64: String,
        hint: String?,
        logDate: String,
    ): MealEntry = error("Not used")
    override suspend fun analyzeTextForQuickAdd(description: String, logDate: String): MealEntry = error("Not used")
}

private class FakeProfileRepository : ProfileRepository {
    private val profile = MutableStateFlow(
        UserProfile(
            id = "user",
            proteinGoal = 150,
            calorieGoal = 2200,
            waterGoal = 2500,
        ),
    )

    override suspend fun profileExists(): Boolean = true
    override suspend fun createProfile(data: com.strakk.shared.domain.model.OnboardingData): UserProfile = profile.value
    override suspend fun getProfile(): UserProfile? = profile.value
    override suspend fun updateProfile(
        proteinGoal: Int?,
        calorieGoal: Int?,
        waterGoal: Int?,
    ): UserProfile = profile.value
    override suspend fun getHevyApiKey(): String? = null
    override suspend fun updateHevyApiKey(apiKey: String) = Unit
    override fun observeProfile(): Flow<UserProfile?> = profile
    override fun clearCache() = Unit
}
