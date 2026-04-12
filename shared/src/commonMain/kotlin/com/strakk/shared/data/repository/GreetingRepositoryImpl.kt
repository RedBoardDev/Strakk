package com.strakk.shared.data.repository

import com.strakk.shared.Platform
import com.strakk.shared.domain.model.Greeting
import com.strakk.shared.domain.repository.GreetingRepository

internal class GreetingRepositoryImpl : GreetingRepository {
    override suspend fun getGreeting(): Greeting {
        return Greeting(
            message = "Hello from Clean Architecture!",
            platform = Platform.name,
        )
    }
}
