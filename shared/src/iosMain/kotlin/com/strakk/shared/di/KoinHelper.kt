package com.strakk.shared.di

import com.strakk.shared.domain.usecase.GenerateCheckInPdfUseCase
import com.strakk.shared.presentation.auth.LoginViewModel
import com.strakk.shared.presentation.checkin.CheckInDetailViewModel
import com.strakk.shared.presentation.checkin.CheckInListViewModel
import com.strakk.shared.presentation.checkin.CheckInStatsViewModel
import com.strakk.shared.presentation.checkin.CheckInWizardViewModel
import org.koin.core.parameter.parametersOf
import com.strakk.shared.presentation.auth.RootViewModel
import com.strakk.shared.presentation.calendar.CalendarViewModel
import com.strakk.shared.presentation.hevy.HevyExportViewModel
import com.strakk.shared.presentation.meal.ManualEntryViewModel
import com.strakk.shared.presentation.meal.MealDraftViewModel
import com.strakk.shared.presentation.meal.QuickAddViewModel
import com.strakk.shared.presentation.meal.SearchFoodViewModel
import com.strakk.shared.presentation.onboarding.OnboardingFlowViewModel
import com.strakk.shared.presentation.settings.SettingsViewModel
import com.strakk.shared.presentation.today.TodayViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(sharedModule)
    }
}

class KoinHelper : KoinComponent {
    fun getRootViewModel(): RootViewModel = get()
    fun getLoginViewModel(): LoginViewModel = get()
    fun getOnboardingFlowViewModel(): OnboardingFlowViewModel = get()
    fun getTodayViewModel(): TodayViewModel = get()
    fun getCalendarViewModel(): CalendarViewModel = get()
    fun getSettingsViewModel(): SettingsViewModel = get()
    fun getHevyExportViewModel(): HevyExportViewModel = get()
    fun getMealDraftViewModel(): MealDraftViewModel = get()
    fun getSearchFoodViewModel(): SearchFoodViewModel = get()
    fun getManualEntryViewModel(): ManualEntryViewModel = get()
    fun getQuickAddViewModel(): QuickAddViewModel = get()
    fun getCheckInListViewModel(): CheckInListViewModel = get()
    fun getCheckInDetailViewModel(checkInId: String): CheckInDetailViewModel = get { parametersOf(checkInId) }
    fun getCheckInWizardViewModel(checkInId: String?): CheckInWizardViewModel = get { parametersOf(checkInId) }
    fun getCheckInStatsViewModel(): CheckInStatsViewModel = get()
    fun getGenerateCheckInPdfUseCase(): GenerateCheckInPdfUseCase = get()
}
