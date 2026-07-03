import { invokeEdge } from './edge.ts'
import { supabase } from './supabase.ts'

// ---- Hevy API key (stored server-side in Vault via RPCs) ---------------------

export async function saveHevyApiKey(plainKey: string): Promise<void> {
  const { error } = await supabase.rpc('save_hevy_api_key', { plain_key: plainKey })
  if (error) throw new Error(error.message)
}

export async function getHevyApiKey(): Promise<string | null> {
  const { data, error } = await supabase.rpc('get_hevy_api_key')
  if (error) throw new Error(error.message)
  return (data as string | null) ?? null
}

// ---- fetch-hevy-workouts ------------------------------------------------------

export type HevySet = {
  type: string
  weight_kg: number | null
  reps: number | null
  duration_seconds: number | null
  rpe: number | null
}
export type HevyExercise = {
  name: string
  muscle_group: string
  superset_id: number | null
  sets: HevySet[]
}
export type HevyWorkout = {
  id: string
  title: string
  date: string
  duration_minutes: number
  total_volume_kg: number
  exercises: HevyExercise[]
}

export async function fetchHevyWorkouts(startDate: string, endDate: string): Promise<HevyWorkout[]> {
  const { workouts } = await invokeEdge<{ workouts: HevyWorkout[] }>('fetch-hevy-workouts', {
    start_date: startDate,
    end_date: endDate,
  })
  return workouts
}
