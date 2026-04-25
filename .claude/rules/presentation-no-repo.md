---
description: "ViewModels must use UseCases, never Repositories directly"
paths:
  - "shared/src/commonMain/kotlin/com/strakk/shared/presentation/**/*.kt"
---
# Presentation Uses Use Cases Only

Presentation coordinates screen state. It does not own business rules or data access.

- ViewModels depend on use cases, never repositories.
- ViewModels expose immutable `StateFlow<FeatureUiState>`.
- Events/intents/effects use `sealed interface`.
- One-off navigation or platform actions are effects, not state booleans.
- Presentation may map domain models to UI state, but validation and business invariants belong in domain use cases.
- Presentation must not import `com.strakk.shared.data.*`, Supabase, Ktor, or serialization.

Wrong: `class MyViewModel(private val repo: SessionRepository)`.
Right: `class MyViewModel(private val observeSessions: ObserveSessionsUseCase)`.
