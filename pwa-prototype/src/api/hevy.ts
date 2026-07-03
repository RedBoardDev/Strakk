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

// Aggregates a week's Hevy workouts into the check-in's training_stats shape:
// session count, total duration & volume, mean RPE, and per-muscle-group volume.
export function aggregateTrainingStats(workouts: HevyWorkout[]): {
  total_sessions: number
  total_duration_minutes: number
  total_volume_kg: number
  avg_rpe: number | null
  muscle_group_volume: Record<string, number>
} {
  let totalDuration = 0
  let totalVolume = 0
  let rpeSum = 0
  let rpeCount = 0
  const muscleVolume: Record<string, number> = {}

  for (const workout of workouts) {
    totalDuration += workout.duration_minutes
    totalVolume += workout.total_volume_kg
    for (const exercise of workout.exercises) {
      for (const set of exercise.sets) {
        if (set.rpe != null) {
          rpeSum += set.rpe
          rpeCount += 1
        }
        const volume = (set.weight_kg ?? 0) * (set.reps ?? 0)
        if (volume > 0) muscleVolume[exercise.muscle_group] = (muscleVolume[exercise.muscle_group] ?? 0) + volume
      }
    }
  }

  return {
    total_sessions: workouts.length,
    total_duration_minutes: totalDuration,
    total_volume_kg: Math.round(totalVolume),
    avg_rpe: rpeCount > 0 ? Math.round((rpeSum / rpeCount) * 10) / 10 : null,
    muscle_group_volume: muscleVolume,
  }
}
