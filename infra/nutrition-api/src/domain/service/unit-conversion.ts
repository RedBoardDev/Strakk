import type { FoodMatch } from "../model/food-match.ts";
import type { ComputedMacros } from "../model/grounded-item.ts";

const VOLUMETRIC_UNITS = new Set([
  "ml",
  "milliliter",
  "milliliters",
  "cup",
  "cups",
  "tbsp",
  "tablespoon",
  "tablespoons",
  "tsp",
  "teaspoon",
  "teaspoons",
  "fl oz",
  "fluid ounce",
  "fluid ounces",
  "l",
  "liter",
  "liters",
]);

export function isVolumetric(unit: string): boolean {
  return VOLUMETRIC_UNITS.has(unit.trim().toLowerCase());
}

/**
 * Convert an AI-predicted amount in the given unit to grams.
 * Falls back to defaultPortionGrams for unknown or volumetric units without density.
 */
export function toGrams(
  amount: number,
  unit: string,
  density: number | null,
  defaultPortionGrams: number,
): number {
  const u = unit.trim().toLowerCase();

  switch (u) {
    case "g":
    case "gram":
    case "grams":
      return amount;

    case "kg":
    case "kilogram":
    case "kilograms":
      return amount * 1000;

    case "oz":
    case "ounce":
    case "ounces":
      return amount * 28.3495;

    case "lb":
    case "pound":
    case "pounds":
      return amount * 453.592;

    case "ml":
    case "milliliter":
    case "milliliters":
      return density !== null ? amount * density : defaultPortionGrams;

    case "cup":
    case "cups":
      return density !== null ? amount * 236.588 * density : defaultPortionGrams;

    case "tbsp":
    case "tablespoon":
    case "tablespoons":
      return density !== null ? amount * 14.787 * density : defaultPortionGrams;

    case "tsp":
    case "teaspoon":
    case "teaspoons":
      return density !== null ? amount * 4.929 * density : defaultPortionGrams;

    case "fl oz":
    case "fluid ounce":
    case "fluid ounces":
      return density !== null ? amount * 29.574 * density : defaultPortionGrams;

    case "piece":
    case "pieces":
    case "item":
    case "items":
    case "unit":
    case "units":
    case "slice":
    case "slices":
      return amount * defaultPortionGrams;

    default:
      console.warn(`[unit-conversion] Unknown unit "${unit}", using defaultPortionGrams`);
      return defaultPortionGrams;
  }
}

export function zeroMacros(grams = 0): ComputedMacros {
  return { grams, kcal: 0, protein: 0, fat: 0, carbs: 0 };
}

export function computeMacros(match: FoodMatch, grams: number): ComputedMacros {
  const g = grams ?? 0;
  const factor = g / 100;
  return {
    grams: Math.round(g * 10) / 10,
    kcal: Math.round(match.kcalPer100g * factor * 10) / 10,
    protein: Math.round(match.proteinPer100g * factor * 10) / 10,
    fat: Math.round((match.fatPer100g ?? 0) * factor * 10) / 10,
    carbs: Math.round((match.carbsPer100g ?? 0) * factor * 10) / 10,
  };
}
