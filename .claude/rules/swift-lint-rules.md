# Swift Lint Rules (SwiftLint)

Every Swift file in `iosApp/` MUST pass SwiftLint. Config at `iosApp/.swiftlint.yml`.
Apply these rules while writing code — not as a fix-up pass afterwards.

## Line length
- Max **120 chars** per line. Break long lines.
- Pattern-match `case let .ready(a, b, c):` instead of `case .ready(let a, let b, let c):` to save chars.

## Identifier names
- Min 2 chars, max 50. Never use single-letter variables (`m`, `s`, `v`).
- Use descriptive names: `mins` not `m`, `meas` not `m`, `vol` not `v`.

## Body lengths
- Function body: max **60 lines**. Extract sub-views or helpers if longer.
- Type body: max **300 lines**. If a View struct exceeds this, extract sections into private sub-structs or a separate file.
- File length: max **600 lines**. Split into multiple files if exceeded.

## When thresholds are exceeded
- Add `// swiftlint:disable <rule>` at file top for legitimate cases (complex single-screen views)
- Prefer extracting code over disabling rules

## Trailing commas
- Swift does NOT allow trailing commas in collection literals (unlike Kotlin)
- `[a, b, c]` — not `[a, b, c,]`

## Other
- No force unwrapping except in tests
- No `print()` in production code — use structured logging
- Prefer `guard let` over nested `if let`
- Use `@ViewBuilder` for conditional view composition
