import { Hono } from "@hono/hono";
import type { EmbeddingPort } from "../../../domain/port/out/embedding.port.ts";
import type { VectorSearchPort } from "../../../domain/port/out/vector-search.port.ts";

interface SearchRequestBody {
  query?: string;
  limit?: number;
}

export function createSearchRoute(
  embedding: EmbeddingPort,
  vectorSearch: VectorSearchPort,
): Hono {
  const app = new Hono();

  app.post("/", async (c) => {
    let body: SearchRequestBody;
    try {
      body = await c.req.json<SearchRequestBody>();
    } catch {
      return c.json({ error: "Invalid JSON body" }, 400);
    }

    const query = body.query?.trim();
    if (!query || query.length === 0) {
      return c.json({ error: "A non-empty query is required" }, 400);
    }

    const limit = body.limit ?? 10;

    try {
      const [embeddings] = await embedding.embed([query]);

      const matches = await vectorSearch.search(embeddings, {
        limit,
        threshold: 0.4,
        requireDensity: false,
      });

      return c.json({
        results: matches.map((m) => ({
          id: m.id,
          source: m.source,
          name: m.name,
          similarity: m.similarity,
          kcal_per_100g: m.kcalPer100g,
          protein_per_100g: m.proteinPer100g,
          fat_per_100g: m.fatPer100g,
          carbs_per_100g: m.carbsPer100g,
        })),
      });
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      console.error("[search] Error:", message);
      return c.json({ error: "Search failed" }, 502);
    }
  });

  return app;
}
