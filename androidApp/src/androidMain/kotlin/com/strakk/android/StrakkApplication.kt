package com.strakk.android

import android.app.Application
import com.strakk.shared.di.sharedModule
import com.strakk.shared.domain.usecase.ConfigureBillingUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class StrakkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@StrakkApplication)
            modules(sharedModule)
        }

        val apiKey = BuildConfig.REVENUECAT_API_KEY
        if (apiKey.isNotBlank()) {
            GlobalContext.get().get<ConfigureBillingUseCase>().invoke(apiKey)
        }
    }
}
