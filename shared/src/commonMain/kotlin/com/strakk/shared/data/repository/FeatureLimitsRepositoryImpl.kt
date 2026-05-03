package com.strakk.shared.data.repository

import com.strakk.shared.data.dto.FeatureLimitsDto
import com.strakk.shared.data.mapper.toDomain
import com.strakk.shared.domain.model.FeatureLimits
import com.strakk.shared.domain.repository.FeatureLimitsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

internal class FeatureLimitsRepositoryImpl(
    private val supabaseClient: SupabaseClient,
) : FeatureLimitsRepository {

    private var cache: Map<String, FeatureLimits>? = null
    private var cacheTimestamp: Long = 0
    private val mutex = Mutex()

    override suspend fun getLimits(featureKey: String): FeatureLimits? {
        return ensureCache()[featureKey]
    }

    override suspend fun getAllLimits(): List<FeatureLimits> {
        return ensureCache().values.toList()
    }

    private suspend fun ensureCache(): Map<String, FeatureLimits> {
        mutex.withLock {
            val now = Clock.System.now().toEpochMilliseconds()
            val existing = cache
            if (existing != null && (now - cacheTimestamp) < CACHE_TTL_MS) {
                return existing
            }

            val fresh = fetchFromRemote()
            cache = fresh
            cacheTimestamp = now
            return fresh
        }
    }

    private suspend fun fetchFromRemote(): Map<String, FeatureLimits> {
        return try {
            supabaseClient
                .from("feature_limits")
                .select()
                .decodeList<FeatureLimitsDto>()
                .associate { it.featureKey to it.toDomain() }
        } catch (_: Exception) {
            cache ?: emptyMap()
        }
    }

    companion object {
        private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
    }
}
