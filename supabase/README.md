# supabase/

Supabase project configuration: Postgres migrations and Deno Edge Functions.

## Structure

```
supabase/
├── migrations/     # Ordered SQL migrations (applied with supabase db push)
├── functions/      # Deno Edge Functions
│   ├── _shared/    # Shared utilities (Supabase client, CORS, types)
│   ├── calculate-goals/
│   ├── delete-account/
│   ├── export-to-hevy/
│   ├── extract-meal-draft/
│   ├── generate-checkin-summary/
│   ├── parse-workout-pdf/
│   ├── revenuecat-webhook/
│   ├── scan-meal/
│   ├── search-off-live/
│   └── deno.json   # Deno config (import map, lint, fmt)
└── config.toml     # Supabase local config
```

## Edge Functions

| Function | Description |
|----------|-------------|
| `calculate-goals` | AI-generated nutrition goals from user profile |
| `delete-account` | GDPR-compliant account deletion |
| `export-to-hevy` | Export workout data to Hevy format |
| `extract-meal-draft` | Parse meal text into structured draft |
| `generate-checkin-summary` | AI summary of check-in progress |
| `parse-workout-pdf` | Extract workout data from PDF |
| `revenuecat-webhook` | RevenueCat subscription event handler |
| `scan-meal` | Photo-based meal recognition |
| `search-off-live` | Live search against Open Food Facts |

## Local development

```bash
# Start local Supabase
supabase start

# Apply migrations
supabase db push
# or: make migrate

# Deploy a single function
supabase functions deploy <function-name> --no-verify-jwt

# Deploy all functions
make deploy-functions

# Reset database (applies all migrations + seed)
supabase db reset --linked
```

## Linting

```bash
# Lint all edge functions
make lint-deno
# Uses deno lint with config from functions/deno.json
```

## Migration workflow

1. Create a new migration: `supabase migration new <name>`
2. Write SQL in the generated file under `migrations/`
3. Test locally: `supabase db reset`
4. Push to linked project: `supabase db push`

Migrations are named with timestamps and applied in order.

## Environment variables

Edge Functions read secrets from Supabase's secret store:

```bash
supabase secrets set KEY=value
```

See [`docs/ENVIRONMENTS.md`](../docs/ENVIRONMENTS.md) for the full list.
