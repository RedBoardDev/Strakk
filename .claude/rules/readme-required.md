# Rule — Every top-level module needs a README

A new contributor must be able to clone the repo and reach a building app within 30 minutes by reading READMEs alone. No tribal knowledge.

## Required READMEs

| File | Purpose |
|------|---------|
| `README.md` (root) | Project overview, prerequisites, setup, build, link to docs |
| `shared/README.md` | KMP module structure, Clean Architecture rules, how to add a feature |
| `androidApp/README.md` | Android variants, env, run, signed-AAB build |
| `iosApp/README.md` | XcodeGen, xcconfig setup, schemes, TestFlight |
| `supabase/README.md` | Local dev, edge functions, migration workflow |
| `infra/nutrition-api/README.md` | Self-hosted API: env, run, deploy, endpoints |
| `scripts/README.md` | Index of available scripts |
| `docs/ARCHITECTURE.md` | Architecture diagram + module rules |
| `docs/ENVIRONMENTS.md` | Per-environment config (`local.properties`, xcconfig, edge function env) |

## Quality bar

Every README must pass these checks:
- Every command in the README must work as written (paths, flags, target names).
- Every link must resolve (no 404s, no dead anchors).
- Every required env var or config key must match what `build.gradle.kts` / Edge Functions actually read.
- Lines under 120 chars where possible (readability in narrow viewers).
- Total length under 300 lines per README — split into linked subdocs if longer.

## What goes in a README vs separate doc

| Topic | Where |
|-------|-------|
| 30-second pitch + setup | Root README |
| Architecture details | `docs/ARCHITECTURE.md` |
| Per-environment secrets | `docs/ENVIRONMENTS.md` |
| Release/store submission | `docs/RELEASE.md` |
| Per-feature deep-dives | `docs/specs/<feature>.md` |
| Internal-only notes | NOT in the public repo |
