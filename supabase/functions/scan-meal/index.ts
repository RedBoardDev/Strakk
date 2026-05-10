// Supabase Edge Function: scan-meal
// =============================================================================
// Authenticated proxy to the self-hosted Nutrition API on the VPS.
// Handles auth, feature gating, photo ownership, base64 download, and proxying.
//
// Input (POST JSON):
//   {
//     "photo_paths": string[],   // Storage paths, e.g. "userId/mealId/itemId.jpg" (max 5)
//     "hint": string,            // Optional user-supplied description
//     "is_text_only": boolean    // True = use hint only, no image download
//   }
//
// Output (200 JSON) — forwarded directly from VPS:
//   {
//     "predictions": AiPrediction[],
//     "items": GroundedItem[]
//   }
//
// Errors:
//   401 — missing/invalid auth
//   400 — bad input (BAD_INPUT: prefix)
//   403 — path ownership violation or feature requires Pro
//   413 — payload too large
//   429 — rate limited or quota exceeded
//   502 — Nutrition API failure
//   500 — unexpected server error

import { corsHeaders } from "../_shared/cors.ts";
import { requireUser } from "../_shared/auth.ts";
import { checkPayloadSize } from "../_shared/rate-limit.ts";
import {
  recordFeatureUsage,
  requireFeatureAccess,
} from "../_shared/feature-guard.ts";
import { assertOwnedPath, downloadPhotoAsBase64 } from "../_shared/storage.ts";

const MAX_BODY_BYTES = 5 * 1024 * 1024; // 5 MB
const MAX_PHOTOS = 5;
const FEATURE_KEY = "ai_photo_analysis";

function json(
  body: unknown,
  status = 200,
  extraHeaders: Record<string, string> = {},
): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      ...corsHeaders,
      ...extraHeaders,
      "Content-Type": "application/json",
    },
  });
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);

  try {
    if (checkPayloadSize(req, MAX_BODY_BYTES) === -1) {
      return json({ error: "Payload too large" }, 413);
    }

    const { userId } = await requireUser(req);

    const gate = await requireFeatureAccess(userId, FEATURE_KEY);
    if (gate) return gate;

    let body: Record<string, unknown>;
    try {
      body = await req.json();
    } catch {
      return json({ error: "Invalid JSON body" }, 400);
    }

    // Validate photo_paths
    const rawPaths = body.photo_paths;
    if (!Array.isArray(rawPaths)) {
      return json({ error: "BAD_INPUT: photo_paths must be an array" }, 400);
    }
    if (rawPaths.length > MAX_PHOTOS) {
      return json({
        error: `BAD_INPUT: photo_paths exceeds maximum of ${MAX_PHOTOS}`,
      }, 400);
    }

    const photoPaths = rawPaths as unknown[];
    for (const p of photoPaths) {
      if (typeof p !== "string" || p.trim().length === 0) {
        return json({
          error: "BAD_INPUT: each photo_path must be a non-empty string",
        }, 400);
      }
    }

    const hint = typeof body.hint === "string" ? body.hint.trim() : undefined;
    const isTextOnly = body.is_text_only === true;

    if (isTextOnly && (!hint || hint.length === 0)) {
      return json({
        error: "BAD_INPUT: hint is required when is_text_only is true",
      }, 400);
    }

    if (!isTextOnly && photoPaths.length === 0 && !hint) {
      return json(
        { error: "BAD_INPUT: photo_paths or hint must be provided" },
        400,
      );
    }

    // Validate ownership of each path
    for (const path of photoPaths as string[]) {
      assertOwnedPath(path, userId);
    }

    // Download photos as base64 unless text-only mode
    let images: string[] = [];
    if (!isTextOnly && photoPaths.length > 0) {
      images = await Promise.all(
        (photoPaths as string[]).map((path) => downloadPhotoAsBase64(path)),
      );
    }

    // Proxy to Nutrition API on VPS
    const nutritionApiUrl = Deno.env.get("NUTRITION_API_URL");
    const nutritionApiKey = Deno.env.get("NUTRITION_API_KEY");

    if (!nutritionApiUrl || !nutritionApiKey) {
      console.error(
        "[scan-meal] NUTRITION_API_URL or NUTRITION_API_KEY not configured",
      );
      return json({ error: "Internal server error" }, 500);
    }

    let vpsResponse: Response;
    try {
      // v2 = single-call Gemini estimator, empirically more accurate than the
      // hybrid GPT-5/Qdrant/disambiguation pipeline. is_text_only is unused
      // because v2 always uses the photo(s).
      vpsResponse = await fetch(`${nutritionApiUrl}/api/v1/scan-v2`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "x-api-key": nutritionApiKey,
        },
        body: JSON.stringify({
          images,
          hint,
        }),
      });
      void isTextOnly;
    } catch (fetchError) {
      const msg = fetchError instanceof Error
        ? fetchError.message
        : String(fetchError);
      console.error("[scan-meal] Failed to reach Nutrition API:", msg);
      return json({ error: "Nutrition API unreachable" }, 502);
    }

    if (!vpsResponse.ok) {
      const errorBody = await vpsResponse.text().catch(() => "(unreadable)");
      console.error(
        `[scan-meal] Nutrition API returned ${vpsResponse.status}: ${errorBody}`,
      );

      if (vpsResponse.status === 429) {
        const retryAfter = vpsResponse.headers.get("Retry-After") ?? "3600";
        return json(
          { error: "AI scanner quota reached. Please try again later." },
          429,
          { "Retry-After": retryAfter },
        );
      }

      return json({
        error: "AI scanner is temporarily unavailable. Please try again later.",
      }, 502);
    }

    // Record usage after successful VPS call
    await recordFeatureUsage(userId, FEATURE_KEY);

    // Forward VPS response body to client
    const responseBody = await vpsResponse.json();
    return json(responseBody, 200);
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);

    if (
      message.includes("Authorization") ||
      message.includes("Invalid or expired token") ||
      message.includes("Empty Bearer")
    ) {
      return json({ error: message }, 401);
    }

    if (message.startsWith("BAD_INPUT:")) {
      return json({ error: message.slice(10).trim() }, 400);
    }

    if (message.includes("does not belong to user")) {
      return json({ error: message }, 403);
    }

    if (message.includes("Nutrition API")) {
      return json({ error: message }, 502);
    }

    console.error("[scan-meal] Unexpected error:", message);
    return json({ error: "Internal server error" }, 500);
  }
});
