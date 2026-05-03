package com.strakk.shared.di

import com.russhwolf.settings.Settings
import com.strakk.shared.data.datasource.OffLiveSearchDataSource
import com.strakk.shared.data.pdf.CheckInPdfBuilderImpl
import com.strakk.shared.data.remote.CurrentUserIdProvider
import com.strakk.shared.data.remote.SupabaseProvider
import com.strakk.shared.data.repository.AuthRepositoryImpl
import com.strakk.shared.data.repository.BarcodeLookupRepositoryImpl
import com.strakk.shared.data.repository.CheckInRepositoryImpl
import com.strakk.shared.data.repository.FoodCatalogRepositoryImpl
import com.strakk.shared.data.repository.GoalsRepositoryImpl
import com.strakk.shared.data.repository.MealDraftRepositoryImpl
import com.strakk.shared.data.repository.MealPhotoRepositoryImpl
import com.strakk.shared.data.repository.MealRepositoryImpl
import com.strakk.shared.data.repository.NutritionRepositoryImpl
import com.strakk.shared.data.repository.ProfileRepositoryImpl
import com.strakk.shared.data.repository.SubscriptionRepositoryImpl
import com.strakk.shared.data.repository.WorkoutRepositoryImpl
import com.strakk.shared.domain.common.ClockProvider
import com.strakk.shared.domain.common.Logger
import com.strakk.shared.domain.common.SystemClockProvider
import com.strakk.shared.domain.common.createLogger
import com.strakk.shared.domain.repository.AuthRepository
import com.strakk.shared.domain.repository.BarcodeLookupRepository
import com.strakk.shared.domain.repository.CheckInRepository
import com.strakk.shared.domain.repository.FoodCatalogRepository
import com.strakk.shared.domain.repository.GoalsRepository
import com.strakk.shared.domain.repository.MealDraftRepository
import com.strakk.shared.domain.repository.MealPhotoRepository
import com.strakk.shared.domain.repository.MealRepository
import com.strakk.shared.domain.repository.NutritionRepository
import com.strakk.shared.domain.repository.ProfileRepository
import com.strakk.shared.domain.repository.SubscriptionRepository
import com.strakk.shared.domain.repository.WorkoutRepository
import com.strakk.shared.domain.service.CheckInPdfGenerator
import com.strakk.shared.domain.usecase.AddItemToDraftUseCase
import com.strakk.shared.domain.usecase.AddWaterUseCase
import com.strakk.shared.domain.usecase.BuildMealEntryUseCase
import com.strakk.shared.domain.usecase.CalculateGoalsUseCase
import com.strakk.shared.domain.usecase.CheckFeatureAccessUseCase
import com.strakk.shared.domain.usecase.CheckProfileExistsUseCase
import com.strakk.shared.domain.usecase.CommitMealDraftUseCase
import com.strakk.shared.domain.usecase.CompleteOnboardingUseCase
import com.strakk.shared.domain.usecase.ComputeNutritionSummaryUseCase
import com.strakk.shared.domain.usecase.CreateCheckInUseCase
import com.strakk.shared.domain.usecase.CreateMealDraftUseCase
import com.strakk.shared.domain.usecase.CreateProfileUseCase
import com.strakk.shared.domain.usecase.DeleteCheckInUseCase
import com.strakk.shared.domain.usecase.DeleteMealContainerUseCase
import com.strakk.shared.domain.usecase.DeleteMealUseCase
import com.strakk.shared.domain.usecase.DeleteWaterUseCase
import com.strakk.shared.domain.usecase.DiscardMealDraftUseCase
import com.strakk.shared.domain.usecase.ExportToHevyUseCase
import com.strakk.shared.domain.usecase.GenerateCheckInPdfUseCase
import com.strakk.shared.domain.usecase.GetCheckInDeltaUseCase
import com.strakk.shared.domain.usecase.GetCheckInPhotoUrlUseCase
import com.strakk.shared.domain.usecase.GetCheckInStatsUseCase
import com.strakk.shared.domain.usecase.GetCurrentUserEmailUseCase
import com.strakk.shared.domain.usecase.GetHevyApiKeyUseCase
import com.strakk.shared.domain.usecase.GetMonthlyActivityUseCase
import com.strakk.shared.domain.usecase.ObserveActiveMealDraftUseCase
import com.strakk.shared.domain.usecase.ObserveAuthStatusUseCase
import com.strakk.shared.domain.usecase.ObserveCheckInQuickStatsUseCase
import com.strakk.shared.domain.usecase.ObserveCheckInsUseCase
import com.strakk.shared.domain.usecase.ObserveCheckInUseCase
import com.strakk.shared.domain.usecase.ObserveDailySummaryUseCase
import com.strakk.shared.domain.usecase.ObserveFrequentItemsUseCase
import com.strakk.shared.domain.usecase.ObserveMealContainersForDateUseCase
import com.strakk.shared.domain.usecase.ObserveMealsForDateUseCase
import com.strakk.shared.domain.usecase.ObserveNutritionMutationsUseCase
import com.strakk.shared.domain.usecase.ObserveProfileUseCase
import com.strakk.shared.domain.usecase.ObserveSubscriptionStateUseCase
import com.strakk.shared.domain.usecase.ObserveWaterEntriesForDateUseCase
import com.strakk.shared.domain.usecase.ParseWorkoutPdfUseCase
import com.strakk.shared.domain.usecase.ProcessMealDraftUseCase
import com.strakk.shared.domain.usecase.QuickAddEntryUseCase
import com.strakk.shared.domain.usecase.QuickAddFromPhotoUseCase
import com.strakk.shared.domain.usecase.QuickAddFromTextUseCase
import com.strakk.shared.domain.usecase.QuickAddKnownEntryUseCase
import com.strakk.shared.domain.usecase.QuickAddManualUseCase
import com.strakk.shared.domain.usecase.RemoveItemFromDraftUseCase
import com.strakk.shared.domain.usecase.RemoveLastWaterEntryUseCase
import com.strakk.shared.domain.usecase.RenameMealDraftUseCase
import com.strakk.shared.domain.usecase.ResetPasswordUseCase
import com.strakk.shared.domain.usecase.SaveHevyApiKeyUseCase
import com.strakk.shared.domain.usecase.SearchFoodUseCase
import com.strakk.shared.domain.usecase.SignInUseCase
import com.strakk.shared.domain.usecase.SignOutUseCase
import com.strakk.shared.domain.usecase.SignUpUseCase
import com.strakk.shared.domain.usecase.UpdateCheckInUseCase
import com.strakk.shared.domain.usecase.UpdateDraftItemUseCase
import com.strakk.shared.domain.usecase.UpdateMealEntryUseCase
import com.strakk.shared.domain.usecase.UpdateProfileUseCase
import com.strakk.shared.presentation.auth.LoginViewModel
import com.strakk.shared.presentation.auth.RootViewModel
import com.strakk.shared.presentation.calendar.CalendarViewModel
import com.strakk.shared.presentation.checkin.CheckInDetailViewModel
import com.strakk.shared.presentation.checkin.CheckInListViewModel
import com.strakk.shared.presentation.checkin.CheckInStatsViewModel
import com.strakk.shared.presentation.checkin.CheckInWizardViewModel
import com.strakk.shared.presentation.hevy.HevyExportViewModel
import com.strakk.shared.presentation.meal.ManualEntryViewModel
import com.strakk.shared.presentation.meal.MealDraftViewModel
import com.strakk.shared.presentation.meal.QuickAddViewModel
import com.strakk.shared.presentation.meal.SearchFoodViewModel
import com.strakk.shared.presentation.onboarding.OnboardingFlowViewModel
import com.strakk.shared.presentation.settings.SettingsViewModel
import com.strakk.shared.presentation.today.TodayViewModel
import io.github.jan.supabase.SupabaseClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val dataModule = module {
    single<SupabaseClient> { SupabaseProvider.createClient() }
    singleOf(::CurrentUserIdProvider)
    single<Logger> { createLogger() }
    singleOf(::SystemClockProvider) { bind<ClockProvider>() }

    single<Settings> { Settings() }

    single { Json { ignoreUnknownKeys = true } }

    single<HttpClient> {
        HttpClient {
            install(ContentNegotiation) {
                json(get())
            }
        }
    }

    singleOf(::AuthRepositoryImpl) { bind<AuthRepository>() }
    singleOf(::ProfileRepositoryImpl) { bind<ProfileRepository>() }
    singleOf(::GoalsRepositoryImpl) { bind<GoalsRepository>() }
    singleOf(::NutritionRepositoryImpl) { bind<NutritionRepository>() }
    singleOf(::WorkoutRepositoryImpl) { bind<WorkoutRepository>() }
    singleOf(::CheckInRepositoryImpl) { bind<CheckInRepository>() }

    singleOf(::MealPhotoRepositoryImpl) { bind<MealPhotoRepository>() }
    singleOf(::MealRepositoryImpl) { bind<MealRepository>() }
    singleOf(::MealDraftRepositoryImpl) { bind<MealDraftRepository>() }
    singleOf(::OffLiveSearchDataSource)
    singleOf(::FoodCatalogRepositoryImpl) { bind<FoodCatalogRepository>() }
    singleOf(::BarcodeLookupRepositoryImpl) { bind<BarcodeLookupRepository>() }
    singleOf(::SubscriptionRepositoryImpl) { bind<SubscriptionRepository>() }

    factoryOf(::CheckInPdfBuilderImpl) { bind<CheckInPdfGenerator>() }
}

