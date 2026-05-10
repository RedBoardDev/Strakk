import type { Prediction } from "../../model/prediction.ts";
import type { ComputedMacros } from "../../model/grounded-item.ts";

/**
 * Item snapshot fed to the validator: the matched name from the database
 * (when grounded) and the macros computed from that match × estimated grams.
 */
export interface ValidationItem {
  predictedName: string;
  matchedName: string | null;
  unit: string;
  amount: number;
  grams: number;
  macros: ComputedMacros;
  isGrounded: boolean;
}

export interface ValidationInput {
  images: string[];
  hint: string | undefined;
  items: ValidationItem[];
  totals: ComputedMacros;
}

export interface ValidationOutput {
  /**
   * The validator's corrected list of predictions. When non-empty, the service
   * re-runs grounding against this list instead of the original.
   */
  predictions: Prediction[];
  /** Whether the validator decided the original list was already correct. */
  unchanged: boolean;
}

export interface MealValidatorPort {
  validate(input: ValidationInput): Promise<ValidationOutput>;
}
