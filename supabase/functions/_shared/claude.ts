// Low-level Claude API client shared by all meal-analysis edge functions.
// Handles retries, rate-limiting backoff, and response extraction.
// No domain logic — just a thin HTTP wrapper around /v1/messages.

const CLAUDE_MODEL = "claude-sonnet-4-6";
export const HAIKU_MODEL = "claude-haiku-4-5-20251001";
const MAX_RETRIES = 3;
const BASE_DELAY_MS = 2000;
const MAX_DELAY_MS = 15000;
const RETRYABLE_STATUSES = new Set([429, 503, 529]);

export type ClaudeContent =
  | { type: "text"; text: string }
  | {
      type: "image";
      source: {
        type: "base64";
        media_type: "image/jpeg" | "image/png" | "image/webp";
        data: string;
      };
    };

export interface ClaudeRequest {
  messages: Array<{
    role: "user" | "assistant";
    content: ClaudeContent[];
  }>;
  maxTokens?: number;
  temperature?: number;
  system?: string;
  model?: string;
}

/**
 * Calls Claude /v1/messages and returns the raw text block.
 * Retries up to 3 times on rate-limit / overload errors with exponential backoff.
 * Throws on hard failures (4xx non-retryable, missing API key, malformed response).
 */
export async function callClaude(req: ClaudeRequest): Promise<string> {
  const apiKey = Deno.env.get("ANTHROPIC_API_KEY");
  if (!apiKey) throw new Error("ANTHROPIC_API_KEY not configured");

  const body = {
    model: req.model ?? CLAUDE_MODEL,
    max_tokens: req.maxTokens ?? 8192,
    temperature: req.temperature ?? 0.1,
    ...(req.system ? { system: req.system } : {}),
    messages: req.messages,
  };

  for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
    if (attempt > 0) {
      const delay = Math.min(BASE_DELAY_MS * 2 ** (attempt - 1), MAX_DELAY_MS);
      await new Promise((r) => setTimeout(r, delay));
    }

    const response = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-api-key": apiKey,
        "anthropic-version": "2023-06-01",
      },
      body: JSON.stringify(body),
    });

    if (RETRYABLE_STATUSES.has(response.status)) continue;

    if (!response.ok) {
      const text = await response.text();
      throw new Error(`Claude API HTTP ${response.status}: ${text.slice(0, 500)}`);
    }

    const json = await response.json();
    const textBlock = json.content?.find((c: { type: string }) => c.type === "text");
    if (!textBlock?.text) {
      throw new Error("No text block in Claude response");
    }
    return textBlock.text as string;
  }
  throw new Error("Claude API unavailable after retries");
}

/**
 * Strips markdown code fences from a Claude response (```json ... ```) if present.
 */
export function stripMarkdownFences(text: string): string {
  const match = text.match(/```(?:json)?\s*([\s\S]*?)```/);
  if (match && match[1]) return match[1].trim();
  return text.trim();
}
