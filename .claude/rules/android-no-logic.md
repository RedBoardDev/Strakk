---
description: "Android Composables must not contain business logic or data layer imports"
paths:
  - "androidApp/**/*.kt"
---
# Android UI Boundaries

Compose renders state and emits events.

- Follow Route / Screen / Content.
- Route obtains the KMP ViewModel with `koinViewModel()` and collects with `collectAsStateWithLifecycle()`.
- Screen and Content are stateless and previewable.
- `modifier: Modifier = Modifier` is the last parameter.
- UI may import shared presentation contracts and domain display models, never `com.strakk.shared.data.*`.
- No Supabase, Ktor, persistence, validation, or data transformation in composables.
- Handle insets with `Scaffold` `innerPadding` or `WindowInsets.safeDrawing`, never hardcoded status/navigation padding.
