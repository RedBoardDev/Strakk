import type {
  MealValidatorPort,
  ValidationInput,
  ValidationOutput,
} from "../../domain/port/out/validator.port.ts";
import type { Prediction } from "../../domain/model/prediction.ts";

const SONNET_MODEL = "claude-sonnet-4-6";
const MAX_RETRIES = 2;
const BASE_DELAY_MS = 2000;
const MAX_DELAY_MS = 15000;
const RETRYABLE_STATUSES = new Set([429, 503, 529]);

const VALIDATOR_SYSTEM_PROMPT = `You are a senior clinical dietitian double-checking a junior's first-pass meal analysis. The junior has already produced a list of items, matched them to a USDA database, and computed macros. Your job is to look at the actual photo(s) and decide whether the items, names, and quantities are realistic — and correct them if they are not.

You will be given:
- The photo(s) of the meal
- The current item list with the database match name, unit, amount, computed grams, and per-item macros
- The total macros (kcal/protein/fat/carbs)

== Your job ==

1. Visually verify each item is actually in the photo. Flag missing or hallucinated items.
2. Re-estimate each quantity. Trust your visual judgement — do NOT use hardcoded size assumptions for forks, glasses, plates, hands, or even the food items themselves; their real-world sizes vary too much. Reason proportionally between visible items, and only use a reference object as calibration when its type is unambiguous.
3. Sanity-check the per-item macros: are they consistent with the visible portion size relative to the rest of the meal? An item that takes up a quarter of the plate should not have wildly different macros than its visible volume suggests.
4. Flag obvious errors:
   - Cooking state confusion (cooked vs raw — protein density differs by ~40%).
   - Overestimated added cooking fat (oil residue on a plate is small; deep-frying is large only if visually evident).
   - Doubled items (e.g., "chicken with sauce" AND "sauce" separately).
   - Wrong cut/match chosen (e.g., generic "ground beef" but the meat is visually lean).
   - Translation drift (food name in a non-English language).

== Output rules ==

- ALWAYS respond by calling the \`return_corrected_predictions\` tool.
- If everything looks correct, set \`unchanged: true\` and return the EXACT same predictions you were given (same names, units, amounts).
- If anything is wrong, set \`unchanged: false\` and return the corrected predictions:
  - You may rename items (use richer English descriptors so the database matches better).
  - You may adjust amounts up or down.
  - You may remove items that are not actually in the photo.
  - You may add missing items.
  - Keep predictions in the same shape: { photo_index, name, unit, amount }.
- ALWAYS use English food names. Use g for solids, ml for liquids unless density is unknown.
- DO NOT include macros in your output — they are recomputed from the database.
- When uncertain between two plausible quantities, pick the smaller one. Overestimation is the most common error.`;

const RETURN_CORRECTED_PREDICTIONS_TOOL = {
  name: "return_corrected_predictions",
  description: "Return the validated/corrected meal predictions",
  input_schema: {
    type: "object" as const,
    properties: {
      unchanged: {
        type: "boolean",
        description: "true if the original predictions were already correct, false otherwise",
      },
      predictions: {
        type: "array",
        items: {
          type: "object",
          properties: {
            photo_index: { type: "integer" },
            name: { type: "string", description: "Food name with rich descriptors in English" },
            unit: { type: "string", description: "g, ml, oz, cup, tbsp, tsp, piece, slice, etc." },
            amount: { type: "number" },
          },
          required: ["photo_index", "name", "unit", "amount"],
        },
      },
    },
    required: ["unchanged", "predictions"],
  },
};

type ClaudeContent =
  | { type: "text"; text: string }
  | {
      type: "image";
      source: {
        type: "base64";
        media_type: "image/jpeg";
        data: string;
      };
    };

interface ClaudeToolUseBlock {
  type: "tool_use";
  id: string;
  name: string;
  input: { unchanged: boolean; predictions: RawPrediction[] };
}

interface RawPrediction {
  photo_index: number;
  name: string;
  unit: string;
  amount: number;
}

interface ClaudeValidatorConfig {
  anthropicApiKey: string;
}

export class ClaudeValidatorAdapter implements MealValidatorPort {
  private readonly apiKey: string;

  constructor(config: ClaudeValidatorConfig) {
    this.apiKey = config.anthropicApiKey;
  }

