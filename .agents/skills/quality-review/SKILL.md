---
name: quality-review
description: Reviews Strakk changes for Clean Architecture, KMP correctness, native UI quality, tests, and security.
argument-hint: "[optional diff scope]"
context: fork
agent: quality-review
disable-model-invocation: true
---
# Quality Review

Review the current changes against Strakk standards.

## Review Areas

- Clean Architecture boundaries.
- KMP safety and iOS/Native compatibility.
- Supabase contracts, migrations, Edge Function auth, and RLS assumptions.
- SwiftUI wrapper lifecycle, `@MainActor`, SKIE AsyncSequence, and navigation.
- Compose state collection, Route/Screen/Content, edge-to-edge, and Material 3 token use.
- Design consistency with `DESIGN.md`.
- Test coverage and weak assertions.
- Security issues: secrets, service-role keys, unsafe logging, user data exposure.

## Output

Findings first, ordered by severity:

- `CRITICAL`: correctness, architecture, security, data loss, or build blockers.
- `WARNING`: likely bug, maintainability, weak tests, convention violation.
- `INFO`: polish or optional cleanup.

If there are no findings, say so clearly and list residual risks or tests not run.
