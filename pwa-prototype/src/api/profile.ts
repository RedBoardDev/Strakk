import { supabase } from './supabase.ts'

// profiles — one row per user, auto-created by the signup trigger.
export type ProfileRow = {
  id: string
  protein_goal: number | null
  calorie_goal: number | null
  fat_goal: number | null
  carb_goal: number | null
  water_goal: number | null
  weight_kg: number | null
  height_cm: number | null
  birth_date: string | null
  biological_sex: 'male' | 'female' | 'unspecified' | null
  fitness_goal: 'lose_fat' | 'gain_muscle' | 'maintain' | 'just_track' | null
  training_frequency: number | null
  training_types: string[] | null
  training_intensity: 'light' | 'moderate' | 'intense' | null
  daily_activity_level: 'sedentary' | 'moderately_active' | 'very_active' | null
  onboarding_completed: boolean
}

const PROFILE_COLUMNS =
  'id, protein_goal, calorie_goal, fat_goal, carb_goal, water_goal, weight_kg, height_cm, birth_date, ' +
  'biological_sex, fitness_goal, training_frequency, training_types, training_intensity, ' +
  'daily_activity_level, onboarding_completed'

export async function fetchProfile(): Promise<ProfileRow | null> {
  const { data, error } = await supabase.from('profiles').select(PROFILE_COLUMNS).maybeSingle()
  if (error) throw new Error(error.message)
  return data as ProfileRow | null
}

export async function updateProfile(patch: Partial<Omit<ProfileRow, 'id'>>): Promise<void> {
  const { data: userData } = await supabase.auth.getUser()
  const uid = userData.user?.id
  if (!uid) throw new Error('Not signed in')
  const { error } = await supabase.from('profiles').update(patch).eq('id', uid)
  if (error) throw new Error(error.message)
}

export type Goals = { calories: number; protein: number; fat: number; carbs: number; water: number }

export function updateGoals(goals: Goals): Promise<void> {
  return updateProfile({
    calorie_goal: Math.round(goals.calories),
    protein_goal: Math.round(goals.protein),
    fat_goal: Math.round(goals.fat),
    carb_goal: Math.round(goals.carbs),
    water_goal: Math.round(goals.water),
  })
}

// ---- subscription (read-only; Pro = trial/active/payment_failed) -------------

export async function fetchIsPro(): Promise<boolean> {
  const { data, error } = await supabase.from('subscriptions').select('status').maybeSingle()
  if (error) return true // fail open client-side; server enforces anyway
  const status = (data as { status?: string } | null)?.status
  return status === 'trial' || status === 'active' || status === 'payment_failed'
}
