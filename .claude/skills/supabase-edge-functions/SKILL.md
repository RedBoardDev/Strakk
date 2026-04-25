---
name: supabase-edge-functions
description: Expert guide for building, deploying, and calling Supabase Edge Functions from a KMP app (supabase-kt 3.x + iOS/Android). Covers authentication, serialization, debugging, deployment, and common pitfalls.
when_to_use: When implementing, deploying, or debugging any Supabase Edge Function in this project. Especially relevant when a function returns 401 or when wiring KMP calls.
paths:
  - "supabase/functions/**/*.ts"
  - "supabase/migrations/**/*.sql"
  - "shared/src/commonMain/kotlin/com/strakk/shared/data/**/*.kt"
---

# Supabase Edge Functions — Expert Guide

## 0. Project layout (MANDATORY)

```
supabase/
├── config.toml
├── migrations/                     # SQL migrations
└── functions/
    ├── _shared/                    # Shared modules — never deployed as a function
    │   ├── cors.ts                 # CORS headers helper
    │   └── auth.ts                 # JWT validation helper (requireUser)
    ├── analyze-meal/
    │   └── index.ts
    ├── delete-account/
    │   └── index.ts
    └── <other-function>/
        └── index.ts
```

- Each function lives in its own folder with an `index.ts` entry point
- The `_shared/` folder (underscore prefix) is ignored by `supabase functions deploy` but accessible via relative imports like `../_shared/cors.ts`
- Keep all cross-function logic (CORS, auth, error mapping) in `_shared/` — DO NOT duplicate

## 1. Authentication — how it ACTUALLY works (critical)

### The three auth modes of an Edge Function

| Mode | Deploy flag | Gateway check | Use when |
|------|-------------|---------------|----------|
| **Default** | (no flag) | Rejects non-JWT requests with 401 | Your function requires a logged-in user |
| **Public** | `--no-verify-jwt` | Accepts any request | Public endpoints, webhooks, cron jobs; **OR projects using ES256 JWTs (see warning below)** |

In **default mode**, Supabase's API gateway verifies the JWT BEFORE your function executes. If the `Authorization` header doesn't contain a valid JWT (user access_token OR anon_key), the request is rejected with 401 and your function never runs.

### ⚠️ ES256 asymmetric JWTs — known gateway incompatibility

Supabase has migrated newer projects to asymmetric JWT signing with **ES256** (vs legacy HS256). When this is active:

- The Auth API issues access tokens with header `{"alg":"ES256"}` — correct and expected.
- **BUT** the Edge Functions gateway's pre-verification layer rejects ES256 tokens with:
  ```
  UNAUTHORIZED_UNSUPPORTED_TOKEN_ALGORITHM
  "Unsupported JWT algorithm ES256"
  ```
- This happens BEFORE your function runs — no logs, no `console.log`, just a 401 from the gateway.

**How to detect:** decode the access token header (base64 of the first dot-separated segment). If `alg: ES256`, you're affected. The error wording `UNSUPPORTED_TOKEN_ALGORITHM` is also a dead giveaway.

**Fix:** Deploy the function with `--no-verify-jwt`. This bypasses the broken gateway check. Your function's own `requireUser(req)` (which calls `supabase.auth.getUser(token)` against the Auth API) remains secure — the Auth API correctly verifies ES256 tokens since it's the issuer.

```bash
supabase functions deploy <name> --no-verify-jwt
```

Security is unchanged: the function still requires a valid user JWT, just validated by our code (`getUser(token)`) instead of the gateway.

Revisit this once Supabase updates the gateway to support ES256.

### What supabase-kt sends (always)

When you call `supabaseClient.functions.invoke(...)` from a KMP client with `Auth` plugin installed AND a session active, the SDK automatically sends TWO headers:

| Header | Value | Added by |
|--------|-------|----------|
| `apikey` | project anon key (`SUPABASE_ANON_KEY`) | `KtorSupabaseHttpClient.DefaultRequest` (always) |
| `Authorization` | `Bearer <user_access_token>` | `AuthenticatedSupabaseApi.bearerAuth()` (auto) |

If no session is active, `Authorization` falls back to the anon key. The gateway accepts it (anon_key passes JWT verify) but your function sees the anon_key as the "user" — `auth.getUser(token)` will then fail.

### Inside the function: extract the user CORRECTLY

```typescript
// supabase/functions/_shared/auth.ts
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

export async function requireUser(
  req: Request,
): Promise<{ userId: string; email: string | null }> {
  const authHeader = req.headers.get("Authorization");
  if (!authHeader) throw new Error("Missing Authorization header");

  // Extract the Bearer token
  const token = authHeader.replace(/^Bearer\s+/i, "").trim();
  if (!token) throw new Error("Empty Bearer token");

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
  );

  // ⚠️ CRITICAL: pass the token explicitly to getUser(token)
  // Calling getUser() without an argument tries to read a local session
  // (which doesn't exist server-side) and ALWAYS fails.
  const { data, error } = await supabase.auth.getUser(token);
  if (error || !data.user) {
    console.error("[auth] getUser failed:", error?.message);
    throw new Error("Invalid or expired token");
  }

  return { userId: data.user.id, email: data.user.email ?? null };
}
```

