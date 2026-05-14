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
// Supabase Edge Function: analyze-meal-single
// =============================================================================
// Analyze ONE food item (photo or text) for quick-add flows.
//
// - type "photo" → Claude (as before)
// - type "text"  → proxied to self-hosted Nutrition API → Gemini 2.5 Pro
//
// Output (200 JSON): AnalyzedEntry (see _shared/meal-analysis.ts)
import { corsHeaders } from "../_shared/cors.ts";
import { requireUser } from "../_shared/auth.ts";
import { analyzeSingle } from "../_shared/meal-analysis.ts";

function jsonResponse(
  body: unknown,
  status = 200,
  extraHeaders: Record<string, string> = {},
): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, ...extraHeaders, "Content-Type": "application/json" },
  });
}

// ---------------------------------------------------------------------------
// Text analysis: proxy to VPS /api/v1/analyze-text (Gemini)
// ---------------------------------------------------------------------------
async function analyzeTextViaVps(description: string): Promise<unknown> {
  const nutritionApiUrl = Deno.env.get("NUTRITION_API_URL");
  const nutritionApiKey = Deno.env.get("NUTRITION_API_KEY");

  if (!nutritionApiUrl || !nutritionApiKey) {
    console.error("[analyze-meal-single] NUTRITION_API_URL or NUTRITION_API_KEY not configured");
    throw new Error("Internal server error");
  }

  const response = await fetch(`${nutritionApiUrl}/api/v1/analyze-text`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-api-key": nutritionApiKey,
    },
    body: JSON.stringify({ description }),
  });

  if (!response.ok) {
    const errorBody = await response.text().catch(() => "(unreadable)");
    console.error(
      `[analyze-meal-single] VPS returned ${response.status}: ${errorBody}`,
    );

    if (response.status === 429) {
      throw new Error("AI analysis quota reached. Please try again later.");
    }
    throw new Error("Text analysis temporarily unavailable");
  }

  return await response.json();
}

// ---------------------------------------------------------------------------
// Main handler
// ---------------------------------------------------------------------------
Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }
  if (req.method !== "POST") {
    return jsonResponse({ error: "Method not allowed" }, 405);
  }

  try {
    await requireUser(req);

    let body: Record<string, unknown>;
    try {
      body = await req.json();
    } catch {
      return jsonResponse({ error: "Invalid JSON body" }, 400);
    }

    const input = parseInput(body);

    // Text → Gemini via VPS
    if (input.type === "text") {
      console.log("[analyze-meal-single] text mode → proxying to VPS");
      const entry = await analyzeTextViaVps(input.description);
      console.log("[analyze-meal-single] VPS response OK");
      return jsonResponse(entry);
    }

    // Photo → Claude (existing path)
    const entry = await analyzeSingle(input);
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
    if (
      message.includes("Claude API") ||
      message.includes("Claude returned") ||
      message.includes("Gemini")
    ) {
      return jsonResponse({ error: message }, 502);
    }

    console.error("analyze-meal-single error:", message);
    return jsonResponse({ error: "Internal server error" }, 500);
  }
});

// ---------------------------------------------------------------------------
// Input parsing
// ---------------------------------------------------------------------------
interface PhotoInput {
  type: "photo";
  imageBase64: string;
  hint?: string;
}
interface TextInput {
  type: "text";
  description: string;
}
type ParsedInput = PhotoInput | TextInput;

function parseInput(body: Record<string, unknown>): ParsedInput {
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
    if (typeof desc !== "string" || (desc as string).trim().length === 0) {
      throw new Error("BAD_INPUT: Missing or empty description");
    }
    if ((desc as string).length > 500) {
      throw new Error("BAD_INPUT: description too long (max 500 chars)");
    }
    return { type: "text", description: desc as string };
  }

  throw new Error("BAD_INPUT: type must be 'photo' or 'text'");
}
