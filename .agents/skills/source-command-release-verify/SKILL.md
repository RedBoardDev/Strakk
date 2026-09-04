---
name: "source-command-release-verify"
description: "Run only the final verification loop (Phase 8): re-runs all auditors and build-verify, reports green/red status. Does not apply fixes."
---

# source-command-release-verify

Use this skill when the user asks to run the migrated source command `release-verify`.

## Command Template

Re-run all audit agents and build verification to confirm the repo is in release-ready state. No fixes will be applied.

Spawn the lead with this prompt:

```
You are the release-readiness-lead. Run ONLY Phase 8 (Final verification loop).

Sequentially:
1. secret-auditor → must report 0 findings
2. repo-hygiene-auditor → must report 0 CRITICAL/HIGH
3. dependency-auditor → all pinned, no critical CVEs
4. dead-code-finder → 0 confirmed dead
5. architecture-reviewer → 0 CRITICAL
6. build-verify → all green (lint-kotlin, lint-deno, shared tests, androidApp build, iOS framework link)
7. quality-review → 0 CRITICAL
8. oss-readiness-checker → all checklist items satisfied

Report each step's status as a markdown table. If any step fails, STOP and tell the user which phase to re-run.
```

Use the Agent tool with `subagent_type: release-readiness-lead`.
