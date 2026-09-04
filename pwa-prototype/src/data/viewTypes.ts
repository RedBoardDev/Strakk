// Shared view-model types for the Strakk PWA. These describe the shapes the
// screens render; live data is fetched from the backend (see src/api/) and
// adapted into these via src/lib/checkinView.ts.

export type Macros = { calories: number; protein: number; fat: number; carbs: number }

// ---- Check-ins ----
// Mirrors the real CheckIn domain model: weight + 8 body measurements (with
// week-over-week deltas), feeling tags (positive/negative), free-text mental &
// physical feeling, a nutrition summary, weekly training stats, and photos.
export type Measurements = {
  weight: number // kg
  shoulders: number
  chest: number
  armLeft: number
  armRight: number
  waist: number
  hips: number
  thighLeft: number
  thighRight: number
} // all cm except weight

export type FeelingTag = { id: string; label: string; positive: boolean }

export type CheckInNutrition = {
  avgCalories: number
  avgProtein: number
  avgFat: number
  avgCarbs: number
  avgWater: number // ml
  days: number // days logged that week
  aiSummary: string
}

export type CheckInTraining = {
  sessions: number
  durationMin: number
  volumeKg: number
  avgRpe: number
}

export type CheckIn = {
  id: string
  weekLabel: string
  dateRange: string
  createdAt: string
  feeling: 'On track' | 'Strong' | 'Tired'
  adherence: number // 0..1
  measurements: Measurements
  deltas: Partial<Measurements> // vs previous week
  feelingTags: FeelingTag[]
  mentalFeeling: string
  physicalFeeling: string
  nutrition: CheckInNutrition
  training: CheckInTraining
  photoCount: number
}

// ---- Trends / Stats dashboard ("View detailed stats" screen) ----
export type SeriesPoint = { week: string; value: number }
export type MacroCompliance = { name: 'Calories' | 'Protein' | 'Carbs' | 'Fat'; pct: number }

// Field metadata for rendering the measurements grid / form (label + unit + whether "down" is good).
export const MEASUREMENT_FIELDS: { key: keyof Measurements; label: string; unit: string; lowerIsBetter: boolean }[] = [
  { key: 'weight', label: 'Weight', unit: 'kg', lowerIsBetter: true },
  { key: 'waist', label: 'Waist', unit: 'cm', lowerIsBetter: true },
  { key: 'chest', label: 'Chest', unit: 'cm', lowerIsBetter: false },
  { key: 'shoulders', label: 'Shoulders', unit: 'cm', lowerIsBetter: false },
  { key: 'armLeft', label: 'Arm (L)', unit: 'cm', lowerIsBetter: false },
  { key: 'armRight', label: 'Arm (R)', unit: 'cm', lowerIsBetter: false },
  { key: 'hips', label: 'Hips', unit: 'cm', lowerIsBetter: true },
  { key: 'thighLeft', label: 'Thigh (L)', unit: 'cm', lowerIsBetter: false },
  { key: 'thighRight', label: 'Thigh (R)', unit: 'cm', lowerIsBetter: false },
]
