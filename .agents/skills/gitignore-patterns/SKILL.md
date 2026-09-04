---
name: gitignore-patterns
description: "Canonical .gitignore patterns for KMP / Android / iOS / Deno / Supabase / shared OS+IDE junk. Source of truth for repo-cleaner."
---

# Gitignore Patterns

Authoritative patterns for the Strakk repo. Used by `repo-cleaner` to update the root `.gitignore` and create per-module ones.

## Root `.gitignore` (canonical)

```gitignore
# ============================================================
# Secrets — NEVER commit
# ============================================================
.env
.env.*
!.env.example
!.env.sample
local.properties
secrets.properties
*.pem
*.key
*.p12
*.jks
*.keystore
*.mobileprovision
*.cer
*service-account*.json
*credentials*.json
scripts/**/.env

# iOS Config (xcconfig may contain env-specific keys)
iosApp/Config/Production.xcconfig
iosApp/Config/Staging.xcconfig
!iosApp/Config/*.xcconfig.example

# ============================================================
# Build artifacts
# ============================================================
build/
.gradle/
.kotlin/
out/
target/
dist/
.cache/

# Android
*.apk
*.aab
local.properties
captures/
.externalNativeBuild/
.cxx/

# iOS
DerivedData/
*.xcworkspace/xcuserdata/
*.xcodeproj/xcuserdata/
*.xcodeproj/project.xcworkspace/xcuserdata/
*.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/Package.resolved.lock
xcuserdata/
.swiftpm/

# Generated Xcode project (regenerated via xcodegen)
iosApp/Strakk.xcodeproj/

# Pods (only if CocoaPods is used; SPM doesn't need this)
Pods/

# Deno
deno.lock.bak

# Node (if any tooling)
node_modules/
.next/
.npm

# Python
.venv/
__pycache__/
*.pyc
*.pyo
.mypy_cache/
.pytest_cache/

# ============================================================
# Database / backups
# ============================================================
*.sql.gz
*.dump
*.bak
*.backup
supabase/backup_*.sql
supabase/.temp/

# ============================================================
# OS / IDE junk
# ============================================================
.DS_Store
.DS_Store?
._*
.Spotlight-V100
.Trashes
ehthumbs.db
Thumbs.db
Desktop.ini

# JetBrains (keep workspace.xml ignored, allow shared run configs)
.idea/
*.iml
!.idea/runConfigurations/

# VS Code
.vscode/*
!.vscode/settings.json.example
!.vscode/extensions.json

# Fleet
.fleet/

# Vim
*.swp
*.swo
*~

# ============================================================
# Logs / temp
# ============================================================
*.log
*.log.*
logs/
tmp/
temp/

# ============================================================
# Codex agent memory (project-local)
# ============================================================
.Codex/agent-memory/
infra/nutrition-api/.Codex/agent-memory/

# ============================================================
# Test outputs / coverage
# ============================================================
coverage/
.nyc_output/
test-results/

# ============================================================
# CI / deploy local artifacts
# ============================================================
.terraform/
*.tfstate
*.tfstate.backup
```

## Per-module `.gitignore` files

### `infra/nutrition-api/.gitignore`
```gitignore
.env
import/data/
import/cache/
*.log
```

### `scripts/backtest/.gitignore`
```gitignore
.env
__pycache__/
*.pyc
results/
runs/
recipes/
variants/
_lib/
```

### `scripts/import-embeddings/.gitignore`
```gitignore
.env
node_modules/
*.log
```

## `.gitattributes` (canonical)

```gitattributes
# Auto-detect text files and normalize line endings
* text=auto eol=lf

# Source code
*.kt text eol=lf
*.kts text eol=lf
*.swift text eol=lf
*.ts text eol=lf
*.js text eol=lf
*.json text eol=lf
*.yml text eol=lf
*.yaml text eol=lf
*.md text eol=lf
*.sh text eol=lf
*.py text eol=lf
*.sql text eol=lf

# Windows scripts
*.bat text eol=crlf
*.cmd text eol=crlf

# Binary
*.png binary
*.jpg binary
*.jpeg binary
*.gif binary
*.webp binary
*.pdf binary
*.zip binary
*.tar binary
*.gz binary
*.jar binary
*.aar binary
*.apk binary
*.aab binary
*.ipa binary
*.keystore binary
*.jks binary

# Linguist overrides (helps GitHub language detection)
*.gradle.kts linguist-language=Kotlin
shared/build/** linguist-generated
**/Generated/** linguist-generated
**/build/generated/** linguist-generated

# Exclude from `git archive` releases
.github export-ignore
.Codex export-ignore
docs/internal export-ignore
.gitattributes export-ignore
.gitignore export-ignore
```

## `.editorconfig` (canonical)

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true
indent_style = space
indent_size = 4

[*.{kt,kts}]
indent_size = 4
ij_kotlin_imports_layout = *,java.**,javax.**,kotlin.**,^

[*.{swift}]
indent_size = 4

[*.{ts,js,json,yml,yaml}]
indent_size = 2

[*.md]
trim_trailing_whitespace = false

[Makefile]
indent_style = tab
```

## What goes in `.gitignore` vs deleted

| Situation | Action |
|-----------|--------|
| File exists locally, should never be tracked | `git rm --cached <path>` + add to `.gitignore` |
| File exists locally, may be re-created (build artifact) | Add to `.gitignore`; delete on disk if huge |
| File should not exist at all | `git rm <path>` (no gitignore needed if won't recur) |
| Pattern of files (e.g., `*.log`) | Add pattern to `.gitignore`; clean existing matches |

## Verification commands

```bash
# Confirm a file is now ignored
git check-ignore -v <path>

# List currently tracked files matching a pattern
git ls-files | grep -E '<pattern>'

# Show all untracked + ignored files
git status --ignored

# Find tracked files that should be ignored
git ls-files | xargs -I{} sh -c 'git check-ignore -q "{}" 2>/dev/null && echo "TRACKED-BUT-IGNORED: {}"'
```

## Anti-patterns

- Leading slash with no directory (`/.env`) — only matches root `.env`. Use `.env` to match anywhere.
- Trailing slash on file pattern (`*.log/`) — only matches directories. Use `*.log` for files.
- Negation without preceding inclusion — `!important.txt` only works if a parent pattern excluded it first.
- Per-module `.gitignore` that contradicts root `.gitignore` — root takes precedence for files in the root scope; per-module narrows further.
- Putting `node_modules/` in a per-module `.gitignore` only — should be at root since it can appear anywhere.
