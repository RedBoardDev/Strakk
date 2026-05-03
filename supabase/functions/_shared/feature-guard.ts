// Unified feature access guard.
// ONE call replaces: requirePro + checkRateLimit + checkMonthlyQuota.
//
// Usage in Edge Functions:
//   const gate = await requireFeatureAccess(userId, "ai_photo_analysis", isPro);
//   if (gate) return gate;
//   // ... do work ...
//   await recordFeatureUsage(userId, "ai_photo_analysis");

import { createClient, SupabaseClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";
import { hasProEntitlement } from "./entitlement.ts";
import { checkRateLimit } from "./rate-limit.ts";
import { corsHeaders } from "./cors.ts";

// =============================================================================
// Types
// =============================================================================

interface FeatureLimits {
  feature_key: string;
  pro_only: boolean;
  quota_free: number;
  quota_pro: number;
  quota_period: string;
  rate_limit_max: number;
  rate_limit_window_s: number;
}

interface FeatureGuardError {
  error: string;
  message: string;
  feature_key?: string;
  used?: number;
  limit?: number;
  period?: string;
}

// =============================================================================
// In-memory cache (per isolate, 5 min TTL)
// =============================================================================

let cachedLimits: Map<string, FeatureLimits> | null = null;
let cacheTimestamp = 0;
const CACHE_TTL_MS = 5 * 60 * 1000;

function getServiceClient(): SupabaseClient {
  const url = Deno.env.get("SUPABASE_URL");
  const key = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !key) throw new Error("Supabase env vars not configured");
  return createClient(url, key);
}

async function getFeatureLimits(featureKey: string): Promise<FeatureLimits | null> {
  const now = Date.now();

  if (cachedLimits && (now - cacheTimestamp) < CACHE_TTL_MS) {
    return cachedLimits.get(featureKey) ?? null;
  }

  // Refresh entire cache
  const supabase = getServiceClient();
  const { data, error } = await supabase
    .from("feature_limits")
    .select("*");

  if (error) {
    console.error("[feature-guard] Failed to load feature_limits:", error.message);
    // Degrade: clear cache, return null (fail open for config load)
    cachedLimits = null;
    return null;
  }

  cachedLimits = new Map();
  for (const row of (data ?? []) as FeatureLimits[]) {
    cachedLimits.set(row.feature_key, row);
  }
  cacheTimestamp = now;

  return cachedLimits.get(featureKey) ?? null;
}

// =============================================================================
// Period helpers
// =============================================================================

function periodStart(period: string): string {
  const now = new Date();
  const y = now.getUTCFullYear();
  const m = now.getUTCMonth();
  const d = now.getUTCDate();
  const dow = now.getUTCDay();

  switch (period) {
    case "day":
      return `${y}-${String(m + 1).padStart(2, "0")}-${String(d).padStart(2, "0")}T00:00:00Z`;
    case "week": {
      // Monday-based week
      const mondayOffset = dow === 0 ? 6 : dow - 1;
      const monday = new Date(Date.UTC(y, m, d - mondayOffset));
      return monday.toISOString().replace(/T.*/, "T00:00:00Z");
    }
    case "month":
    default:
      return `${y}-${String(m + 1).padStart(2, "0")}-01T00:00:00Z`;
  }
}

// =============================================================================
// Usage counting (from feature_usage table)
// =============================================================================

async function countUsage(userId: string, featureKey: string, since: string): Promise<number> {
  const supabase = getServiceClient();
  const { count, error } = await supabase
    .from("feature_usage")
    .select("*", { count: "exact", head: true })
    .eq("user_id", userId)
    .eq("feature_key", featureKey)
    .gte("created_at", since);

  if (error) {
    console.error(`[feature-guard] Failed to count usage for ${featureKey}:`, error.message);
    return 0; // Fail open on count errors
  }

  return count ?? 0;
}

// =============================================================================
// Public API
// =============================================================================

function jsonError(body: FeatureGuardError, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

/**
 * Unified feature access check. Returns a Response if access is denied, or null if allowed.
 *
 * Checks in order:
 * 1. PRO gate (if feature is pro_only and user is not PRO)
 * 2. Rate limit (via Deno KV sliding window)
 * 3. Quota (from feature_usage table)
 *
 * If isPro is not provided, it will be fetched from the subscriptions table.
 */
export async function requireFeatureAccess(
  userId: string,
  featureKey: string,
  isPro?: boolean,
): Promise<Response | null> {
  const limits = await getFeatureLimits(featureKey);

  if (!limits) {
    // Unknown feature or config load failure — fail open
    console.warn(`[feature-guard] No limits found for feature: ${featureKey}, allowing access`);
    return null;
  }

  // 1. PRO gate
  const userIsPro = isPro ?? await hasProEntitlement(userId);

  if (limits.pro_only && !userIsPro) {
    return jsonError({
      error: "pro_required",
      message: "This feature requires Strakk Pro.",
      feature_key: featureKey,
    }, 403);
  }

  // 2. Rate limit
  if (limits.rate_limit_max > 0 && limits.rate_limit_window_s > 0) {
    const allowed = await checkRateLimit(
      userId,
      featureKey,
      limits.rate_limit_max,
      limits.rate_limit_window_s,
    );
    if (!allowed) {
      return jsonError({
        error: "rate_limited",
        message: "Too many requests. Please wait.",
        feature_key: featureKey,
      }, 429);
    }
  }

  // 3. Quota
  const quota = userIsPro ? limits.quota_pro : limits.quota_free;

  if (quota === -1) {
    // Unlimited
    return null;
  }

  if (quota === 0) {
    // No quota at all for this tier
    return jsonError({
      error: userIsPro ? "quota_exceeded" : "pro_required",
      message: userIsPro
        ? `Quota exhausted for ${featureKey}.`
        : "This feature requires Strakk Pro.",
      feature_key: featureKey,
      used: 0,
      limit: 0,
      period: limits.quota_period,
    }, userIsPro ? 429 : 403);
  }

  const since = periodStart(limits.quota_period);
  const used = await countUsage(userId, featureKey, since);

  if (used >= quota) {
    return jsonError({
      error: "quota_exceeded",
      message: `Monthly limit reached (${used}/${quota}).`,
      feature_key: featureKey,
      used,
      limit: quota,
      period: limits.quota_period,
    }, 429);
  }

  return null;
}

/**
 * Record a feature usage after the action succeeds.
 * Call this AFTER the actual work is done (not before).
 */
export async function recordFeatureUsage(
  userId: string,
  featureKey: string,
): Promise<void> {
  try {
    const supabase = getServiceClient();
    const { error } = await supabase
      .from("feature_usage")
      .insert({ user_id: userId, feature_key: featureKey });

    if (error) {
      console.error(`[feature-guard] Failed to record usage for ${featureKey}:`, error.message);
      // Non-blocking: usage recording failure should not fail the request
    }
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err);
    console.error(`[feature-guard] Unexpected error recording usage for ${featureKey}:`, msg);
  }
}
