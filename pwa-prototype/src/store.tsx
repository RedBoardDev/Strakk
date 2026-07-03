import { createContext, useContext, useEffect, useMemo, useReducer, type ReactNode } from 'react'
import { sumMacros } from './lib/macros.ts'
import {
  calendarDays as seedCalendarDays,
  checkIns as seedCheckIns,
  goals as seedGoals,
  meals as seedMeals,
  user as seedUser,
  water as seedWater,
  type CheckIn,
  type Macros,
  type MealEntry,
  type MealType,
} from './data/mock.ts'

// ============================================================================
// App store — the single mutable state behind every screen. Shapes mirror the
// future API contracts, so "wiring the API" later means swapping the reducer
// internals for network calls; screens don't change.
// ============================================================================

export type AppState = {
  // isPro stays in the model (quota logic lives server-side) but every account
  // is Pro for now — no payment UI anywhere in the app.
  user: { firstName: string; streak: number; isPro: boolean }
  goals: Macros & { water: number }
  waterMl: number
  meals: MealEntry[]
  favoriteMealIds: string[]
  favoriteFoodIds: string[]
  checkIns: CheckIn[]
  integrations: { hevy: boolean }
}

const INITIAL: AppState = {
  user: seedUser,
  goals: seedGoals,
  waterMl: seedWater.current,
  meals: seedMeals,
  favoriteMealIds: [],
  favoriteFoodIds: ['f1', 'f3'],
  checkIns: seedCheckIns,
  integrations: { hevy: true },
}

// ---- actions ---------------------------------------------------------------

export type NewMeal = {
  type?: MealType
  title: string
  macros: Macros
  items: string[]
  source: MealEntry['source']
}

type Action =
  | { kind: 'water/add'; deltaMl: number }
  | { kind: 'meal/add'; meal: NewMeal }
  | { kind: 'meal/update'; id: string; patch: Partial<Pick<MealEntry, 'items' | 'macros' | 'title' | 'type'>> }
  | { kind: 'meal/delete'; id: string }
  | { kind: 'meal/toggleFavorite'; id: string }
  | { kind: 'food/toggleFavorite'; id: string }
  | { kind: 'checkin/add'; checkIn: CheckIn }
  | { kind: 'checkin/update'; id: string; patch: Partial<CheckIn> }
  | { kind: 'checkin/delete'; id: string }
  | { kind: 'goals/update'; goals: AppState['goals'] }
  | { kind: 'user/setPro'; isPro: boolean }
  | { kind: 'user/setName'; firstName: string }
  | { kind: 'integration/toggle'; name: keyof AppState['integrations'] }
  | { kind: 'reset' }

let mealSeq = 100

function nowTime(): string {
  const now = new Date()
  return `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
}

function inferMealType(): MealType {
  const hour = new Date().getHours()
  if (hour < 11) return 'Breakfast'
  if (hour < 15) return 'Lunch'
  if (hour < 18) return 'Snack'
  return 'Dinner'
}

function toggle(list: string[], id: string): string[] {
  return list.includes(id) ? list.filter((x) => x !== id) : [...list, id]
}

function reducer(state: AppState, action: Action): AppState {
  switch (action.kind) {
    case 'water/add': {
      const waterMl = Math.max(0, Math.min(8000, state.waterMl + action.deltaMl))
      return { ...state, waterMl }
    }
    case 'meal/add': {
      mealSeq += 1
      const entry: MealEntry = {
        id: `m${mealSeq}`,
        type: action.meal.type ?? inferMealType(),
        time: nowTime(),
        title: action.meal.title,
        macros: action.meal.macros,
        items: action.meal.items,
        source: action.meal.source,
      }
      // Newest first (Today renders reverse-chronological).
      return { ...state, meals: [entry, ...state.meals] }
    }
    case 'meal/update':
      return {
        ...state,
        meals: state.meals.map((meal) => (meal.id === action.id ? { ...meal, ...action.patch } : meal)),
      }
    case 'meal/delete':
      return { ...state, meals: state.meals.filter((meal) => meal.id !== action.id) }
    case 'meal/toggleFavorite':
      return { ...state, favoriteMealIds: toggle(state.favoriteMealIds, action.id) }
    case 'food/toggleFavorite':
      return { ...state, favoriteFoodIds: toggle(state.favoriteFoodIds, action.id) }
    case 'checkin/add':
      return { ...state, checkIns: [action.checkIn, ...state.checkIns] }
    case 'checkin/update':
      return {
        ...state,
        checkIns: state.checkIns.map((checkIn) =>
          checkIn.id === action.id ? { ...checkIn, ...action.patch } : checkIn,
        ),
      }
    case 'checkin/delete':
      return { ...state, checkIns: state.checkIns.filter((checkIn) => checkIn.id !== action.id) }
    case 'goals/update':
      return { ...state, goals: action.goals }
    case 'user/setPro':
      return { ...state, user: { ...state.user, isPro: action.isPro } }
    case 'user/setName':
      return { ...state, user: { ...state.user, firstName: action.firstName } }
    case 'integration/toggle':
      return {
        ...state,
        integrations: { ...state.integrations, [action.name]: !state.integrations[action.name] },
      }
    case 'reset':
      return INITIAL
    default:
      return state
  }
}

// ---- persistence -----------------------------------------------------------

// Bump when the seed/shape changes so stale saved state doesn't shadow it.
const STORAGE_KEY = 'strakk-proto-state-v2'

function hydrate(): AppState {
  if (typeof window === 'undefined') return INITIAL
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) return INITIAL
    const saved = JSON.parse(raw) as AppState
    // Shallow sanity check; anything off falls back to the seed.
    if (!Array.isArray(saved.meals) || !Array.isArray(saved.checkIns) || !saved.goals) return INITIAL
    return { ...INITIAL, ...saved }
  } catch {
    return INITIAL
  }
}

// ---- context ---------------------------------------------------------------

type Store = {
  state: AppState
  consumed: Macros
  dispatch: (action: Action) => void
}

const StoreContext = createContext<Store | null>(null)

export function StoreProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(reducer, undefined, hydrate)

  useEffect(() => {
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
    } catch {
      // Storage full/unavailable — state simply won't survive a reload.
    }
  }, [state])

  const consumed = useMemo(() => sumMacros(state.meals.map((meal) => meal.macros)), [state.meals])

  return <StoreContext.Provider value={{ state, consumed, dispatch }}>{children}</StoreContext.Provider>
}

export function useStore(): Store {
  const ctx = useContext(StoreContext)
  if (!ctx) throw new Error('useStore must be used within StoreProvider')
  return ctx
}

// Calendar day logs stay seed-only (past days are read-only in the prototype).
export const calendarDays = seedCalendarDays
