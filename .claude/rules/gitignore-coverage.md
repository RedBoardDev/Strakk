# Rule — .gitignore must cover the canonical patterns

The root `.gitignore` must always cover (at minimum):

- All secret patterns: `.env`, `.env.*` (negate `.env.example`), `local.properties`, `*.pem`, `*.key`, `*.p12`, `*.jks`, `*.keystore`, `*.mobileprovision`, `*.cer`, `*service-account*.json`, `*credentials*.json`
- iOS env config: `iosApp/Config/Production.xcconfig`, `iosApp/Config/Staging.xcconfig` (negate `*.xcconfig.example` if used)
- Build artifacts: `build/`, `.gradle/`, `.kotlin/`, `out/`, `target/`, `dist/`, `node_modules/`
- iOS build: `DerivedData/`, `xcuserdata/`, `.swiftpm/`, generated `iosApp/Strakk.xcodeproj/`
- Android build: `*.apk`, `*.aab`, `captures/`, `.cxx/`
- Database: `*.sql.gz`, `*.dump`, `*.bak`, `*.backup`, `supabase/backup_*.sql`
- OS / IDE: `.DS_Store`, `Thumbs.db`, `.idea/`, `*.iml`, `.vscode/*`, `.fleet/`, `*.swp`
- Logs / temp: `*.log`, `logs/`, `tmp/`
- Agent memory: `.claude/agent-memory/`, `infra/nutrition-api/.claude/agent-memory/`

The full canonical list is in the `gitignore-patterns` skill.

## Per-module .gitignore

Required where confusion is possible:
- `infra/nutrition-api/.gitignore` (`.env`, `import/data/`)
- `scripts/<each subdir>/.gitignore` (`.env`, generated outputs)

## Verification

```bash
# A file that should be ignored:
git check-ignore -v <path>      # exits 0 if ignored, 1 otherwise

# Tracked files that match an ignore pattern (should be empty):
git ls-files | xargs -I{} sh -c 'git check-ignore -q "{}" && echo "TRACKED-BUT-IGNORED: {}"'
```

## Anti-patterns

- Trusting that "git won't commit it because it's gitignored" — git only respects gitignore for **untracked** files. If a file was committed before the ignore rule existed, you must `git rm --cached` it.
- Per-module `.gitignore` that contradicts root — keep root authoritative; per-module narrows further.
- Adding personal IDE preferences (e.g., `.idea/workspace.xml`) without negating shared workspace files (e.g., `!.idea/runConfigurations/`).
