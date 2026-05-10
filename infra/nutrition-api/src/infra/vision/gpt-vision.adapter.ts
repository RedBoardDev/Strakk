import type { VisionPort } from "../../domain/port/out/vision.port.ts";
import type { Prediction } from "../../domain/model/prediction.ts";

const VISION_MODEL = "gpt-5";
const MAX_RETRIES = 3;
const BASE_DELAY_MS = 2000;
const MAX_DELAY_MS = 15000;
const RETRYABLE_STATUSES = new Set([429, 503, 529]);

const IDENTIFY_SYSTEM_PROMPT = `You are a clinical dietitian analyzing meal photos. Identify EVERY visible food and estimate quantities as accurately as you can.

Output language: ALWAYS English (the food database is in English — translate any non-English item to its standard English name).

== STEP 1 — Identify each visible item ==

List ONLY the components that are visible as SEPARATE items on the plate (the way a diner perceives the meal). Do NOT decompose composed dishes into their hidden ingredients.

✅ DO list:
- Each protein, starch, vegetable component visible.
- Sauces or jus that are drizzled, pooled, or clearly added on top of an item.
- Garnishes that are visibly distinct (jam dollop next to the meat, chutney quenelle, fruit slice on the side).
- Salad dressings only if visible/glossy on greens — ignore otherwise.
- Cheese, croutons, nuts sprinkled visibly on top.
- Side items (bread, crackers) and beverages.

❌ DO NOT list:
- The butter/milk INSIDE mashed potatoes — they are part of "mashed potatoes" already.
- The cooking oil/butter used to fry/roast a protein — that fat is already accounted for downstream by the cooking method (pan-seared, fried, etc.) you describe in the protein's name.
- Salt, pepper, herbs, spices.
- Background plate, cutlery, napkin.

Rule of thumb: if removing the ingredient would change how the item LOOKS on the plate, list it. If removing it would not change the visual appearance, don't list it.

Use richly descriptive names so the database picks the right entry. The cooking state alone changes nutrient density by 30-50%, so it is mandatory.

Required descriptors when applicable:
- Cooking state: "raw", "cooked", "grilled", "pan-fried", "deep-fried", "boiled", "steamed", "roasted", "baked", "poached", "raw cured"
- Cut / part: "chicken breast", "chicken thigh", "ground beef 80/20", "salmon fillet", "skinless", "boneless", "lean", "trimmed"
- Form: "whole-grain", "white", "refined", "sliced", "diced", "shredded", "mashed", "pureed"
- Sauce / topping: "with cream sauce", "with tomato sauce", "with butter", "with olive oil", "plain", "unseasoned"

Examples of well-described names:
- "chicken breast, grilled, skinless, boneless"
- "white rice, cooked, plain"
- "broccoli florets, steamed, no butter"
- "olive oil, used for cooking"
- "salmon fillet, pan-fried, with skin"

== STEP 2 — Estimate the quantity ==

Trust your visual judgement — you have been trained on millions of food photos and know what typical portions look like. Do NOT use hardcoded size assumptions for any object: forks, glasses, plates, hands, food items themselves, all vary widely (a dessert plate looks like a dinner plate at the wrong angle, a wine glass is half a water glass, a chicken breast may be a child's portion or a 250 g cut).

Reason proportionally instead:
- Compare items to each other (does the rice mound look bigger or smaller than the protein? by how much?).
- If a reference object is in frame and you can identify its likely type with confidence, use it as a calibration — but if its type is ambiguous, ignore it rather than guessing.
- When uncertain between two plausible quantities, pick the smaller one. Overestimation is the most common error.

== STEP 3 — Be conservative on quantities ==

- Do NOT double-count.
  - If you list "duck breast with shallot pan sauce" together, do NOT also list the sauce separately.
  - If you list the sauce separately, list the duck without it.
  - Do NOT add "olive oil" or "butter" as separate items when they are simply the cooking medium — the cooking method in the protein name (pan-seared, fried, sauteed) is enough.
- If you cannot tell whether something is raw or cooked, assume cooked (most photographed meals are).
- When uncertain between two plausible quantities, pick the smaller.

== Output ==

Standard units only: g, ml, oz, fl oz, cup, tbsp, tsp, piece, slice. Prefer g for solids and ml for liquids.
Do NOT estimate calories or macronutrients — the database handles that.`;

