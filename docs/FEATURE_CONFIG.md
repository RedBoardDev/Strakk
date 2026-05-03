# Feature Configuration System

Single source of truth for all feature gating, quotas, and rate limits in Strakk.

## Architecture

```
Code (compile-time)                    Database (hot-reloadable)
─────────────────────                  ────────────────────────────
Feature.kt (enum)                      feature_limits table
  → which features exist                 → pro_only, quotas, rate limits
  → stable key per feature               → changeable without redeploy

FeatureRegistry.kt (object)            feature_usage table
  → UI metadata per feature              → usage counters per user/feature
  → title/description keys               → queried by server + client
  → platform icons
```

## Files

| File | Layer | Purpose |
|------|-------|---------|
| `shared/.../domain/model/Feature.kt` | Domain | Feature enum with stable `key` |
| `shared/.../domain/model/FeatureMetadata.kt` | Domain | UI metadata (titleKey, icons) |
| `shared/.../domain/model/FeatureRegistry.kt` | Domain | Maps Feature → FeatureMetadata |
| `shared/.../domain/model/FeatureLimits.kt` | Domain | Runtime config (proOnly, quotas, rate limits) |
| `shared/.../domain/model/FeatureAccess.kt` | Domain | Result: Granted / ProRequired / QuotaExhausted / RateLimited |
| `shared/.../domain/model/QuotaPeriod.kt` | Domain | DAY / WEEK / MONTH |
| `shared/.../domain/model/QuotaStatus.kt` | Domain | Unlimited / Blocked / Limited(used, limit) |
| `shared/.../domain/usecase/CheckFeatureAccessUseCase.kt` | Domain | Checks tier + quota in one call |
| `shared/.../domain/usecase/GetFeatureQuotaStatusUseCase.kt` | Domain | Returns quota status for UI display |
| `shared/.../data/repository/FeatureLimitsRepositoryImpl.kt` | Data | Reads feature_limits table, caches 5min |
| `shared/.../data/repository/FeatureUsageRepositoryImpl.kt` | Data | Counts feature_usage rows |
| `supabase/functions/_shared/feature-guard.ts` | Server | Unified guard: PRO + quota + rate limit |
| `supabase/migrations/..._add_feature_limits_and_usage.sql` | Server | Tables + RPC + seed data |

## Database Tables

### `feature_limits` (config — modify via Supabase Dashboard)

| Column | Type | Description |
|--------|------|-------------|
| `feature_key` | TEXT PK | Matches `Feature.key` in Kotlin |
| `pro_only` | BOOLEAN | Requires PRO subscription? |
| `quota_free` | INTEGER | Monthly quota for free users (0=blocked, -1=unlimited) |
| `quota_pro` | INTEGER | Monthly quota for PRO users (-1=unlimited) |
| `quota_period` | TEXT | `day`, `week`, or `month` |
| `rate_limit_max` | INTEGER | Max requests per window |
| `rate_limit_window_s` | INTEGER | Window in seconds |

### Current quotas

| Feature | PRO only | Free quota | PRO quota | Period |
|---------|----------|------------|-----------|--------|
| ai_photo_analysis | Yes | 0 | 100 | month |
| ai_text_analysis | Yes | 0 | 100 | month |
| ai_weekly_summary | Yes | 0 | 5 | month |
| hevy_export | Yes | 0 | 2 | month |
| health_sync | Yes | 0 | unlimited | — |
| unlimited_history | Yes | 0 | unlimited | — |
| photo_comparison | Yes | 0 | unlimited | — |

### `feature_usage` (counters — auto-populated)

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID PK | |
| `user_id` | UUID FK | |
| `feature_key` | TEXT | |
| `created_at` | TIMESTAMPTZ | Used for period filtering |

## How to Add a New Feature

### Step 1: Kotlin enum
```kotlin
// Feature.kt
BARCODE_SCAN("barcode_scan"),
```

### Step 2: Metadata
```kotlin
// FeatureRegistry.kt
Feature.BARCODE_SCAN to FeatureMetadata(
    feature = Feature.BARCODE_SCAN,
    titleKey = "feature_barcode_title",
    descriptionKey = "feature_barcode_description",
    iconIos = "barcode.viewfinder",
    iconAndroid = "QrCodeScanner",
)
```

### Step 3: Database config
```sql
INSERT INTO feature_limits VALUES
  ('barcode_scan', false, 5, -1, 'day', 10, 60);
```

### Step 4: Edge Function guard (if server-side)
```typescript
const guard = await requireFeatureAccess(userId, "barcode_scan");
if (guard) return guard;
// ... action ...
await recordFeatureUsage(userId, "barcode_scan");
```

### Step 5: Localization strings
- iOS: Add `feature_barcode_title` and `feature_barcode_description` to `Localizable.xcstrings`
- Android: Add to `res/values/strings.xml` and `res/values-fr/strings.xml`

## How to Change a Quota

No redeploy needed:
```sql
UPDATE feature_limits SET quota_pro = 200 WHERE feature_key = 'ai_photo_analysis';
```

Server cache refreshes every 5 minutes. Client refreshes on next app launch.

## Server Enforcement Flow

```
Edge Function receives request
  → requireUser(req)           // auth check
  → requireFeatureAccess(      // unified guard
      userId,
      "ai_photo_analysis"
    )
    ├── Reads feature_limits (cached 5min per isolate)
    ├── Checks PRO entitlement via hasProEntitlement()
    ├── Checks rate limit via Deno KV
    ├── Counts feature_usage rows for current period
    └── Returns null (allowed) or Response (403/429)
  → ... do expensive work (AI call, etc.) ...
  → recordFeatureUsage(userId, "ai_photo_analysis")
```

## Client Gating Flow

```
User taps PRO feature
  → guardProFeature(feature) [sync, checks isProUser()]
    ├── PRO? → proceed
    └── Free? → show FeatureGateSheet → PaywallView

OR (async, effect-based for direct actions like check-in create)
  → VM calls CheckFeatureAccessUseCase(feature)
    ├── Granted → proceed
    ├── ProRequired → emit effect → FeatureGateSheet
    └── QuotaExhausted → emit effect → QuotaExhaustedSheet
```

## Cost Model (internal)

| Feature | Cost/call | 100 calls/mo |
|---------|-----------|--------------|
| AI Photo (Claude Sonnet, 1024px) | ~0.005 EUR | 0.50 EUR |
| AI Text (Claude Sonnet) | ~0.002 EUR | 0.20 EUR |
| AI Weekly Summary | ~0.008 EUR | 0.04 EUR |
| Hevy Export | ~0 EUR | ~0 EUR |

Revenue net: 1.17 EUR/mo (annual) to 1.39 EUR/mo (monthly).
Worst case with current quotas: 0.74 EUR/mo → margin 0.43-0.65 EUR.
