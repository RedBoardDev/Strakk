import { parseIntClamped } from '../lib/numbers.ts'
import type { Macros } from '../data/viewTypes.ts'

const FIELDS = [
  { key: 'calories', label: 'kcal', max: 5000, cls: 'text-primary-light' },
  { key: 'protein', label: 'Protein', max: 500, cls: 'text-protein' },
  { key: 'fat', label: 'Fat', max: 500, cls: 'text-fat' },
  { key: 'carbs', label: 'Carbs', max: 700, cls: 'text-carbs' },
] as const

// Editable 4-field macro row (kcal / P / F / C) — shared by the meal editor and
// the AI review item editor. `ariaPrefix` keeps labels unique per context
// ("Meal kcal", "Scrambled eggs kcal", …).
export function MacroInputGrid({
  values,
  onChange,
  ariaPrefix,
  variant = 'surface',
}: {
  values: Macros
  onChange: (values: Macros) => void
  ariaPrefix: string
  variant?: 'surface' | 'inset'
}) {
  const tile =
    variant === 'surface'
      ? 'bg-surface-1 rounded-card px-2.5 py-2'
      : 'bg-surface-2 rounded-[10px] px-2 py-1.5'
  const text = variant === 'surface' ? 'text-[16px]' : 'text-[15px]'

  return (
    <div className="grid grid-cols-4 gap-2">
      {FIELDS.map((field) => (
        <div key={field.key} className={`${tile} flex flex-col gap-0.5`}>
          <span className="text-[10px] text-ink-4">{field.label}</span>
          <input
            value={values[field.key]}
            onChange={(event) =>
              onChange({ ...values, [field.key]: parseIntClamped(event.target.value, field.max) })
            }
            inputMode="numeric"
            aria-label={`${ariaPrefix} ${field.label}`}
            className={`w-full bg-transparent ${text} font-semibold tnum outline-none ${field.cls}`}
          />
        </div>
      ))}
    </div>
  )
}
