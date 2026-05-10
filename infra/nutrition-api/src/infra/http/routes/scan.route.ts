import { Hono } from "@hono/hono";
import type { ScanMealPort } from "../../../domain/port/in/scan-meal.port.ts";

interface ScanRequestBody {
  images?: string[];
  hint?: string;
  text_only?: boolean;
}

export function createScanRoute(scanService: ScanMealPort): Hono {
  const app = new Hono();

  app.post("/", async (c) => {
    let body: ScanRequestBody;
    try {
      body = await c.req.json<ScanRequestBody>();
    } catch {
      return c.json({ error: "Invalid JSON body" }, 400);
    }

    const images = body.images ?? [];
    const hint = body.hint;
    const textOnly = body.text_only ?? false;

    if (!textOnly && images.length === 0) {
      return c.json({ error: "At least one image is required when text_only is false" }, 400);
    }

    if (textOnly && (!hint || hint.trim().length === 0)) {
      return c.json({ error: "A hint is required when text_only is true" }, 400);
    }

    try {
      const result = await scanService.scan({ images, hint, textOnly });

      return c.json({
        predictions: result.predictions.map((p) => ({
          photo_index: p.photoIndex,
          name: p.name,
          unit: p.unit,
          amount: p.amount,
        })),
        items: result.items.map((item) => ({
          prediction: {
            photo_index: item.prediction.photoIndex,
            name: item.prediction.name,
            unit: item.prediction.unit,
            amount: item.prediction.amount,
          },
          match: item.match
            ? {
                id: item.match.id,
                source: item.match.source,
                name: item.match.name,
                similarity: item.match.similarity,
              }
            : null,
          macros: item.macros,
          is_grounded: item.isGrounded,
          confidence: item.confidence,
        })),
      });
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);

      if (message.startsWith("BAD_INPUT:")) {
        return c.json({ error: message.replace("BAD_INPUT: ", "") }, 400);
      }

      console.error("[scan] Pipeline error:", message);
      return c.json({ error: "Upstream AI service failure" }, 502);
    }
  });

  return app;
}