const REPORT_FOOD_ITEMS_TOOL = {
  name: "report_food_items",
  description: "Report all identified food items from the meal image(s)",
  parameters: {
    type: "object" as const,
    properties: {
      predictions: {
        type: "array",
        items: {
          type: "object",
          properties: {
            photo_index: {
              type: "integer",
              description: "Index of the photo (0-based)",
            },
            name: {
              type: "string",
              description: "Food name with descriptors, e.g. 'chicken breast, grilled, skinless'",
            },
            unit: {
              type: "string",
              description: "Unit of measurement: g, ml, oz, cups, pieces, slices, etc.",
            },
            amount: {
              type: "number",
              description: "Numeric quantity in the given unit",
            },
          },
          required: ["photo_index", "name", "unit", "amount"],
        },
      },
    },
    required: ["predictions"],
  },
};

type GPTContent =
  | { type: "text"; text: string }
  | { type: "image_url"; image_url: { url: string; detail: "high" } };

interface RawPrediction {
  photo_index: number;
  name: string;
  unit: string;
  amount: number;
}

interface GptVisionConfig {
  openaiApiKey: string;
}

export class GptVisionAdapter implements VisionPort {
  private readonly apiKey: string;

  constructor(config: GptVisionConfig) {
    this.apiKey = config.openaiApiKey;
  }

  async identify(images: string[], hint?: string): Promise<Prediction[]> {
    const userContent: GPTContent[] = [];

    if (hint && hint.trim().length > 0) {
      userContent.push({ type: "text", text: hint.trim() });
    }

    for (const base64 of images) {
      userContent.push({
        type: "image_url",
        image_url: { url: `data:image/jpeg;base64,${base64}`, detail: "high" },
      });
    }

    if (userContent.length === 0) {
      throw new Error("BAD_INPUT: at least one image or a hint is required for food identification");
    }

    const body = {
      model: VISION_MODEL,
      // GPT-5 spends "reasoning tokens" before producing output; "minimal"
      // keeps it fast for visual identification (no chain-of-thought needed).
      reasoning_effort: "minimal",
      max_completion_tokens: 4096,
      messages: [
        { role: "system", content: IDENTIFY_SYSTEM_PROMPT },
        { role: "user", content: userContent },
      ],
      tools: [
        {
          type: "function",
          function: {
            name: REPORT_FOOD_ITEMS_TOOL.name,
            description: REPORT_FOOD_ITEMS_TOOL.description,
            parameters: REPORT_FOOD_ITEMS_TOOL.parameters,
          },
        },
      ],
      tool_choice: { type: "function", function: { name: REPORT_FOOD_ITEMS_TOOL.name } },
    };

    for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
      if (attempt > 0) {
        const delay = Math.min(BASE_DELAY_MS * 2 ** (attempt - 1), MAX_DELAY_MS);
        await new Promise((r) => setTimeout(r, delay));
      }

      let response: Response;
      try {
        response = await fetch("https://api.openai.com/v1/chat/completions", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${this.apiKey}`,
          },
          body: JSON.stringify(body),
        });
      } catch (err) {
        if (attempt === MAX_RETRIES) {
          throw new Error(`OpenAI Vision network error: ${err instanceof Error ? err.message : String(err)}`);
        }
        continue;
      }

      if (RETRYABLE_STATUSES.has(response.status)) {
        if (attempt === MAX_RETRIES) {
          throw new Error(`OpenAI Vision API unavailable after ${MAX_RETRIES} retries (HTTP ${response.status})`);
        }
        continue;
      }

      if (!response.ok) {
        const text = await response.text();
        throw new Error(`OpenAI Vision API HTTP ${response.status}: ${text.slice(0, 500)}`);
      }

      const json = await response.json();
      const toolCall = json.choices?.[0]?.message?.tool_calls?.[0];

      if (!toolCall || toolCall.function?.name !== REPORT_FOOD_ITEMS_TOOL.name) {
        throw new Error(
          `OpenAI returned no function call for "${REPORT_FOOD_ITEMS_TOOL.name}". Finish reason: ${json.choices?.[0]?.finish_reason}`,
        );
      }

      const result = JSON.parse(toolCall.function.arguments) as { predictions: RawPrediction[] };

      if (!Array.isArray(result.predictions)) {
        throw new Error("OpenAI returned invalid predictions structure");
      }

      return result.predictions.map((raw) => ({
        photoIndex: raw.photo_index,
        name: raw.name,
        unit: raw.unit,
        amount: raw.amount,
      }));
    }

    throw new Error("OpenAI Vision API unavailable after retries");
  }
}
