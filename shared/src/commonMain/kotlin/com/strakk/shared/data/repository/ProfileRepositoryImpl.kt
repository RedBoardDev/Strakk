package com.strakk.shared.data.repository

import com.strakk.shared.data.dto.ProfileDto
import com.strakk.shared.data.mapper.toDomain
import com.strakk.shared.data.remote.CurrentUserIdProvider
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

/**
 * Supabase-backed implementation of [ProfileRepository].
 *
 * Queries and mutates the `profiles` table. The current user's ID is obtained
 * from [SupabaseClient.auth] to tie each row to `auth.uid()`.
 */
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
        val shouldFetch = fetchMutex.withLock {
            if (profileFetched) false else { profileFetched = true; true }
        }
        if (shouldFetch) { profileCache.value = getProfile() }
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
            data.proteinGoal?.let { put("protein_goal", it) }
            data.calorieGoal?.let { put("calorie_goal", it) }
            data.waterGoal?.let { put("water_goal", it) }
            data.reminderTrackingTime?.let { put("reminder_tracking_time", it) }
            data.reminderCheckinDay?.let { put("reminder_checkin_day", it) }
            data.reminderCheckinTime?.let { put("reminder_checkin_time", it) }
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

    override suspend fun updateProfile(
        proteinGoal: Int?,
        calorieGoal: Int?,
        waterGoal: Int?,
        reminderTrackingTime: String?,
        reminderCheckinDay: Int?,
        reminderCheckinTime: String?,
    ): UserProfile {
        val userId = userIdProvider.currentOrThrow()

        val json = buildJsonObject {
            if (proteinGoal != null) put("protein_goal", proteinGoal) else put("protein_goal", JsonNull)
            if (calorieGoal != null) put("calorie_goal", calorieGoal) else put("calorie_goal", JsonNull)
            if (waterGoal != null) put("water_goal", waterGoal) else put("water_goal", JsonNull)
            if (reminderTrackingTime != null) put("reminder_tracking_time", reminderTrackingTime) else put("reminder_tracking_time", JsonNull)
            if (reminderCheckinDay != null) put("reminder_checkin_day", reminderCheckinDay) else put("reminder_checkin_day", JsonNull)
            if (reminderCheckinTime != null) put("reminder_checkin_time", reminderCheckinTime) else put("reminder_checkin_time", JsonNull)
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
