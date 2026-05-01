// search-off-live — proxy + cache for Open Food Facts.
//
// Authenticated users post a query, we hit OFF (search-a-licious with v1
// fallback), filter aberrations, upsert hits into food_catalog with the
// service role, and return the rows shaped for the client DTO.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";
import { corsHeaders } from "../_shared/cors.ts";
import { requireUser } from "../_shared/auth.ts";
import { checkRateLimit, checkPayloadSize } from "../_shared/rate-limit.ts";

const OFF_SEARCH_URL = "https://search.openfoodfacts.org/search";
const OFF_LEGACY_URL = "https://world.openfoodfacts.org/cgi/search.pl";
const USER_AGENT = "Strakk/1.0 (https://github.com/RedBoardDev/Strakk)";

const MAX_LIMIT = 20;
const MAX_KCAL = 900;

// Deno KV cache TTL for OFF query results. OFF rate-limits at 10 req/s per IP
// and the data changes rarely. 1 hour means a single popular search ("kinder",
// "danone") hits OFF once, then is served from KV across users until expiry.
const OFF_CACHE_TTL_MS = 60 * 60 * 1000; // 1 hour
const OFF_CACHE_VERSION = "v1";          // bump to invalidate every key at once

// Deno KV instance — opened lazily, shared across requests in the same isolate.
let kvCache: Deno.Kv | null = null;
async function getKv(): Promise<Deno.Kv | null> {
  if (kvCache) return kvCache;
  try {
    kvCache = await Deno.openKv();
    return kvCache;
  } catch (e) {
    console.warn("[search-off-live] Deno.openKv failed (cache disabled):", (e as Error).message);
    return null;
  }
}

function normalizeCacheQuery(q: string): string {
  return q.toLowerCase().normalize("NFKD").replace(/\p{M}/gu, "").replace(/\s+/g, " ").trim();
}

interface OffProduct {
  code?: string | number;
  product_name?: string;
  product_name_fr?: string;
  // search-a-licious returns array, legacy v1 returns comma-separated string.
  brands?: string | string[];
  nutriments?: Record<string, number | string | undefined>;
  serving_size?: string;
  serving_quantity?: number | string;
  nutriscore_grade?: string;
  nova_group?: number;
  image_front_url?: string;
  image_url?: string;
  popularity_key?: number;
  countries_tags?: string[];
}

function pickFirstBrand(brands: string | string[] | undefined): string | null {
  if (!brands) return null;
  if (Array.isArray(brands)) {
    const first = brands.find((b) => typeof b === "string" && b.trim().length > 0);
    return first ? first.trim() : null;
  }
  if (typeof brands !== "string") return null;
  const first = brands.split(",")[0]?.trim();
  return first && first.length > 0 ? first : null;
}

