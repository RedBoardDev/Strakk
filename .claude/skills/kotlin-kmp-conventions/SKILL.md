---
description: "Kotlin/KMP coding conventions for the shared module"
paths:
  - "shared/**/*.kt"
---

# Kotlin / KMP Conventions for Strakk

## Language & Multiplatform

- Target: Kotlin 2.1.20+ with K2 compiler (upgrade path: 2.2.x for guard conditions, 2.3.x for nested type aliases)
- Platforms: Android + iOS (via Kotlin/Native)
- ALL shared code in `commonMain` unless platform-specific
- Use `expect`/`actual` for platform bridging
- iOS interop via SKIE (sealed classes -> Swift enums, suspend -> async/await, Flow -> AsyncSequence)

## Kotlin 2.2+ Language Features (adopt when upgrading)

### Guard conditions in `when` (Stable in 2.2)
Use `if` guard after the primary condition to avoid nested `if` / early returns:
```kotlin
when (event) {
    is SessionEvent.OnSave if sessionName.isNotBlank() -> save()
    is SessionEvent.OnSave -> showValidationError()
    is SessionEvent.OnDelete if !isLoading -> confirmDelete()
    else -> Unit
}
```

### Non-local `break` and `continue` (Stable in 2.2)
Works inside inline lambdas like `forEach`, `map`, `filter`:
```kotlin
items.forEach { item ->
    if (item.isInvalid) continue  // skip — no return@forEach needed
    if (item.isTerminal) break    // exit — no labeled return needed
    process(item)
}
```

### Multi-dollar string interpolation (Stable in 2.2)
Use `$$` prefix for strings with literal `$` (e.g., JSON templates, regex):
```kotlin
val template = $$"Price: $${price} USD"  // literal $ before variable
```

### Nested type aliases (Stable in 2.3)
Scope type aliases inside classes to reduce global namespace pollution:
```kotlin
class WorkoutPlanner {
    typealias ExerciseSlots = Map<DayOfWeek, List<Exercise>>
}
```

### Context parameters (Preview in 2.2 — DO NOT use yet)
Track for future adoption. Will replace manual DI passing for cross-cutting concerns (logging, auth context). Awaiting stable release (expected 2.4).

## Type System

- `sealed interface` for state hierarchies, events, errors — NEVER `sealed class`
- `value class` for type-safe IDs: `value class WorkoutId(val value: String)`
- `data class` for value objects and DTOs
- Prefer `val` over `var` — always
- Immutable collections in state: `List<T>` not `MutableList<T>`

## Use Cases

```kotlin
class GetWorkoutsUseCase(
    private val repository: WorkoutRepository,
) {
    operator fun invoke(): Flow<List<Workout>> = repository.getWorkouts()
}

class AddWorkoutUseCase(
    private val repository: WorkoutRepository,
) {
    suspend operator fun invoke(workout: Workout): Result<Unit> =
        repository.addWorkout(workout)
}
```

- Single responsibility per use case
- `operator fun invoke()` for callable syntax
- Return `Flow<T>` for reactive data, `Result<T>` for one-shot operations

## Data Layer

- ALL classes and functions are `internal`
- DTOs: `@Serializable internal data class WorkoutDto(...)`
- Mappers: `internal fun WorkoutDto.toDomain(): Workout`
- Repository implementations: `internal class WorkoutRepositoryImpl : WorkoutRepository`

## ViewModels (Presentation)

```kotlin
class FeatureViewModel(
    private val useCase: GetFeatureUseCase,
) : ViewModel() {

    val uiState: StateFlow<FeatureUiState> = useCase()
        .map { FeatureUiState.Success(it) }
        .catch { emit(FeatureUiState.Error(it.message.orEmpty())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeatureUiState.Loading)
}
```

- ViewModel from `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel` (KMP artifact)
- `viewModelScope` from lifecycle-viewmodel KMP — works on both Android and iOS
- Inject via Koin `koinViewModel<FeatureViewModel>()` in Compose

## Serialization

