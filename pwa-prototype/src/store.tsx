import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useToast } from './components/Toast.tsx'
import { sumMacros, ZERO_MACROS } from './lib/macros.ts'
import { todayIso } from './lib/dates.ts'
import { supabase } from './api/supabase.ts'
import * as authApi from './api/auth.ts'
import * as mealsApi from './api/meals.ts'
import * as waterApi from './api/water.ts'
import * as checkinsApi from './api/checkins.ts'
import * as profileApi from './api/profile.ts'
import * as foodsApi from './api/foods.ts'
import type { Macros } from './data/mock.ts'

// ============================================================================
// Server-backed app store. Mirrors the KMP repositories: every mutation is
// server-first (await the API, then update the in-memory cache) so the UI is
// always consistent with the database. Errors surface as toasts and reject.
// ============================================================================

export type Goals = Macros & { water: number }

const DEFAULT_GOALS: Goals = { calories: 2200, protein: 160, fat: 70, carbs: 240, water: 2500 }

export type AppState = {
  ready: boolean
  user: { firstName: string; email: string }
  goals: Goals
  today: string
  meals: mealsApi.Meal[]
  orphans: mealsApi.Entry[]
  waterEntries: waterApi.WaterEntry[]
  checkins: checkinsApi.CheckinRow[]
  favoriteFoods: foodsApi.FavoriteFood[]
  hevyConnected: boolean
}

// ---- derived helpers (exported for screens) ---------------------------------

export function entryMacros(entry: mealsApi.Entry): Macros {
  return {
    calories: Math.round(entry.calories),
    protein: Math.round(entry.protein),
    fat: Math.round(entry.fat ?? 0),
    carbs: Math.round(entry.carbs ?? 0),
  }
}

export function mealMacros(meal: mealsApi.Meal): Macros {
  return sumMacros(meal.meal_entries.map(entryMacros))
}

export type TimelineItem =
  | { kind: 'meal'; meal: mealsApi.Meal; createdAt: string }
  | { kind: 'entry'; entry: mealsApi.Entry; createdAt: string }

export function timelineOf(state: AppState): TimelineItem[] {
  const items: TimelineItem[] = [
    ...state.meals.map((meal) => ({ kind: 'meal' as const, meal, createdAt: meal.created_at })),
    ...state.orphans.map((entry) => ({ kind: 'entry' as const, entry, createdAt: entry.created_at })),
  ]
  return items.sort((a, b) => b.createdAt.localeCompare(a.createdAt)) // newest first
}

function goalsFromProfile(profile: profileApi.ProfileRow | null): Goals {
  if (!profile) return DEFAULT_GOALS
  return {
    calories: profile.calorie_goal ?? DEFAULT_GOALS.calories,
    protein: profile.protein_goal ?? DEFAULT_GOALS.protein,
    fat: profile.fat_goal ?? DEFAULT_GOALS.fat,
    carbs: profile.carb_goal ?? DEFAULT_GOALS.carbs,
    water: profile.water_goal ?? DEFAULT_GOALS.water,
  }
}

// ---- store surface ------------------------------------------------------------

type Store = {
  state: AppState
  consumed: Macros
  waterMl: number
  reload: () => Promise<void>
  // water
  addWater: (deltaMl: number) => Promise<void>
  // logging
  addOrphanEntry: (input: mealsApi.NewEntryInput, logDate?: string) => Promise<void>
  createMeal: (name: string, inputs: mealsApi.NewEntryInput[], logDate?: string) => Promise<void>
  addEntryToMeal: (mealId: string, input: mealsApi.NewEntryInput) => Promise<void>
  updateEntry: (id: string, patch: mealsApi.EntryPatch) => Promise<void>
  deleteEntry: (id: string) => Promise<void>
  deleteMeal: (id: string) => Promise<void>
  renameMeal: (id: string, name: string) => Promise<void>
  // check-ins
  createCheckin: (input: checkinsApi.CheckinInput, photos: Blob[]) => Promise<checkinsApi.CheckinRow>
  updateCheckin: (id: string, input: Omit<checkinsApi.CheckinInput, 'week_label' | 'covered_dates'>) => Promise<void>
  deleteCheckin: (checkin: checkinsApi.CheckinRow) => Promise<void>
  attachAiSummary: (id: string, summary: string) => Promise<void>
  // profile
  updateGoals: (goals: Goals) => Promise<void>
  setFirstName: (name: string) => Promise<void>
  setHevyConnected: (connected: boolean) => void
  // favorites
  toggleFavoriteFood: (food: { name: string; protein: number; calories: number; fat?: number | null; carbs?: number | null; quantity?: string | null }) => Promise<void>
  isFavoriteFood: (name: string) => boolean
}

