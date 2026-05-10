import type { VectorSearchPort, SearchOptions } from "../../domain/port/out/vector-search.port.ts";
import type { FoodMatch } from "../../domain/model/food-match.ts";

interface QdrantHit {
  id: number;
  score: number;
  payload: Record<string, unknown>;
}

interface QdrantSearchResponse {
  result: QdrantHit[];
}

interface QdrantSearchConfig {
  qdrantUrl: string;
  qdrantApiKey?: string;
}

export class QdrantSearchAdapter implements VectorSearchPort {
  private readonly qdrantUrl: string;
  private readonly qdrantApiKey?: string;

  constructor(config: QdrantSearchConfig) {
    this.qdrantUrl = config.qdrantUrl;
    this.qdrantApiKey = config.qdrantApiKey;
  }

  async search(embedding: number[], opts: SearchOptions): Promise<FoodMatch[]> {
    const filter: Record<string, unknown> = {};
    if (opts.requireDensity) {
      filter.must_not = [{ is_null: { key: "density" } }];
    }

    const body = {
      vector: embedding,
      limit: opts.limit,
      score_threshold: opts.threshold,
      with_payload: true,
      ...(opts.requireDensity ? { filter } : {}),
    };

    const headers: Record<string, string> = {
      "Content-Type": "application/json",
    };
    if (this.qdrantApiKey) {
      headers["api-key"] = this.qdrantApiKey;
    }

    let response: Response;
    try {
      response = await fetch(
        `${this.qdrantUrl}/collections/food_catalog/points/search`,
        {
          method: "POST",
          headers,
          body: JSON.stringify(body),
        },
      );
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      throw new Error(`Qdrant fetch error: ${msg}`);
    }

    if (!response.ok) {
      const text = await response.text();
      throw new Error(`Qdrant HTTP ${response.status}: ${text.slice(0, 200)}`);
    }

    const json = (await response.json()) as QdrantSearchResponse;

    return (json.result ?? []).map((hit) => ({
      id: hit.id,
      source: String(hit.payload.source ?? ""),
      name: String(hit.payload.name ?? ""),
      similarity: hit.score,
      kcalPer100g: (hit.payload.kcal as number) ?? 0,
      proteinPer100g: (hit.payload.protein as number) ?? 0,
      fatPer100g: (hit.payload.fat as number | null) ?? null,
      carbsPer100g: (hit.payload.carbs as number | null) ?? null,
      density: (hit.payload.density as number | null) ?? null,
      defaultPortionGrams: (hit.payload.default_portion_grams as number) ?? 100,
    }));
  }

  /**
   * Health check: verifies Qdrant connectivity and returns collection item count.
   */
  async healthCheck(): Promise<{ status: string; itemsCount: number }> {
    const headers: Record<string, string> = {};
    if (this.qdrantApiKey) {
      headers["api-key"] = this.qdrantApiKey;
    }

    const response = await fetch(
      `${this.qdrantUrl}/collections/food_catalog`,
      { headers },
    );

    if (!response.ok) {
      throw new Error(`Qdrant health check failed: HTTP ${response.status}`);
    }

    const json = await response.json();
    const pointsCount = json.result?.points_count ?? 0;

    return { status: "ok", itemsCount: pointsCount };
  }
}
