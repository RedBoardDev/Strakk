/**
 * Gemini 3 Flash single-call meal estimator.
 *
 * Bypasses the embedding/Qdrant/disambiguation/cooking-adjustment pipeline
 * entirely: feeds the photo to Gemini and asks for items + portions + macros
 * in one shot. Used as a v2 baseline to A/B against the current hybrid pipeline.
 *
 * Reference: empirical research (BioLayne 2025) shows VLMs achieve ~40% error
 * on direct macro estimation; we want to verify this matches our experience
 * before committing to a flow change.
 */

const GEMINI_MODEL = "gemini-3-flash-preview";
const GEMINI_ENDPOINT =
  `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent`;

const MAX_RETRIES = 2;
const BASE_DELAY_MS = 2000;
const RETRYABLE_STATUSES = new Set([429, 500, 503]);

/**
 * Minimal prompt: no hardcoded reference objects, no portion anchors, no
 * "look for" lists. Just clear task description + structured output schema.
 * Lets the model use its full knowledge without our biases.
 */
// Backtest-optimized prompt (auto-r2-20260508-115245).
// MAPE on 244 HelloFresh recipes: 30.93% (vs 32.84% baseline, -1.91pt absolute).
// Key improvement: explicit cooked-weight density for starches, which were the
// dominant carb over-estimation source.
const TEXT_SYSTEM_PROMPT =
  `You are a nutrition analyst. Given a user-typed meal description, identify every distinct food component and return their macros for the estimated portion.

For each item return:
- name: English, with cooking method and descriptors that affect nutrient density (e.g. "cooked", "pan-seared", "creamy sauce")
- amount: grams (ml for liquids)
- kcal, protein_g, fat_g, carbs_g: for that exact portion

**CRITICAL — cooked starches (pasta, rice, grains):**
Cooked weight is ~3× dry weight. Use cooked-weight nutrient density: ~130 kcal, ~4g protein, ~1g fat, ~27g carbs per 100g cooked. Do NOT use dry-weight values on a cooked portion — this is the single most common catastrophic error.

**Sauces, oils, and cheese carry most of the fat.**
Cream sauces, pesto, and oil-based dressings are calorie-dense. Estimate their fat contribution explicitly and realistically — do not treat them as negligible.

**Portion sizes: default to the smaller plausible estimate.**
Standard home-cooking portions are the baseline. Overestimation is the most common error.

**Other rules:**
- Use home-cooking nutrient profiles by default unless preparation is clearly otherwise.
- Use French food culture defaults when ambiguous (baguette not sliced bread, fromage blanc not yogurt).
- Do not list cooking oil, salt, pepper, or dry spices as separate items unless explicitly mentioned.
- Do not double-count; merge duplicates.
- For composite descriptions ("steak frites", "pasta carbonara"), break down into sub-items.
- For simple descriptions ("2 eggs", "an apple"), return a single item.

Reason through starches and sauces carefully before finalizing.`;

const SYSTEM_PROMPT =
  `You are a nutrition analyst. Given one meal photo, identify every distinct food component (including sauces, cheese, garnishes) and return their macros for the exact visible portion.

For each item return:
- name: English, with cooking method and descriptors that affect nutrient density (e.g. "cooked", "pan-seared", "creamy sauce")
- amount: grams (ml for liquids)
- kcal, protein_g, fat_g, carbs_g: for that exact portion

**CRITICAL — cooked starches (pasta, rice, grains):**
Cooked weight is ~3× dry weight. Use cooked-weight nutrient density: ~130 kcal, ~4g protein, ~1g fat, ~27g carbs per 100g cooked. Do NOT use dry-weight values on a cooked portion — this is the single most common catastrophic error.

**Sauces, oils, and cheese carry most of the fat.**
Cream sauces, pesto, and oil-based dressings are calorie-dense. Estimate their fat contribution explicitly and realistically — do not treat them as negligible.

**Portion sizes: default to the smaller plausible estimate.**
Plated portions are typically smaller than they appear. Overestimation is the most common error.

**Other rules:**
- Use home-cooking nutrient profiles by default unless preparation is clearly otherwise.
- Do not list cooking oil, salt, pepper, or dry spices as separate items.
- Do not double-count; merge duplicates.

Reason through starches and sauces carefully before finalizing.`;

const RESPONSE_SCHEMA = {
  type: "object",
  properties: {
    items: {
      type: "array",
      items: {
        type: "object",
        properties: {
          name: { type: "string" },
          unit: { type: "string", enum: ["g", "ml"] },
          amount: { type: "number" },
          kcal: { type: "number" },
          protein_g: { type: "number" },
          fat_g: { type: "number" },
          carbs_g: { type: "number" },
        },
        required: [
          "name",
          "unit",
          "amount",
          "kcal",
          "protein_g",
          "fat_g",
          "carbs_g",
        ],
      },
    },
  },
  required: ["items"],
};

export interface EstimatedItem {
  name: string;
  unit: string;
  amount: number;
  kcal: number;
  protein: number;
  fat: number;
  carbs: number;
}

export interface MealEstimate {
  items: EstimatedItem[];
}

interface RawItem {
  name: string;
  unit: string;
  amount: number;
  kcal: number;
  protein_g: number;
  fat_g: number;
  carbs_g: number;
}

interface GeminiConfig {
  apiKey: string;
}

