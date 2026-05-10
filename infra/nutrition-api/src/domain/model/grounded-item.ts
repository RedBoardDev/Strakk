import type { Prediction } from "./prediction.ts";
import type { FoodMatch } from "./food-match.ts";

export interface ComputedMacros {
  grams: number;
  kcal: number;
  protein: number;
  fat: number;
  carbs: number;
}

export interface GroundedItem {
  prediction: Prediction;
  match: FoodMatch | null;
  macros: ComputedMacros;
  isGrounded: boolean;
  confidence: number;
}
