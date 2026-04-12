package com.strakk.shared.data.mapper

import com.strakk.shared.data.dto.QuoteDto
import com.strakk.shared.domain.model.Quote

internal fun QuoteDto.toDomain(): Quote = Quote(
    id = id,
    text = quote,
    author = author,
)
