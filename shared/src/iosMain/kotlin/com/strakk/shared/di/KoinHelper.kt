package com.strakk.shared.di

import com.strakk.shared.domain.usecase.QuickAddFromBarcodeUseCase
import com.strakk.shared.domain.usecase.QuickAddFromPhotoUseCase
import com.strakk.shared.domain.usecase.QuickAddFromTextUseCase
import com.strakk.shared.presentation.auth.AuthFlowViewModel
import com.strakk.shared.presentation.auth.RootViewModel
import com.strakk.shared.presentation.calendar.CalendarViewModel
import com.strakk.shared.presentation.hevy.HevyExportViewModel
import com.strakk.shared.presentation.meal.ManualEntryViewModel
import com.strakk.shared.presentation.meal.MealDraftViewModel
import com.strakk.shared.presentation.meal.SearchFoodViewModel
import com.strakk.shared.presentation.onboarding.OnboardingViewModel
import com.strakk.shared.presentation.settings.SettingsViewModel
import com.strakk.shared.presentation.today.TodayViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin

/**
 * Starts Koin with the shared module. Must be called once from iOS app startup
 * (see `iOSApp.swift`) before any ViewModel is obtained.
 */
fun initKoin() {
    startKoin {
        modules(sharedModule)
    }
}

/**
 * Bridge used by SwiftUI wrappers to obtain KMP ViewModels.
 *
 * Method names are chosen over `inline fun <reified T>` because Swift interop
 * doesn't see reified generics.
 */
class KoinHelper : KoinComponent {
    fun getRootViewModel(): RootViewModel = get()
    fun getAuthFlowViewModel(): AuthFlowViewModel = get()
    fun getOnboardingViewModel(): OnboardingViewModel = get()
    fun getTodayViewModel(): TodayViewModel = get()
    fun getCalendarViewModel(): CalendarViewModel = get()
    fun getSettingsViewModel(): SettingsViewModel = get()
    fun getHevyExportViewModel(): HevyExportViewModel = get()
    fun getMealDraftViewModel(): MealDraftViewModel = get()
    fun getSearchFoodViewModel(): SearchFoodViewModel = get()
    fun getManualEntryViewModel(): ManualEntryViewModel = get()
    fun getQuickAddFromPhotoUseCase(): QuickAddFromPhotoUseCase = get()
    fun getQuickAddFromTextUseCase(): QuickAddFromTextUseCase = get()
    fun getQuickAddFromBarcodeUseCase(): QuickAddFromBarcodeUseCase = get()
}
