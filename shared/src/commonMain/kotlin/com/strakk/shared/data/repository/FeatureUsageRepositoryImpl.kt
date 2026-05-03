package com.strakk.shared.data.repository

import com.strakk.shared.data.dto.FeatureUsageCountDto
import com.strakk.shared.data.remote.CurrentUserIdProvider
import com.strakk.shared.domain.repository.FeatureUsageRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.datetime.Instant

internal class FeatureUsageRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val userIdProvider: CurrentUserIdProvider,
) : FeatureUsageRepository {

    override suspend fun countUsage(featureKey: String, since: Instant): Int {
        val userId = userIdProvider.currentOrThrow()

        return try {
            val rows = supabaseClient
                .from("feature_usage")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("feature_key", featureKey)
                        gte("created_at", since.toString())
                    }
                }
                .decodeList<FeatureUsageCountDto>()

            rows.size
        } catch (_: Exception) {
            0
        }
    }
}
