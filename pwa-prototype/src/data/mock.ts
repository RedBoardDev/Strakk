// Mock data for the Strakk PWA prototype — no backend, purely illustrative.

export type Macros = { calories: number; protein: number; fat: number; carbs: number }
export type MealType = 'Breakfast' | 'Lunch' | 'Snack' | 'Dinner'

export type MealEntry = {
  id: string
  type: MealType
  time: string
  title: string
  macros: Macros
  items: string[]
  source: 'photo' | 'search' | 'barcode' | 'manual' | 'ai'
}

export const goals: Macros & { water: number } = {
  calories: 2200,
  protein: 160,
  fat: 70,
  carbs: 240,
  water: 2500,
}

export const meals: MealEntry[] = [
  {
    id: 'm1',
    type: 'Breakfast',
    time: '08:24',
    title: 'Greek yogurt, berries & granola',
    macros: { calories: 420, protein: 32, fat: 11, carbs: 52 },
    items: ['Greek yogurt 0% — 200g', 'Blueberries — 80g', 'Granola — 40g'],
    source: 'photo',
  },
  {
    id: 'm2',
    type: 'Lunch',
    time: '12:46',
    title: 'Chicken rice bowl',
    macros: { calories: 640, protein: 52, fat: 16, carbs: 78 },
    items: ['Chicken breast — 180g', 'Basmati rice — 150g', 'Avocado — 40g', 'Olive oil — 8g'],
    source: 'search',
  },
  {
    id: 'm3',
    type: 'Snack',
    time: '16:10',
    title: 'Protein shake & banana',
    macros: { calories: 280, protein: 30, fat: 4, carbs: 34 },
    items: ['Whey protein — 30g', 'Banana — 120g', 'Almond milk — 250ml'],
    source: 'barcode',
  },
  {
    id: 'm4',
    type: 'Dinner',
    time: '20:02',
    title: 'Salmon, quinoa & greens',
    macros: { calories: 500, protein: 28, fat: 23, carbs: 46 },
    items: ['Salmon fillet — 150g', 'Quinoa — 120g', 'Broccoli — 150g'],
    source: 'manual',
  },
]

export const consumed: Macros = meals.reduce(
  (acc, m) => ({
    calories: acc.calories + m.macros.calories,
    protein: acc.protein + m.macros.protein,
    fat: acc.fat + m.macros.fat,
    carbs: acc.carbs + m.macros.carbs,
  }),
  { calories: 0, protein: 0, fat: 0, carbs: 0 },
)

export const water = { current: 1500, goal: goals.water } // ml

export const user = { firstName: 'Thomas', streak: 12, isPro: true }

// ---- Food search ----
export type Food = {
  id: string
  name: string
  brand?: string
  per100: Macros
  serving: { label: string; grams: number }
  verified?: boolean
}

export const recentFoods: Food[] = [
  { id: 'f1', name: 'Chicken breast, grilled', per100: { calories: 165, protein: 31, fat: 3.6, carbs: 0 }, serving: { label: '1 fillet', grams: 120 }, verified: true },
  { id: 'f2', name: 'Banana', per100: { calories: 89, protein: 1.1, fat: 0.3, carbs: 23 }, serving: { label: '1 medium', grams: 118 }, verified: true },
  { id: 'f3', name: 'Whey protein, vanilla', brand: 'MyProtein', per100: { calories: 392, protein: 80, fat: 7, carbs: 6 }, serving: { label: '1 scoop', grams: 30 } },
]

export const searchResults: Food[] = [
  { id: 's1', name: 'Greek yogurt 0%', brand: 'Fage', per100: { calories: 57, protein: 10, fat: 0, carbs: 4 }, serving: { label: '1 pot', grams: 170 }, verified: true },
  { id: 's2', name: 'Oats, rolled', per100: { calories: 379, protein: 13, fat: 6.5, carbs: 67 }, serving: { label: '1 serving', grams: 40 }, verified: true },
  { id: 's3', name: 'Almond butter', brand: 'Meridian', per100: { calories: 614, protein: 25, fat: 55, carbs: 7 }, serving: { label: '1 tbsp', grams: 15 } },
  { id: 's4', name: 'Brown rice, cooked', per100: { calories: 123, protein: 2.7, fat: 1, carbs: 26 }, serving: { label: '1 cup', grams: 195 }, verified: true },
  { id: 's5', name: 'Eggs, whole', per100: { calories: 143, protein: 13, fat: 9.5, carbs: 0.7 }, serving: { label: '1 large', grams: 50 }, verified: true },
  { id: 's6', name: 'Peanut M&Ms', brand: 'Mars', per100: { calories: 504, protein: 9.2, fat: 26, carbs: 59 }, serving: { label: '1 pack', grams: 45 } },
]

