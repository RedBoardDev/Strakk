import type { DisambiguationPort } from "../../domain/port/out/disambiguation.port.ts";
import type { Prediction } from "../../domain/model/prediction.ts";
import type { FoodMatch } from "../../domain/model/food-match.ts";

const HAIKU_MODEL = "claude-haiku-4-5-20251001";
const MAX_RETRIES = 3;
const BASE_DELAY_MS = 2000;
const MAX_DELAY_MS = 15000;
const RETRYABLE_STATUSES = new Set([429, 503, 529]);

const DISAMBIGUATION_SYSTEM =
  "You are a world class nutrition expert that specializes in matching food items to their corresponding nutritional database entry. " +
  "You will be provided a list of potential matches to the target food specified, along with their ID. " +
  "Your job is to choose the best match for the target food based on the user's comments and images provided. " +
  "Select the option that most closely matches the target food, then provide that option's ID. " +
  'If there is no match, return { "id": 0 }.';

const SELECT_BEST_MATCH_TOOL = {
  name: "select_best_match",
  description:
    "Select the best matching food catalog entry for the target food item. Return id: 0 if none matches.",
  input_schema: {
    type: "object" as const,
    properties: {
      id: {
        type: "integer",
        description: "The ID of the best matching catalog entry, or 0 if no match.",
      },
      name: {
        type: "string",
        description: "The name of the selected catalog entry.",
      },
    },
    required: ["id", "name"],
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
  input: { id: number; name: string };
}

interface ClaudeDisambiguationConfig {
  anthropicApiKey: string;
}

export class ClaudeDisambiguationAdapter implements DisambiguationPort {
  private readonly apiKey: string;

  constructor(config: ClaudeDisambiguationConfig) {
    this.apiKey = config.anthropicApiKey;
  }

  async pickBest(
    prediction: Prediction,
    candidates: FoodMatch[],
    images: string[],
  ): Promise<FoodMatch | null> {
    const candidateList = candidates
      .map((m) => `ID ${m.id}: ${m.name} (similarity: ${m.similarity.toFixed(3)})`)
      .join("\n");

    const userText =
      `Target food: "${prediction.name}" (${prediction.amount} ${prediction.unit})\n\nCandidates:\n${candidateList}`;

    const userContent: ClaudeContent[] = [{ type: "text", text: userText }];

    for (const base64 of images) {
      userContent.push({
        type: "image",
        source: { type: "base64", media_type: "image/jpeg", data: base64 },
      });
    }

    const body = {
      model: HAIKU_MODEL,
      max_tokens: 256,
      temperature: 0.02,
      system: DISAMBIGUATION_SYSTEM,
      tools: [SELECT_BEST_MATCH_TOOL],
      tool_choice: { type: "tool", name: SELECT_BEST_MATCH_TOOL.name },
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
          console.error(`[disambiguation] Network error: ${err instanceof Error ? err.message : String(err)}`);
          return null;
        }
        continue;
      }

      if (RETRYABLE_STATUSES.has(response.status)) {
        if (attempt === MAX_RETRIES) {
          console.error(`[disambiguation] Claude API unavailable after ${MAX_RETRIES} retries`);
          return null;
        }
        continue;
      }

      if (!response.ok) {
        const text = await response.text();
        console.error(`[disambiguation] Claude API HTTP ${response.status}: ${text.slice(0, 500)}`);
        return null;
      }

      const json = await response.json();
      const toolUseBlock = (json.content as ClaudeToolUseBlock[] | undefined)?.find(
        (c) => c.type === "tool_use" && c.name === SELECT_BEST_MATCH_TOOL.name,
      );

      if (!toolUseBlock) {
        console.error(`[disambiguation] Claude returned no tool_use block. Stop reason: ${json.stop_reason}`);
        return null;
      }

      const result = toolUseBlock.input;
      if (!result.id || result.id === 0) return null;

      const matched = candidates.find((c) => c.id === result.id);
      return matched ?? null;
    }

    console.error("[disambiguation] Claude API unavailable after retries");
    return null;
  }
}