function asString(value: unknown): string {
  if (value === undefined || value === null) return "";
  if (typeof value === "string") return value;
  if (typeof value === "number" || typeof value === "boolean") return String(value);
  if (Array.isArray(value)) return value.filter((v) => typeof v === "string").join(", ");
  return "";
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

function normalize(s: string): string {
  return s
    .toLowerCase()
    .normalize("NFKD")
    .replace(/\p{M}/gu, "")
    .replace(/\s+/g, " ")
    .trim();
}

function pickNumber(value: unknown): number | null {
  if (value === undefined || value === null || value === "") return null;
  const n = typeof value === "number" ? value : parseFloat(String(value).replace(",", "."));
  return Number.isFinite(n) ? n : null;
}

interface OffSearchResult {
  hits: OffProduct[];
  /** true only when at least one API responded with a successful HTTP status */
  apiOk: boolean;
}

async function searchOff(query: string, limit: number): Promise<OffSearchResult> {
  const url = new URL(OFF_SEARCH_URL);
  url.searchParams.set("q", query);
  url.searchParams.set("countries_tags", "en:france");
  url.searchParams.set("page_size", String(Math.min(limit * 2, 40)));
  url.searchParams.set("fields", [
    "code", "product_name", "product_name_fr", "brands", "nutriments",
    "serving_size", "serving_quantity", "nutriscore_grade", "nova_group",
    "image_front_url", "image_url", "popularity_key", "countries_tags",
  ].join(","));

  try {
    const res = await fetch(url, { headers: { "User-Agent": USER_AGENT } });
    if (res.ok) {
      const json = await res.json();
      const hits = (json?.hits ?? []) as OffProduct[];
      console.log(`[search-off-live] search-a-licious returned ${hits.length} hits`);
      return { hits, apiOk: true };
    }
    console.warn(`[search-off-live] search-a-licious HTTP ${res.status}`);
  } catch (e) {
    console.warn("[search-off-live] search-a-licious failed:", (e as Error).message);
  }

  // Legacy v1 fallback
  const legacy = new URL(OFF_LEGACY_URL);
  legacy.searchParams.set("search_terms", query);
  legacy.searchParams.set("tagtype_0", "countries");
  legacy.searchParams.set("tag_contains_0", "contains");
  legacy.searchParams.set("tag_0", "france");
  legacy.searchParams.set("json", "1");
  legacy.searchParams.set("page_size", String(Math.min(limit * 2, 40)));
  try {
    const res = await fetch(legacy, { headers: { "User-Agent": USER_AGENT } });
    if (!res.ok) {
      console.warn(`[search-off-live] legacy HTTP ${res.status}`);
      // Both arms failed with non-2xx — do not cache.
      return { hits: [], apiOk: false };
    }
    const json = await res.json();
    const hits = (json?.products ?? []) as OffProduct[];
    console.log(`[search-off-live] legacy v1 returned ${hits.length} hits`);
    return { hits, apiOk: true };
  } catch (e) {
    console.error("[search-off-live] legacy fallback failed:", (e as Error).message);
    // Network error — do not cache.
    return { hits: [], apiOk: false };
  }
}

interface MappedRow {
  ext_id: string;
  name: string;
  name_normalized: string;
  brand: string | null;
  brand_normalized: string | null;
  protein: number;
  calories: number;
  fat: number | null;
  carbs: number | null;
  sugar_100g: number | null;
  fiber_100g: number | null;
  salt_100g: number | null;
  default_portion_grams: number;
  serving_label: string | null;
  nutriscore: string | null;
  nova_group: number | null;
  barcode: string;
  image_url: string | null;
  popularity: number;
  source: "off_live";
  source_version: string;
  is_active: boolean;
}

function mapToCatalog(p: OffProduct): MappedRow | null {
  const code = asString(p.code).trim();
  if (!code) return null;
  const name = (asString(p.product_name_fr) || asString(p.product_name)).trim();
  if (!name) return null;

  const nut = p.nutriments ?? {};
  const protein = pickNumber(nut["proteins_100g"]);
  const calories = pickNumber(nut["energy-kcal_100g"]) ?? pickNumber(nut["energy_100g"]);
  const fat = pickNumber(nut["fat_100g"]);
  const carbs = pickNumber(nut["carbohydrates_100g"]);

  if (calories === null || protein === null || fat === null || carbs === null) return null;
  if (calories <= 0 || calories > MAX_KCAL) return null;
  if ((protein + fat + carbs) > 105) return null;

  const brand = pickFirstBrand(p.brands);
  const servingQty = pickNumber(p.serving_quantity);
  const nutriscore = asString(p.nutriscore_grade).trim().toLowerCase();
  const nutriscoreClean = nutriscore.length === 1 && /[a-e]/.test(nutriscore) ? nutriscore : null;
  const nova = typeof p.nova_group === "number" && p.nova_group >= 1 && p.nova_group <= 4
    ? p.nova_group
    : null;

  const image = asString(p.image_front_url) || asString(p.image_url) || null;
  const popularity = typeof p.popularity_key === "number"
    ? Math.max(0, Math.min(1000, Math.round(p.popularity_key / 100)))
    : 600;

  return {
    ext_id: code,
    name: name.slice(0, 240),
    name_normalized: normalize(name).slice(0, 240),
    brand,
    brand_normalized: brand ? normalize(brand) : null,
    protein,
    calories,
    fat,
    carbs,
    sugar_100g: pickNumber(nut["sugars_100g"]),
    fiber_100g: pickNumber(nut["fiber_100g"]),
    salt_100g: pickNumber(nut["salt_100g"]),
    default_portion_grams: servingQty && servingQty > 0 && servingQty <= 1000 ? servingQty : 100,
    serving_label: asString(p.serving_size).trim() || null,
    nutriscore: nutriscoreClean,
    nova_group: nova,
    barcode: code,
    image_url: image,
    popularity,
    source: "off_live",
    source_version: "off_live",
    is_active: true,
  };
}

async function handle(req: Request): Promise<Response> {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return jsonResponse({ error: "Method not allowed" }, 405);
  }

  if (checkPayloadSize(req, 64 * 1024) === -1) {
    return jsonResponse({ error: "Payload too large" }, 413);
  }

  let user: { userId: string };
  try {
    user = await requireUser(req);
  } catch (e) {
    return jsonResponse({ error: (e as Error).message }, 401);
  }

  if (!(await checkRateLimit(user.userId, "search-off-live", 30, 60))) {
    return jsonResponse({ error: "Rate limit exceeded" }, 429);
  }

  let body: { q?: string; limit?: number };
  try {
    body = await req.json();
  } catch {
    return jsonResponse({ error: "Invalid JSON body" }, 400);
  }

  const q = (body.q ?? "").trim();
  const limit = Math.max(1, Math.min(MAX_LIMIT, body.limit ?? 20));
  if (q.length < 2) return jsonResponse({ items: [] });
  if (q.length > 200) return jsonResponse({ error: "Query too long" }, 400);

  console.log(`[search-off-live] q="${q}" limit=${limit}`);

  // ── KV cache lookup ────────────────────────────────────────────────────
  // Cache key includes the limit and a version tag so changing the response
  // shape later just requires bumping OFF_CACHE_VERSION. Hits skip OFF and
  // skip the upsert entirely — the corresponding food_catalog rows already
  // exist from the request that originally populated this cache entry.
  const cacheKey = ["off-search", OFF_CACHE_VERSION, normalizeCacheQuery(q), limit];
  const kv = await getKv();
  if (kv) {
    const cached = await kv.get<{ items: unknown[] }>(cacheKey);
    if (cached.value && Array.isArray(cached.value.items)) {
      console.log(`[search-off-live] cache HIT (${cached.value.items.length} items)`);
      return jsonResponse({ items: cached.value.items });
    }
  }

  const offResult = await searchOff(q, limit);
  if (offResult.hits.length === 0) {
    if (offResult.apiOk) {
      // OFF responded successfully but found no matches — safe to cache so we
      // don't hammer OFF for misspelled words. Same TTL as positive results.
      if (kv) await kv.set(cacheKey, { items: [] }, { expireIn: OFF_CACHE_TTL_MS });
    } else {
      // Transient API error — do not cache; the next request should retry OFF.
      console.warn("[search-off-live] skipping cache write: OFF API error");
    }
    return jsonResponse({ items: [] });
  }

  const mapped: MappedRow[] = [];
  const seen = new Set<string>();
  for (const p of offResult.hits) {
    const m = mapToCatalog(p);
    if (!m) continue;
    if (seen.has(m.ext_id)) continue;
    seen.add(m.ext_id);
    mapped.push(m);
    if (mapped.length >= limit) break;
  }
  console.log(`[search-off-live] mapped ${mapped.length} rows`);
  if (mapped.length === 0) return jsonResponse({ items: [] });

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceKey) {
    console.error("[search-off-live] missing env: url?", !!supabaseUrl, "serviceKey?", !!serviceKey);
    return jsonResponse({ error: "server misconfiguration: missing env vars" }, 500);
  }
  const admin = createClient(supabaseUrl, serviceKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const rowsToUpsert = mapped.map((m) => ({
    ...m,
    last_synced_at: new Date().toISOString(),
  }));

  let upsertRes;
  try {
    upsertRes = await admin
      .from("food_catalog")
      .upsert(rowsToUpsert, { onConflict: "source,ext_id", ignoreDuplicates: false })
      .select(
        "id,source,name,brand,protein,calories,fat,carbs," +
        "default_portion_grams,serving_label,nutriscore,nova_group,barcode,image_url,popularity",
      );
  } catch (e) {
    console.error("[search-off-live] upsert threw:", (e as Error).message, (e as Error).stack);
    return jsonResponse({ error: `upsert threw: ${(e as Error).message}` }, 500);
  }

  if (upsertRes.error) {
    console.error("[search-off-live] upsert error:", JSON.stringify(upsertRes.error));
    return jsonResponse(
      {
        error: `upsert failed: ${upsertRes.error.message}`,
        details: upsertRes.error.details ?? null,
        hint: upsertRes.error.hint ?? null,
        code: upsertRes.error.code ?? null,
      },
      500,
    );
  }

  console.log(`[search-off-live] upserted ${upsertRes.data?.length ?? 0} rows`);

  const items = (upsertRes.data ?? []).map((r) => ({
    id: r.id,
    source: r.source,
    name: r.name,
    brand: r.brand,
    protein: r.protein,
    calories: r.calories,
    fat: r.fat,
    carbs: r.carbs,
    default_portion_grams: r.default_portion_grams,
    serving_label: r.serving_label,
    nutriscore: r.nutriscore,
    nova_group: r.nova_group,
    barcode: r.barcode,
    image_url: r.image_url,
    rank: r.popularity / 1000,
  }));

  if (kv) {
    // Fire-and-forget; cache failures must never break the response.
    kv.set(cacheKey, { items }, { expireIn: OFF_CACHE_TTL_MS })
      .catch((e) => console.warn("[search-off-live] kv.set failed:", e?.message));
  }

  return jsonResponse({ items });
}

Deno.serve(async (req: Request) => {
  try {
    return await handle(req);
  } catch (e) {
    const err = e as Error;
    console.error("[search-off-live] UNCAUGHT:", err.message, err.stack);
    return jsonResponse({ error: `uncaught: ${err.message}` }, 500);
  }
});