// Barcode-scanned product (the feature users doubt a PWA can do)
export const scannedProduct: Food = {
  id: 'b1',
  name: 'Protein Bar — Cookies & Cream',
  brand: 'Barebells',
  per100: { calories: 350, protein: 35, fat: 13, carbs: 26 },
  serving: { label: '1 bar', grams: 55 },
  verified: true,
}

// ---- Calendar ----
export type DayLog = { date: number; kcal: number; goal: number } // day-of-month
export const calendarDays: DayLog[] = Array.from({ length: 30 }, (_, i) => {
  const variance = [0.62, 0.78, 1.04, 0.95, 0.88, 1.12, 0.7, 0.99, 0.83, 1.0][i % 10]
  return { date: i + 1, kcal: Math.round(goals.calories * variance), goal: goals.calories }
})

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

const TAG = (id: string, label: string, positive: boolean): FeelingTag => ({ id, label, positive })

export const checkIns: CheckIn[] = [
  {
    id: 'c1',
    weekLabel: 'Week 24',
    dateRange: 'Jun 17 – Jun 23',
    createdAt: 'Jun 23, 2026',
    feeling: 'On track',
    adherence: 0.91,
    measurements: { weight: 78.4, shoulders: 121, chest: 103.5, armLeft: 38.4, armRight: 38.6, waist: 83.2, hips: 97.5, thighLeft: 58.2, thighRight: 58.4 },
    deltas: { weight: -0.6, chest: 0.3, armLeft: 0.2, armRight: 0.1, waist: -0.9, hips: -0.4, thighLeft: 0.2, thighRight: 0.1, shoulders: 0.2 },
    feelingTags: [TAG('good_energy', 'Good energy', true), TAG('motivated', 'Motivated', true), TAG('strong_training', 'Strong training', true), TAG('good_sleep', 'Slept well', true), TAG('light_body', 'Light body', true)],
    mentalFeeling: 'Felt sharp and consistent all week, motivation high going into the weekend.',
    physicalFeeling: 'Strength up on pulls, recovery solid. Minor tightness in left hip on leg day.',
    nutrition: { avgCalories: 2180, avgProtein: 168, avgFat: 66, avgCarbs: 232, avgWater: 2600, days: 7, aiSummary: 'Protein on point every day and a clean −0.6 kg. Carbs dipped slightly on rest days — nudge them up around training to protect output.' },
    training: { sessions: 4, durationMin: 312, volumeKg: 48250, avgRpe: 8.1 },
    photoCount: 3,
  },
  {
    id: 'c2',
    weekLabel: 'Week 23',
    dateRange: 'Jun 10 – Jun 16',
    createdAt: 'Jun 16, 2026',
    feeling: 'Strong',
    adherence: 0.86,
    measurements: { weight: 79.0, shoulders: 120.8, chest: 103.2, armLeft: 38.2, armRight: 38.5, waist: 84.1, hips: 97.9, thighLeft: 58.0, thighRight: 58.3 },
    deltas: { weight: -0.4, chest: 0.1, armLeft: 0.1, waist: -0.5, hips: -0.2, thighRight: 0.1 },
    feelingTags: [TAG('disciplined', 'Disciplined', true), TAG('focused', 'Focused', true), TAG('good_recovery', 'Good recovery', true), TAG('hungry', 'Hungry', false)],
    mentalFeeling: 'Locked in on work and training. A bit of evening hunger but managed it.',
    physicalFeeling: 'Best bench week in a while. Sleep good, no joint issues.',
    nutrition: { avgCalories: 2240, avgProtein: 162, avgFat: 70, avgCarbs: 240, avgWater: 2400, days: 6, aiSummary: 'Strong adherence and a new strength PR. Evening hunger lines up with low-protein lunches — front-load protein earlier.' },
    training: { sessions: 4, durationMin: 298, volumeKg: 46100, avgRpe: 7.8 },
    photoCount: 2,
  },
  {
    id: 'c3',
    weekLabel: 'Week 22',
    dateRange: 'Jun 3 – Jun 9',
    createdAt: 'Jun 9, 2026',
    feeling: 'Tired',
    adherence: 0.74,
    measurements: { weight: 79.4, shoulders: 120.6, chest: 103.1, armLeft: 38.1, armRight: 38.4, waist: 84.6, hips: 98.1, thighLeft: 57.9, thighRight: 58.2 },
    deltas: { weight: -0.8, waist: -0.6, armRight: -0.1, thighLeft: -0.1 },
    feelingTags: [TAG('tired', 'Tired', false), TAG('poor_sleep', 'Slept poorly', false), TAG('stress', 'Stress', false), TAG('sore', 'Sore', false), TAG('low_motivation', 'Low motivation', false)],
    mentalFeeling: 'Rough week — work stress and poor sleep dragged motivation down.',
    physicalFeeling: 'Legs felt heavy, DOMS lingered. Skipped one session to recover.',
    nutrition: { avgCalories: 2090, avgProtein: 148, avgFat: 64, avgCarbs: 214, avgWater: 2100, days: 5, aiSummary: 'Sleep and stress hit adherence and energy. Weight still dropped — hold calories steady and prioritise sleep over extra training this week.' },
    training: { sessions: 3, durationMin: 214, volumeKg: 33800, avgRpe: 8.6 },
    photoCount: 2,
  },
  {
    id: 'c4',
    weekLabel: 'Week 21',
    dateRange: 'May 27 – Jun 2',
    createdAt: 'Jun 2, 2026',
    feeling: 'On track',
    adherence: 0.88,
    measurements: { weight: 80.2, shoulders: 120.4, chest: 103.0, armLeft: 38.0, armRight: 38.4, waist: 85.2, hips: 98.3, thighLeft: 58.0, thighRight: 58.3 },
    deltas: { weight: -0.5, waist: -0.4, chest: 0.2 },
    feelingTags: [TAG('energy_stable', 'Stable energy', true), TAG('good_mood', 'Good mood', true), TAG('disciplined', 'Disciplined', true)],
    mentalFeeling: 'Steady, good baseline week. Felt in control of food choices.',
    physicalFeeling: 'Consistent energy across sessions, no niggles.',
    nutrition: { avgCalories: 2210, avgProtein: 158, avgFat: 68, avgCarbs: 236, avgWater: 2500, days: 6, aiSummary: 'Solid, unremarkable-in-a-good-way week. Everything trending the right direction — keep doing this.' },
    training: { sessions: 4, durationMin: 305, volumeKg: 45200, avgRpe: 7.9 },
    photoCount: 3,
  },
]

