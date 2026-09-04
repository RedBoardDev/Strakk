import { supabase } from './supabase.ts'

export type CheckinPhoto = { id: string; checkin_id: string; storage_path: string; position: number }

export type CheckinRow = {
  id: string
  week_label: string
  covered_dates: string[]
  weight_kg: number | null
  shoulders_cm: number | null
  chest_cm: number | null
  arm_left_cm: number | null
  arm_right_cm: number | null
  waist_cm: number | null
  hips_cm: number | null
  thigh_left_cm: number | null
  thigh_right_cm: number | null
  feeling_tags: string[] | null
  mental_feeling: string | null
  physical_feeling: string | null
  avg_protein: number | null
  avg_calories: number | null
  avg_fat: number | null
  avg_carbs: number | null
  avg_water: number | null
  nutrition_days: number | null
  ai_summary: string | null
  training_stats: TrainingStats | null
  created_at: string
  checkin_photos: CheckinPhoto[]
}

export type TrainingStats = {
  total_sessions: number
  total_duration_minutes: number
  total_volume_kg: number
  avg_rpe: number | null
  muscle_group_volume: Record<string, number>
}

const COLUMNS = '*, checkin_photos(*)'

async function uid(): Promise<string> {
  const { data } = await supabase.auth.getUser()
  const id = data.user?.id
  if (!id) throw new Error('Not signed in')
  return id
}

export async function fetchCheckins(): Promise<CheckinRow[]> {
  const { data, error } = await supabase
    .from('checkins')
    .select(COLUMNS)
    .order('week_label', { ascending: false })
  if (error) throw new Error(error.message)
  return (data ?? []) as CheckinRow[]
}

export type CheckinInput = {
  week_label: string
  covered_dates: string[]
  measurements: Partial<
    Pick<
      CheckinRow,
      | 'weight_kg'
      | 'shoulders_cm'
      | 'chest_cm'
      | 'arm_left_cm'
      | 'arm_right_cm'
      | 'waist_cm'
      | 'hips_cm'
      | 'thigh_left_cm'
      | 'thigh_right_cm'
    >
  >
  feeling_tags: string[]
  mental_feeling: string | null
  physical_feeling: string | null
  nutrition?: {
    avg_protein: number
    avg_calories: number
    avg_fat: number
    avg_carbs: number
    avg_water: number
    nutrition_days: number
    ai_summary?: string | null
  }
  training_stats?: TrainingStats | null
}

export async function createCheckin(input: CheckinInput): Promise<CheckinRow> {
  const payload: Record<string, unknown> = {
    user_id: await uid(),
    week_label: input.week_label,
    covered_dates: input.covered_dates,
    ...input.measurements,
    feeling_tags: input.feeling_tags,
    mental_feeling: input.mental_feeling,
    physical_feeling: input.physical_feeling,
    ...(input.nutrition ?? {}),
    ...(input.training_stats ? { training_stats: input.training_stats } : {}),
  }
  const { data, error } = await supabase.from('checkins').insert(payload).select(COLUMNS).single()
  if (error) {
    if (error.message.includes('unique_user_week')) {
      throw new Error('A check-in already exists for this week.')
    }
    throw new Error(error.message)
  }
  return data as CheckinRow
}

export async function updateCheckin(id: string, input: Omit<CheckinInput, 'week_label' | 'covered_dates'>): Promise<CheckinRow> {
  const payload: Record<string, unknown> = {
    // Explicit nulls clear absent measurements (KMP convention).
    weight_kg: input.measurements.weight_kg ?? null,
    shoulders_cm: input.measurements.shoulders_cm ?? null,
    chest_cm: input.measurements.chest_cm ?? null,
    arm_left_cm: input.measurements.arm_left_cm ?? null,
    arm_right_cm: input.measurements.arm_right_cm ?? null,
    waist_cm: input.measurements.waist_cm ?? null,
    hips_cm: input.measurements.hips_cm ?? null,
    thigh_left_cm: input.measurements.thigh_left_cm ?? null,
    thigh_right_cm: input.measurements.thigh_right_cm ?? null,
    feeling_tags: input.feeling_tags,
    mental_feeling: input.mental_feeling,
    physical_feeling: input.physical_feeling,
    ...(input.nutrition ?? {}),
  }
  const { data, error } = await supabase.from('checkins').update(payload).eq('id', id).select(COLUMNS).single()
  if (error) throw new Error(error.message)
  return data as CheckinRow
}

