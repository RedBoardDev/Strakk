# Rule — No secrets in tracked files

**Hard rule. Zero exceptions.**

A **secret** is any value that grants access to a system or service: API keys, tokens, JWTs, passwords, private keys, signing certificates, OAuth client secrets, webhook secrets, or service account JSONs.

## What this rule means

- No secret value may appear in any tracked file at any commit.
- Secrets live only in: `.env`, `local.properties`, `*.xcconfig` (gitignored), GitHub Actions secrets, Supabase Edge Function env, Keychain/1Password.
- Anon JWTs designed to be public (Supabase anon, Firebase web key) are technically embeddable in clients — but they must be paired with strong server-side authorization (RLS, security rules) and the URL+key combination should still flow through env-based config, not hardcoded constants.

## What to do if you find a secret in tracked code

1. **STOP** what you were doing.
2. Tell the user immediately. Use plain words: "I found what looks like a `<provider>` API key at `<path>:<line>`. This needs to be revoked at the provider and replaced with an empty placeholder."
3. Use the `release-readiness-lead` orchestration with the `secret-rotation` skill. The `secret-cleaner` agent replaces the value with an empty placeholder in the working tree; the user discards old git history when pushing to a new repo.
4. Do not commit any new code touching that file until the cleanup is done.

## What to do when adding new code that needs a secret

- Read the value from an environment variable or config file (`.env`, `local.properties`, `xcconfig`).
- Add the variable name to the example file (`.env.example`, `local.properties.example` if any).
- Document the variable in `docs/ENVIRONMENTS.md` with: name, where to obtain it, scope (prod/staging).
- Never default a real value in code; only safe defaults like empty string or `localhost:6333`.

## Enforcement

- `secret-auditor` agent runs in Phase 1 of release-readiness.
- `gitleaks` pre-commit hook (added by Phase 7) blocks committing future secrets.
- GitHub secret scanning + push protection (manual user setup).
