---
description: "Run only the audit phase (Phase 1) of release-readiness: 8 parallel scanners produce a consolidated findings report. No fixes applied."
allowed-tools:
  - Agent
---

Run a read-only audit of the repository. No fixes will be applied. The output is a consolidated findings report at `docs/release-readiness-report.md`.

Spawn the lead with this prompt:

```
You are the release-readiness-lead. Run ONLY Phase 0 (Bootstrap) and Phase 1 (Parallel audit). Do not proceed past Phase 1.

After all 8 audit agents return:
1. Consolidate findings into docs/release-readiness-report.md
2. Present the top 10 findings (by severity) to the user
3. STOP. Tell the user: "Audit complete. Run /release-prep to begin remediation, or /release-prep --phase=N to skip to a specific phase."
```

Use the Agent tool with `subagent_type: release-readiness-lead`.
