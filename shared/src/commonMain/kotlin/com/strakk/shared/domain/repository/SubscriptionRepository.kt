package com.strakk.shared.domain.repository

import com.strakk.shared.domain.model.DevSubscriptionOverride
import com.strakk.shared.domain.model.SubscriptionState
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun observeState(): Flow<SubscriptionState>
    fun observeDevOverride(): Flow<DevSubscriptionOverride>
    suspend fun getState(): SubscriptionState
    suspend fun refreshState()
    suspend fun setDevOverride(override: DevSubscriptionOverride)
    val cachedState: SubscriptionState
}
