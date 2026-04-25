---
description: "Security and sensitive data rules"
---
# Security

Security rules are enforceable constraints, not style preferences.

- Never read, edit, print, or commit `.env`, `.env.*`, credentials, tokens, private keys, or local Supabase secrets.
- Never hardcode API keys, Supabase service-role keys, JWTs, or user-specific URLs.
- Supabase service-role keys are server-only and never appear in shared, iOS, or Android code.
- Client code uses anon key + authenticated user session only.
- Edge Functions validate user identity server-side before touching user data.
- Migrations must preserve RLS and user scoping; every user-owned table must include an auth boundary.
- Logs must not include JWTs, API keys, photos, personal measurements, or full request payloads containing sensitive data.
