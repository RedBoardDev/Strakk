import { createContext, useContext } from 'react'
import type { Food, MealEntry } from './data/mock.ts'
import type { TabKey } from './components/TabBar.tsx'

// Active modal flow presented over the tab content. One at a time — selecting in
// one sheet replaces it with the next (add → search → food detail), which reads
// as an iOS push within a modal for the prototype.
export type Flow =
  | { kind: 'add' }
  | { kind: 'mealBuilder' }
  | { kind: 'search' }
  | { kind: 'scan' }
  | { kind: 'manual' }
  | { kind: 'quickAdd' }
  | { kind: 'photoMeal' }
  | { kind: 'foodDetail'; food: Food; from?: 'search' | 'scan' }
  | { kind: 'mealDetail'; meal: MealEntry }

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
