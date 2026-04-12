package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.Greeting
import com.strakk.shared.domain.repository.GreetingRepository

class GetGreetingUseCase(private val repository: GreetingRepository) {
    suspend operator fun invoke(): Greeting = repository.getGreeting()
}