export async function deleteCheckin(checkin: CheckinRow): Promise<void> {
  const paths = checkin.checkin_photos.map((p) => p.storage_path)
  const { error } = await supabase.from('checkins').delete().eq('id', checkin.id)
  if (error) throw new Error(error.message)
  if (paths.length > 0) await supabase.storage.from('checkin-photos').remove(paths) // best effort
}

export async function setAiSummary(id: string, summary: string): Promise<void> {
  const { error } = await supabase.from('checkins').update({ ai_summary: summary }).eq('id', id)
  if (error) throw new Error(error.message)
}

// ---- photos --------------------------------------------------------------------

export async function uploadCheckinPhoto(checkinId: string, blob: Blob, position: number): Promise<CheckinPhoto> {
  const userId = await uid()
  const path = `${userId}/${checkinId}/${crypto.randomUUID()}.jpg`
  const { error: upErr } = await supabase.storage
    .from('checkin-photos')
    .upload(path, blob, { contentType: 'image/jpeg' })
  if (upErr) throw new Error(upErr.message)
  const { data, error } = await supabase
    .from('checkin_photos')
    .insert({ checkin_id: checkinId, storage_path: path, position })
    .select('id, checkin_id, storage_path, position')
    .single()
  if (error) throw new Error(error.message)
  return data as CheckinPhoto
}

export async function deleteCheckinPhoto(photo: CheckinPhoto): Promise<void> {
  await supabase.storage.from('checkin-photos').remove([photo.storage_path]) // best effort
  const { error } = await supabase.from('checkin_photos').delete().eq('id', photo.id)
  if (error) throw new Error(error.message)
}

export async function signedPhotoUrl(storagePath: string): Promise<string | null> {
  const { data } = await supabase.storage.from('checkin-photos').createSignedUrl(storagePath, 3600)
  return data?.signedUrl ?? null
}

// ---- weekly nutrition summary (computed client-side, like the KMP app) ----------

export type NutritionSummary = {
  avg_protein: number
  avg_calories: number
  avg_fat: number
  avg_carbs: number
  avg_water: number
  nutrition_days: number
  top_foods: string[]
  protein_per_day: number[]
  days_with_water: number
}

export async function computeNutritionSummary(coveredDates: string[]): Promise<NutritionSummary | null> {
  const [entriesRes, waterRes] = await Promise.all([
    supabase.from('meal_entries').select('log_date, name, protein, calories, fat, carbs').in('log_date', coveredDates),
    supabase.from('water_entries').select('log_date, amount').in('log_date', coveredDates),
  ])
  if (entriesRes.error || waterRes.error) return null
  const entries = (entriesRes.data ?? []) as { log_date: string; name: string | null; protein: number; calories: number; fat: number | null; carbs: number | null }[]
  const water = (waterRes.data ?? []) as { log_date: string; amount: number }[]
  if (entries.length === 0) return null

  const byDate = new Map<string, { protein: number; calories: number; fat: number; carbs: number }>()
  const nameCounts = new Map<string, number>()
  for (const e of entries) {
    const day = byDate.get(e.log_date) ?? { protein: 0, calories: 0, fat: 0, carbs: 0 }
    day.protein += e.protein
    day.calories += e.calories
    day.fat += e.fat ?? 0
    day.carbs += e.carbs ?? 0
    byDate.set(e.log_date, day)
    if (e.name) nameCounts.set(e.name, (nameCounts.get(e.name) ?? 0) + 1)
  }
  const waterByDate = new Map<string, number>()
  for (const w of water) {
    const key = String(w.log_date)
    waterByDate.set(key, (waterByDate.get(key) ?? 0) + w.amount)
  }

  const days = [...byDate.values()]
  const n = days.length
  const avg = (pick: (d: (typeof days)[number]) => number) => Math.round(days.reduce((s, d) => s + pick(d), 0) / n)
  const waterTotals = [...waterByDate.values()]
  return {
    avg_protein: avg((d) => d.protein),
    avg_calories: avg((d) => d.calories),
    avg_fat: avg((d) => d.fat),
    avg_carbs: avg((d) => d.carbs),
    avg_water: waterTotals.length > 0 ? Math.round(waterTotals.reduce((s, v) => s + v, 0) / waterTotals.length) : 0,
    nutrition_days: n,
    top_foods: [...nameCounts.entries()].sort((a, b) => b[1] - a[1]).slice(0, 8).map(([name]) => name),
    protein_per_day: [...byDate.entries()].sort((a, b) => a[0].localeCompare(b[0])).map(([, d]) => Math.round(d.protein)),
    days_with_water: waterTotals.length,
  }
}
