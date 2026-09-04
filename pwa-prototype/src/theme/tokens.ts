// Raw token values for use in JS/SVG (rings, charts) where Tailwind classes
// don't reach. Mirror of DESIGN.md §2.
export const colors = {
  bg: '#050918',
  bgElevated: '#080D1F',
  surface1: '#10162F',
  surface2: '#151B38',
  surface3: '#1A2142',
  ink: '#F4F6FF',
  ink2: '#9CA1B8',
  ink3: '#6F748C',
  primary: '#FF7A3D',
  primaryLight: '#FF9A55',
  protein: '#34C7B5',
  water: '#4B8DFF',
  fat: '#FFC84D',
  carbs: '#637CFF',
  success: '#4DAE6A',
  error: '#E05252',
} as const

// Macro accent palette inside cards/charts (DESIGN.md §5 Macro card).
export const macroColor = {
  protein: colors.protein,
  calories: colors.primaryLight,
  fat: colors.fat,
  carbs: colors.carbs,
} as const

export type Macro = keyof typeof macroColor
