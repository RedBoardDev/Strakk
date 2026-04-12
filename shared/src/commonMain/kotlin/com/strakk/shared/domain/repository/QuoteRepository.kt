package com.strakk.shared.domain.repository

import com.strakk.shared.domain.model.Quote

interface QuoteRepository {
    suspend fun getRandomQuote(): Quote
}
