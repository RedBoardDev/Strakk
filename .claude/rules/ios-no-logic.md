---
description: "SwiftUI renders state only"
paths:
  - "iosApp/**/*.swift"
---
# iOS UI Boundaries

- KMP state enters SwiftUI through `@MainActor @Observable` wrappers.
- Wrappers observe SKIE AsyncSequence flows and cancel tasks in `deinit`.
- Views use `NavigationStack`, `.task {}`, SF Symbols, `@Environment` for DI.
- No Supabase, Ktor, persistence, or business logic in views.

See `swiftui-conventions` skill for full patterns and examples.
