package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.Quote
import com.strakk.shared.domain.repository.QuoteRepository

class GetRandomQuoteUseCase(private val repository: QuoteRepository) {
    suspend operator fun invoke(): Quote = repository.getRandomQuote()
}
