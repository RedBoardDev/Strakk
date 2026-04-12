package com.strakk.shared.data.repository

import com.strakk.shared.data.dto.QuoteDto
import com.strakk.shared.data.mapper.toDomain
import com.strakk.shared.data.remote.httpClient
import com.strakk.shared.domain.model.Quote
import com.strakk.shared.domain.repository.QuoteRepository
import io.ktor.client.call.body
import io.ktor.client.request.get

internal class QuoteRepositoryImpl : QuoteRepository {
    override suspend fun getRandomQuote(): Quote {
        val dto: QuoteDto = httpClient.get("https://dummyjson.com/quotes/random").body()
        return dto.toDomain()
    }
}
