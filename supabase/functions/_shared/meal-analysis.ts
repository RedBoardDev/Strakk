// Meal-analysis prompts and response parsing.
// Shared by `analyze-meal-single` (quick-add) and `extract-meal-draft` (batch).
//
// Contract with the client (KMP shared):
//   AnalyzedEntry  <->  MealEntry (domain) + BreakdownItem[] (when photo → composite)
//
// All nutrients are totals for the estimated portion, NOT per 100g.
// Quantity is a free-text label ("1 portion", "~250g", "2 œufs").

import { callClaude, ClaudeContent, stripMarkdownFences } from "./claude.ts";

// =============================================================================
// Public types — MUST match the Kotlin DTOs in shared/data/dto/
// =============================================================================

export interface BreakdownItem {
  name: string;
  protein_g: number;
  calories_kcal: number;
  fat_g: number | null;
  carbs_g: number | null;
  quantity: string | null;
}

export interface AnalyzedEntry {
  name: string;
  protein_g: number;
  calories_kcal: number;
  fat_g: number | null;
  carbs_g: number | null;
  quantity: string | null;
  breakdown: BreakdownItem[] | null;
}

// =============================================================================
// Prompts
// =============================================================================

const SYSTEM_PROMPT = `You are a precise nutrition-estimation system powering a personal food tracker.

CORE RULES
- Think in TOTAL portion macros. Never emit per-100g values.
- When an input is ambiguous, make your best-effort call and set a conservative quantity.
- Ignore beverages unless they are the only food item.
- Do NOT decompose composite dishes into sub-ingredients (mashed potatoes with herbs = 1 item).
- For protein sources, distinguish meat types via shape, color, texture, skin.
- For leafy greens: name the specific variety when possible.
- Merge hidden cooking fats (oil, butter) into the nearest relevant item's macros. Do NOT emit them separately.

QUANTITY FIELD
- Human-readable, short, French or neutral: "1 portion", "~250g", "2 œufs", "1 bol".
- When unknown, use "1 portion".

BREAKDOWN
- Only populate "breakdown" when an item is visibly composite (multiple distinct foods on the same plate or in the same dish).
- If a single-item input (e.g. "2 œufs au plat") → breakdown = null.
- Each breakdown item carries its own total macros (not per-100g).

OUTPUT
- Respond with ONLY a JSON object matching the schema shown. No preamble, no markdown fences.
- All numeric fields are NUMBERS, never strings.
- Use null for missing fat_g/carbs_g when you have no estimate (don't fabricate zeros).`;

const SINGLE_PHOTO_SCHEMA = `{
  "name": "<short name, 2-5 words, synthesizes the meal>",
  "protein_g": <number>,
  "calories_kcal": <number>,
  "fat_g": <number|null>,
  "carbs_g": <number|null>,
  "quantity": "<string|null>",
  "breakdown": [
    {
      "name": "<item name>",
      "protein_g": <number>,
      "calories_kcal": <number>,
      "fat_g": <number|null>,
      "carbs_g": <number|null>,
      "quantity": "<string|null>"
    }
  ] | null
}`;

const BATCH_SCHEMA = `{
  "items": [
    {
      "id": "<echo the request id>",
      "name": "<short name>",
      "protein_g": <number>,
      "calories_kcal": <number>,
      "fat_g": <number|null>,
      "carbs_g": <number|null>,
      "quantity": "<string|null>",
      "breakdown": [ ... same as single ... ] | null
    }
  ]
}`;

// =============================================================================
// Single-item analyzer — photo OR text
// =============================================================================

export interface SinglePhotoInput {
  type: "photo";
  imageBase64: string;
  hint?: string;
}

export interface SingleTextInput {
  type: "text";
  description: string;
}

export type SingleInput = SinglePhotoInput | SingleTextInput;

export async function analyzeSingle(input: SingleInput): Promise<AnalyzedEntry> {
  const content: ClaudeContent[] = [];

  if (input.type === "photo") {
    content.push({
      type: "image",
      source: {
        type: "base64",
        media_type: "image/jpeg",
        data: input.imageBase64,
      },
    });
    const hint = input.hint?.trim();
    content.push({
      type: "text",
      text: buildSinglePhotoUserMessage(hint),
    });
  } else {
    const desc = input.description.trim();
    if (!desc) throw new Error("Empty text description");
    content.push({
      type: "text",
      text: buildSingleTextUserMessage(desc),
    });
  }

  const raw = await callClaude({
    system: SYSTEM_PROMPT,
    messages: [{ role: "user", content }],
    temperature: 0.1,
    maxTokens: 2048,
  });

  return parseSingleResponse(raw);
}

function buildSinglePhotoUserMessage(hint: string | undefined): string {
  const hintBlock = hint
    ? `\n\nUser hint: "${hint}"\nUse this hint to refine your identification if the image is ambiguous.`
    : "";
  return `Analyze this single meal photo and return one AnalyzedEntry.${hintBlock}\n\nJSON schema:\n${SINGLE_PHOTO_SCHEMA}`;
}

function buildSingleTextUserMessage(description: string): string {
  return `Estimate nutrition for this user-described meal:\n"${description}"\n\nReturn one AnalyzedEntry.\nJSON schema:\n${SINGLE_PHOTO_SCHEMA}`;
}

function parseSingleResponse(raw: string): AnalyzedEntry {
  const stripped = stripMarkdownFences(raw);
  let parsed: unknown;
  try {
    parsed = JSON.parse(stripped);
  } catch {
    throw new Error(`Claude returned non-JSON: ${stripped.slice(0, 200)}`);
  }
  return coerceEntry(parsed);
}

