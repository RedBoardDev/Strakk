# shared/

Kotlin Multiplatform module containing all business logic, compiled for both Android (JVM) and iOS (Native).

## Architecture

Clean Architecture with three layers:

```
domain/     # Pure Kotlin — models, repository interfaces, use cases
data/       # Internal — repository impls, DTOs, mappers, Supabase client
presentation/ # ViewModels (MVVM + contract pattern), UiState, Effects
di/         # Koin dependency injection modules
```

### Layer rules

| Layer | Can import | Cannot import |
|-------|-----------|---------------|
| `domain/` | Kotlin stdlib, kotlinx | data/, presentation/, Ktor, supabase-kt |
| `data/` | domain/ | presentation/ |
| `presentation/` | domain/ | data/, Android, UIKit |

### Visibility

- `data/` classes are `internal` (not visible to platform apps)
- `domain/` types are `public`
- `presentation/` ViewModels are `public` (consumed by iOS and Android)

## Key conventions

- **ViewModels** inject `UseCase` types only, never `Repository` directly
- **State**: single immutable `data class` via `MutableStateFlow`
- **Effects**: `SharedFlow<Effect>` of a sealed interface
- **Error mapping**: `data/` catches SDK exceptions and maps to `domain/` sealed error types
- **No platform types**: ViewModels never reference Android or iOS types
- **SKIE**: iOS consumes ViewModels via `@Observable` wrappers using SKIE bridging

## Adding a feature

1. Define models in `domain/model/`
2. Define repository interface in `domain/repository/`
3. Implement use case(s) in `domain/usecase/`
4. Implement repository in `data/repository/` (mark `internal`)
5. Add DTOs in `data/dto/`, mappers in `data/mapper/`
6. Create ViewModel in `presentation/<feature>/`
7. Wire DI in `di/DiModules.kt`
8. Write tests in `commonTest/`

## Build

```bash
# All tests (JVM + iOS simulator)
./gradlew :shared:allTests

# iOS framework for simulator
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# iOS framework for device
./gradlew :shared:linkDebugFrameworkIosArm64
```

## Testing

- **Framework**: `kotlin.test`
- **Mocking**: Mokkery (multiplatform, not MockK)
- **Flow testing**: Turbine
- **Coroutines**: `kotlinx-coroutines-test`

Tests live in `src/commonTest/`. No JVM-only test dependencies.
