# Rule — Dependencies must be pinned

Reproducible builds are non-negotiable. A build today must produce the same artifact as a build six months from now.

## Hard rules

- **Gradle**: every dependency goes through `gradle/libs.versions.toml` with an exact version (`1.2.3`). No inline `implementation("group:artifact:version")` strings (rare exceptions for plugin DSL must be commented). No `+`, no `latest.release`, no version ranges.
- **Swift Package Manager**: every package has an exact pin (`from: "1.2.3"` with `.upToNextMajor` is acceptable; `.branch("main")` is not). `Package.resolved` must be committed.
- **Deno**: every URL import has `@vX.Y.Z`. No `@latest`, no missing version, no `@main`. `deno.lock` must be committed for the nutrition-api.
- **GitHub Actions**: every `uses:` references a commit SHA. A trailing comment `# v4.1.7` documents the version.
- **Docker**: base images use exact tags or digests, never `latest`.

## Where versions live

- Kotlin: `gradle/libs.versions.toml` only.
- Swift: `Package.resolved` (committed). The Xcode project's package references should use `.upToNextMajor(from: "X.Y.Z")` or exact.
- Deno: inline in import URL (`@vX.Y.Z`) or in `deno.json` `imports` block.
- Actions: in workflow `uses:` line.

## Why

- An unpinned action can be force-pushed by its maintainer, replacing the code behind a tag with malicious code. (Real attacks have happened.)
- Range version specs make CI non-reproducible — a build that passed yesterday can fail today because a transitive lib released a breaking minor.
- "latest" pins make rollbacks ambiguous.

## Updating dependencies

Use Dependabot or Renovate (see `oss-public-repo` skill) for PR-based updates. Each PR is reviewed, tested, and merged individually so regressions are bisectable.

## Verification

```bash
# Anti-pattern detector — Gradle inline versions
grep -rEn 'implementation\("[^"]+:[^"]+:[^"]+"\)' --include='*.gradle.kts' . | grep -v 'libs\.'

# Anti-pattern detector — version ranges
grep -rEn ':\+|latest\.release|\.\+|version *= *"[^"]*\+' --include='*.gradle.kts' --include='*.toml' .

# Anti-pattern detector — Deno unpinned
grep -rEn 'https://(deno\.land|esm\.sh)/[^"]+' --include='*.ts' supabase/ infra/ | grep -vE '@v?[0-9]+\.[0-9]+\.[0-9]+'

# Anti-pattern detector — actions on tag
grep -rEn 'uses:\s*[^@]+@(main|master|latest|v[0-9]+(\.[0-9]+)?\s*$)' .github/workflows/
```
