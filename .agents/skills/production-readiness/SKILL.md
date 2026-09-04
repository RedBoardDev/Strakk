---
name: production-readiness
description: "Master checklist for taking the Strakk repo to clean, secure, well-architected, public-OSS-safe state. App Store / Play Store submission is OUT OF SCOPE for now."
---

# Production Readiness — Master Checklist

This skill is the source of truth for what "production-ready" means for the Strakk repository **at the current stage**: still developing locally with Xcode, not yet preparing for store submission.

## The four target states

A finished release-readiness pass leaves the repo:

1. **Secure** — no secrets, no leak vectors
2. **Public-safe** — no internal docs in tracked files, all community files present, license clear
3. **Cleanly architected** — Clean Architecture compliant, no dead code, no duplication
4. **Well-documented** — every module has a usable README

**Out of scope** (intentionally deferred until the user is ready to submit to stores):
- Privacy manifest (`PrivacyInfo.xcprivacy`)
- App Store Connect / Play Console signing
- Store metadata, screenshots, descriptions
- Production signing config (release keystore management)

## Phase tree

```
release-readiness
├── 0. Bootstrap (orchestrator alone)
├── 1. Audit (11 agents in parallel)
│    ├── secret-auditor
│    ├── repo-hygiene-auditor
│    ├── oss-readiness-checker
│    ├── dependency-auditor
│    ├── ci-cd-hardener (audit mode)
│    ├── dead-code-finder
│    ├── duplicate-finder
│    ├── architecture-reviewer
│    ├── quality-review
│    ├── refactor-cleaner (knip/depcheck/ts-prune for TS/Deno)
│    └── build-verify (baseline state of lint+tests+builds)
├── 2. Secret remediation (working tree only — history will be discarded)
│    ├── secret-cleaner (replace values in current files)
│    └── secret-auditor (re-verify)
├── 3. Repo cleanup
│    ├── repo-cleaner (.gitignore, untrack, remove artifacts)
│    └── repo-hygiene-auditor (re-verify)
├── 4. Architecture + dead code + duplicates
│    ├── kotlin-shared / android-ui / swift-ios / supabase-edge (per-finding fixes)
│    ├── build-verify (loop)
│    └── quality-review
├── 5. Documentation
│    └── docs-curator (READMEs, ARCHITECTURE, ENVIRONMENTS)
├── 6. OSS scaffolding
│    └── oss-publisher (mode: oss only)
├── 7. CI/CD hardening
│    └── ci-cd-hardener (mode: fix)
└── 8. Final verification + fresh-push handoff
     ├── all 8 verification checks must be green
     └── user manually runs: rm -rf .git && git init && commit && push to NEW repo
```

## Fresh-push approach

The user has decided to **discard the current git history** and push everything as a clean first commit to a brand-new public GitHub repo. This means:

- **No history rewriting** is performed by any agent. No `git filter-repo`, no force-push, no BFG.
- Phase 2 only fixes secrets in **current working tree files**.
- The auditors prioritize the current working tree over historical scans.
- After Phase 8, the user manually:
  ```bash
  rm -rf .git
  git init
  git add .
  git commit -m "chore: initial commit"
  gh repo create <name> --public --source=. --remote=origin --push
  ```
- Despite history being discarded, **leaked secrets must still be revoked at the provider** — they may already have been scraped from the old public repo.

## Master checklist

### Security
- [ ] No tracked file contains a secret (any provider)
- [ ] All previously-leaked secrets have been revoked at the provider (history will be discarded but leaked values may already have been scraped)
- [ ] `.gitignore` covers all secret file patterns
- [ ] Pre-commit hook (gitleaks) blocks future leaks in the new repo

### Repository hygiene
- [ ] No build artifacts tracked (`build/`, `.gradle/`, `node_modules/`)
- [ ] No OS junk tracked (`.DS_Store`, `Thumbs.db`, `xcuserdata/`)
- [ ] No IDE files tracked (`.idea/`, `.vscode/`)
- [ ] No DB dumps or backups tracked
- [ ] No internal-only docs in public path
- [ ] No file >5MB without justification
- [ ] `.gitattributes` present (line endings, binary markers)
- [ ] `.editorconfig` present
- [ ] Per-module `.gitignore` where useful

