import type { Transition } from 'motion/react'

// iOS UIKit-matching spring presets. Tuned to feel like SwiftUI's default
// interactive springs (snappy, low overshoot) rather than bouncy web springs.
export const spring = {
  // Sheet present / dismiss — like .sheet on iOS
  sheet: { type: 'spring', stiffness: 380, damping: 38, mass: 1 } as Transition,
  // Quick interactive (press, toggle)
  snappy: { type: 'spring', stiffness: 600, damping: 32 } as Transition,
  // Page push / pop
  page: { type: 'spring', stiffness: 340, damping: 36 } as Transition,
  // Gentle settle (rings, bars)
  gentle: { type: 'spring', stiffness: 220, damping: 30 } as Transition,
} as const

// Press feedback: scale 0.97 ~100ms (DESIGN.md §6)
export const pressable = {
  whileTap: { scale: 0.97 },
  transition: { duration: 0.1 },
} as const

// Visual + (when available) real haptic feedback. iOS Safari has no Vibration
// API, so on iOS this degrades to the visual press only — which is exactly the
// nuance worth demoing.
type HapticKind = 'light' | 'medium' | 'success' | 'error'

const patterns: Record<HapticKind, number | number[]> = {
  light: 8,
  medium: 14,
  success: [10, 30, 12],
  error: [20, 40, 20],
}

export function haptic(kind: HapticKind = 'light'): void {
  if (typeof navigator !== 'undefined' && 'vibrate' in navigator) {
    navigator.vibrate(patterns[kind])
  }
}
