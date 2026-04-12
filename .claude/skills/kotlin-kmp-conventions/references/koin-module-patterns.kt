package com.strakk.shared.di

import com.strakk.shared.data.di.dataModule
import com.strakk.shared.data.repository.SessionRepositoryImpl
import com.strakk.shared.domain.repository.SessionRepository
import com.strakk.shared.domain.usecase.CreateSessionUseCase
import com.strakk.shared.domain.usecase.GetSessionsUseCase
import com.strakk.shared.presentation.session.CreateSessionViewModel
import com.strakk.shared.presentation.session.SessionDetailViewModel
import com.strakk.shared.presentation.session.SessionListViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.verify.verify
import kotlin.test.Test

// =============================================================================
// Pattern 1: Feature module — typical structure
// =============================================================================

/**
 * Data layer module — repositories and data sources.
 *
 * All bindings use named parameters for readability.
 * Repositories are `single` (one instance), use cases are `factory` (new each time).
 */
val sessionDataModule = module {
    single<SessionRepository> {
        SessionRepositoryImpl(
            supabaseClient = get(),
        )
    }
}

/**
 * Domain layer module — use cases.
 */
val sessionDomainModule = module {
    factory {
        GetSessionsUseCase(
            sessionRepository = get(),
        )
    }
    factory {
        CreateSessionUseCase(
            sessionRepository = get(),
        )
    }
}

/**
 * Presentation layer module — ViewModels.
 */
val sessionPresentationModule = module {
    viewModel {
        SessionListViewModel(
            getSessionsUseCase = get(),
        )
    }
    viewModel {
        CreateSessionViewModel(
            createSessionUseCase = get(),
        )
    }
}

// =============================================================================
// Pattern 2: Keyed ViewModel for detail screens
// =============================================================================

/**
 * Use keyed ViewModels when multiple instances of the same screen
 * can exist (e.g., navigating to different session details).
 *
 * The key ensures each detail screen gets its own ViewModel instance.
 */
val sessionDetailModule = module {
    viewModel { params ->
        SessionDetailViewModel(
            sessionId = params.get(),
            getSessionUseCase = get(),
        )
    }
}

// Usage in Compose Route:
// val viewModel: SessionDetailViewModel = koinViewModel(
//     key = "session_detail_$sessionId",
//     parameters = { parametersOf(sessionId) },
// )

// =============================================================================
// Pattern 3: Platform module with expect/actual
// =============================================================================

// In commonMain:
expect val platformModule: Module

// In androidMain:
// actual val platformModule = module {
//     single<ImageCompressor> { AndroidImageCompressor(context = get()) }
//     single<HapticFeedback> { AndroidHapticFeedback(context = get()) }
// }

// In iosMain:
// actual val platformModule = module {
//     single<ImageCompressor> { IosImageCompressor() }
//     single<HapticFeedback> { IosHapticFeedback() }
// }

// =============================================================================
// Pattern 4: Root app module composing all feature modules
// =============================================================================

val appModule = module {
    includes(
        platformModule,
        supabaseModule,
        sessionDataModule,
        sessionDomainModule,
        sessionPresentationModule,
        sessionDetailModule,
    )
}

// iOS init from Swift:
// import ComposeApp
// @main struct iOSApp: App {
//     init() { InitKoinKt.doInitKoin(config: nil) }
// }

// =============================================================================
// Pattern 5: Koin module verification test
// =============================================================================

/**
 * Verifies that all Koin modules can resolve their dependencies.
 *
 * This catches missing bindings at test time instead of runtime.
 * Add extra types for classes that Koin cannot introspect (e.g., SavedStateHandle).
 */
class KoinModuleVerificationTest : KoinTest {

    @Test
    fun `all modules resolve correctly`() {
        appModule.verify(
            extraTypes = listOf(
                // Add types that Koin cannot introspect automatically
                // SavedStateHandle::class,
            ),
        )
    }
}
