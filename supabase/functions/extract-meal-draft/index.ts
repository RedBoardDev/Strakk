// Supabase Edge Function: extract-meal-draft
// =============================================================================
// Batch-analyze all pending items of a Draft at commit time.
//
// Input (POST JSON):
// {
//   "items": [
//     { "id": "<uuid>", "type": "photo", "photo_path": "{userId}/{draftId}/{itemId}.jpg", "hint": "<optional>" },
//     { "id": "<uuid>", "type": "text",  "description": "<user-typed>" }
//     ...
//   ]
// }
//
// Output (200 JSON):
// {
//   "items":    [ { "id": "...", "entry": <AnalyzedEntry> }, ... ],
//   "failures": [ { "id": "...", "reason": "<short message>" }, ... ]
// }
//
// Batching strategy (spec D16, § 8.4):
//   - Max 2 photos per Claude call (quality degrades beyond that empirically).
//   - All texts go into the first batch (in addition to its photos, up to the 2-photo cap).
//   - If a batch fails, per-item fallback: retry each of its items individually.
//
// Errors:
//   401 — auth
//   400 — malformed input
//   207 — partial success (some items failed) — still returns 200 with `failures` populated
//   502 — Claude unavailable everywhere (all items failed)
//   500 — unexpected

import { corsHeaders } from "../_shared/cors.ts";
import { requireUser } from "../_shared/auth.ts";
import {
  analyzeBatch,
  analyzeSingle,
  AnalyzedEntry,
  BatchItem,
  BatchResult,
} from "../_shared/meal-analysis.ts";
import { assertOwnedPath, downloadPhotoAsBase64 } from "../_shared/storage.ts";

const MAX_PHOTOS_PER_BATCH = 2;
const MAX_ITEMS_PER_REQUEST = 16;

interface InputPhoto {
  id: string;
  type: "photo";
  photoPath: string;
  hint?: string;
}
interface InputText {
  id: string;
  type: "text";
  description: string;
}
type InputItem = InputPhoto | InputText;

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
    const { userId } = await requireUser(req);

    let body: Record<string, unknown>;
    try {
      body = await req.json();
    } catch {
      return jsonResponse({ error: "Invalid JSON body" }, 400);
    }

    const items = parseInput(body, userId);
    if (items.length === 0) return jsonResponse({ items: [], failures: [] });

    // 1. Resolve photo payloads (download from Storage).
    const hydrated = await hydratePhotos(items);

    // 2. Split into batches of ≤ MAX_PHOTOS_PER_BATCH photos.
    const batches = splitIntoBatches(hydrated);

    // 3. Run each batch; fall back per-item on failure.
    const successes: BatchResult[] = [];
    const failures: Array<{ id: string; reason: string }> = [];

    for (const batch of batches) {
      try {
        const results = await analyzeBatch(batch);
        successes.push(...results);
      } catch (batchError) {
        const reason = batchError instanceof Error ? batchError.message : String(batchError);
        console.warn(`batch failed (${batch.length} items), retrying per-item: ${reason}`);
        for (const item of batch) {
          try {
            const entry = await runSingleFallback(item);
            successes.push({ id: item.id, entry });
          } catch (itemError) {
            failures.push({
              id: item.id,
              reason: itemError instanceof Error ? itemError.message : String(itemError),
            });
          }
        }
      }
    }

    if (successes.length === 0) {
      return jsonResponse(
        { error: "All items failed", items: [], failures },
        502,
      );
    }

    return jsonResponse({ items: successes, failures });
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

    console.error("extract-meal-draft error:", message);
    return jsonResponse({ error: "Internal server error" }, 500);
  }
});

// =============================================================================
// Input parsing
// =============================================================================

function parseInput(body: Record<string, unknown>, userId: string): InputItem[] {
  const raw = body.items;
  if (!Array.isArray(raw)) throw new Error("BAD_INPUT: items must be an array");
  if (raw.length === 0) return [];
  if (raw.length > MAX_ITEMS_PER_REQUEST) {
    throw new Error(`BAD_INPUT: too many items (max ${MAX_ITEMS_PER_REQUEST})`);
  }

  return raw.map((entry, i) => {
    const r = (entry ?? {}) as Record<string, unknown>;
    const id = typeof r.id === "string" && r.id.length > 0 ? r.id : null;
    if (!id) throw new Error(`BAD_INPUT: items[${i}].id missing`);

    if (r.type === "photo") {
      const photoPath = r.photo_path;
      if (typeof photoPath !== "string" || !photoPath.includes("/")) {
        throw new Error(`BAD_INPUT: items[${i}].photo_path invalid`);
      }
      assertOwnedPath(photoPath, userId);
      return {
        id,
        type: "photo",
        photoPath,
        hint: typeof r.hint === "string" ? r.hint : undefined,
      };
    }

    if (r.type === "text") {
      const desc = r.description;
      if (typeof desc !== "string" || desc.trim().length === 0) {
        throw new Error(`BAD_INPUT: items[${i}].description empty`);
      }
      if (desc.length > 500) {
        throw new Error(`BAD_INPUT: items[${i}].description too long (max 500 chars)`);
      }
      return { id, type: "text", description: desc };
    }

    throw new Error(`BAD_INPUT: items[${i}].type must be 'photo' or 'text'`);
  });
}

// =============================================================================
// Photo hydration (Storage → base64)
// =============================================================================

interface HydratedPhoto {
  id: string;
  type: "photo";
  imageBase64: string;
  hint?: string;
}
interface HydratedText {
  id: string;
  type: "text";
  description: string;
}
type HydratedItem = HydratedPhoto | HydratedText;

async function hydratePhotos(items: InputItem[]): Promise<HydratedItem[]> {
  return Promise.all(
    items.map(async (item): Promise<HydratedItem> => {
      if (item.type === "text") return item;
      const imageBase64 = await downloadPhotoAsBase64(item.photoPath);
      return { id: item.id, type: "photo", imageBase64, hint: item.hint };
    }),
  );
}

// =============================================================================
// Batch splitting — ≤ MAX_PHOTOS_PER_BATCH photos per batch; texts all ride along
// the first batch (where photos don't already saturate) or the final text-only batch.
// =============================================================================

function splitIntoBatches(items: HydratedItem[]): BatchItem[][] {
  const photos = items.filter((i): i is HydratedPhoto => i.type === "photo");
  const texts = items.filter((i): i is HydratedText => i.type === "text");

  const batches: HydratedItem[][] = [];

  for (let i = 0; i < photos.length; i += MAX_PHOTOS_PER_BATCH) {
    batches.push(photos.slice(i, i + MAX_PHOTOS_PER_BATCH));
  }

  if (texts.length > 0) {
    if (batches.length === 0) {
      batches.push([...texts]);
    } else {
      batches[0] = [...batches[0], ...texts];
    }
  }

  return batches.map((b): BatchItem[] =>
    b.map((item) =>
      item.type === "photo"
        ? { id: item.id, type: "photo", imageBase64: item.imageBase64, hint: item.hint }
        : { id: item.id, type: "text", description: item.description },
    ),
  );
}

// =============================================================================
// Per-item fallback
// =============================================================================

async function runSingleFallback(item: BatchItem): Promise<AnalyzedEntry> {
  if (item.type === "photo") {
    return analyzeSingle({ type: "photo", imageBase64: item.imageBase64, hint: item.hint });
  }
  return analyzeSingle({ type: "text", description: item.description });
}
