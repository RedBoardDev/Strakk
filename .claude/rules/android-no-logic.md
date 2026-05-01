---
description: "Android Compose renders state only"
paths:
  - "androidApp/**/*.kt"
---
# Android UI Boundaries

- Follow Route / Screen / Content pattern. `modifier: Modifier = Modifier` is last parameter.
- Route obtains KMP ViewModel with `koinViewModel()`, collects with `collectAsStateWithLifecycle()`.
- Screen and Content are stateless and previewable.
- Never import `com.strakk.shared.data.*`, Supabase, Ktor, or persistence.

See `compose-conventions` skill for full patterns and examples.