- `kotlinx.serialization` only — NEVER Gson, Moshi, Jackson
- `@SerialName("snake_case")` for Supabase JSON mapping
- DTOs live in data layer only
- supabase-kt uses `kotlinx.serialization` by default — no custom serializer config needed

## Date/Time

- `kotlinx.datetime` only — NEVER `java.time`, `java.util.Date`
- `Clock.System.now()` for current time
- `Instant` for timestamps, `LocalDate` for calendar dates

## Coroutines

- `Dispatchers.Default` for CPU work in commonMain
- NEVER use `Dispatchers.IO` (unavailable on Kotlin/Native)
- `viewModelScope` from lifecycle-viewmodel KMP artifact
- `SharingStarted.WhileSubscribed(5_000)` as canonical sharing strategy
- `GlobalScope` is forbidden

## DI (Koin 4.x)

```kotlin
val featureModule = module {
    single<WorkoutRepository> {
        WorkoutRepositoryImpl(
            supabaseClient = get(),
        )
    }
    factory { GetWorkoutsUseCase(repository = get()) }
    viewModel { WorkoutListViewModel(getWorkoutsUseCase = get()) }
}
```

- Named parameters in all Koin bindings
- `single` for repositories, `factory` for use cases, `viewModel` for ViewModels
- Constructor DSL shortcuts available: `singleOf(::WorkoutRepositoryImpl)`, `factoryOf(::GetWorkoutsUseCase)`, `viewModelOf(::WorkoutListViewModel)` — use when constructor params match Koin graph exactly
- `koinViewModel<T>()` in Compose (from `koin-compose-viewmodel`), with `parametersOf(...)` for keyed VMs
- Koin 4.x: Lazy Modules supported for parallel startup loading — use for large apps

## supabase-kt (3.x / Ktor 3)

- Uses Ktor 3 — incompatible with Ktor 2 dependencies in the same project
- Plugin installation: `Auth`, `Postgrest`, `Storage`, `Functions`, `Realtime`
- CRUD: `supabaseClient.from("table").select()`, `.insert()`, `.update()`, `.delete()`
- Decoding: `.decodeSingle<Dto>()`, `.decodeList<Dto>()` — uses kotlinx.serialization
- Realtime: `channel.postgrestChangeFlow<PostgrestAction.Insert>()` for live updates
- Edge Functions: `supabaseClient.functions.invoke("function-name", body = ...)`
- Storage: `supabaseClient.storage.from("bucket").upload(path, data)`

## SKIE (iOS Swift Interop)

SKIE generates Swift wrappers over Kotlin/Native Objective-C headers. Features:
- **Sealed classes/interfaces** -> exhaustive Swift `switch` via generated enums
- **Suspend functions** -> Swift `async throws` functions (bidirectional cancellation)
- **Flow<T>** -> Swift `AsyncSequence` (preserves generic type T)
- **Default arguments** -> generated Swift overloads (opt-in, enable in Gradle or via annotation)
- **Enum classes** -> native Swift enums with proper case names

All features enabled by default except default arguments. Design Kotlin APIs with SKIE in mind:
- Prefer `sealed interface` over `sealed class` — cleaner Swift enums
- Keep Flow return types explicit (`Flow<List<Session>>`) — SKIE preserves the type
- Avoid deeply nested generics in public API — they complicate Swift bridging

## Error Handling

- `Result<T>` for suspend functions that can fail
- `sealed interface` for domain-specific errors
- NEVER throw exceptions across layer boundaries
- Map exceptions to domain errors in data layer

## Style

- Trailing commas on ALL multiline declarations
- 120 char line limit
- 4-space indentation
- No wildcard imports
- KDoc on all public API
- One class per file

## References

- For complete use case patterns, see [references/use-case-patterns.kt](references/use-case-patterns.kt)
- For ViewModel examples, see [references/viewmodel-patterns.kt](references/viewmodel-patterns.kt)
- For Koin DI module patterns, see [references/koin-module-patterns.kt](references/koin-module-patterns.kt)
- For Supabase repository implementations, see [references/supabase-repository-patterns.kt](references/supabase-repository-patterns.kt)
