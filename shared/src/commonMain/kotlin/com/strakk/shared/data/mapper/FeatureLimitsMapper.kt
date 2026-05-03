package com.strakk.shared.data.mapper

import com.strakk.shared.data.dto.FeatureLimitsDto
import com.strakk.shared.domain.model.FeatureLimits
import com.strakk.shared.domain.model.QuotaPeriod

internal fun FeatureLimitsDto.toDomain(): FeatureLimits = FeatureLimits(
    featureKey = featureKey,
    proOnly = proOnly,
    quotaFree = quotaFree,
    quotaPro = quotaPro,
    quotaPeriod = QuotaPeriod.fromKey(quotaPeriod),
    rateLimitMax = rateLimitMax,
    rateLimitWindowSeconds = rateLimitWindowS,
)