// Weight series for the trend curve (oldest → newest).
export const weightTrend = [81.2, 80.7, 80.2, 79.4, 79.0, 78.4]

// ---- Trends / Stats dashboard ("View detailed stats" screen) ----
export type SeriesPoint = { week: string; value: number }
export type MacroCompliance = { name: 'Calories' | 'Protein' | 'Carbs' | 'Fat'; pct: number }

export const weightSeries: SeriesPoint[] = [
  { week: 'W15', value: 80.6 }, { week: 'W16', value: 80.3 }, { week: 'W17', value: 80.1 },
  { week: 'W18', value: 79.9 }, { week: 'W19', value: 79.7 }, { week: 'W20', value: 79.5 },
  { week: 'W21', value: 79.2 }, { week: 'W22', value: 79.0 }, { week: 'W23', value: 78.9 },
  { week: 'W24', value: 78.7 }, { week: 'W25', value: 78.6 }, { week: 'W26', value: 78.4 },
]

export const volumeSeries: SeriesPoint[] = [
  { week: 'W15', value: 9800 }, { week: 'W16', value: 10250 }, { week: 'W17', value: 10800 },
  { week: 'W18', value: 11200 }, { week: 'W19', value: 10950 }, { week: 'W20', value: 11500 },
  { week: 'W21', value: 11850 }, { week: 'W22', value: 12100 }, { week: 'W23', value: 11700 },
  { week: 'W24', value: 12400 }, { week: 'W25', value: 12050 }, { week: 'W26', value: 12600 },
]

export const checkInStats = {
  weightKg: 78.4,
  weightDelta: -1.7, // over the period (loss = good)
  avgWeeklyVolumeKg: 11850,
  sessionsPerWeek: 3.8,
  nutritionCompliance: 86, // %
  weightSeries,
  volumeSeries,
  training: { totalSessions: 45, totalDurationMin: 3120, totalVolumeKg: 142200 },
  macros: [
    { name: 'Calories', pct: 92 },
    { name: 'Protein', pct: 88 },
    { name: 'Carbs', pct: 79 },
    { name: 'Fat', pct: 84 },
  ] as MacroCompliance[],
  water: { avg: 2600, goal: 3000 }, // ml
  consistency: { weeks: 11, total: 12 },
}

// Older check-ins gated behind Pro (history-limit banner on the list).
export const hiddenCheckInCount = 8

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
