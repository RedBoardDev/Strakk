package com.strakk.shared.data.repository

import com.strakk.shared.data.dto.ProfileDto
import com.strakk.shared.data.mapper.toDomain
import com.strakk.shared.data.remote.CurrentUserIdProvider
import com.strakk.shared.domain.model.NutritionGoals
import com.strakk.shared.domain.model.OnboardingData
import com.strakk.shared.domain.model.UserProfile
import com.strakk.shared.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

internal class ProfileRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val userIdProvider: CurrentUserIdProvider,
) : ProfileRepository {

    private val profileCache = MutableStateFlow<UserProfile?>(null)
    private var profileFetched = false
    private val fetchMutex = Mutex()

    override fun observeProfile(): Flow<UserProfile?> =
        profileCache.onStart { ensureProfileFetched() }

    override fun clearCache() {
        profileCache.value = null
        profileFetched = false
    }

    private suspend fun ensureProfileFetched() {
        fetchMutex.withLock {
            if (!profileFetched) {
                profileFetched = true
                profileCache.value = getProfile()
            }
        }
    }

    override suspend fun profileExists(): Boolean {
        val userId = try {
            userIdProvider.currentOrThrow()
        } catch (_: Exception) {
            return false
        }

        return try {
            supabaseClient
                .from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeList<ProfileDto>()
                .isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun getProfile(): UserProfile? {
        val userId = userIdProvider.currentOrThrow()

        return supabaseClient
            .from("profiles")
            .select {
                filter {
                    eq("id", userId)
                }
            }
            .decodeList<ProfileDto>()
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun createProfile(data: OnboardingData): UserProfile {
        val userId = userIdProvider.currentOrThrow()

        val json = buildJsonObject {
            put("id", userId)
            put("weight_kg", data.weightKg)
            data.heightCm?.let { put("height_cm", it) }
            data.birthDate?.let { put("birth_date", it.toString()) }
            data.biologicalSex?.let { put("biological_sex", it.name.lowercase()) }
            data.fitnessGoal?.let { put("fitness_goal", it.name.lowercase()) }
            data.trainingFrequency?.let { put("training_frequency", it) }
            if (data.trainingTypes.isNotEmpty()) {
                putJsonArray("training_types") {
                    data.trainingTypes.forEach { add(kotlinx.serialization.json.JsonPrimitive(it.name.lowercase())) }
                }
            }
            data.trainingIntensity?.let { put("training_intensity", it.name.lowercase()) }
            data.dailyActivityLevel?.let { put("daily_activity_level", it.name.lowercase()) }
            data.proteinGoal?.let { put("protein_goal", it) }
            data.calorieGoal?.let { put("calorie_goal", it) }
            data.fatGoal?.let { put("fat_goal", it) }
            data.carbGoal?.let { put("carb_goal", it) }
            data.waterGoal?.let { put("water_goal", it) }
            put("onboarding_completed", false)
        }

        val dto = supabaseClient
            .from("profiles")
            .upsert(json) {
                select()
            }
            .decodeSingle<ProfileDto>()

        val profile = dto.toDomain()
        profileCache.value = profile
        return profile
    }

    override suspend fun completeOnboarding(goals: NutritionGoals): UserProfile {
        val userId = userIdProvider.currentOrThrow()

        val json = buildJsonObject {
            if (goals.proteinGoal != null) put("protein_goal", goals.proteinGoal) else put("protein_goal", JsonNull)
            if (goals.calorieGoal != null) put("calorie_goal", goals.calorieGoal) else put("calorie_goal", JsonNull)
            if (goals.fatGoal != null) put("fat_goal", goals.fatGoal) else put("fat_goal", JsonNull)
            if (goals.carbGoal != null) put("carb_goal", goals.carbGoal) else put("carb_goal", JsonNull)
            if (goals.waterGoal != null) put("water_goal", goals.waterGoal) else put("water_goal", JsonNull)
            put("onboarding_completed", true)
            put("updated_at", Clock.System.now().toString())
        }

        val profile = supabaseClient
            .from("profiles")
            .update(json) {
                select()
                filter { eq("id", userId) }
            }
            .decodeSingle<ProfileDto>()
            .toDomain()

        profileCache.value = profile
        return profile
    }

    override suspend fun updateProfile(
        proteinGoal: Int?,
        calorieGoal: Int?,
        waterGoal: Int?,
    ): UserProfile {
        val userId = userIdProvider.currentOrThrow()

        val json = buildJsonObject {
            if (proteinGoal != null) put("protein_goal", proteinGoal) else put("protein_goal", JsonNull)
            if (calorieGoal != null) put("calorie_goal", calorieGoal) else put("calorie_goal", JsonNull)
            if (waterGoal != null) put("water_goal", waterGoal) else put("water_goal", JsonNull)
            put("updated_at", Clock.System.now().toString())
        }

        val profile = supabaseClient
            .from("profiles")
            .update(json) {
                select()
                filter { eq("id", userId) }
            }
            .decodeSingle<ProfileDto>()
            .toDomain()

        profileCache.value = profile
        return profile
    }

    override suspend fun getHevyApiKey(): String? {
        val result = supabaseClient.postgrest.rpc("get_hevy_api_key")
        val text = result.data
        if (text.isBlank() || text == "null") return null
        return text.trim('"')
    }

    override suspend fun updateHevyApiKey(apiKey: String) {
        val body = buildJsonObject {
            put("plain_key", apiKey)
        }

        supabaseClient.postgrest.rpc("save_hevy_api_key", body)
    }
}
