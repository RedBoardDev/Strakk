export interface FoodMatch {
  id: number;
  source: string;
  name: string;
  similarity: number;
  kcalPer100g: number;
  proteinPer100g: number;
  fatPer100g: number | null;
  carbsPer100g: number | null;
  density: number | null;
  defaultPortionGrams: number;
}
