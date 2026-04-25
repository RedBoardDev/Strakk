---
description: "Specification and research document conventions"
paths:
  - "docs/specs/**/*.md"
  - "docs/research/**/*.md"
  - "docs/reviews/**/*.md"
---
# Docs And Specs

Docs are planning artifacts, not dumping grounds.

- Keep specs actionable: scope, flows, acceptance criteria, data model, and test expectations.
- Use repo-relative paths, never machine-specific absolute paths.
- Separate product decisions from implementation notes.
- When a spec affects UI, reference `DESIGN.md` explicitly.
- When a spec affects shared logic, name the Clean Architecture layer that owns the behavior.
- Research docs must distinguish current project decisions from external examples.
