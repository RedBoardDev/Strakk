---
description: "Testing conventions for shared code"
paths:
  - "shared/src/commonTest/**/*.kt"
  - "shared/src/*Test/**/*.kt"
---
# Testing

- Use `kotlin.test`, Turbine, kotlinx-coroutines-test, and Mokkery.
- Never use MockK (JVM-only, breaks iOS/Native). Prefer fakes for domain tests.
- Use `runTest` for coroutines, Turbine for all Flow/StateFlow assertions.
- Test names use backticks and describe behavior.

See `kmp-testing` skill for full patterns.
