import { supabase } from './supabase.ts'
import { invokeEdge } from './edge.ts'

// Catalog item — per-100g macros, as returned by both the `search_food_catalog`
// RPC and the `search-off-live` Edge Function.
export type CatalogFood = {
  id: number | string
  source: string
  name: string
  brand: string | null
  protein: number
  calories: number
  fat: number | null
  carbs: number | null
  default_portion_grams: number
  serving_label: string | null
  nutriscore: string | null
  nova_group: number | null
  barcode: string | null
  image_url: string | null
  rank: number
}

async function uid(): Promise<string> {
  const { data } = await supabase.auth.getUser()
  const id = data.user?.id
  if (!id) throw new Error('Not signed in')
  return id
}

// ---- search: local catalog RPC + OFF live, merged (live first, deduped) ------

async function searchLocal(q: string, limit: number): Promise<CatalogFood[]> {
  const { data, error } = await supabase.rpc('search_food_catalog', { q, lim: limit })
  if (error) return []
  return (data ?? []) as CatalogFood[]
}

async function searchLive(q: string, limit: number): Promise<CatalogFood[]> {
  try {
    const { items } = await invokeEdge<{ items: CatalogFood[] }>('search-off-live', { q, limit })
    return items
  } catch {
    return [] // OFF being down must never break search
  }
}

export async function searchFoods(q: string, limit = 20): Promise<CatalogFood[]> {
  const query = q.trim()
  if (query.length < 2) return []
  const [live, local] = await Promise.all([searchLive(query, limit), searchLocal(query, limit)])
  const merged: CatalogFood[] = []
  const seenBarcodes = new Set<string>()
  const seenNames = new Set<string>()
  for (const item of [...live, ...local]) {
    const nameKey = `${item.name.toLowerCase()}|${(item.brand ?? '').toLowerCase()}`
    if (item.barcode && seenBarcodes.has(item.barcode)) continue
    if (seenNames.has(nameKey)) continue
    if (item.barcode) seenBarcodes.add(item.barcode)
    seenNames.add(nameKey)
    merged.push(item)
    if (merged.length >= limit) break
  }
  return merged
}

export async function lookupBarcode(barcode: string): Promise<CatalogFood | null> {
  const { data, error } = await supabase
    .from('food_catalog')
    .select(
      'id, source, name, brand, protein, calories, fat, carbs, default_portion_grams, serving_label, nutriscore, nova_group, barcode, image_url',
    )
    .eq('barcode', barcode)
    .eq('is_active', true)
    .limit(1)
    .maybeSingle()
  if (error || !data) return null
  return { ...(data as Omit<CatalogFood, 'rank'>), rank: 1 }
}

// ---- favorites ----------------------------------------------------------------

export type FavoriteFood = {
  id: string
  name: string
  name_normalized: string
  protein: number
  calories: number
  fat: number | null
  carbs: number | null
  quantity: string | null
}

// KMP normalization: lowercase + French accent folding + trim.
export function normalizeFoodName(name: string): string {
  return name
    .trim()
    .toLowerCase()
    .replace(/[àâä]/g, 'a')
    .replace(/[éèêë]/g, 'e')
    .replace(/[îï]/g, 'i')
    .replace(/[ôö]/g, 'o')
    .replace(/[ùûü]/g, 'u')
    .replace(/ç/g, 'c')
}

export async function fetchFavoriteFoods(): Promise<FavoriteFood[]> {
  const { data, error } = await supabase
    .from('favorite_foods')
    .select('id, name, name_normalized, protein, calories, fat, carbs, quantity')
    .order('created_at', { ascending: false })
  if (error) throw new Error(error.message)
  return (data ?? []) as FavoriteFood[]
}

export async function addFavoriteFood(food: {
  name: string
  protein: number
  calories: number
  fat?: number | null
  carbs?: number | null
  quantity?: string | null
}): Promise<void> {
  const name = food.name.trim()
  const { error } = await supabase.from('favorite_foods').upsert(
    {
      user_id: await uid(),
      name,
      name_normalized: normalizeFoodName(name),
      protein: food.protein,
      calories: food.calories,
      ...(food.fat != null ? { fat: food.fat } : {}),
      ...(food.carbs != null ? { carbs: food.carbs } : {}),
      ...(food.quantity != null ? { quantity: food.quantity } : {}),
    },
    { onConflict: 'user_id,name_normalized' },
  )
  if (error) throw new Error(error.message)
}

export async function removeFavoriteFood(normalizedName: string): Promise<void> {
  const { error } = await supabase.from('favorite_foods').delete().eq('name_normalized', normalizedName)
  if (error) throw new Error(error.message)
}

// ---- recents (RPCs; failures degrade to empty) ---------------------------------

export type RecentFood = {
  name_normalized: string
  name: string
  protein: number
  calories: number
  fat: number | null
  carbs: number | null
  quantity: string | null
}

export async function fetchRecentFoods(): Promise<RecentFood[]> {
  const { data, error } = await supabase.rpc('recent_foods_v1', { days_window: 60, max_rows: 30 })
  if (error) return []
  return (data ?? []) as RecentFood[]
}
