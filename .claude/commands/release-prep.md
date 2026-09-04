---
description: "Run the full release-readiness orchestration: audit, secret cleanup, repo hygiene, dead code, duplicates, architecture, docs, OSS scaffolding, CI/CD hardening, final verification."
allowed-tools:
  - Agent
argument-hint: "[--phase=<n>] (optional: skip directly to phase N if a previous run completed earlier phases)"
---

You are about to delegate the full release-readiness orchestration to the `release-readiness-lead` agent.

The orchestration is **gated, audited, and verified at every phase**. The user will be asked to confirm before any potentially destructive action (deleting tracked files, large refactors).

**No git history rewriting** is performed — the user will discard the current history at the end and push to a brand-new public GitHub repo.

**Out of scope** for this orchestration: App Store / Play Store submission, signing, privacy manifests, store listings. The user is still in dev with Xcode and will tackle store work later.

Spawn the lead with this prompt:

```
You are the release-readiness-lead. Run the full workflow described in your agent definition.

Operating mode: full pipeline starting at Phase 0 (Bootstrap).

If the user passed a `--phase=N` argument, start at that phase instead of 0. Verify all earlier phases are complete by checking their gate criteria before proceeding.

Begin by reading:
- docs/security-audit.md (if present — prior findings)
- docs/release-readiness-report.md (if present — running report)

Then post a one-screen status summary:
- Current branch + remote sync state
- Phases planned (8 total)
- Phase you are about to enter
- One-line tell-the-user message

Then create the master task list with TaskCreate (one parent per phase) and begin Phase 1.

User arguments: $ARGUMENTS
```

Use the Agent tool with `subagent_type: release-readiness-lead`.
