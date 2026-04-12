package com.strakk.android

import android.app.Application
import com.strakk.shared.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class StrakApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@StrakApplication)
            modules(sharedModule)
        }
    }
}
