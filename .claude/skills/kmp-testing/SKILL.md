---
name: kmp-testing
description: Testing playbook for Strakk shared KMP code using kotlin.test, Mokkery, Turbine, and coroutine tests.
paths:
  - "shared/src/commonTest/**/*.kt"
  - "shared/src/commonMain/**/*.kt"
---
# KMP Testing

Use this skill when writing or reviewing tests for shared domain, data, or presentation code.

## Stack

- `kotlin.test` for assertions and test annotations.
- Turbine for `Flow` and `StateFlow`.
- `kotlinx-coroutines-test` with `runTest`.
- Mokkery for mocks when fakes are not enough.
- Never use MockK in `commonTest`.

## Test Strategy

- Domain: pure tests, no mocks by default.
- Data: mapper tests, repository error mapping, DTO decoding behavior, external dependency mocked or faked.
- Presentation: ViewModel state transitions and effects with Turbine.
- Use fake repositories when they make behavior clearer than mocks.

## Required Coverage Shape

For every feature-bearing change, cover:

- Happy path with expected output.
- Empty or boundary state when relevant.
- Error path from repository/function/network failure.
- Cancellation path when coroutine code catches exceptions.

## Flow Test Pattern

Prefer:

```kotlin
viewModel.uiState.test {
    assertEquals(ExpectedInitialState, awaitItem())
    viewModel.onEvent(FeatureEvent.Submit)
    assertEquals(ExpectedNextState, awaitItem())
}
```

Avoid manual `collect` and arbitrary delays.
