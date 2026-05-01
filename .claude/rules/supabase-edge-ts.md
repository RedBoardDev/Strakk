---
description: "Edge Function TypeScript conventions"
paths:
  - "supabase/functions/**/*.ts"
---
# Edge Function TypeScript

- Reuse `_shared/` helpers (cors, auth, claude, rate-limit) — never duplicate.
- Every function handles OPTIONS, validates method and payload, returns JSON errors.
- Use `Deno.serve()` entry point. Access env via `Deno.env.get()`.
- ESM URL imports with pinned versions. No import maps.
- Run `make lint-deno` before committing.

See `supabase-edge-functions` skill for full patterns.
