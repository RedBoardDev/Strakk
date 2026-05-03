package com.strakk.shared.data.repository

import com.strakk.shared.data.dto.SubscriptionDto
import com.strakk.shared.data.mapper.toDomain
import com.strakk.shared.data.remote.CurrentUserIdProvider
import com.strakk.shared.domain.model.SubscriptionState
import com.strakk.shared.domain.repository.SubscriptionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class SubscriptionRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val userIdProvider: CurrentUserIdProvider,
) : SubscriptionRepository {

    private val cache = MutableStateFlow<SubscriptionState>(SubscriptionState.Free)
    private var fetched = false
    private val fetchMutex = Mutex()

    override fun observeState(): Flow<SubscriptionState> = cache.onStart { ensureFetched() }

    override suspend fun getState(): SubscriptionState {
        ensureFetched()
        return cache.value
    }

    override suspend fun refreshState() {
        fetchMutex.withLock {
            cache.value = fetchFromRemote()
            fetched = true
        }
    }

    private suspend fun ensureFetched() {
        fetchMutex.withLock {
            if (!fetched) {
                fetched = true
                cache.value = fetchFromRemote()
            }
        }
    }

    private suspend fun fetchFromRemote(): SubscriptionState {
        val userId = try {
            userIdProvider.currentOrThrow()
        } catch (_: Exception) {
            return SubscriptionState.Free
        }

        return try {
            val dto = supabaseClient
                .from("subscriptions")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<SubscriptionDto>()
                .firstOrNull()
                ?: return SubscriptionState.Free

            val state = dto.toDomain()

            if (dto.status == "trial" && state is SubscriptionState.Expired) {
                expireTrialRemote(userId)
            }

            state
        } catch (_: Exception) {
            SubscriptionState.Free
        }
    }

    private suspend fun expireTrialRemote(userId: String) {
        try {
            supabaseClient
                .from("subscriptions")
                .update(buildJsonObject { put("status", "expired") }) {
                    filter { eq("user_id", userId) }
                }
        } catch (_: Exception) {
            // Best-effort — client already treats it as expired
        }
    }
}
