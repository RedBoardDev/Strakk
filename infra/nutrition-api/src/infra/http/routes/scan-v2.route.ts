import { Hono } from "@hono/hono";
import {
  GeminiHttpError,
  type GeminiMealEstimatorAdapter,
} from "../../vision/gemini-meal-estimator.adapter.ts";

interface ScanV2RequestBody {
  images?: string[];
  hint?: string;
}

/**
 * Single-call estimator route. Sends the photo(s) directly to Gemini 2.5 Pro
 * which returns items + portions + macros in one shot. No Qdrant lookup,
 * no cooking adjustment, no species filter — pure VLM output.
 *
 * Returned in the same shape as /scan so the caller can A/B compare.
 */
export function createScanV2Route(
  estimator: GeminiMealEstimatorAdapter | null,
): Hono {
  const app = new Hono();

  app.post("/", async (c) => {
    if (!estimator) {
      return c.json(
        { error: "GEMINI_API_KEY not configured — v2 route disabled" },
        503,
      );
    }

    let body: ScanV2RequestBody;
    try {
      body = await c.req.json<ScanV2RequestBody>();
    } catch {
      return c.json({ error: "Invalid JSON body" }, 400);
    }

    const images = body.images ?? [];
    const hint = body.hint;

    if (images.length === 0) {
      return c.json({ error: "At least one image is required" }, 400);
    }

    try {
      const estimate = await estimator.estimate(images, hint);

      const totals = estimate.items.reduce(
        (acc, it) => ({
          grams: acc.grams + (it.unit === "g" ? it.amount : 0),
          kcal: acc.kcal + it.kcal,
          protein: acc.protein + it.protein,
          fat: acc.fat + it.fat,
          carbs: acc.carbs + it.carbs,
        }),
        { grams: 0, kcal: 0, protein: 0, fat: 0, carbs: 0 },
      );

      console.log(
        "[scan-v2] items:",
        JSON.stringify(estimate.items),
      );
      console.log("[scan-v2] totals:", JSON.stringify(totals));

      const predictions = estimate.items.map((it) => ({
        photo_index: 0,
        name: it.name,
        unit: it.unit,
        amount: it.amount,
      }));

      const items = estimate.items.map((it, i) => ({
        prediction: predictions[i],
        match: null,
        macros: {
          grams: it.amount,
          kcal: it.kcal,
          protein: it.protein,
          fat: it.fat,
          carbs: it.carbs,
        },
        is_grounded: true,
        confidence: 1.0,
      }));

      // Same envelope as /scan so the KMP client (ScanMealResponseDto)
      // deserializes without changes — the iOS app can swap routes freely.
      return c.json({ predictions, items, totals });
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);

      if (message.startsWith("BAD_INPUT:")) {
        return c.json({ error: message.replace("BAD_INPUT: ", "") }, 400);
      }

      console.error("[scan-v2] error:", message);

      if (err instanceof GeminiHttpError && err.status === 429) {
        c.header("Retry-After", "3600");
        return c.json(
          { error: "AI scanner quota reached. Please try again later." },
          429,
        );
      }

      return c.json({ error: "Gemini estimator failure" }, 502);
    }
  });

  return app;
}
