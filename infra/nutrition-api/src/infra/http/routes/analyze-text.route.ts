import { Hono } from "@hono/hono";
import {
  GeminiHttpError,
  type GeminiMealEstimatorAdapter,
} from "../../vision/gemini-meal-estimator.adapter.ts";

interface AnalyzeTextRequestBody {
  description?: string;
}

/**
 * Text-only meal analysis via Gemini 2.5 Pro.
 * Takes a user-typed description and returns nutrition estimates
 * in the same AnalyzedEntry shape the KMP client expects.
 */
export function createAnalyzeTextRoute(
  estimator: GeminiMealEstimatorAdapter | null,
): Hono {
  const app = new Hono();

  app.post("/", async (c) => {
    if (!estimator) {
      return c.json(
        { error: "GEMINI_API_KEY not configured — text analysis disabled" },
        503,
      );
    }

    let body: AnalyzeTextRequestBody;
    try {
      body = await c.req.json<AnalyzeTextRequestBody>();
    } catch {
      return c.json({ error: "Invalid JSON body" }, 400);
    }

    const description = body.description?.trim();
    if (!description || description.length === 0) {
      return c.json({ error: "A non-empty description is required" }, 400);
    }
    if (description.length > 500) {
      return c.json({ error: "Description too long (max 500 chars)" }, 400);
    }

    try {
      const estimate = await estimator.analyzeText(description);

      const totalKcal = estimate.items.reduce((s, it) => s + it.kcal, 0);
      const totalProtein = estimate.items.reduce((s, it) => s + it.protein, 0);
      const totalFat = estimate.items.reduce((s, it) => s + it.fat, 0);
      const totalCarbs = estimate.items.reduce((s, it) => s + it.carbs, 0);

      // Single item → no breakdown
      if (estimate.items.length <= 1) {
        const item = estimate.items[0];
        return c.json({
          name: item?.name ?? description,
          protein_g: item?.protein ?? 0,
          calories_kcal: item?.kcal ?? 0,
          fat_g: item?.fat ?? null,
          carbs_g: item?.carbs ?? null,
          quantity: item ? `${item.amount}${item.unit}` : null,
          breakdown: null,
        });
      }

      // Multiple items → synthesized name + breakdown
      const mealName = estimate.items
        .slice(0, 3)
        .map((it) => it.name)
        .join(", ");

      return c.json({
        name: mealName,
        protein_g: totalProtein,
        calories_kcal: totalKcal,
        fat_g: totalFat,
        carbs_g: totalCarbs,
        quantity: "1 portion",
        breakdown: estimate.items.map((it) => ({
          name: it.name,
          protein_g: it.protein,
          calories_kcal: it.kcal,
          fat_g: it.fat,
          carbs_g: it.carbs,
          quantity: `${it.amount}${it.unit}`,
        })),
      });
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      console.error("[analyze-text] error:", message);

      if (err instanceof GeminiHttpError && err.status === 429) {
        c.header("Retry-After", "3600");
        return c.json(
          { error: "AI analysis quota reached. Please try again later." },
          429,
        );
      }

      return c.json({ error: "Text analysis failed" }, 502);
    }
  });

  return app;
}
