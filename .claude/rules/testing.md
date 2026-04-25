---
description: "Testing conventions for Strakk shared code"
paths:
  - "shared/src/commonTest/**/*.kt"
  - "shared/src/*Test/**/*.kt"
---
# Testing

Use tests to prove behavior, not implementation details.

- Shared tests live under `shared/src/commonTest/kotlin/com/strakk/shared/`.
- Use `kotlin.test`, Turbine, kotlinx-coroutines-test, and Mokkery.
- Never use MockK in shared tests; it is JVM-only and breaks iOS/Native.
- Prefer fakes for domain tests. Use Mokkery for external dependencies and interaction checks.
- Use Turbine for all `Flow` / `StateFlow` assertions.
- Use `runTest` for coroutine tests.
- Every feature-bearing happy path should have at least one failure or edge case test.
- Test names use backticks and describe behavior.
