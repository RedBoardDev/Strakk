package com.strakk.shared.di

import com.russhwolf.settings.Settings
import com.strakk.shared.data.remote.CurrentUserIdProvider
import com.strakk.shared.data.remote.SupabaseProvider
import com.strakk.shared.data.repository.AuthRepositoryImpl
import com.strakk.shared.data.repository.BarcodeLookupRepositoryImpl
import com.strakk.shared.data.repository.FoodCatalogRepositoryImpl
import com.strakk.shared.data.repository.MealDraftRepositoryImpl
import com.strakk.shared.data.repository.MealPhotoRepositoryImpl
import com.strakk.shared.data.repository.MealRepositoryImpl
import com.strakk.shared.data.repository.NutritionRepositoryImpl
import com.strakk.shared.data.repository.ProfileRepositoryImpl
import com.strakk.shared.data.repository.WorkoutRepositoryImpl
import com.strakk.shared.domain.common.ClockProvider
import com.strakk.shared.domain.common.Logger
import com.strakk.shared.domain.common.SystemClockProvider
import com.strakk.shared.domain.common.createLogger
import com.strakk.shared.domain.repository.AuthRepository
import com.strakk.shared.domain.repository.BarcodeLookupRepository
import com.strakk.shared.domain.repository.FoodCatalogRepository
import com.strakk.shared.domain.repository.MealDraftRepository
import com.strakk.shared.domain.repository.MealPhotoRepository
import com.strakk.shared.domain.repository.MealRepository
import com.strakk.shared.domain.repository.NutritionRepository
import com.strakk.shared.domain.repository.ProfileRepository
import com.strakk.shared.domain.repository.WorkoutRepository
import com.strakk.shared.domain.usecase.AddItemToDraftUseCase
import com.strakk.shared.domain.usecase.AddItemToProcessedMealUseCase
import com.strakk.shared.domain.usecase.AddWaterUseCase
import com.strakk.shared.domain.usecase.CheckProfileExistsUseCase
import com.strakk.shared.domain.usecase.CommitMealDraftUseCase
import com.strakk.shared.domain.usecase.CreateMealDraftUseCase
import com.strakk.shared.domain.usecase.CreateProfileUseCase
import com.strakk.shared.domain.usecase.DeleteMealContainerUseCase
import com.strakk.shared.domain.usecase.DeleteMealUseCase
import com.strakk.shared.domain.usecase.DeleteWaterUseCase
import com.strakk.shared.domain.usecase.DiscardMealDraftUseCase
import com.strakk.shared.domain.usecase.ExportToHevyUseCase
import com.strakk.shared.domain.usecase.GetCurrentUserEmailUseCase
import com.strakk.shared.domain.usecase.GetHevyApiKeyUseCase
import com.strakk.shared.domain.usecase.GetMonthlyActivityUseCase
import com.strakk.shared.domain.usecase.ObserveActiveMealDraftUseCase
import com.strakk.shared.domain.usecase.ObserveAuthStatusUseCase
import com.strakk.shared.domain.usecase.ObserveDailySummaryUseCase
import com.strakk.shared.domain.usecase.ObserveFrequentItemsUseCase
import com.strakk.shared.domain.usecase.ObserveMealContainersForDateUseCase
import com.strakk.shared.domain.usecase.ObserveMealUseCase
import com.strakk.shared.domain.usecase.ObserveMealsForDateUseCase
import com.strakk.shared.domain.usecase.ObserveNutritionMutationsUseCase
import com.strakk.shared.domain.usecase.ObserveProfileUseCase
import com.strakk.shared.domain.usecase.ObserveWaterEntriesForDateUseCase
import com.strakk.shared.domain.usecase.ParseWorkoutPdfUseCase
import com.strakk.shared.domain.usecase.ProcessMealDraftUseCase
import com.strakk.shared.domain.usecase.QuickAddEntryUseCase
import com.strakk.shared.domain.usecase.QuickAddFromBarcodeUseCase
import com.strakk.shared.domain.usecase.QuickAddFromPhotoUseCase
import com.strakk.shared.domain.usecase.QuickAddFromTextUseCase
import com.strakk.shared.domain.usecase.QuickAddManualUseCase
import com.strakk.shared.domain.usecase.RemoveItemFromDraftUseCase
import com.strakk.shared.domain.usecase.RemoveLastWaterEntryUseCase
import com.strakk.shared.domain.usecase.RenameMealDraftUseCase
import com.strakk.shared.domain.usecase.RenameMealUseCase
import com.strakk.shared.domain.usecase.SaveHevyApiKeyUseCase
import com.strakk.shared.domain.usecase.SearchFoodUseCase
import com.strakk.shared.domain.usecase.SignInUseCase
import com.strakk.shared.domain.usecase.SignOutUseCase
import com.strakk.shared.domain.usecase.SignUpUseCase
import com.strakk.shared.domain.usecase.UpdateDraftItemUseCase
import com.strakk.shared.domain.usecase.UpdateProfileUseCase
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
import io.github.jan.supabase.SupabaseClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Data layer bindings: remote clients, repositories, platform services. */
internal val dataModule = module {
    single<SupabaseClient> { SupabaseProvider.createClient() }
    singleOf(::CurrentUserIdProvider)
    single<Logger> { createLogger() }
    singleOf(::SystemClockProvider) { bind<ClockProvider>() }

    // Local KV storage (multiplatform-settings no-arg factory — picks
    // NSUserDefaults on iOS and SharedPreferences on Android automatically).
    single<Settings> { Settings() }

    // Standalone Ktor client for third-party APIs (Open Food Facts, etc.) —
    // kept separate from the Supabase client to avoid coupling.
    single<HttpClient> {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    singleOf(::AuthRepositoryImpl) { bind<AuthRepository>() }
    singleOf(::ProfileRepositoryImpl) { bind<ProfileRepository>() }
    singleOf(::NutritionRepositoryImpl) { bind<NutritionRepository>() }
    singleOf(::WorkoutRepositoryImpl) { bind<WorkoutRepository>() }

    // Meal refonte v2
    singleOf(::MealPhotoRepositoryImpl) { bind<MealPhotoRepository>() }
    singleOf(::MealRepositoryImpl) { bind<MealRepository>() }
    singleOf(::MealDraftRepositoryImpl) { bind<MealDraftRepository>() }
    singleOf(::FoodCatalogRepositoryImpl) { bind<FoodCatalogRepository>() }
    singleOf(::BarcodeLookupRepositoryImpl) { bind<BarcodeLookupRepository>() }
}

/** Domain layer bindings: pure use cases. */
internal val domainModule = module {
    // Auth
    factoryOf(::ObserveAuthStatusUseCase)
    factoryOf(::SignInUseCase)
    factoryOf(::SignUpUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::GetCurrentUserEmailUseCase)

    // Profile
    factoryOf(::CheckProfileExistsUseCase)
    factoryOf(::CreateProfileUseCase)
    factoryOf(::UpdateProfileUseCase)
    factoryOf(::ObserveProfileUseCase)

    // Nutrition — streams
    factoryOf(::ObserveMealsForDateUseCase)
    factoryOf(::ObserveWaterEntriesForDateUseCase)
    factoryOf(::ObserveNutritionMutationsUseCase)
    factoryOf(::ObserveDailySummaryUseCase)
    factoryOf(::ObserveFrequentItemsUseCase)
    factoryOf(::GetMonthlyActivityUseCase)

    // Nutrition — mutations (orphan entries)
    factoryOf(::DeleteMealUseCase)
    factoryOf(::AddWaterUseCase)
    factoryOf(::DeleteWaterUseCase)
    factoryOf(::RemoveLastWaterEntryUseCase)

    // Meal container (Processed)
    factoryOf(::ObserveMealUseCase)
    factoryOf(::ObserveMealContainersForDateUseCase)
    factoryOf(::RenameMealUseCase)
    factoryOf(::DeleteMealContainerUseCase)
    factoryOf(::AddItemToProcessedMealUseCase)

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
    factoryOf(::QuickAddFromBarcodeUseCase)

    // Catalogue
    factoryOf(::SearchFoodUseCase)

    // Workout / Hevy
    factoryOf(::ParseWorkoutPdfUseCase)
    factoryOf(::ExportToHevyUseCase)
    factoryOf(::GetHevyApiKeyUseCase)
    factoryOf(::SaveHevyApiKeyUseCase)
}

/** Presentation layer bindings: ViewModels. */
internal val presentationModule = module {
    viewModelOf(::RootViewModel)
    viewModelOf(::AuthFlowViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::TodayViewModel)
    viewModelOf(::CalendarViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::HevyExportViewModel)

    // Meal refonte v2
    viewModelOf(::MealDraftViewModel)
    viewModelOf(::SearchFoodViewModel)
    viewModelOf(::ManualEntryViewModel)
}

/** Aggregated module registered by each platform on startup. */
val sharedModule = module {
    includes(dataModule, domainModule, presentationModule)
}
