import { invokeEdge, type AnalyzedEntry } from './edge.ts'

// ---- analyze-meal-single (quick add: text or single photo) ------------------

export function analyzeText(description: string): Promise<AnalyzedEntry> {
  return invokeEdge<AnalyzedEntry>('analyze-meal-single', { type: 'text', description })
}

export function analyzePhoto(imageBase64: string, hint?: string): Promise<AnalyzedEntry> {
  return invokeEdge<AnalyzedEntry>('analyze-meal-single', {
    type: 'photo',
    image_base64: imageBase64,
    ...(hint ? { hint } : {}),
  })
}

// ---- scan-meal (plate photo → grounded items) --------------------------------

export type ScanPrediction = { photo_index: number; name: string; unit: string; amount: number }
export type ScanItem = {
  prediction: ScanPrediction
  match: unknown | null
  macros: { grams: number; kcal: number; protein: number; fat: number; carbs: number }
  is_grounded: boolean
  confidence: number
}
export type ScanMealResponse = {
  predictions: ScanPrediction[]
  items: ScanItem[]
  totals: { grams: number; kcal: number; protein: number; fat: number; carbs: number }
}

export function scanMeal(photoPaths: string[], hint?: string): Promise<ScanMealResponse> {
  return invokeEdge<ScanMealResponse>('scan-meal', {
    photo_paths: photoPaths,
    ...(hint ? { hint } : {}),
  })
}

// ---- calculate-goals (onboarding) --------------------------------------------

export type CalculateGoalsRequest = {
  weight_kg: number
  height_cm?: number | null
  age?: number | null
  biological_sex?: string | null
  fitness_goal?: string | null
  training_frequency_per_week?: number | null
  training_types?: string[] | null
  training_intensity?: string | null
  daily_activity_level?: string | null
  // NEAT bucket ('under_7k' | '7k_to_10k' | 'over_10k') + extra endurance load.
  daily_steps?: string | null
  weekly_cardio_sessions?: number | null
}

export type CalculatedGoals = {
  protein_g: number
  calories_kcal: number
  fat_g: number
  carbs_g: number
  water_ml: number
  reasoning: string
}

export function calculateGoals(input: CalculateGoalsRequest): Promise<CalculatedGoals> {
  return invokeEdge<CalculatedGoals>('calculate-goals', input)
}

// ---- generate-checkin-summary --------------------------------------------------

export type CheckinSummaryRequest = {
  avg_protein: number
  avg_calories: number
  avg_fat?: number
  avg_carbs?: number
  avg_water?: number
  nutrition_days?: number
  weight_kg?: number
  feeling_tags?: string[]
  mental_feeling?: string
  physical_feeling?: string
  goals?: { protein_goal?: number; calorie_goal?: number; water_goal?: number }
}

export async function generateCheckinSummary(input: CheckinSummaryRequest): Promise<string> {
  const { summary } = await invokeEdge<{ summary: string }>('generate-checkin-summary', input)
  return summary
}
