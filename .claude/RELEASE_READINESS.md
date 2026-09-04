# Release Readiness — System Overview

> Goal: take the Strakk repo to **clean, secure, well-architected**, ready for a fresh push to a brand-new public GitHub repo.
>
> The user will discard the current git history and start over: `rm -rf .git && git init && git add . && git commit && push to new repo`. This means **no history rewriting** is performed — every fix happens in the current working tree.
>
> **Out of scope (for now):** App Store / Play Store submission, signing config, privacy manifests. The user is still in dev with Xcode and will tackle store work later.

## How to use it

```
/release-prep           # full pipeline (audit → fix → verify), gated at every phase
/release-audit          # read-only audit, produces docs/release-readiness-report.md
/release-verify         # final verification only (re-runs all auditors)
/release-prep --phase=4 # skip ahead to a specific phase
```

The `release-readiness-lead` agent (Opus) drives the pipeline. It never edits code itself — it delegates to specialized agents and verifies their output.

## Pipeline

```
Phase 0 — Bootstrap                      (lead alone)
Phase 1 — Parallel audit                 (8 read-only agents)
Phase 2 — Secret remediation             (working tree only — no history rewrite)
Phase 3 — Repo cleanup                   (.gitignore, untrack, .editorconfig, .gitattributes)
Phase 4 — Architecture + dead code + duplicates  (deep clean)
Phase 5 — Documentation                  (READMEs, ARCHITECTURE.md, ENVIRONMENTS.md)
Phase 6 — OSS public-repo polish         (LICENSE, SECURITY.md, etc.)
Phase 7 — CI/CD hardening                (pinned actions, permissions, gitleaks)
Phase 8 — Final verification + handoff   (8 sequential checks; user pushes manually)
```

Each phase has a **gate**: the lead stops and asks the user before continuing.

## Why no git history rewriting

The user will manually do `rm -rf .git && git init && ...` at the end of Phase 8 to publish a fresh first commit on a new public GitHub repo. This eliminates the need for `git filter-repo`, force-push, or any destructive history operations. The pipeline only cleans the **current working tree**.

**Important caveat:** even though local history is discarded, **any secret that was previously pushed to a public repo is considered compromised** and must be revoked at the provider. History discard does not undo a leak that was already public.

## Agents involved

### Lead
- **release-readiness-lead** (Opus) — orchestrator, never writes code

### Audit (read-only, run in parallel in Phase 1)
- **secret-auditor** — secrets in tracked files (history scan is informational only, since history will be discarded)
- **repo-hygiene-auditor** — build artifacts, OS junk, large files, internal docs
- **oss-readiness-checker** — LICENSE, SECURITY, CONTRIBUTING, etc.
- **dependency-auditor** — Gradle/SwiftPM/Deno pinning + CVEs
- **ci-cd-hardener** (audit mode) — workflow safety
- **dead-code-finder** — unused code, files, resources
- **duplicate-finder** — copy-pasted blocks, near-duplicates
- **architecture-reviewer** — Clean Architecture compliance, design smells, refactor proposals

### Fix (sequential, with verification loops)
- **secret-cleaner** — replaces secret values in tracked files with empty placeholders (no history rewrite)
- **repo-cleaner** — non-secret hygiene (`.gitignore`, untracking, scaffolding)
- **docs-curator** — all READMEs + ARCHITECTURE.md + ENVIRONMENTS.md
- **oss-publisher** — community files (LICENSE, SECURITY, CONTRIBUTING, COC, templates, dependabot)
- **ci-cd-hardener** (fix mode) — applies workflow hardening

### Verification
- **build-verify** — runs lint, tests, builds (already existed)
- **quality-review** — line-level conventions audit (already existed)

### Implementation (called from Phase 4 with focused tasks)
- **kotlin-shared**, **android-ui**, **swift-ios**, **supabase-edge** — apply per-finding fixes
- **architect**, **test-writer** — when refactor needs interface design or new tests

## Skills (knowledge bases)

- **production-readiness** — master checklist (this is the source of truth)
- **secret-rotation** — provider-by-provider revocation playbook
- **oss-public-repo** — templates: LICENSE, SECURITY.md, CONTRIBUTING, issue/PR templates, dependabot
- **gitignore-patterns** — canonical `.gitignore` for KMP/iOS/Android/Deno + `.editorconfig`, `.gitattributes`

## Rules (project-wide constraints)

- **no-secrets-in-tracked-files** — hard rule, zero exceptions
- **gitignore-coverage** — what `.gitignore` must always cover
- **architecture-boundaries** — Clean Architecture layers are not negotiable
- **no-dead-code** — no unused code, no duplication >30 lines
- **dependency-pinning** — exact versions everywhere, no ranges
- **public-repo-hygiene** — no internal docs in tracked files
- **readme-required** — every top-level module needs a README

## Decision points (where the lead pauses for the user)

| Phase | Question |
|-------|----------|
| 1 | "Here are the audit findings. Approve the fix plan?" |
| 1 | For each internal doc: "delete or just gitignore?" |
| 2 | "Confirm secret X has been revoked at provider Y?" |
| 4 | For each suspicious dead code item: "delete or keep (might be WIP)?" |
| 4 | For each duplicate cluster: "approve the proposed extraction?" |
| 6 | "Pick a license: MIT / Apache-2.0 / AGPL-3.0 / other?" |

## Files this system creates or updates

```
docs/
├── release-readiness-report.md   ← living report, updated each phase
├── ARCHITECTURE.md               ← created in Phase 5
├── ENVIRONMENTS.md               ← updated in Phase 5
└── RELEASE.md                    ← optional: tag → build workflow

LICENSE                            ← Phase 6
SECURITY.md                        ← Phase 6
CONTRIBUTING.md                    ← Phase 6
CODE_OF_CONDUCT.md                 ← Phase 6
.editorconfig                      ← Phase 3
.gitattributes                     ← Phase 3
.github/
├── ISSUE_TEMPLATE/
│   ├── bug_report.yml
│   ├── feature_request.yml
│   └── config.yml
├── PULL_REQUEST_TEMPLATE.md
└── dependabot.yml

shared/README.md                   ← Phase 5
androidApp/README.md               ← Phase 5
iosApp/README.md                   ← Phase 5
supabase/README.md                 ← Phase 5
infra/nutrition-api/README.md      ← Phase 5 (verified or rewritten)
scripts/README.md                  ← Phase 5
```

## Final manual step (after Phase 8)

When the orchestration completes, the user runs (manually):

```bash
rm -rf .git
git init
git add .
git status                         # review what's staged
git commit -m "chore: initial commit"
gh repo create <new-name> --public --source=. --remote=origin --push
```

Then in repo settings:
- Description, topics, homepage URL
- Enable Private Vulnerability Reporting
- Enable Dependabot alerts + security updates
- Enable secret scanning + push protection
- Branch protection on `main`

## Estimated effort

For a repo of Strakk's current state:

| Phase | Time | Notes |
|-------|------|-------|
| 1. Audit | ~5 min | parallel agents |
| 2. Secret remediation | ~5 min | edit files + revoke at provider (user side) |
| 3. Repo cleanup | ~10 min | mostly mechanical |
| 4. Architecture + dead code + dup | **highest variance** — could be hours | depends on findings |
| 5. Documentation | ~30 min | several READMEs |
| 6. OSS scaffolding | ~10 min | mostly templates |
| 7. CI/CD hardening | ~15 min | pinning + workflow edits |
| 8. Final verification | ~5 min | re-runs everything + handoff message |

Total: ~half a day for a clean run. More if Phase 4 surfaces large refactors.
