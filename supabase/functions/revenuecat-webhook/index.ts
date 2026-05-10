// Supabase Edge Function: revenuecat-webhook
// =============================================================================
// Handles RevenueCat server-to-server webhook events and updates the
// subscriptions table accordingly.
//
// Security: HMAC-SHA256 signature verification via X-RevenueCat-Signature header.
// No CORS — server-to-server only. No JWT auth — uses HMAC + service_role.
//
// Handled events:
//   INITIAL_PURCHASE  — UPSERT active or trial subscription
//   RENEWAL           — UPDATE current_period_end, set status='active'
//   CANCELLATION      — log only (still active until period end)
//   EXPIRATION        — UPDATE status='expired'
//   BILLING_ISSUE     — UPDATE status='payment_failed'
//   PRODUCT_CHANGE    — UPDATE plan from new product_id
//   SUBSCRIBER_ALIAS  — log only
//   TRANSFER          — log only
//
// Errors:
//   401 — missing/invalid HMAC signature
//   400 — missing required fields or invalid UUID
//   200 — successful processing or unknown event (acknowledge to avoid retry)
//   500 — unexpected server error (RevenueCat will retry)

import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface RevenueCatEvent {
  id?: string;
  type: string;
  app_user_id: string;
  subscriber_id?: string;
  product_id?: string;
  expiration_at_ms?: number;
  period_type?: string;
  event_timestamp_ms?: number;
}