// =============================================================================
// Batch analyzer — used by extract-meal-draft
// =============================================================================

export interface BatchPhotoItem {
  id: string;
  type: "photo";
  imageBase64: string;
  hint?: string;
}

export interface BatchTextItem {
  id: string;
  type: "text";
  description: string;
}

export type BatchItem = BatchPhotoItem | BatchTextItem;

export interface BatchResult {
  id: string;
  entry: AnalyzedEntry;
}

/**
 * Analyze a single batch (≤ 2 photos + unlimited texts) in ONE Claude call.
 * The caller (edge function) is responsible for splitting larger inputs first.
 */
export async function analyzeBatch(items: BatchItem[]): Promise<BatchResult[]> {
  if (items.length === 0) return [];

  const photoCount = items.filter((i) => i.type === "photo").length;
  if (photoCount > 2) {
    throw new Error(
      `analyzeBatch received ${photoCount} photos; caller must split to ≤ 2 per batch`,
    );
  }

  const content: ClaudeContent[] = [];
  const itemIndex: Array<{ id: string; label: string }> = [];

  items.forEach((item, i) => {
    const label = `item_${i + 1}`;
    itemIndex.push({ id: item.id, label });

    if (item.type === "photo") {
      content.push({
        type: "image",
        source: {
          type: "base64",
          media_type: "image/jpeg",
          data: item.imageBase64,
        },
      });
      const hint = item.hint?.trim();
      content.push({
        type: "text",
        text: hint
          ? `(${label} = id "${item.id}", photo above)\nHint: "${hint}"`
          : `(${label} = id "${item.id}", photo above)`,
      });
    } else {
      content.push({
        type: "text",
        text: `(${label} = id "${item.id}", text description)\n"${item.description.trim()}"`,
      });
    }
  });

  content.push({
    type: "text",
    text: buildBatchInstruction(itemIndex),
  });

  const raw = await callClaude({
    system: SYSTEM_PROMPT,
    messages: [{ role: "user", content }],
    temperature: 0.1,
    maxTokens: 4096,
  });

  return parseBatchResponse(raw);
}

function buildBatchInstruction(items: Array<{ id: string; label: string }>): string {
  const lines = items.map((x) => `- ${x.label} → "${x.id}"`).join("\n");
  return `Analyze each labeled input above independently. Return ONE AnalyzedEntry per input.

id mapping:
${lines}

Set "id" in each output to the exact string on the right. Order of "items" must match the input order.

JSON schema:
${BATCH_SCHEMA}`;
}

function parseBatchResponse(raw: string): BatchResult[] {
  const stripped = stripMarkdownFences(raw);
  let parsed: unknown;
  try {
    parsed = JSON.parse(stripped);
  } catch {
    throw new Error(`Claude returned non-JSON: ${stripped.slice(0, 200)}`);
  }

  const obj = parsed as { items?: unknown };
  if (!Array.isArray(obj.items)) {
    throw new Error("Claude response missing 'items' array");
  }

  return obj.items.map((raw: unknown) => {
    const r = raw as { id?: unknown };
    const id = typeof r.id === "string" && r.id.length > 0 ? r.id : "";
    if (!id) throw new Error("Batch item missing 'id'");
    return { id, entry: coerceEntry(raw) };
  });
}

// =============================================================================
// Coercion helpers
// =============================================================================

function coerceEntry(raw: unknown): AnalyzedEntry {
  const r = (raw ?? {}) as Record<string, unknown>;
  return {
    name: coerceString(r.name, "Aliment"),
    protein_g: coerceNumber(r.protein_g),
    calories_kcal: coerceNumber(r.calories_kcal),
    fat_g: coerceNullableNumber(r.fat_g),
    carbs_g: coerceNullableNumber(r.carbs_g),
    quantity: coerceNullableString(r.quantity),
    breakdown: coerceBreakdown(r.breakdown),
  };
}

function coerceBreakdown(raw: unknown): BreakdownItem[] | null {
  if (!Array.isArray(raw) || raw.length === 0) return null;
  return raw.map((item) => {
    const r = (item ?? {}) as Record<string, unknown>;
    return {
      name: coerceString(r.name, "Ingrédient"),
      protein_g: coerceNumber(r.protein_g),
      calories_kcal: coerceNumber(r.calories_kcal),
      fat_g: coerceNullableNumber(r.fat_g),
      carbs_g: coerceNullableNumber(r.carbs_g),
      quantity: coerceNullableString(r.quantity),
    };
  });
}

function coerceNumber(v: unknown, fallback = 0): number {
  if (typeof v === "number" && !Number.isNaN(v)) return Math.max(0, v);
  if (typeof v === "string") {
    const parsed = Number(v);
    if (!Number.isNaN(parsed)) return Math.max(0, parsed);
  }
  return fallback;
}

function coerceNullableNumber(v: unknown): number | null {
  if (v === null || v === undefined) return null;
  if (typeof v === "number" && !Number.isNaN(v)) return Math.max(0, v);
  if (typeof v === "string") {
    const parsed = Number(v);
    if (!Number.isNaN(parsed)) return Math.max(0, parsed);
  }
  return null;
}

function coerceString(v: unknown, fallback: string): string {
  return typeof v === "string" && v.trim().length > 0 ? v.trim() : fallback;
}

function coerceNullableString(v: unknown): string | null {
  if (typeof v === "string") {
    const t = v.trim();
    return t.length > 0 ? t : null;
  }
  return null;
}