**The single most common 401 cause:** calling `supabase.auth.getUser()` without the token. Always pass it: `getUser(token)`.

### Alternative: `getClaims(token)` (faster, no network)

```typescript
const { data } = await supabase.auth.getClaims(token);
const userId = data?.claims?.sub;
```

`getClaims()` verifies the JWT locally using the project's secret (no Auth API roundtrip). Faster and lighter. Use when you just need the user id.

## 2. KMP client side — calling a function properly

### Prerequisites
- `Functions` plugin installed in `SupabaseProvider`:
  ```kotlin
  createSupabaseClient(url, key) {
      install(Auth) { /* ... */ }
      install(Postgrest)
      install(Functions)  // <-- required
  }
  ```
- `supabase-functions-kt` in `libs.versions.toml` and `shared/build.gradle.kts`

### Correct invocation pattern

**Always define a `@Serializable` DTO for the request body — don't use `JsonObject` + `setBody()`.**

```kotlin
// 1. Define request + response DTOs in data/dto/
@Serializable
internal data class AnalyzeMealRequestDto(
    @SerialName("image_base64") val imageBase64: String,
)

@Serializable
internal data class AnalyzeMealResponseDto(
    @SerialName("suggested_name") val suggestedName: String?,
    val items: List<AnalyzedItemDto>,
    // ...
)

// 2. In the repository implementation
override suspend fun analyzeMeal(imageBase64: String): MealAnalysis {
    if (supabaseClient.auth.currentSessionOrNull() == null) {
        throw DomainError.AuthError("No active session.")
    }

    val response = try {
        supabaseClient.functions.invoke(
            function = "analyze-meal",
            body = AnalyzeMealRequestDto(imageBase64 = imageBase64),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val msg = e.message.orEmpty()
        val userMessage = when {
            "401" in msg -> "Your session expired. Please sign in again."
            "400" in msg -> "Bad request."
            else -> "Couldn't reach the service. ($msg)"
        }
        throw DomainError.DataError(userMessage, e)
    }

    if (response.status.value !in 200..299) {
        throw DomainError.DataError(mapStatusToMessage(response.status.value))
    }

    return try {
        response.body<AnalyzeMealResponseDto>().toDomain()
    } catch (e: Exception) {
        throw DomainError.DataError("Unexpected response.", e)
    }
}
```

### DON'T do this (common mistakes)

❌ Setting body manually with JsonObject:
```kotlin
supabaseClient.functions.invoke("foo") {
    setBody(buildJsonObject { put("x", "y") })  // broken — no content negotiation
}
```
✅ Use a `@Serializable` DTO via the `body` parameter.

❌ Manually attaching `Authorization: Bearer` header:
```kotlin
supabaseClient.functions.invoke("foo") {
    headers[HttpHeaders.Authorization] = "Bearer $token"  // useless — already auto-attached
}
```
✅ Just make sure Auth plugin is installed and user is signed in.

❌ Forgetting the `Functions` plugin in `SupabaseProvider`:
```kotlin
// Missing install(Functions) → .functions property doesn't exist
```

## 3. Deploy & secrets

### Deploy
```bash
supabase functions deploy <function-name>              # default: verify JWT
supabase functions deploy <function-name> --no-verify-jwt  # public endpoint
```

### Manage secrets (env vars available to the function)
```bash
supabase secrets set ANTHROPIC_API_KEY=sk-ant-xxx
supabase secrets set STRIPE_KEY=sk_live_xxx
supabase secrets list
supabase secrets unset OLD_KEY
```

Secrets are accessible via `Deno.env.get("NAME")`. `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY` are always available — don't set them manually.

### Never commit secrets
- `.env.local` → gitignored
- `config.toml` → committed
- Use `supabase secrets set` for anything sensitive

## 4. Debugging 101

### Logs
```bash
# Via dashboard (best for real-time debugging)
open "https://supabase.com/dashboard/project/<ref>/functions/<name>/logs"

# Via CLI (older versions don't have logs command)
supabase functions logs <name> --tail   # if supported
```

### Console logs in the function
```typescript
console.log("[tag] info:", data);       // shown as "Log"
console.warn("[tag] warn:", msg);       // shown as "Warning"
console.error("[tag] error:", err);     // shown as "Error" (highlighted)
```

Use structured prefixes (`[auth]`, `[claude]`, `[parse]`) to filter.

### Quick reachability test (no auth)
```bash
curl -i -X POST https://<ref>.supabase.co/functions/v1/<name> \
  -H "Content-Type: application/json" \
  -d '{}'
# Expect: 401 (gateway rejects no-JWT) — confirms the function is deployed
```

