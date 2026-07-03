import { createContext, useContext } from 'react'
import type { CatalogFood } from './api/foods.ts'
import type { Entry, Meal } from './api/meals.ts'
import type { TabKey } from './components/TabBar.tsx'

// Active modal flow presented over the tab content. One at a time — selecting in
// one sheet replaces it with the next (add → search → food detail), which reads
// as an iOS push within a modal. `logDate` (yyyy-MM-dd) scopes logging flows to
// a specific day (calendar "Add for this day"); omitted = today.
export type Flow =
  | { kind: 'add'; logDate?: string }
  | { kind: 'mealBuilder'; logDate?: string }
  | { kind: 'search'; logDate?: string }
  | { kind: 'scan'; logDate?: string }
  | { kind: 'manual'; logDate?: string }
  | { kind: 'quickAdd'; logDate?: string }
  | { kind: 'photoMeal'; logDate?: string }
  | { kind: 'foodDetail'; food: CatalogFood; from?: 'search' | 'scan'; logDate?: string }
  | { kind: 'mealDetail'; meal: Meal }
  | { kind: 'entryDetail'; entry: Entry }

export type Nav = {
  open: (flow: Flow) => void
  close: () => void
  setTab: (tab: TabKey) => void
  signOut: () => void
}

export const NavContext = createContext<Nav | null>(null)

export function useNav(): Nav {
  const ctx = useContext(NavContext)
  if (!ctx) throw new Error('useNav must be used within NavContext')
  return ctx
}
