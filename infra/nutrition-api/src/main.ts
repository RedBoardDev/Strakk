import { loadEnv } from "./config/env.ts";
import { GeminiMealEstimatorAdapter } from "./infra/vision/gemini-meal-estimator.adapter.ts";
import { OpenAiEmbeddingAdapter } from "./infra/embedding/openai-embedding.adapter.ts";
import { QdrantSearchAdapter } from "./infra/search/qdrant-search.adapter.ts";
import { createApp } from "./infra/http/app.ts";

// Validate env — fail fast on missing config
const env = loadEnv();

// Create adapters (infrastructure)
const embedding = new OpenAiEmbeddingAdapter({ openaiApiKey: env.openaiApiKey });
const qdrant = new QdrantSearchAdapter({
  qdrantUrl: env.qdrantUrl,
  qdrantApiKey: env.qdrantApiKey,
});

// Optional: Gemini single-call estimator (scan-v2 + text analysis)
const geminiEstimator = env.geminiApiKey
  ? new GeminiMealEstimatorAdapter({ apiKey: env.geminiApiKey })
  : null;

if (!geminiEstimator) {
  console.log("[nutrition-api] GEMINI_API_KEY not set — /api/v1/scan-v2 and /api/v1/analyze-text disabled");
}

// Create HTTP app with injected dependencies
const app = createApp({
  apiKey: env.apiKey,
  embedding,
  qdrant,
  geminiEstimator,
});

console.log(`[nutrition-api] Starting on port ${env.port}`);

Deno.serve({ port: env.port }, app.fetch);
