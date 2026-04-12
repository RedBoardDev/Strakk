# Strakk

Personal fitness & nutrition tracking app. Solo user, no coach/client model.
KMP (Kotlin Multiplatform) — shared business logic, native UI on each platform.
Backend: Supabase (Postgres, Auth, Storage, Edge Functions).

**Base package:** `com.strakk.shared`

## Stack

- **Shared:** Kotlin 2.1, supabase-kt, Ktor, Koin, kotlinx.serialization, SKIE (iOS bridge)
- **iOS:** Swift 6, SwiftUI, iOS 17+
- **Android:** Jetpack Compose, Material 3, API 26+
- **Design:** see `DESIGN.md` at project root — source of truth for all UI decisions

## Environment

- `JAVA_HOME` — JDK 17+ (`/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`)
- `ANDROID_HOME` — Android SDK (`~/Library/Android/sdk`)
- Gradle wrapper: `./gradlew` (never install Gradle globally)
- iOS: `cd iosApp && xcodegen generate` to regenerate Xcode project

## Project Structure

```
shared/src/commonMain/kotlin/com/strakk/shared/
  domain/        # Models, UseCases, Repository interfaces — DEPENDS ON NOTHING
  data/          # RepositoryImpl, DTOs, Mappers, Supabase calls — depends on domain only
  presentation/  # ViewModels, UiState — depends on domain only
  di/            # Koin modules
androidApp/      # Jetpack Compose UI (Material 3)
iosApp/          # SwiftUI views + ViewModel wrappers
docs/specs/      # Feature specifications
```

## Build

```bash
./gradlew :shared:allTests                # Shared tests
./gradlew :androidApp:assembleDebug       # Android APK
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64  # iOS framework
```

## Architecture (Clean Architecture)

- **domain/** — ZERO dependencies. Pure Kotlin. See `architecture-rules` skill.
- **data/** — depends on domain only. All classes `internal`.
- **presentation/** — depends on domain only. ViewModels call UseCases, never Repositories.
- **UI** (iosApp, androidApp) — depends on presentation + domain models. Never imports data/.

Detailed conventions in skills: `kotlin-kmp-conventions`, `swiftui-conventions`, `compose-conventions`.

## Anti-Patterns (NEVER)

- Business logic in UI — delegate to UseCases
- ViewModel calls Repository directly — go through UseCase
- `collectAsState()` — use `collectAsStateWithLifecycle()`
- `sealed class` — use `sealed interface`
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
| `build-verify` | Runs builds, linters, tests | Sonnet |
| `quality-review` | Reviews architecture + conventions | Opus |
| `test-writer` | Writes shared tests | Sonnet |

## File Ownership

| Directory | Owner |
|-----------|-------|
| shared/ | @kotlin-shared |
| iosApp/ | @swift-ios |
| androidApp/ | @android-ui |
| docs/specs/ | @project-manager |

## Testing

- `kotlin.test` + Mokkery (mocking) + Turbine (Flow) + kotlinx-coroutines-test
- NO MockK (JVM-only, doesn't work on iOS/Native)
