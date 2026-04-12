package com.strakk.shared.di

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(sharedModule)
    }
}

class KoinHelper : KoinComponent {
    fun getGreetingViewModel() = get<com.strakk.shared.presentation.GreetingViewModel>()
    fun getQuoteViewModel() = get<com.strakk.shared.presentation.QuoteViewModel>()
}