export class GeminiHttpError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = "GeminiHttpError";
  }
}

export class GeminiMealEstimatorAdapter {
  private readonly apiKey: string;

  constructor(config: GeminiConfig) {
    this.apiKey = config.apiKey;
  }

  async estimate(images: string[], hint?: string): Promise<MealEstimate> {
    if (images.length === 0) {
      throw new Error("BAD_INPUT: at least one image is required");
    }

    const parts: Array<Record<string, unknown>> = [];

    if (hint && hint.trim().length > 0) {
      parts.push({ text: `User hint: ${hint.trim()}` });
    }

    for (const base64 of images) {
      parts.push({
        inline_data: {
          mime_type: "image/jpeg",
          data: base64,
        },
      });
    }

    const body = {
      systemInstruction: {
        parts: [{ text: SYSTEM_PROMPT }],
      },
      contents: [{ role: "user", parts }],
      generationConfig: {
        temperature: 0.2,
        responseMimeType: "application/json",
        responseSchema: RESPONSE_SCHEMA,
      },
    };

    for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
      if (attempt > 0) {
        await new Promise((r) => setTimeout(r, BASE_DELAY_MS * attempt));
      }

      let response: Response;
      try {
        response = await fetch(`${GEMINI_ENDPOINT}?key=${this.apiKey}`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        });
      } catch (err) {
        if (attempt === MAX_RETRIES) {
          throw new Error(
            `Gemini network error: ${
              err instanceof Error ? err.message : String(err)
            }`,
          );
        }
        continue;
      }

      if (RETRYABLE_STATUSES.has(response.status)) {
        if (attempt === MAX_RETRIES) {
          throw new GeminiHttpError(
            response.status,
            `Gemini unavailable (HTTP ${response.status})`,
          );
        }
        continue;
      }

      if (!response.ok) {
        const text = await response.text();
        throw new GeminiHttpError(
          response.status,
          `Gemini HTTP ${response.status}: ${text.slice(0, 500)}`,
        );
      }

      const json = await response.json();
      const text = json.candidates?.[0]?.content?.parts?.[0]?.text;
      if (typeof text !== "string") {
        throw new Error(
          `Gemini returned no text content. finish_reason=${
            json.candidates?.[0]?.finishReason
          }`,
        );
      }

      let parsed: { items: RawItem[] };
      try {
        parsed = JSON.parse(text);
      } catch (err) {
        throw new Error(
          `Gemini returned invalid JSON: ${
            err instanceof Error ? err.message : err
          }`,
        );
      }

      const items: EstimatedItem[] = (parsed.items ?? []).map((it) => ({
        name: it.name,
        unit: it.unit,
        amount: it.amount,
        kcal: it.kcal,
        protein: it.protein_g,
        fat: it.fat_g,
        carbs: it.carbs_g,
      }));

      return { items };
    }

    throw new Error("Gemini unavailable after retries");
  }

  async analyzeText(description: string): Promise<MealEstimate> {
    const trimmed = description.trim();
    if (!trimmed) {
      throw new Error("BAD_INPUT: description must not be empty");
    }

    const body = {
      systemInstruction: {
        parts: [{ text: TEXT_SYSTEM_PROMPT }],
      },
      contents: [{
        role: "user",
        parts: [{ text: `Estimate nutrition for this meal:\n"${trimmed}"` }],
      }],
      generationConfig: {
        temperature: 0.2,
        responseMimeType: "application/json",
        responseSchema: RESPONSE_SCHEMA,
      },
    };

    for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
      if (attempt > 0) {
        await new Promise((r) => setTimeout(r, BASE_DELAY_MS * attempt));
      }

      let response: Response;
      try {
        response = await fetch(`${GEMINI_ENDPOINT}?key=${this.apiKey}`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        });
      } catch (err) {
        if (attempt === MAX_RETRIES) {
          throw new Error(
            `Gemini network error: ${
              err instanceof Error ? err.message : String(err)
            }`,
          );
        }
        continue;
      }

      if (RETRYABLE_STATUSES.has(response.status)) {
        if (attempt === MAX_RETRIES) {
          throw new GeminiHttpError(
            response.status,
            `Gemini unavailable (HTTP ${response.status})`,
          );
        }
        continue;
      }

      if (!response.ok) {
        const text = await response.text();
        throw new GeminiHttpError(
          response.status,
          `Gemini HTTP ${response.status}: ${text.slice(0, 500)}`,
        );
      }

      const json = await response.json();
      const text = json.candidates?.[0]?.content?.parts?.[0]?.text;
      if (typeof text !== "string") {
        throw new Error(
          `Gemini returned no text content. finish_reason=${
            json.candidates?.[0]?.finishReason
          }`,
        );
      }

      let parsed: { items: RawItem[] };
      try {
        parsed = JSON.parse(text);
      } catch (err) {
        throw new Error(
          `Gemini returned invalid JSON: ${
            err instanceof Error ? err.message : err
          }`,
        );
      }

      const items: EstimatedItem[] = (parsed.items ?? []).map((it) => ({
        name: it.name,
        unit: it.unit,
        amount: it.amount,
        kcal: it.kcal,
        protein: it.protein_g,
        fat: it.fat_g,
        carbs: it.carbs_g,
      }));

      return { items };
    }

    throw new Error("Gemini unavailable after retries");
  }
}
