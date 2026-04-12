package com.strakk.shared.domain.repository

import com.strakk.shared.domain.model.Greeting

interface GreetingRepository {
    suspend fun getGreeting(): Greeting
}