const StoreContext = createContext<Store | null>(null)

export function StoreProvider({ children }: { children: ReactNode }) {
  const toast = useToast()
  const today = useMemo(() => todayIso(), [])
  const [state, setState] = useState<AppState>({
    ready: false,
    user: { firstName: '', email: '' },
    goals: DEFAULT_GOALS,
    today,
    meals: [],
    orphans: [],
    waterEntries: [],
    checkins: [],
    favoriteFoods: [],
    hevyConnected: false,
  })
  const stateRef = useRef(state)
  stateRef.current = state

  const patch = useCallback((partial: Partial<AppState>) => {
    setState((prev) => ({ ...prev, ...partial }))
  }, [])

  // Wraps a mutation: reject → toast the error, rethrow for callers that care.
  const guard = useCallback(
    async <T,>(work: () => Promise<T>): Promise<T> => {
      try {
        return await work()
      } catch (err) {
        toast.show(err instanceof Error ? err.message : 'Something went wrong')
        throw err
      }
    },
    [toast],
  )

  const reload = useCallback(async () => {
    const [{ data: userData }, profile, meals, orphans, water, checkins, favorites] = await Promise.all([
      supabase.auth.getUser(),
      profileApi.fetchProfile().catch(() => null),
      mealsApi.fetchMealsForDate(today).catch(() => []),
      mealsApi.fetchOrphanEntriesForDate(today).catch(() => []),
      waterApi.fetchWaterForDate(today).catch(() => []),
      checkinsApi.fetchCheckins().catch(() => []),
      foodsApi.fetchFavoriteFoods().catch(() => []),
    ])
    const authUser = userData.user
    const email = authUser?.email ?? ''
    const metaName = (authUser?.user_metadata as { first_name?: string } | undefined)?.first_name
    patch({
      ready: true,
      user: { firstName: metaName || email.split('@')[0] || 'You', email },
      goals: goalsFromProfile(profile),
      meals,
      orphans,
      waterEntries: water,
      checkins,
      favoriteFoods: favorites,
    })
  }, [today, patch])

  useEffect(() => {
    void reload()
  }, [reload])

  const store: Store = {
    state,
    consumed: useMemo(
      () =>
        sumMacros([
          ...state.meals.map(mealMacros),
          ...state.orphans.map(entryMacros),
        ]) ?? ZERO_MACROS,
      [state.meals, state.orphans],
    ),
    waterMl: useMemo(() => state.waterEntries.reduce((s, w) => s + w.amount, 0), [state.waterEntries]),
    reload,

    addWater: (deltaMl) =>
      guard(async () => {
        const current = stateRef.current.waterEntries
        if (deltaMl > 0) {
          const row = await waterApi.addWaterEntry(today, deltaMl)
          patch({ waterEntries: [...current, row] })
        } else if (deltaMl < 0) {
          const next = await waterApi.removeWater(today, -deltaMl, current)
          patch({ waterEntries: next })
        }
      }),

    addOrphanEntry: (input, logDate = today) =>
      guard(async () => {
        const row = await mealsApi.addOrphanEntry(input, logDate)
        if (logDate === today) patch({ orphans: [...stateRef.current.orphans, row] })
      }),

    createMeal: (name, inputs, logDate = today) =>
      guard(async () => {
        const meal = await mealsApi.createMealWithEntries(name, logDate, inputs)
        if (logDate === today) patch({ meals: [...stateRef.current.meals, meal] })
      }),

    addEntryToMeal: (mealId, input) =>
      guard(async () => {
        const row = await mealsApi.addEntryToMeal(mealId, today, input)
        patch({
          meals: stateRef.current.meals.map((m) =>
            m.id === mealId ? { ...m, meal_entries: [...m.meal_entries, row] } : m,
          ),
        })
      }),

    updateEntry: (id, entryPatch) =>
      guard(async () => {
        await mealsApi.updateEntry(id, entryPatch)
        const apply = (entry: mealsApi.Entry): mealsApi.Entry =>
          entry.id === id ? { ...entry, ...entryPatch } : entry
        patch({
          meals: stateRef.current.meals.map((m) => ({ ...m, meal_entries: m.meal_entries.map(apply) })),
          orphans: stateRef.current.orphans.map(apply),
        })
      }),

    deleteEntry: (id) =>
      guard(async () => {
        await mealsApi.deleteEntry(id)
        patch({
          meals: stateRef.current.meals.map((m) => ({
            ...m,
            meal_entries: m.meal_entries.filter((e) => e.id !== id),
          })),
          orphans: stateRef.current.orphans.filter((e) => e.id !== id),
        })
      }),

    deleteMeal: (id) =>
      guard(async () => {
        await mealsApi.deleteMeal(id)
        patch({ meals: stateRef.current.meals.filter((m) => m.id !== id) })
      }),

    renameMeal: (id, name) =>
      guard(async () => {
        await mealsApi.renameMeal(id, name)
        patch({ meals: stateRef.current.meals.map((m) => (m.id === id ? { ...m, name } : m)) })
      }),

    createCheckin: (input, photos) =>
      guard(async () => {
        let row = await checkinsApi.createCheckin(input)
        for (const [i, blob] of photos.entries()) {
          const photo = await checkinsApi.uploadCheckinPhoto(row.id, blob, i)
          row = { ...row, checkin_photos: [...row.checkin_photos, photo] }
        }
        patch({ checkins: [row, ...stateRef.current.checkins].sort((a, b) => b.week_label.localeCompare(a.week_label)) })
        return row
      }),

    updateCheckin: (id, input) =>
      guard(async () => {
        const row = await checkinsApi.updateCheckin(id, input)
        patch({ checkins: stateRef.current.checkins.map((c) => (c.id === id ? row : c)) })
      }),

    deleteCheckin: (checkin) =>
      guard(async () => {
        await checkinsApi.deleteCheckin(checkin)
        patch({ checkins: stateRef.current.checkins.filter((c) => c.id !== checkin.id) })
      }),

    attachAiSummary: (id, summary) =>
      guard(async () => {
        await checkinsApi.setAiSummary(id, summary)
        patch({
          checkins: stateRef.current.checkins.map((c) => (c.id === id ? { ...c, ai_summary: summary } : c)),
        })
      }),

    updateGoals: (goals) =>
      guard(async () => {
        await profileApi.updateGoals(goals)
        patch({ goals })
      }),

    setFirstName: (name) =>
      guard(async () => {
        await authApi.updateFirstName(name)
        patch({ user: { ...stateRef.current.user, firstName: name } })
      }),

    setHevyConnected: (connected) => patch({ hevyConnected: connected }),

    toggleFavoriteFood: (food) =>
      guard(async () => {
        const normalized = foodsApi.normalizeFoodName(food.name)
        const existing = stateRef.current.favoriteFoods.find((f) => f.name_normalized === normalized)
        if (existing) {
          await foodsApi.removeFavoriteFood(normalized)
          patch({ favoriteFoods: stateRef.current.favoriteFoods.filter((f) => f.name_normalized !== normalized) })
        } else {
          await foodsApi.addFavoriteFood(food)
          const favorites = await foodsApi.fetchFavoriteFoods().catch(() => stateRef.current.favoriteFoods)
          patch({ favoriteFoods: favorites })
        }
      }),

    isFavoriteFood: (name) =>
      state.favoriteFoods.some((f) => f.name_normalized === foodsApi.normalizeFoodName(name)),
  }

  return <StoreContext.Provider value={store}>{children}</StoreContext.Provider>
}

export function useStore(): Store {
  const ctx = useContext(StoreContext)
  if (!ctx) throw new Error('useStore must be used within StoreProvider')
  return ctx
}
