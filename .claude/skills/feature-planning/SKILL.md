---
name: feature-planning
description: Plans Strakk features across shared KMP, Supabase, SwiftUI, and Android Compose before implementation.
argument-hint: "[feature-or-spec-path]"
disable-model-invocation: true
---
# Feature Planning

Use this skill manually when turning a feature idea or `docs/specs/*.md` file into an implementation plan.

## Inputs

- Feature request or spec path from `$ARGUMENTS`.
- Relevant docs in `docs/specs/`.
- Project constraints from `CLAUDE.md`, `.claude/rules/`, and `DESIGN.md`.

## Planning Checklist

1. Define the product goal and non-goals.
2. Identify affected surfaces: domain, data, presentation, iOS, Android, Supabase, tests.
3. Ask only blocking questions. Do not ask questions already answered by specs.
4. Define contracts before files:
   - domain models and use cases
   - repository interfaces
   - DTOs and mappers
   - ViewModel state/event/effect
   - native UI entry points
5. Sequence work by dependency.
6. Assign owner agents: `architect`, `kotlin-shared`, `swift-ios`, `android-ui`, `test-writer`, `build-verify`, `quality-review`.
7. Include focused verification for each layer.

## Output

Return a concise plan with:

- Scope and assumptions.
- File list with repo-relative paths.
- Implementation units.
- Test scenarios.
- Risks and deferred questions.

Do not write code while planning.
