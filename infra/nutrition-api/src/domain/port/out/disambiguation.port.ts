import type { Prediction } from "../../model/prediction.ts";
import type { FoodMatch } from "../../model/food-match.ts";

export interface DisambiguationPort {
  pickBest(
    prediction: Prediction,
    candidates: FoodMatch[],
    images: string[],
  ): Promise<FoodMatch | null>;
}
