import { loadEnv } from "./config/env.ts";
import { GptVisionAdapter } from "./infra/vision/gpt-vision.adapter.ts";
import { GeminiMealEstimatorAdapter } from "./infra/vision/gemini-meal-estimator.adapter.ts";
import { ClaudeValidatorAdapter } from "./infra/validation/claude-validator.adapter.ts";
import { OpenAiEmbeddingAdapter } from "./infra/embedding/openai-embedding.adapter.ts";
import { QdrantSearchAdapter } from "./infra/search/qdrant-search.adapter.ts";
import { ClaudeDisambiguationAdapter } from "./infra/disambiguation/claude-disambiguation.adapter.ts";
import { ScanMealService } from "./domain/service/scan-meal.service.ts";
import { createApp } from "./infra/http/app.ts";

// Validate env — fail fast on missing config
const env = loadEnv();

// Create adapters (infrastructure)
const vision = new GptVisionAdapter({ openaiApiKey: env.openaiApiKey });
const validator = new ClaudeValidatorAdapter({ anthropicApiKey: env.anthropicApiKey });
const embedding = new OpenAiEmbeddingAdapter({ openaiApiKey: env.openaiApiKey });
const qdrant = new QdrantSearchAdapter({
  qdrantUrl: env.qdrantUrl,
  qdrantApiKey: env.qdrantApiKey,
});
const disambiguation = new ClaudeDisambiguationAdapter({
  anthropicApiKey: env.anthropicApiKey,
});

// Create domain service with injected ports
const scanService = new ScanMealService({
  vision,
  embedding,
  vectorSearch: qdrant,
  disambiguation,
  validator,
});

// Optional: Gemini single-call estimator (v2 route, A/B comparison)
const geminiEstimator = env.geminiApiKey
  ? new GeminiMealEstimatorAdapter({ apiKey: env.geminiApiKey })
  : null;

if (!geminiEstimator) {
  console.log("[nutrition-api] GEMINI_API_KEY not set — /api/v1/scan-v2 disabled");
}

// Create HTTP app with injected dependencies
const app = createApp({
  apiKey: env.apiKey,
  scanService,
  embedding,
  qdrant,
  geminiEstimator,
});

console.log(`[nutrition-api] Starting on port ${env.port}`);

Deno.serve({ port: env.port }, app.fetch);
