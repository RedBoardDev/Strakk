---
description: "iOS views must not contain business logic or data layer imports"
paths:
  - "iosApp/**/*.swift"
---
# iOS UI Boundaries

SwiftUI renders state and emits user intent.

- Views use native SwiftUI patterns: `NavigationStack`, data-driven `.sheet(item:)`, `.task {}` for async.
- KMP state enters SwiftUI through `@MainActor @Observable` wrappers.
- Wrappers observe SKIE AsyncSequence flows and cancel tasks in `deinit`.
- Swift files import the shared framework, not KMP `data` concepts.
- No Supabase, Ktor, persistence, validation, or business calculations in views.
- Platform APIs are allowed only for UI/platform concerns such as camera, photos, haptics, and notifications.
