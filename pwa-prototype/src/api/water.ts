import { supabase } from './supabase.ts'

export type WaterEntry = { id: string; log_date: string; amount: number; created_at: string }

async function uid(): Promise<string> {
  const { data } = await supabase.auth.getUser()
  const id = data.user?.id
  if (!id) throw new Error('Not signed in')
  return id
}

export async function fetchWaterForDate(date: string): Promise<WaterEntry[]> {
  const { data, error } = await supabase
    .from('water_entries')
    .select('id, log_date, amount, created_at')
    .eq('log_date', date)
    .order('created_at', { ascending: true })
  if (error) throw new Error(error.message)
  return (data ?? []) as WaterEntry[]
}

export async function addWaterEntry(date: string, amountMl: number): Promise<WaterEntry> {
  const { data, error } = await supabase
    .from('water_entries')
    .insert({ user_id: await uid(), log_date: date, amount: amountMl })
    .select('id, log_date, amount, created_at')
    .single()
  if (error) throw new Error(error.message)
  return data as WaterEntry
}

// Rows only allow positive amounts, so removal deletes newest entries until the
// requested amount is covered, re-inserting any overshoot as a fresh row.
export async function removeWater(date: string, amountMl: number, entries: WaterEntry[]): Promise<WaterEntry[]> {
  let remaining = amountMl
  const next = [...entries]
  while (remaining > 0 && next.length > 0) {
    const last = next[next.length - 1]
    const { error } = await supabase.from('water_entries').delete().eq('id', last.id)
    if (error) throw new Error(error.message)
    next.pop()
    remaining -= last.amount
  }
  if (remaining < 0) {
    const compensation = await addWaterEntry(date, -remaining)
    next.push(compensation)
  }
  return next
}