internal val domainModule = module {
    // Auth
    factoryOf(::ObserveAuthStatusUseCase)
    factoryOf(::SignInUseCase)
    factoryOf(::SignUpUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::GetCurrentUserEmailUseCase)
    factoryOf(::ResetPasswordUseCase)

    // Profile
    factoryOf(::CheckProfileExistsUseCase)
    factoryOf(::CreateProfileUseCase)
    factoryOf(::UpdateProfileUseCase)
    factoryOf(::ObserveProfileUseCase)
    factoryOf(::CompleteOnboardingUseCase)

    // Goals (AI)
    factoryOf(::CalculateGoalsUseCase)

    // Nutrition — streams
    factoryOf(::BuildMealEntryUseCase)
    factoryOf(::ObserveMealsForDateUseCase)
    factoryOf(::ObserveWaterEntriesForDateUseCase)
    factoryOf(::ObserveNutritionMutationsUseCase)
    factoryOf(::ObserveDailySummaryUseCase)
    factoryOf(::ObserveFrequentItemsUseCase)
    factoryOf(::GetMonthlyActivityUseCase)

    // Nutrition — mutations (orphan entries)
    factoryOf(::DeleteMealUseCase)
    factoryOf(::UpdateMealEntryUseCase)
    factoryOf(::AddWaterUseCase)
    factoryOf(::DeleteWaterUseCase)
    factoryOf(::RemoveLastWaterEntryUseCase)

    // Meal container (Processed)
    factoryOf(::ObserveMealContainersForDateUseCase)
    factoryOf(::DeleteMealContainerUseCase)

    // Meal draft lifecycle
    factoryOf(::ObserveActiveMealDraftUseCase)
    factoryOf(::CreateMealDraftUseCase)
    factoryOf(::AddItemToDraftUseCase)
    factoryOf(::UpdateDraftItemUseCase)
    factoryOf(::RemoveItemFromDraftUseCase)
    factoryOf(::RenameMealDraftUseCase)
    factoryOf(::DiscardMealDraftUseCase)
    factoryOf(::ProcessMealDraftUseCase)
    factoryOf(::CommitMealDraftUseCase)

    // Quick-add
    factoryOf(::QuickAddEntryUseCase)
    factoryOf(::QuickAddManualUseCase)
    factoryOf(::QuickAddFromPhotoUseCase)
    factoryOf(::QuickAddFromTextUseCase)
    factoryOf(::QuickAddKnownEntryUseCase)

    // Catalogue
    factoryOf(::SearchFoodUseCase)

    // Workout / Hevy
    factoryOf(::ParseWorkoutPdfUseCase)
    factoryOf(::ExportToHevyUseCase)
    factoryOf(::GetHevyApiKeyUseCase)
    factoryOf(::SaveHevyApiKeyUseCase)

    // Check-in
    factoryOf(::ObserveCheckInsUseCase)
    factoryOf(::ObserveCheckInUseCase)
    factoryOf(::CreateCheckInUseCase)
    factoryOf(::UpdateCheckInUseCase)
    factoryOf(::DeleteCheckInUseCase)
    factoryOf(::ComputeNutritionSummaryUseCase)
    factoryOf(::GetCheckInDeltaUseCase)
    factoryOf(::GetCheckInPhotoUrlUseCase)
    factoryOf(::GetCheckInStatsUseCase)
    factoryOf(::ObserveCheckInQuickStatsUseCase)

    // Check-in PDF
    factoryOf(::GenerateCheckInPdfUseCase)

    // Subscription
    factoryOf(::ObserveSubscriptionStateUseCase)
    factoryOf(::CheckFeatureAccessUseCase)
}

