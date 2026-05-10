export interface EnvConfig {
  openaiApiKey: string;
  anthropicApiKey: string;
  geminiApiKey?: string;
  qdrantUrl: string;
  qdrantApiKey?: string;
  apiKey: string;
  port: number;
}

/**
 * Validates and returns typed environment variables.
 * Throws immediately on missing required values — fail fast.
 */
export function loadEnv(): EnvConfig {
  const missing: string[] = [];

  const openaiApiKey = Deno.env.get("OPENAI_API_KEY");
  if (!openaiApiKey) missing.push("OPENAI_API_KEY");

  const anthropicApiKey = Deno.env.get("ANTHROPIC_API_KEY");
  if (!anthropicApiKey) missing.push("ANTHROPIC_API_KEY");

  const apiKey = Deno.env.get("API_KEY");
  if (!apiKey) missing.push("API_KEY");

  if (missing.length > 0) {
    throw new Error(`Missing required environment variables: ${missing.join(", ")}`);
  }

  const qdrantUrl = Deno.env.get("QDRANT_URL") ?? "http://localhost:6333";
  const qdrantApiKey = Deno.env.get("QDRANT_API_KEY") || undefined;
  const geminiApiKey = Deno.env.get("GEMINI_API_KEY") || undefined;
  const port = parseInt(Deno.env.get("PORT") ?? "3000", 10);

  if (isNaN(port) || port < 1 || port > 65535) {
    throw new Error(`Invalid PORT value: ${Deno.env.get("PORT")}`);
  }

  return {
    openaiApiKey: openaiApiKey!,
    anthropicApiKey: anthropicApiKey!,
    geminiApiKey,
    qdrantUrl,
    qdrantApiKey,
    apiKey: apiKey!,
    port,
  };
}
