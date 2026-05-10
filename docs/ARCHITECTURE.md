# Architecture

## Overview

Strakk uses **Kotlin Multiplatform (KMP)** with **Clean Architecture** to share business logic between iOS and Android, with native UI on each platform.

```
┌─────────────────┐   ┌─────────────────┐
│   iosApp/       │   │   androidApp/   │
│   SwiftUI       │   │   Compose       │
└────────┬────────┘   └────────┬────────┘
         │                     │
         └──────────┬──────────┘
                    │
         ┌──────────▼──────────┐
         │   shared/           │
         │   presentation/     │  ViewModels, UiState, Effects
         ├─────────────────────┤
         │   domain/           │  Models, UseCases, Repository interfaces
         ├─────────────────────┤
         │   data/             │  Repository impls, DTOs, Mappers (internal)
         └──────────┬──────────┘
                    │
         ┌──────────▼──────────┐
         │   Supabase          │  Postgres, Auth, Storage, Edge Functions
         └─────────────────────┘
                    │
         ┌──────────▼──────────┐
         │   nutrition-api     │  Self-hosted Deno API (vision + vector search)
         └─────────────────────┘
```

## Layer rules

### domain/ (pure Kotlin)

- **Zero external dependencies.** Only Kotlin stdlib and kotlinx libraries.
- Contains: models, repository interfaces, use cases, domain errors, value objects.
- Everything is `public`.
- No imports from `data/`, `presentation/`, Ktor, supabase-kt, Android, or Foundation.

### data/ (internal)

- Implements domain repository interfaces.
- All classes are **`internal`** (invisible to platform apps).
- Catches SDK-specific exceptions and maps them to domain error types.
- Contains: repository implementations, DTOs, mappers, Supabase client configuration.
- Can import from `domain/` only.

### presentation/ (ViewModels)

- ViewModels inject **UseCase** types, never repositories directly.
- State is a single immutable `data class` exposed via `StateFlow`.
- One-shot effects use `SharedFlow<Effect>` with a sealed interface.
- No platform-specific types (no Android, no UIKit).
- Can import from `domain/` only.

### Platform apps

| App | Can import | Cannot import |
|-----|-----------|---------------|
| `androidApp/` | `presentation/`, `domain/` | `data/` (won't compile — it's `internal`) |
| `iosApp/` | `presentation/`, `domain/` (via SKIE) | `data/` |

## ViewModel contract pattern

Each ViewModel exposes:

```kotlin
class FooViewModel : ViewModel() {
    val uiState: StateFlow<FooUiState>       // Current state
    val effects: SharedFlow<FooEffect>       // One-shot events
    fun onEvent(event: FooEvent)             // UI actions
}
```

- **UiState**: immutable data class, updated via `MutableStateFlow`
- **Effect**: sealed interface (navigation, snackbar, etc.)
- **Event**: sealed interface of user actions

## Error handling

```
SDK exception (Ktor, Supabase)
  → data/ catches and maps to domain sealed error
    → presentation/ maps domain error to UiState
      → UI displays localized message
```

No SDK exception type leaks beyond `data/`.

## Dependency injection

Koin is used for DI across all layers:

- `domainModule` — use cases
- `dataModule` — repositories (binds interface to `internal` impl)
- `presentationModule` — ViewModels

Platform apps call `initKoin()` at startup with platform-specific overrides.

## iOS bridging (SKIE)

[SKIE](https://skie.touchlab.co/) generates Swift-friendly wrappers for Kotlin sealed types, coroutines, and flows. iOS ViewModels are wrapped in `@Observable` classes that bridge `StateFlow` to SwiftUI's observation system.

## Edge Functions

Supabase Edge Functions (Deno/TypeScript) handle server-side logic:

- AI-powered operations (goal calculation, meal extraction, check-in summaries)
- Webhook processing (RevenueCat subscription events)
- External API proxying (Open Food Facts search)

Shared utilities live in `supabase/functions/_shared/`.

## Nutrition API

A separate self-hosted Deno service (`infra/nutrition-api/`) handles meal photo scanning:

1. Vision analysis (OpenAI) identifies food items in a photo
2. Text embeddings are generated for each identified item
3. Vector similarity search (Qdrant) matches against the food catalog
4. Results are disambiguated and returned with nutritional data

## Database

PostgreSQL via Supabase with:

- Row-Level Security (RLS) on all user-facing tables
- `pgvector` extension for food catalog embeddings
- Migrations applied in timestamp order from `supabase/migrations/`