### Test with a real user token
Log the JWT from the iOS app or use the `/auth/v1/token` REST endpoint, then:
```bash
curl -i -X POST https://<ref>.supabase.co/functions/v1/<name> \
  -H "Authorization: Bearer <user_jwt>" \
  -H "apikey: <anon_key>" \
  -H "Content-Type: application/json" \
  -d '{"image_base64":"..."}'
```

### Local serve (rare — needs Docker)
```bash
supabase functions serve <name> --env-file .env.local
```

## 5. Common patterns / boilerplate

### Standard handler skeleton

```typescript
// supabase/functions/my-function/index.ts
import { corsHeaders } from "../_shared/cors.ts";
import { requireUser } from "../_shared/auth.ts";

interface RequestBody { foo: string }
interface ResponseBody { bar: string }

Deno.serve(async (req: Request) => {
  // 1. CORS preflight
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  // 2. Method gating
  if (req.method !== "POST") {
    return json({ error: "Method not allowed" }, 405);
  }

  try {
    // 3. Auth
    const { userId } = await requireUser(req);

    // 4. Input validation
    let body: RequestBody;
    try {
      body = await req.json();
    } catch {
      return json({ error: "Invalid JSON body" }, 400);
    }
    if (!body.foo) return json({ error: "Missing 'foo'" }, 400);

    // 5. Business logic
    const result: ResponseBody = { bar: `hello ${body.foo} (user ${userId})` };

    // 6. Success
    return json(result, 200);

  } catch (error) {
    const msg = error instanceof Error ? error.message : String(error);
    if (msg.includes("Authorization") || msg.includes("Invalid or expired token")) {
      return json({ error: msg }, 401);
    }
    console.error("[my-function] error:", msg);
    return json({ error: "Internal server error" }, 500);
  }
});

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}
```

### `_shared/cors.ts`

```typescript
export const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};
```

Restrict `Access-Control-Allow-Origin` if a web client will be added.

### Calling external APIs with retry (e.g. Claude, OpenAI, Stripe)

```typescript
async function callExternal(url: string, body: unknown, apiKey: string): Promise<unknown> {
  const MAX_RETRIES = 3;
  const BASE_DELAY_MS = 2000;

  for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
    if (attempt > 0) {
      const delay = Math.min(BASE_DELAY_MS * 2 ** (attempt - 1), 15_000);
      await new Promise((r) => setTimeout(r, delay));
    }

    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-api-key": apiKey,
      },
      body: JSON.stringify(body),
    });

    // Transient errors — retry
    if ([429, 503, 504].includes(response.status)) continue;

    if (!response.ok) {
      throw new Error(`External API HTTP ${response.status}: ${await response.text()}`);
    }
    return await response.json();
  }
  throw new Error("External API unavailable after retries");
}
```

## 6. Checklist before considering a function "done"

- [ ] `_shared/cors.ts` + `_shared/auth.ts` imported (no duplication)
- [ ] `OPTIONS` preflight handled
- [ ] Non-POST methods return 405 (or restrict as needed)
- [ ] `requireUser(req)` called — JWT validated with `getUser(token)` (token passed EXPLICITLY)
- [ ] JSON body parsed in try/catch (→ 400 on malformed)
- [ ] Field-level validation (→ 400 with specific error)
- [ ] External API calls wrapped in try/catch with retries on transient errors
- [ ] All errors return JSON `{error: "..."}` with proper status code
- [ ] `console.error()` on every catch (for debugging)
- [ ] Secrets set via `supabase secrets set` (never hardcoded, never committed)
- [ ] Deployed with `supabase functions deploy <name>` (default JWT verify)
- [ ] Client DTOs defined `@Serializable internal data class` with `@SerialName("snake_case")`
- [ ] Client uses `supabaseClient.functions.invoke(function, body = dto)` — never manual `setBody`
- [ ] Client maps HTTP errors to user-friendly `DomainError.*` messages
- [ ] Tested reachability with `curl` (should return 401 without JWT — confirms gateway verify)
- [ ] Tested end-to-end from the app with logs open in dashboard

## 7. Quick-reference cheat sheet

| Thing | Where |
|-------|-------|
| Function code | `supabase/functions/<name>/index.ts` |
| Shared helpers | `supabase/functions/_shared/*.ts` |
| Deploy | `supabase functions deploy <name>` |
| Deploy public | `supabase functions deploy <name> --no-verify-jwt` |
| Set secret | `supabase secrets set KEY=value` |
| List secrets | `supabase secrets list` |
| View logs | Dashboard → Functions → `<name>` → Logs |
| Function URL | `https://<project-ref>.supabase.co/functions/v1/<name>` |
| Extract user JWT in function | `supabase.auth.getUser(token)` — **pass token explicitly** |
| KMP body | `@Serializable internal data class XxxDto` |
| KMP call | `supabaseClient.functions.invoke(function, body = dto)` |
| Auto headers from supabase-kt | `apikey` + `Authorization: Bearer <jwt>` (no manual setup) |
