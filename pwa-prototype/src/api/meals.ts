import { supabase } from './supabase.ts'
import type { BreakdownItem } from './edge.ts'

export type EntrySource = 'search' | 'barcode' | 'manual' | 'text_ai' | 'photo_ai' | 'frequent'

// Mirrors meal_entries (subset the UI needs).
export type Entry = {
  id: string
  meal_id: string | null
  log_date: string
  name: string | null
  protein: number
  calories: number
  fat: number | null
  carbs: number | null
  quantity: string | null
  source: EntrySource
  breakdown_json: string | null // stringified BreakdownItem[] (KMP convention)
  photo_path: string | null
  created_at: string
}

export type Meal = {
  id: string
  date: string
  name: string
  created_at: string
  meal_entries: Entry[]
}

const ENTRY_COLUMNS =
  'id, meal_id, log_date, name, protein, calories, fat, carbs, quantity, source, breakdown_json, photo_path, created_at'
const MEAL_EMBED = `id, date, name, created_at, meal_entries(${ENTRY_COLUMNS})`

async function uid(): Promise<string> {
  const { data } = await supabase.auth.getUser()
  const id = data.user?.id
  if (!id) throw new Error('Not signed in')
  return id
}

export function parseBreakdown(entry: Entry): BreakdownItem[] | null {
  if (!entry.breakdown_json) return null
  try {
    const parsed = JSON.parse(entry.breakdown_json) as BreakdownItem[]
    return Array.isArray(parsed) ? parsed : null
  } catch {
    return null
  }
}

// ---- reads -------------------------------------------------------------------

export async function fetchMealsForDate(date: string): Promise<Meal[]> {
  const { data, error } = await supabase
    .from('meals')
    .select(MEAL_EMBED)
    .eq('date', date)
    .order('created_at', { ascending: true })
  if (error) throw new Error(error.message)
  const meals = (data ?? []) as Meal[]
  for (const meal of meals) {
    meal.meal_entries.sort((a, b) => a.created_at.localeCompare(b.created_at))
  }
  return meals
}

export async function fetchOrphanEntriesForDate(date: string): Promise<Entry[]> {
  const { data, error } = await supabase
    .from('meal_entries')
    .select(ENTRY_COLUMNS)
    .eq('log_date', date)
    .is('meal_id', null)
    .order('created_at', { ascending: true })
  if (error) throw new Error(error.message)
  return (data ?? []) as Entry[]
}

// Distinct active dates in a month (meal entries ∪ water entries).
export async function fetchActiveDates(monthStart: string, monthEnd: string): Promise<string[]> {
  const [entries, water] = await Promise.all([
    supabase.from('meal_entries').select('log_date').gte('log_date', monthStart).lte('log_date', monthEnd),
    supabase.from('water_entries').select('log_date').gte('log_date', monthStart).lte('log_date', monthEnd),
  ])
  if (entries.error) throw new Error(entries.error.message)
  if (water.error) throw new Error(water.error.message)
  const dates = new Set<string>()
  for (const row of (entries.data ?? []) as { log_date: string }[]) dates.add(row.log_date)
  for (const row of (water.data ?? []) as { log_date: string }[]) dates.add(String(row.log_date))
  return [...dates].sort()
}

// ---- writes ------------------------------------------------------------------

export type NewEntryInput = {
  name: string | null
  protein: number
  calories: number
  fat?: number | null
  carbs?: number | null
  quantity?: string | null
  source: EntrySource
  breakdown?: BreakdownItem[] | null
  photo_path?: string | null
}

async function entryPayload(input: NewEntryInput, logDate: string, mealId?: string) {
  const payload: Record<string, unknown> = {
    user_id: await uid(),
    log_date: logDate,
    protein: input.protein,
    calories: input.calories,
    source: input.source,
  }
  if (mealId) payload.meal_id = mealId
  if (input.name != null) payload.name = input.name
  if (input.fat != null) payload.fat = input.fat
  if (input.carbs != null) payload.carbs = input.carbs
  if (input.quantity != null) payload.quantity = input.quantity
  // KMP convention: breakdown_json is a *stringified* array inside the jsonb column.
  if (input.breakdown && input.breakdown.length > 0) payload.breakdown_json = JSON.stringify(input.breakdown)
  if (input.photo_path != null) payload.photo_path = input.photo_path
  return payload
}

// Orphan quick-add entry (no meal container).
export async function addOrphanEntry(input: NewEntryInput, logDate: string): Promise<Entry> {
  const { data, error } = await supabase
    .from('meal_entries')
    .insert(await entryPayload(input, logDate))
    .select(ENTRY_COLUMNS)
    .single()
  if (error) throw new Error(error.message)
  return data as Entry
}

// Meal + its entries, with compensation if the entries insert fails.
export async function createMealWithEntries(name: string, logDate: string, inputs: NewEntryInput[]): Promise<Meal> {
  const userId = await uid()
  const { data: mealRow, error: mealError } = await supabase
    .from('meals')
    .insert({ user_id: userId, date: logDate, name: name.slice(0, 60) })
    .select('id, date, name, created_at')
    .single()
  if (mealError) throw new Error(mealError.message)
  const meal = mealRow as Omit<Meal, 'meal_entries'>

  const payloads = await Promise.all(inputs.map((input) => entryPayload(input, logDate, meal.id)))
  const { data: entryRows, error: entriesError } = await supabase
    .from('meal_entries')
    .insert(payloads)
    .select(ENTRY_COLUMNS)
  if (entriesError) {
    await supabase.from('meals').delete().eq('id', meal.id) // compensate the orphan meal
    throw new Error(entriesError.message)
  }
  return { ...meal, meal_entries: (entryRows ?? []) as Entry[] }
}

export async function addEntryToMeal(mealId: string, logDate: string, input: NewEntryInput): Promise<Entry> {
  const { data, error } = await supabase
    .from('meal_entries')
    .insert(await entryPayload(input, logDate, mealId))
    .select(ENTRY_COLUMNS)
    .single()
  if (error) throw new Error(error.message)
  return data as Entry
}

// Exactly the 6 columns the KMP client updates.
export type EntryPatch = {
  name: string | null
  protein: number
  calories: number
  fat: number | null
  carbs: number | null
  quantity: string | null
}

export async function updateEntry(id: string, patch: EntryPatch): Promise<void> {
  const { error } = await supabase.from('meal_entries').update(patch).eq('id', id)
  if (error) throw new Error(error.message)
}

export async function deleteEntry(id: string): Promise<void> {
  const { error } = await supabase.from('meal_entries').delete().eq('id', id)
  if (error) throw new Error(error.message)
}

export async function renameMeal(id: string, name: string): Promise<void> {
  const { error } = await supabase.from('meals').update({ name: name.slice(0, 60) }).eq('id', id)
  if (error) throw new Error(error.message)
}

export async function deleteMeal(id: string): Promise<void> {
  const { error } = await supabase.from('meals').delete().eq('id', id) // cascades entries
  if (error) throw new Error(error.message)
}
