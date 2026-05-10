package com.strakk.shared.data.repository

import com.strakk.shared.data.dto.SubscriptionDto
import com.strakk.shared.data.mapper.toDomain
import com.strakk.shared.data.remote.CurrentUserIdProvider
import com.strakk.shared.domain.model.DevSubscriptionOverride
import com.strakk.shared.domain.model.SubscriptionPlan
import com.strakk.shared.domain.model.SubscriptionState
import com.strakk.shared.domain.repository.SubscriptionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.days

internal class SubscriptionRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val userIdProvider: CurrentUserIdProvider,
) : SubscriptionRepository {
    private companion object {
        const val REFRESH_RETRY_ATTEMPTS = 3
        const val REFRESH_RETRY_DELAY_MS = 1200L
    }

    private val remoteState = MutableStateFlow<SubscriptionState>(SubscriptionState.Free)
    private val effectiveState = MutableStateFlow<SubscriptionState>(SubscriptionState.Free)
    private val devOverride = MutableStateFlow(DevSubscriptionOverride.SERVER)
    private var fetched = false
    private val fetchMutex = Mutex()

    override val cachedState: SubscriptionState get() = effectiveState.value

    override fun observeState(): Flow<SubscriptionState> = effectiveState.onStart { ensureFetched() }

    override fun observeDevOverride(): Flow<DevSubscriptionOverride> = devOverride

    override suspend fun getState(): SubscriptionState {
        ensureFetched()
        return effectiveState.value
    }

    override suspend fun refreshState() {
        fetchMutex.withLock {
            remoteState.value = fetchFromRemoteWithRetry()
            applyDevOverride()
            fetched = true
        }
    }

    override suspend fun setDevOverride(override: DevSubscriptionOverride) {
        fetchMutex.withLock {
            devOverride.value = override
            applyDevOverride()
        }
    }

    private suspend fun ensureFetched() {
        fetchMutex.withLock {
            if (!fetched) {
                fetched = true
                remoteState.value = fetchFromRemote()
                applyDevOverride()
            }
        }
    }

    private fun applyDevOverride() {
        effectiveState.value = devOverride.value.toSubscriptionState(remoteState.value)
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

    private suspend fun fetchFromRemoteWithRetry(): SubscriptionState {
        var last: SubscriptionState = SubscriptionState.Free
        repeat(REFRESH_RETRY_ATTEMPTS) { attempt ->
            last = fetchFromRemote()
            if (last is SubscriptionState.Active || last is SubscriptionState.Trial) return last
            if (attempt < REFRESH_RETRY_ATTEMPTS - 1) {
                delay(REFRESH_RETRY_DELAY_MS)
            }
        }
        return last
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

private fun DevSubscriptionOverride.toSubscriptionState(serverState: SubscriptionState): SubscriptionState =
    when (this) {
        DevSubscriptionOverride.SERVER -> serverState
        DevSubscriptionOverride.FREE -> SubscriptionState.Free
        DevSubscriptionOverride.TRIAL_7_DAYS -> SubscriptionState.Trial(endsAt = Clock.System.now() + 7.days)
        DevSubscriptionOverride.TRIAL_1_DAY -> SubscriptionState.Trial(endsAt = Clock.System.now() + 1.days)
        DevSubscriptionOverride.EXPIRED -> SubscriptionState.Expired
        DevSubscriptionOverride.PRO_MONTHLY -> SubscriptionState.Active(
            plan = SubscriptionPlan.MONTHLY,
            expiresAt = Clock.System.now() + 30.days,
        )
        DevSubscriptionOverride.PRO_ANNUAL -> SubscriptionState.Active(
            plan = SubscriptionPlan.ANNUAL,
            expiresAt = Clock.System.now() + 365.days,
        )
        DevSubscriptionOverride.PAYMENT_FAILED -> SubscriptionState.PaymentFailed
    }
