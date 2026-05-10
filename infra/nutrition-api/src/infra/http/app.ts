import { Hono } from "@hono/hono";
import { apiKeyMiddleware } from "./middleware/api-key.ts";
import { createScanRoute } from "./routes/scan.route.ts";
import { createScanV2Route } from "./routes/scan-v2.route.ts";
import { createSearchRoute } from "./routes/search.route.ts";
import { createHealthRoute } from "./routes/health.route.ts";
import type { ScanMealPort } from "../../domain/port/in/scan-meal.port.ts";
import type { EmbeddingPort } from "../../domain/port/out/embedding.port.ts";
import type { QdrantSearchAdapter } from "../search/qdrant-search.adapter.ts";
import type { GeminiMealEstimatorAdapter } from "../vision/gemini-meal-estimator.adapter.ts";

export interface AppDependencies {
  apiKey: string;
  scanService: ScanMealPort;
  embedding: EmbeddingPort;
  qdrant: QdrantSearchAdapter;
  geminiEstimator: GeminiMealEstimatorAdapter | null;
}

export function createApp(deps: AppDependencies): Hono {
  const app = new Hono();

  // Health check — no auth required
  app.route("/healthz", createHealthRoute(deps.qdrant));

  // All /api routes require API key
  const api = new Hono();
  api.use("*", apiKeyMiddleware(deps.apiKey));
  api.route("/v1/scan", createScanRoute(deps.scanService));
  api.route("/v1/scan-v2", createScanV2Route(deps.geminiEstimator));
  api.route("/v1/search", createSearchRoute(deps.embedding, deps.qdrant));

  app.route("/api", api);

  return app;
}
