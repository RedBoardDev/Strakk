package com.strakk.shared.data.mapper

import com.strakk.shared.data.dto.WaterEntryDto
import com.strakk.shared.domain.model.WaterEntry

/** Maps a [WaterEntryDto] from the data layer to a [WaterEntry] domain entity. */
internal fun WaterEntryDto.toDomain(): WaterEntry = WaterEntry(
    id = id,
    logDate = logDate,
    amount = amount,
    createdAt = createdAt,
)
