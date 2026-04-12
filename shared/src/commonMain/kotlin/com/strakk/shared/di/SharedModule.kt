package com.strakk.shared.di

import com.strakk.shared.data.repository.GreetingRepositoryImpl
import com.strakk.shared.data.repository.QuoteRepositoryImpl
import com.strakk.shared.domain.repository.GreetingRepository
import com.strakk.shared.domain.repository.QuoteRepository
import com.strakk.shared.domain.usecase.GetGreetingUseCase
import com.strakk.shared.domain.usecase.GetRandomQuoteUseCase
import com.strakk.shared.presentation.GreetingViewModel
import com.strakk.shared.presentation.QuoteViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sharedModule = module {
    single<GreetingRepository> { GreetingRepositoryImpl() }
    factory { GetGreetingUseCase(repository = get()) }
    viewModel { GreetingViewModel(getGreeting = get()) }

    single<QuoteRepository> { QuoteRepositoryImpl() }
    factory { GetRandomQuoteUseCase(repository = get()) }
    viewModel { QuoteViewModel(getRandomQuote = get()) }
}
