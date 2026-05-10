import type { FoodMatch } from "../../model/food-match.ts";

export interface SearchOptions {
  limit: number;
  threshold: number;
  requireDensity: boolean;
}

export interface VectorSearchPort {
  search(embedding: number[], opts: SearchOptions): Promise<FoodMatch[]>;
}