internal val presentationModule = module {
    viewModelOf(::RootViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::OnboardingFlowViewModel)
    viewModelOf(::TodayViewModel)
    viewModelOf(::CalendarViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::HevyExportViewModel)

    // Meal
    viewModelOf(::MealDraftViewModel)
    viewModelOf(::SearchFoodViewModel)
    viewModelOf(::ManualEntryViewModel)
    viewModelOf(::QuickAddViewModel)

    // Check-in
    viewModelOf(::CheckInListViewModel)
    viewModel { params ->
        CheckInDetailViewModel(
            checkInId = params.get(),
            observeCheckIn = get(),
            getCheckInDelta = get(),
            getCheckInPhotoUrl = get(),
            deleteCheckIn = get(),
        )
    }
    viewModel { params ->
        CheckInWizardViewModel(
            checkInId = params.getOrNull(),
            observeCheckIn = get(),
            observeCheckIns = get(),
            createCheckIn = get(),
            updateCheckIn = get(),
            computeNutritionSummary = get(),
            getCheckInDelta = get(),
            getCheckInPhotoUrl = get(),
            clock = get(),
        )
    }
    viewModelOf(::CheckInStatsViewModel)
}

val sharedModule = module {
    includes(dataModule, domainModule, presentationModule)
}
