import { Hono } from "@hono/hono";
import type { QdrantSearchAdapter } from "../../search/qdrant-search.adapter.ts";

export function createHealthRoute(qdrant: QdrantSearchAdapter): Hono {
  const app = new Hono();

  app.get("/", async (c) => {
    try {
      const health = await qdrant.healthCheck();
      return c.json({
        status: "ok",
        qdrant: health.status,
        items_count: health.itemsCount,
      });
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      console.error("[health] Qdrant check failed:", message);
      return c.json(
        {
          status: "degraded",
          qdrant: "unreachable",
          items_count: 0,
        },
        503,
      );
    }
  });

  return app;
}
