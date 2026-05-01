---
description: "SQL migration safety rules"
paths:
  - "supabase/migrations/**/*.sql"
---
# Migration Safety

- Every user-owned table must have RLS enabled with auth boundary.
- Migrations must not weaken existing RLS policies.
- Use timestamped filenames: `YYYYMMDDHHMMSS_description.sql`.
- Destructive operations (DROP, TRUNCATE) require explicit confirmation.
- Never include secrets, tokens, or user data in migration files.