  async validate(input: ValidationInput): Promise<ValidationOutput> {
    const summary = this.buildSummary(input);
    const userContent: ClaudeContent[] = [{ type: "text", text: summary }];

    if (input.hint && input.hint.trim().length > 0) {
      userContent.push({ type: "text", text: `User hint: ${input.hint.trim()}` });
    }

    for (const base64 of input.images) {
      userContent.push({
        type: "image",
        source: { type: "base64", media_type: "image/jpeg", data: base64 },
      });
    }

    const body = {
      model: SONNET_MODEL,
      max_tokens: 1024,
      temperature: 0.1,
      // Anthropic prompt caching: the system prompt is identical across scans
      // → cache it ephemerally for 90% off on repeat hits within ~5min.
      system: [
        {
          type: "text",
          text: VALIDATOR_SYSTEM_PROMPT,
          cache_control: { type: "ephemeral" },
        },
      ],
      tools: [RETURN_CORRECTED_PREDICTIONS_TOOL],
      tool_choice: { type: "tool", name: RETURN_CORRECTED_PREDICTIONS_TOOL.name },
      messages: [{ role: "user", content: userContent }],
    };

    for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
      if (attempt > 0) {
        const delay = Math.min(BASE_DELAY_MS * 2 ** (attempt - 1), MAX_DELAY_MS);
        await new Promise((r) => setTimeout(r, delay));
      }

      let response: Response;
      try {
        response = await fetch("https://api.anthropic.com/v1/messages", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "x-api-key": this.apiKey,
            "anthropic-version": "2023-06-01",
          },
          body: JSON.stringify(body),
        });
      } catch (err) {
        if (attempt === MAX_RETRIES) {
          throw new Error(
            `Validator network error: ${err instanceof Error ? err.message : String(err)}`,
          );
        }
        continue;
      }

      if (RETRYABLE_STATUSES.has(response.status)) {
        if (attempt === MAX_RETRIES) {
          throw new Error(`Validator unavailable (HTTP ${response.status})`);
        }
        continue;
      }

      if (!response.ok) {
        const text = await response.text();
        throw new Error(`Validator HTTP ${response.status}: ${text.slice(0, 500)}`);
      }

      const json = await response.json();
      const toolUseBlock = (json.content as ClaudeToolUseBlock[] | undefined)?.find(
        (c) => c.type === "tool_use" && c.name === RETURN_CORRECTED_PREDICTIONS_TOOL.name,
      );

      if (!toolUseBlock) {
        throw new Error(`Validator returned no tool_use block. Stop reason: ${json.stop_reason}`);
      }

      const result = toolUseBlock.input;
      const predictions: Prediction[] = (result.predictions ?? []).map((raw) => ({
        photoIndex: raw.photo_index,
        name: raw.name,
        unit: raw.unit,
        amount: raw.amount,
      }));

      return { unchanged: result.unchanged === true, predictions };
    }

    throw new Error("Validator unavailable after retries");
  }

  private buildSummary(input: ValidationInput): string {
    const lines = ["First-pass meal analysis to verify:\n", "Items:"];
    input.items.forEach((it, i) => {
      const matched = it.matchedName ?? "(no database match)";
      const grounded = it.isGrounded ? "" : " [NOT GROUNDED]";
      lines.push(
        `${i + 1}. predicted="${it.predictedName}", matched="${matched}", ` +
          `${it.amount} ${it.unit} ≈ ${it.grams.toFixed(0)}g → ` +
          `${it.macros.kcal.toFixed(0)} kcal, ` +
          `P ${it.macros.protein.toFixed(1)}g, ` +
          `F ${it.macros.fat.toFixed(1)}g, ` +
          `C ${it.macros.carbs.toFixed(1)}g${grounded}`,
      );
    });
    lines.push(
      "",
      `Totals: ${input.totals.kcal.toFixed(0)} kcal, ` +
        `${input.totals.protein.toFixed(1)}g protein, ` +
        `${input.totals.fat.toFixed(1)}g fat, ` +
        `${input.totals.carbs.toFixed(1)}g carbs`,
      "",
      "Verify against the photo(s) and return the corrected predictions list.",
    );
    return lines.join("\n");
  }
}
