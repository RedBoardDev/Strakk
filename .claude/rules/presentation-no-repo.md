---
description: "ViewModels must use UseCases, never Repositories directly"
globs:
  - "shared/src/commonMain/kotlin/com/strakk/shared/presentation/**/*.kt"
---
ViewModels must NOT depend on Repository interfaces or implementations.
All data access goes through UseCases.

WRONG: `class MyViewModel(private val repo: SessionRepository)`
RIGHT: `class MyViewModel(private val getSessions: GetSessionsUseCase)`
