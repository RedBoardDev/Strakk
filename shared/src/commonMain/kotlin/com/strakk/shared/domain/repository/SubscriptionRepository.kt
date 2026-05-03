package com.strakk.shared.domain.repository

import com.strakk.shared.domain.model.SubscriptionState
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun observeState(): Flow<SubscriptionState>
    suspend fun getState(): SubscriptionState
    suspend fun refreshState()
    val cachedState: SubscriptionState
}
