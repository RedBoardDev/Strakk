---
name: koin-di-patterns
description: Koin dependency injection patterns for Strakk's KMP shared module and native app entry points.
paths:
  - "shared/src/commonMain/kotlin/com/strakk/shared/di/**/*.kt"
  - "shared/src/iosMain/**/*.kt"
  - "androidApp/**/*.kt"
  - "iosApp/**/*.swift"
---
# Koin DI Patterns

Use Koin to wire shared domain, data, presentation, and platform adapters.

## Binding Rules

- Repositories: `single<Repository> { RepositoryImpl(...) }`.
- Use cases: `factory { SomeUseCase(repository = get()) }`.
- ViewModels: `viewModel { FeatureViewModel(useCase = get()) }`.
- Prefer named constructor arguments in bindings.
- Keep data implementations `internal`; bind them behind domain interfaces.
- Platform-specific services should be interfaces in common code with platform bindings in `androidMain` / `iosMain`, or provided from native app setup when needed.

## Layer Ownership

- `domain/` never imports Koin.
- `data/di` may bind repository implementations and data sources.
- `presentation/di` may bind ViewModels.
- Root DI modules compose feature modules; avoid one giant module once feature modules exist.

## iOS Bridge

- Swift uses `KoinHelper` / generated bridge helpers to obtain shared ViewModels.
- Do not instantiate Kotlin repositories or data implementations directly from Swift.
- Keep Koin startup in app initialization, not screen bodies.

## Tests

- Add Koin module verification when module count grows.
- Prefer testing use cases and ViewModels directly rather than testing Koin itself.