interface RevenueCatWebhookPayload {
  api_version: string;
  event: RevenueCatEvent;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function logEvent(level: "info" | "warn" | "error", message: string, context: Record<string, unknown> = {}) {
  const payload = JSON.stringify({ level, message, ...context });
  if (level === "error") console.error(payload);
  else if (level === "warn") console.warn(payload);
  else console.log(payload);
}

function productToPlan(productId: string): "monthly" | "annual" | null {
  if (productId.includes("monthly")) return "monthly";
  if (productId.includes("annual") || productId.includes("yearly")) return "annual";
  return null;
}

const UUID_REGEX =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function isValidUuid(value: string): boolean {
  return UUID_REGEX.test(value);
}

async function verifySignature(
  rawBody: string,
  signature: string | null,
  secret: string,
): Promise<boolean> {
  if (!signature) return false;
  const normalizedSignature = signature.trim().toLowerCase();

  const encoder = new TextEncoder();
  const keyData = encoder.encode(secret);
  const bodyData = encoder.encode(rawBody);

  const cryptoKey = await crypto.subtle.importKey(
    "raw",
    keyData,
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );

  const signatureBuffer = await crypto.subtle.sign("HMAC", cryptoKey, bodyData);
  const expectedHex = Array.from(new Uint8Array(signatureBuffer))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");

  // Constant-time comparison to prevent timing attacks
  if (expectedHex.length !== normalizedSignature.length) return false;

  let mismatch = 0;
  for (let i = 0; i < expectedHex.length; i++) {
    mismatch |= expectedHex.charCodeAt(i) ^ normalizedSignature.charCodeAt(i);
  }
  return mismatch === 0;
}

function msToTimestamptz(ms: number): string {
  return new Date(ms).toISOString();
}

async function sha256Hex(value: string): Promise<string> {
  const bytes = new TextEncoder().encode(value);
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

// ---------------------------------------------------------------------------
// Event handlers
// ---------------------------------------------------------------------------

type SubscriptionUpdate = {
  status?: string;
  plan?: string | null;
  trial_end?: string | null;
  current_period_end?: string | null;
  revenuecat_customer_id?: string;
  updated_at: string;
};

function buildUpsertForInitialPurchase(
  event: RevenueCatEvent,
  userId: string,
  now: string,
): Record<string, unknown> {
  const isTrialPeriod = event.period_type === "TRIAL";
  const expirationIso = event.expiration_at_ms
    ? msToTimestamptz(event.expiration_at_ms)
    : null;
  const plan = event.product_id ? productToPlan(event.product_id) : null;

  if (isTrialPeriod) {
    return {
      user_id: userId,
      status: "trial",
      plan: null,
      trial_end: expirationIso,
      current_period_end: null,
      revenuecat_customer_id: event.subscriber_id ?? null,
      updated_at: now,
    };
  }

  return {
    user_id: userId,
    status: "active",
    plan,
    trial_end: null,
    current_period_end: expirationIso,
    revenuecat_customer_id: event.subscriber_id ?? null,
    updated_at: now,
  };
}

function buildUpdateForEvent(
  event: RevenueCatEvent,
  now: string,
): SubscriptionUpdate | null {
  switch (event.type) {
    case "RENEWAL": {
      const expirationIso = event.expiration_at_ms
        ? msToTimestamptz(event.expiration_at_ms)
        : null;
      return { status: "active", current_period_end: expirationIso, updated_at: now };
    }
    case "EXPIRATION": {
      return { status: "expired", plan: null, updated_at: now };
    }
    case "BILLING_ISSUE": {
      return { status: "payment_failed", updated_at: now };
    }
    case "PRODUCT_CHANGE": {
      const plan = event.product_id ? productToPlan(event.product_id) : null;
      return { plan, updated_at: now };
    }
    default:
      return null;
  }
}

function getEventId(event: RevenueCatEvent, rawBodyHash: string): string {
  if (event.id && event.id.trim().length > 0) return event.id.trim();
  return `hash:${rawBodyHash}`;
}

function getEventTimestampIso(event: RevenueCatEvent): string {
  if (event.event_timestamp_ms && Number.isFinite(event.event_timestamp_ms)) {
    return msToTimestamptz(event.event_timestamp_ms);
  }
  if (event.expiration_at_ms && Number.isFinite(event.expiration_at_ms)) {
    return msToTimestamptz(event.expiration_at_ms);
  }
  return new Date().toISOString();
}

async function registerEvent(
  supabase: any,
  eventId: string,
  userId: string,
  eventType: string,
  payloadHash: string,
  details: Record<string, unknown>,
): Promise<{ isDuplicate: boolean; ledgerId?: string }> {
  const { data, error } = await supabase
    .from("revenuecat_webhook_events")
    .upsert(
      {
        event_id: eventId,
        app_user_id: userId,
        event_type: eventType,
        payload_hash: payloadHash,
        processing_status: "processed",
        details,
      },
      { onConflict: "event_id", ignoreDuplicates: true },
    )
    .select("id")
    .maybeSingle();

  if (error) {
    throw new Error(`failed to register idempotency event: ${error.message}`);
  }

  const ledgerId = (data as { id?: string } | null)?.id;
  return { isDuplicate: !ledgerId, ledgerId };
}

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

Deno.serve(async (req: Request): Promise<Response> => {
  if (req.method !== "POST") {
    return jsonResponse({ ok: false, error: "Method not allowed" }, 405);
  }

  // 1. Read raw body (needed for HMAC before JSON parse)
  let rawBody: string;
  try {
    rawBody = await req.text();
  } catch {
    return jsonResponse({ ok: false, error: "Failed to read request body" }, 400);
  }

  // 2. Verify HMAC signature
  const secret = Deno.env.get("REVENUECAT_WEBHOOK_SECRET");
  if (!secret) {
    logEvent("error", "REVENUECAT_WEBHOOK_SECRET is not configured");
    return jsonResponse({ ok: false, error: "Webhook secret not configured" }, 500);
  }

  const signature = req.headers.get("X-RevenueCat-Signature");
  const signatureValid = await verifySignature(rawBody, signature, secret);
  if (!signatureValid) {
    logEvent("warn", "Invalid or missing HMAC signature");
    return jsonResponse({ ok: false, error: "Invalid signature" }, 401);
  }

  // 3. Parse JSON body
  let payload: RevenueCatWebhookPayload;
  try {
    payload = JSON.parse(rawBody) as RevenueCatWebhookPayload;
  } catch {
    return jsonResponse({ ok: false, error: "Invalid JSON body" }, 400);
  }

  const event = payload?.event;
  if (!event || typeof event.type !== "string") {
    return jsonResponse({ ok: false, error: "Missing event or event.type" }, 400);
  }

  const rawBodyHash = await sha256Hex(rawBody);

  if (typeof event.app_user_id !== "string" || event.app_user_id.length === 0) {
    return jsonResponse({ ok: false, error: "Missing event.app_user_id" }, 400);
  }

  // 4. Validate user ID
  const userId = event.app_user_id;
  if (!isValidUuid(userId)) {
    logEvent("warn", "Invalid UUID app_user_id", { userId });
    return jsonResponse({ ok: false, error: "Invalid app_user_id — must be a UUID" }, 400);
  }

  const eventId = getEventId(event, rawBodyHash);
  const eventTimestampIso = getEventTimestampIso(event);

  logEvent("info", "Received RevenueCat webhook event", {
    eventId,
    type: event.type,
    userId,
    productId: event.product_id ?? null,
  });

  // 5. Unknown events — acknowledge to avoid blocking RevenueCat's queue
  const HANDLED_EVENTS = [
    "INITIAL_PURCHASE",
    "RENEWAL",
    "EXPIRATION",
    "BILLING_ISSUE",
    "PRODUCT_CHANGE",
    "CANCELLATION",
    "SUBSCRIBER_ALIAS",
    "TRANSFER",
  ];
  if (!HANDLED_EVENTS.includes(event.type)) {
    logEvent("warn", "Unknown RevenueCat event type acknowledged", { eventId, type: event.type, userId });
    return jsonResponse({ ok: true });
  }

  // 6. Database operations
  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceRoleKey) {
    logEvent("error", "Supabase env vars not configured");
    return jsonResponse({ ok: false, error: "Server configuration error" }, 500);
  }

  const supabase = createClient(supabaseUrl, serviceRoleKey);
  const now = new Date().toISOString();

  try {
    const dedupe = await registerEvent(
      supabase,
      eventId,
      userId,
      event.type,
      rawBodyHash,
      {
        eventTimestamp: eventTimestampIso,
        productId: event.product_id ?? null,
      },
    );

    if (dedupe.isDuplicate) {
      logEvent("info", "Duplicate RevenueCat event ignored", { eventId, userId, type: event.type });
      return jsonResponse({ ok: true, deduplicated: true });
    }

    // Log-only events
    if (
      event.type === "CANCELLATION" ||
      event.type === "SUBSCRIBER_ALIAS" ||
      event.type === "TRANSFER"
    ) {
      logEvent("info", "Log-only RevenueCat event", { eventId, userId, type: event.type });
      return jsonResponse({ ok: true });
    }

    // Protect against stale events overwriting newer state
    const { data: currentRow, error: fetchError } = await supabase
      .from("subscriptions")
      .select("updated_at")
      .eq("user_id", userId)
      .maybeSingle();

    if (fetchError) {
      logEvent("error", "Failed to fetch current subscription row", { eventId, userId, error: fetchError.message });
      return jsonResponse({ ok: false, error: "Database error" }, 500);
    }

    if (currentRow?.updated_at && new Date(currentRow.updated_at) > new Date(eventTimestampIso)) {
      logEvent("warn", "Stale RevenueCat event ignored", {
        eventId,
        userId,
        eventType: event.type,
        eventTimestamp: eventTimestampIso,
        currentUpdatedAt: currentRow.updated_at,
      });
      return jsonResponse({ ok: true, staleIgnored: true });
    }

    if (event.type === "INITIAL_PURCHASE") {
      const upsertData = buildUpsertForInitialPurchase(event, userId, now);
      const { error } = await supabase
        .from("subscriptions")
        .upsert(upsertData, { onConflict: "user_id" });

      if (error) {
        logEvent("error", "UPSERT subscription failed", { eventId, userId, error: error.message });
        return jsonResponse({ ok: false, error: "Database error" }, 500);
      }
      const upsertStatus = (upsertData as { status?: string }).status ?? null;
      logEvent("info", "UPSERT subscription success", { eventId, userId, status: upsertStatus });
    } else {
      const updateData = buildUpdateForEvent(event, now);
      if (!updateData) {
        return jsonResponse({ ok: true });
      }

      const { data, error } = await supabase
        .from("subscriptions")
        .update(updateData)
        .eq("user_id", userId)
        .select("id");

      if (error) {
        logEvent("error", "UPDATE subscription failed", { eventId, userId, error: error.message });
        return jsonResponse({ ok: false, error: "Database error" }, 500);
      }

      if (!data || data.length === 0) {
        logEvent("warn", "No subscription row found for non-initial event", {
          eventId,
          userId,
          eventType: event.type,
        });
        return jsonResponse({ ok: true, skipped: true });
      }

      logEvent("info", "UPDATE subscription success", { eventId, userId, eventType: event.type });
    }
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    logEvent("error", "Unexpected webhook error", { eventId, userId, error: message });
    return jsonResponse({ ok: false, error: "Internal server error" }, 500);
  }

  return jsonResponse({ ok: true });
});
