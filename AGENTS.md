# Strakk

Personal nutrition & weekly check-in app. KMP — shared logic, native UI.
Backend: Supabase (Postgres, Auth, Storage, Edge Functions). Base pkg: `com.strakk.shared`

## Stack
- **Shared:** Kotlin 2.1, supabase-kt 3.1.1, Ktor, Koin, kotlinx.serialization, SKIE
- **iOS:** Swift 6, SwiftUI, iOS 17+
- **Android:** Jetpack Compose, Material 3, API 26+
- **Design:** `DESIGN.md` — source of truth for all UI. Read before any UI work.

## Implemented Features
- **Auth:** Login, Reset Password
- **Onboarding V2:** Full flow — Welcome → Weight/Height → Bio → Goal → Activity → SignUp → AI Goals → Preview
- **Home:** `TodayView` (2×2 macro grid), stub features

## Domain Model (`shared/domain/`)
**Models:** `UserProfile`, `NutritionGoals`, `OnboardingData`, `AiGoalsResult`, `CalculateGoalsRequest`
**Enums:** `BiologicalSex`, `FitnessGoal`, `TrainingType`, `ActivityLevel`
**UseCases:** `CompleteOnboardingUseCase`, `CalculateGoalsUseCase`, `CreateProfileUseCase`, `ResetPasswordUseCase`
**Repos (interfaces):** `ProfileRepository`, `GoalsRepository`, `AuthRepository`

## Project Layout
```
shared/src/commonMain/.../strakk/shared/
  domain/       # Pure Kotlin, zero deps
  data/         # internal — RepositoryImpl, DTOs, Mappers, Supabase calls
  presentation/ # ViewModels (MVVM+ contract pattern), UiState
  di/           # Koin modules
iosApp/         # SwiftUI — @Observable wrappers, SKIE bridge
androidApp/     # Compose — Route/Screen/Content pattern, Material 3
supabase/       # Migrations + Edge Functions (Deno/TypeScript)
docs/specs/     # Feature specs
```

## Build & Lint
```bash
./gradlew :shared:allTests                                      # tests
./gradlew :androidApp:assembleDebug                             # Android APK
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64           # iOS framework
cd iosApp && xcodegen generate                                  # Regen Xcode project
make lint           # all linters (Detekt + SwiftLint + Deno)
make lint-kotlin    # Detekt strict, zero tolerance
```
CI blocks merge on: lint-kotlin, lint-deno, test-shared, build-android.

## Architecture Rules
- **domain/** — ZERO external deps. Interfaces only.
- **data/** — depends on domain only. All classes `internal`.
- **presentation/** — ViewModels call UseCases only, never Repositories.
- **UI** — depends on presentation + domain. Never imports data/.

## Anti-Patterns (NEVER)
- Business logic in UI → delegate to UseCases
- ViewModel → Repository directly → go through UseCase
- `collectAsState()` → use `collectAsStateWithLifecycle()`
- `sealed class` → use `sealed interface` (except when extending Exception)
- `java.time`, `Gson`, `MockK`, `Dispatchers.IO` in shared/
- Generic LLM-looking UI — read DESIGN.md, every screen must feel intentional

## Agent Orchestration

| Agent | Role | Model |
|-------|------|-------|
| `project-manager` | Challenges specs, plans, delegates | Opus |
| `architect` | Designs interfaces and contracts | Opus |
| `ui-designer` | Proposes screen designs (reads DESIGN.md) | Opus |
| `kotlin-shared` | Implements shared/ | Sonnet |
| `swift-ios` | Implements iosApp/ | Sonnet |
| `android-ui` | Implements androidApp/ | Sonnet |
| `supabase-edge` | Implements Supabase migrations, RLS, Edge Functions | Sonnet |
| `build-verify` | Runs builds, linters, tests — reports only | Haiku |
| `quality-review` | Reviews architecture + conventions | Opus |
| `test-writer` | Writes shared tests | Sonnet |

## File Ownership

| Directory | Owner |
|-----------|-------|
| shared/ | @kotlin-shared |
| iosApp/ | @swift-ios |
| androidApp/ | @android-ui |
| supabase/ | @supabase-edge |
| docs/specs/ | @project-manager |

## Testing
`kotlin.test` + Mokkery + Turbine + kotlinx-coroutines-test. **No MockK** (JVM-only).

- `kotlin.test` + Mokkery (mocking) + Turbine (Flow) + kotlinx-coroutines-test
- NO MockK (JVM-only, doesn't work on iOS/Native)

## Linting & DevOps

- **Detekt + ktlint**: `make lint-kotlin` — strict config at `config/detekt/detekt.yml`, zero tolerance
- **SwiftLint**: `make lint-swift` — config at `iosApp/.swiftlint.yml`, local macOS only (no macOS CI runner)
- **Deno lint + check**: `make lint-deno` — config at `supabase/functions/deno.json`
- **All lints**: `make lint`
- **Pre-commit hook**: Lefthook runs SwiftLint + Detekt on commit (`make setup` to activate)
- **CI**: `.github/workflows/ci.yml` — lint-kotlin, lint-deno, test-shared, build-android (blocks merge)
- **CD**: `.github/workflows/cd.yml` — on merge to `release` branch (skeleton)
