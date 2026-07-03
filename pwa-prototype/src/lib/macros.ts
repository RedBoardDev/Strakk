import type { Macros } from '../data/viewTypes.ts'

// Shared macro arithmetic — every total/rescale in the app goes through here so
// displayed totals always match what gets saved.
export const ZERO_MACROS: Macros = { calories: 0, protein: 0, fat: 0, carbs: 0 }

export function addMacros(a: Macros, b: Macros): Macros {
  return {
    calories: a.calories + b.calories,
    protein: a.protein + b.protein,
    fat: a.fat + b.fat,
    carbs: a.carbs + b.carbs,
  }
}

export function sumMacros(list: Macros[]): Macros {
  return list.reduce(addMacros, ZERO_MACROS)
}

// Scales and rounds, clamped to zero — used when a portion size changes.
export function scaleMacros(macros: Macros, ratio: number): Macros {
  return {
    calories: Math.max(0, Math.round(macros.calories * ratio)),
    protein: Math.max(0, Math.round(macros.protein * ratio)),
    fat: Math.max(0, Math.round(macros.fat * ratio)),
    carbs: Math.max(0, Math.round(macros.carbs * ratio)),
  }
}