### Architecture quality
- [ ] `domain/` has zero deps on `data/`, `presentation/`, SDKs (Ktor, supabase-kt, Android, Foundation)
- [ ] All `data/` classes are `internal` unless DI requires `public`
- [ ] ViewModels never inject `Repository` directly — only `UseCase`s
- [ ] No `Dispatchers.IO` in shared (KMP)
- [ ] Public API minimal — no accidental exposure
- [ ] No "god classes" (ViewModels >300 lines, repos >400 lines)
- [ ] Naming consistent across features

### Dead code
- [ ] Detekt unused warnings: 0
- [ ] No unused imports
- [ ] No unused private members
- [ ] No unused public symbols (verified via cross-file grep)
- [ ] No unused string resources
- [ ] No unused drawable/mipmap resources
- [ ] No orphan files (zero references)

### Duplication
- [ ] No 30+ line duplicated blocks
- [ ] Repository error-handling extracted to a helper (no duplicated try/catch boilerplate)
- [ ] Mappers either share base logic or are intentionally distinct
- [ ] Cross-platform UI components share constants where possible

### Documentation
- [ ] Root `README.md` complete (overview, setup, build, contribute, license, security)
- [ ] `shared/README.md` exists
- [ ] `androidApp/README.md` exists
- [ ] `iosApp/README.md` exists
- [ ] `supabase/README.md` exists
- [ ] `infra/nutrition-api/README.md` accurate
- [ ] `scripts/README.md` indexes all scripts
- [ ] `docs/ARCHITECTURE.md` present
- [ ] `docs/ENVIRONMENTS.md` accurate
- [ ] All commands in docs verified to work

### OSS community
- [ ] `LICENSE` at root (user-chosen)
- [ ] `SECURITY.md` (vulnerability disclosure policy)
- [ ] `CONTRIBUTING.md`
- [ ] `CODE_OF_CONDUCT.md` (Contributor Covenant 2.1)
- [ ] `.github/ISSUE_TEMPLATE/{bug_report,feature_request,config}.yml`
- [ ] `.github/PULL_REQUEST_TEMPLATE.md`
- [ ] `.github/dependabot.yml`
- [ ] Status badges in README
- [ ] GitHub repo settings (description, topics, homepage) — manual

### CI/CD
- [ ] All `uses:` actions pinned to commit SHA + version comment
- [ ] Every workflow has `permissions:` (least privilege)
- [ ] Every workflow has `concurrency:` block
- [ ] No `pull_request_target` without explicit justification
- [ ] gitleaks workflow runs on every push
- [ ] Branch protection rules documented for user to apply

### Dependencies
- [ ] All Gradle deps in `libs.versions.toml`, exact versions
- [ ] All Deno imports pinned to `@vX.Y.Z`
- [ ] `deno.lock` and `Package.resolved` committed
- [ ] No known critical CVEs
- [ ] Dependabot or Renovate configured

### Final builds
- [ ] `make lint-kotlin` passes
- [ ] `make lint-swift` passes (if available locally)
- [ ] `make lint-deno` passes
- [ ] `./gradlew :shared:allTests` passes
- [ ] `./gradlew :androidApp:assembleDebug` passes
- [ ] `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` passes
- [ ] `quality-review` reports 0 CRITICAL findings
- [ ] `architecture-reviewer` reports 0 CRITICAL findings

## Operating principles

- **Audit before fix.** Every phase starts with a read-only audit.
- **Gate before proceed.** No phase advances without user confirmation when irreversible.
- **Verify after fix.** Every fix phase ends with a re-audit.
- **Track everything.** TaskCreate per phase + TaskUpdate per agent invocation.
- **Stop on red.** Build verification failures block the next phase.

## Glossary

| Term | Meaning |
|------|---------|
| **Tracked file** | File visible to `git ls-files` |
| **History** | All blobs reachable from any ref |
| **CRITICAL** | Blocks release; do not push |
| **HIGH** | Should fix before next release |
| **MEDIUM** | Should fix in this cycle if cheap |
| **LOW** | Backlog |
| **Out of scope** | Will be addressed in a future release-readiness pass when the user prepares for store submission |
