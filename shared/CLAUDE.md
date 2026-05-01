# shared/ — KMP Module

Package: `com.strakk.shared`. Skill: `kotlin-kmp-conventions` (read before implementing).

## Layer Rules
- `domain/` — zero deps, pure Kotlin. Interfaces, models, use cases only.
- `data/` — all classes `internal`. Depends on domain only. DTOs + mappers + RepositoryImpl.
- `presentation/` — depends on domain only. ViewModels + contract pattern (UiState/Intent/Effect).
- `di/` — Koin modules wiring all layers. Skill: `koin-di-patterns`.

## What Exists
**Domain models:** `UserProfile`, `NutritionGoals`, `OnboardingData`, `AiGoalsResult`, `CalculateGoalsRequest`
**Enums:** `BiologicalSex`, `FitnessGoal`, `TrainingType`, `ActivityLevel`
**UseCases:** `CompleteOnboardingUseCase`, `CalculateGoalsUseCase`, `CreateProfileUseCase`, `ResetPasswordUseCase`
**Repos:** `ProfileRepository`, `GoalsRepository`, `AuthRepository` (interfaces in domain, impls in data)
**ViewModels:** `OnboardingFlowViewModel`, `LoginViewModel`, `RootViewModel`

## Key Patterns
- Use `sealed interface`, never `sealed class` (except extending Exception)
- Use `runSuspendCatching { }` (in `domain/common/`) for wrapping suspending calls → `Result<T>`
- No `java.time`, `Gson`, `MockK`, `Dispatchers.IO`
- ViewModels expose `StateFlow<UiState>`, receive intents, emit one-shot effects via `Channel`

## Testing (`src/commonTest/`)
`kotlin.test` + Mokkery (mocking) + Turbine (Flow). Skill: `kmp-testing`. No MockK.
Run: `./gradlew :shared:allTests`
