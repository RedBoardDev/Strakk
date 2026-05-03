// Supabase Edge Function: analyze-meal-single
// =============================================================================
// Analyze ONE food item (photo or text) for quick-add flows.
// Called when the user adds a single item outside of a Draft — no batching.
//
// Input (POST JSON) — one of:
//   { "type": "photo", "image_base64": "<jpeg-base64>", "hint": "<optional>" }
//   { "type": "text",  "description":  "<user-typed description>" }
//
// Output (200 JSON): AnalyzedEntry (see _shared/meal-analysis.ts)
//
// Errors:
//   401 — missing/invalid auth
//   400 — missing or malformed input
//   502 — Claude API failure after retries
//   500 — unexpected server error

import { corsHeaders } from "../_shared/cors.ts";
import { requireUser } from "../_shared/auth.ts";
import { checkPayloadSize } from "../_shared/rate-limit.ts";
import { analyzeSingle, SingleInput } from "../_shared/meal-analysis.ts";
import { requireFeatureAccess, recordFeatureUsage } from "../_shared/feature-guard.ts";

const MAX_BODY_BYTES = 5 * 1024 * 1024; // 5 MB (covers base64 photo)

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response(null, { headers: corsHeaders });
  if (req.method !== "POST") return jsonResponse({ error: "Method not allowed" }, 405);

  try {
    if (checkPayloadSize(req, MAX_BODY_BYTES) === -1) {
      return jsonResponse({ error: "Payload too large" }, 413);
    }

    const { userId } = await requireUser(req);

    let body: Record<string, unknown>;
    try {
      body = await req.json();
    } catch {
      return jsonResponse({ error: "Invalid JSON body" }, 400);
    }

    const input = parseInput(body);

    const featureKey = input.type === "photo" ? "ai_photo_analysis" : "ai_text_analysis";
    const gate = await requireFeatureAccess(userId, featureKey);
    if (gate) return gate;

    const entry = await analyzeSingle(input);
    await recordFeatureUsage(userId, featureKey);
    return jsonResponse(entry);
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);

    if (
      message.includes("Authorization") ||
      message.includes("Invalid or expired token") ||
      message.includes("Empty Bearer")
    ) {
      return jsonResponse({ error: message }, 401);
    }

    if (message.startsWith("BAD_INPUT:")) {
      return jsonResponse({ error: message.slice(10).trim() }, 400);
    }

    if (message.includes("Claude API") || message.includes("Claude returned")) {
      return jsonResponse({ error: message }, 502);
    }

    console.error("analyze-meal-single error:", message);
    return jsonResponse({ error: "Internal server error" }, 500);
  }
});

function parseInput(body: Record<string, unknown>): SingleInput {
  const type = body.type;

  if (type === "photo") {
    const image = body.image_base64;
    if (typeof image !== "string" || image.length === 0) {
      throw new Error("BAD_INPUT: Missing or invalid image_base64");
    }
    const hint = body.hint;
    return {
      type: "photo",
      imageBase64: image,
      hint: typeof hint === "string" ? hint : undefined,
    };
  }

  if (type === "text") {
    const desc = body.description;
    if (typeof desc !== "string" || desc.trim().length === 0) {
      throw new Error("BAD_INPUT: Missing or empty description");
    }
    if (desc.length > 500) {
      throw new Error("BAD_INPUT: description too long (max 500 chars)");
    }
    return { type: "text", description: desc };
  }

  throw new Error("BAD_INPUT: type must be 'photo' or 'text'");
}
