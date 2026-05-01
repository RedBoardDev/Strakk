package com.strakk.shared.domain.repository

import com.strakk.shared.domain.model.NutritionGoals
import com.strakk.shared.domain.model.OnboardingData
import com.strakk.shared.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Operations on the `profiles` table.
 *
 * Implementations live in the data layer and are `internal`.
 * Methods throw on failure — use cases wrap calls in [runCatching].
 */
interface ProfileRepository {

    /**
     * Checks whether a `profiles` row exists for the current authenticated user.
     *
     * @return `true` if the profile exists (returning user), `false` otherwise.
     * @throws Exception on network or database errors.
     */
    suspend fun profileExists(): Boolean

    /**
     * Creates a new `profiles` row for the current authenticated user.
     *
     * Called after onboarding completes. The profile ID is set to `auth.uid()`
     * by the data layer implementation.
     *
     * @param data Goals collected during onboarding.
     * @return The created [UserProfile].
     * @throws Exception on network, database, or conflict errors.
     */
    suspend fun createProfile(data: OnboardingData): UserProfile

    /**
     * Fetches the `profiles` row for the current authenticated user.
     *
     * @return The [UserProfile] if it exists, or `null` if no profile has been created yet.
     * @throws Exception on network or database errors.
     */
    suspend fun getProfile(): UserProfile?

    /**
     * Updates the current user's profile with the given values.
     *
     * Null values are explicitly stored as NULL in the database (clearing a goal).
     * Called from the Settings screen with per-field debounce.
     *
     * @param proteinGoal Daily protein goal in grams, or null to clear.
     * @param calorieGoal Daily calorie goal in kcal, or null to clear.
     * @param waterGoal Daily water goal in mL, or null to clear.
     * @return The updated [UserProfile].
     * @throws Exception on network or database errors.
     */
    suspend fun updateProfile(
        proteinGoal: Int?,
        calorieGoal: Int?,
        waterGoal: Int?,
    ): UserProfile

    /**
     * Retrieves the decrypted Hevy API key for the current authenticated user.
     *
     * @return The API key string, or `null` if not configured.
     * @throws Exception on network or database errors.
     */
    suspend fun getHevyApiKey(): String?

    /**
     * Stores the Hevy API key (encrypted) for the current authenticated user.
     *
     * @param apiKey The Hevy API key to save.
     * @throws Exception on network or database errors.
     */
    suspend fun updateHevyApiKey(apiKey: String)

    /**
     * Saves the nutrition goals and marks onboarding as completed in a single update.
     *
     * Called from the Day Preview (step 7) to finalize the onboarding flow.
     *
     * @param goals Nutrition goals chosen by the user (or AI-calculated).
     * @return The updated [UserProfile].
     * @throws Exception on network or database errors.
     */
    suspend fun completeOnboarding(goals: NutritionGoals): UserProfile

    /**
     * Returns a [Flow] that emits the current [UserProfile] (or null if none),
     * fetching from the network on first subscription and updating on mutations.
     */
    fun observeProfile(): Flow<UserProfile?>

    /**
     * Clears the in-memory profile cache, forcing the next observation to re-fetch.
     */
    fun clearCache()
}
