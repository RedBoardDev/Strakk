import type { ComputedMacros } from "../model/grounded-item.ts";

/**
 * Empirical cooking-method adjustments applied to USDA macros AFTER matching.
 *
 * USDA "cooked" entries assume a specific preparation (typically oven-roasted
 * for meats), but real-world meals are often pan-seared, fried, or grilled in
 * butter/oil — which retains or adds fat compared to roasted. We correct for
 * that systematic bias here.
 *
 * Multipliers are derived from USDA SR-21 "Nutrient Retention Factors" Release
 * 6 averaged across categories (poultry, beef, fish), and from food-science
 * literature for fat retention/uptake during pan-cooking and frying.
 *
 * Protein and carbs are largely unaffected by cooking method (≤5%), so we
 * only adjust kcal and fat.
 */

export interface CookingAdjustment {
  method: string;
  fatMultiplier: number;
  kcalMultiplier: number;
}

interface CookingProfile {
  match: RegExp;
  adjustment: CookingAdjustment;
}

const COOKING_PROFILES: readonly CookingProfile[] = [
  {
    // Deep-frying absorbs significant cooking oil into the food (~10-25% of
    // food weight in oil). Reflects breaded/battered fried items too.
    match: /\b(deep[-\s]?fried|battered|breaded\s+fried|tempura)\b/i,
    adjustment: { method: "deep-fried", fatMultiplier: 2.5, kcalMultiplier: 1.45 },
  },
  {
    // Pan-frying adds 5-10g of cooking fat that stays with the food, plus
    // animal fats melt and re-absorb (vs draining away when roasted).
    match: /\b(pan[-\s]?fried|pan[-\s]?seared|sauteed?|sautéed?|stir[-\s]?fried|seared)\b/i,
    adjustment: { method: "pan-fried", fatMultiplier: 1.4, kcalMultiplier: 1.18 },
  },
  {
    // Grilled/BBQ drips fat away, slightly less than the USDA roasted baseline.
    match: /\b(grilled|barbecued?|bbq|chargrilled|flame[-\s]?grilled)\b/i,
    adjustment: { method: "grilled", fatMultiplier: 0.9, kcalMultiplier: 0.96 },
  },
  {
    // Boiled / poached / steamed leach a bit of fat into the cooking water.
    match: /\b(boiled|poached|simmered|steamed|braised)\b/i,
    adjustment: { method: "boiled", fatMultiplier: 0.92, kcalMultiplier: 0.97 },
  },
  // Roasted/baked/broiled = USDA baseline → no adjustment, intentionally not listed.
];

export function detectCookingMethod(name: string): CookingAdjustment | null {
  for (const profile of COOKING_PROFILES) {
    if (profile.match.test(name)) return profile.adjustment;
  }
  return null;
}

/**
 * Apply the adjustment if the cooking method in the prediction does NOT
 * already match the cooking method baked into the database entry name.
 *
 * Example: prediction "duck breast pan-seared" matched to "Duck breast,
 * cooked, roasted" → method mismatch → apply pan-fried adjustment.
 *
 * Example: prediction "duck breast pan-seared" matched to "Duck breast,
 * pan-fried" → method match → no adjustment.
 */
export function applyCookingAdjustment(
  macros: ComputedMacros,
  predictionName: string,
  matchedName: string | null,
): ComputedMacros {
  const predMethod = detectCookingMethod(predictionName);
  if (!predMethod) return macros;

  const matchMethod = matchedName ? detectCookingMethod(matchedName) : null;
  if (matchMethod && matchMethod.method === predMethod.method) return macros;

  return {
    ...macros,
    kcal: round1(macros.kcal * predMethod.kcalMultiplier),
    fat: round1(macros.fat * predMethod.fatMultiplier),
  };
}

function round1(n: number): number {
  return Math.round(n * 10) / 10;
}
