---
name: secret-rotation
description: "Provider-by-provider playbook for revoking and rotating API keys, JWTs, and credentials. Triggered before going public — even though we'll discard local git history, leaked values may already have been scraped, so revocation is mandatory."
---

# Secret Rotation Playbook

**Hard rule: revoke FIRST, before pushing to the new public repo.** Even though local history will be discarded by `rm -rf .git`, any value that was previously pushed to a public repo (or might have been seen by anyone with read access) is considered compromised. Revocation is the only fix.

## General workflow

1. **Identify** the leaked secret (provider, env, scope).
2. **Revoke** at the provider's dashboard. The key must stop working.
3. **Issue** a replacement.
4. **Distribute** the new key via the proper channel (env var, GitHub Actions secret, KeyVault, etc.).
5. **Update** the local `local.properties`, `.xcconfig`, `.env` files (never commit them — they're gitignored in the new repo).
6. **Verify** the app builds with the new key.

---

## Per-provider procedures

### Supabase

**Anon key (JWT)**
- Anon keys are designed to be public (the client embeds them). However, an exposed anon key with weak RLS is dangerous.
- Action if leaked WITH weak RLS: rotate JWT secret in Supabase Dashboard → Project Settings → API. This invalidates ALL existing JWTs, including user sessions. Users will be signed out.
- Steps:
  1. Dashboard → Settings → API → "Reset JWT secret" (require re-auth)
  2. Copy new `anon` key + `service_role` key
  3. Update `local.properties` / Edge Function envs
  4. Rebuild + redeploy edge functions
  5. Communicate to users that they will be signed out

**Service role key**
- High-risk; bypasses RLS.
- Action if leaked: same reset procedure. Audit recent server-side activity in Logs for the exposure period.

**Database password**
- Dashboard → Settings → Database → Reset password.
- Update connection strings in CI / local tools.

### RevenueCat

**Public API key (per platform)**
- Dashboard → Project → API keys → Generate new public API key for the platform → mark old one as deprecated → revoke when no client traffic remains.
- Update iOS `.xcconfig` and Android `local.properties`.
- Note: revoking immediately may affect users on old app versions still using the old key. Plan a grace period unless the leak is critical.

**Secret API key (server-side)**
- Same flow but no grace period.
- Used for webhooks, REST API. Update receiver immediately.

**Webhook secret**
- Dashboard → Webhooks → Edit → "Generate new secret" → update Edge Function env (`REVENUECAT_WEBHOOK_SECRET`).

### OpenAI

- Platform → API keys → Revoke the leaked key → Create new key.
- Prefer project-scoped keys (`sk-proj-`) over user keys (`sk-`).
- Audit usage: Platform → Usage → check for spikes during the exposure window.
- Update `infra/nutrition-api/.env` and any GitHub Actions secrets.

### Anthropic

- Console → Settings → API keys → Revoke → Generate new.
- Audit usage: Console → Usage.
- Update `infra/nutrition-api/.env` (`ANTHROPIC_API_KEY`).

### Google / Gemini

- Cloud Console → APIs & Services → Credentials → Find the API key → Regenerate or Delete + Create.
- Restrict the new key by API and HTTP referrer / IP if possible.
- For Firebase keys (similar `AIza` prefix): Firebase Console → Project Settings → Web API Key.

### Stripe

- Dashboard → Developers → API keys → Roll the secret key. Old key keeps working for 12 hours by default; set rollover to immediate if compromise is severe.
- Webhook signing secrets are separate per endpoint.

### GitHub

**Personal access token (`ghp_`, `github_pat_`)**: Settings → Developer settings → PATs → Revoke.
**OAuth app secret**: App settings → Generate new client secret.
**SSH key**: Settings → SSH and GPG keys → Delete leaked public key → generate new keypair locally.

### AWS

- IAM → Users → Security credentials → Make access key inactive → Delete → Create new.
- Audit: CloudTrail for the exposure window.
- Strongly prefer **IAM roles + OIDC federation** over long-lived keys for CI.

---

## Update channels (where new secrets go in the new repo)

| Channel | What for | How |
|---------|----------|-----|
| `local.properties` | Android Gradle reads (Supabase URL/key, RevenueCat) | Edit on dev machine; gitignored |
| `iosApp/Config/*.xcconfig` | iOS Xcode build settings | Edit on dev machine; gitignored |
| `infra/nutrition-api/.env` | Deno runtime on the VPS | Edit locally; deploy via `./deploy.sh`; update server-side `.env` directly |
| GitHub Actions secrets | CI workflows (signing, deploy) | `gh secret set NAME` or via repo settings |
| Supabase Edge Function env | Functions runtime | `supabase secrets set NAME=value` |
| KeyChain / 1Password | Personal store of all of the above | Out of repo always |

---

## Anti-patterns (do not do these)

- **Don't** assume "I'm starting a new repo, so the old key is safe" — if the value was ever public, it's compromised. Revoke.
- **Don't** keep the old and new key valid in parallel "to be safe" — doubles attack surface.
- **Don't** rotate a Supabase service-role / JWT secret without warning users (signs everyone out).
- **Don't** put secrets in git, even briefly, even in a private branch.
- **Don't** assume an anon JWT is safe just because it's a JWT — combined with weak RLS, it's a breach.

---

## Quick reference card

```
Leaked secret found in working tree
  ↓
Identify provider + scope          →  document in incident note
  ↓
Revoke at provider                 →  verify it stops working
  ↓
Issue replacement                  →  store in proper gitignored channel
  ↓
Update local config                →  rebuild and verify
  ↓
secret-cleaner edits the file      →  replaces value with empty placeholder
  ↓
secret-auditor re-verifies         →  0 hits in tracked files
  ↓
... continue release-readiness phases ...
```
