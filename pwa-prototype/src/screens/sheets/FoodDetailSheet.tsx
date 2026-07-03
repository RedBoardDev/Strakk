import { useState } from 'react'
import { motion } from 'motion/react'
import { Sheet } from '../../components/Sheet.tsx'
import { Stepper } from '../../components/Stepper.tsx'
import { Button } from '../../components/Button.tsx'
import { AnimatedNumber } from '../../components/AnimatedNumber.tsx'
import { Icon } from '../../components/Icon.tsx'
import { useToast } from '../../components/Toast.tsx'
import { haptic } from '../../lib/ios.ts'
import { useStore } from '../../store.tsx'
import type { CatalogFood } from '../../api/foods.ts'
import type { Macros } from '../../data/viewTypes.ts'

// Macro rows, in the spec's order, each tinted with its accent.
const MACRO_ROWS: { key: keyof Macros; label: string; unit: string; cls: string }[] = [
  { key: 'protein', label: 'Protein', unit: 'g', cls: 'text-protein' },
  { key: 'calories', label: 'Calories', unit: 'kcal', cls: 'text-primary-light' },
  { key: 'fat', label: 'Fat', unit: 'g', cls: 'text-fat' },
  { key: 'carbs', label: 'Carbs', unit: 'g', cls: 'text-carbs' },
]

const Overline = ({ children }: { children: string }) => (
  <div className="text-[11px] font-semibold uppercase tracking-[0.06em] text-ink-3 mb-3">{children}</div>
)

export function FoodDetailSheet({
  open,
  onClose,
  food,
  logDate,
}: {
  open: boolean
  onClose: () => void
  food: CatalogFood | null
  logDate?: string
}) {
  const store = useStore()
  const toast = useToast()
  const [saving, setSaving] = useState(false)

  // Portion in grams — reset to the food's default serving when the food changes.
  const [grams, setGrams] = useState(100)
  const [trackedId, setTrackedId] = useState<string | number | null>(null)
  if (food && food.id !== trackedId) {
    setTrackedId(food.id)
    setGrams(Math.round(food.default_portion_grams) || 100)
  }

  const scale = grams / 100
  const per100: Macros = food
    ? { calories: food.calories, protein: food.protein, fat: food.fat ?? 0, carbs: food.carbs ?? 0 }
    : { calories: 0, protein: 0, fat: 0, carbs: 0 }
  const isFav = food ? store.isFavoriteFood(food.name) : false

  const addToLog = async () => {
    if (!food || saving) return
    haptic('medium')
    setSaving(true)
    try {
      await store.addOrphanEntry(
        {
          name: food.name,
          protein: Math.round(food.protein * scale * 10) / 10,
          calories: Math.round(food.calories * scale),
          fat: food.fat != null ? Math.round(food.fat * scale * 10) / 10 : null,
          carbs: food.carbs != null ? Math.round(food.carbs * scale * 10) / 10 : null,
          quantity: `${grams}g`,
          source: food.barcode ? 'barcode' : 'search',
        },
        logDate,
      )
      onClose()
      toast.show(`Added · ${Math.round(food.calories * scale)} kcal`)
    } catch {
      // error already toasted by the store
    } finally {
      setSaving(false)
    }
  }

  return (
    <Sheet
      open={open}
      onClose={onClose}
      title=""
      detents={['medium', 'large']}
      footer={
        food && (
          <Button variant="primary" full glow disabled={saving} onClick={() => void addToLog()}>
            <Icon name="plus" size={17} /> {saving ? 'Adding…' : 'Add to log'}
          </Button>
        )
      }
    >
      {food && (
        <div className="pb-2">
          {/* Header */}
          <div className="pt-1 pb-6 flex items-start gap-3">
            <div className="flex-1 min-w-0">
              <h2 className="text-[22px] font-bold text-ink leading-tight">{food.name}</h2>
              <div className="flex flex-wrap items-center gap-x-2 gap-y-1.5 mt-2">
                {food.brand && <span className="text-[13px] text-ink-2">{food.brand}</span>}
                {food.nutriscore && (
                  <span className="inline-flex items-center gap-1 rounded-full bg-success/15 px-2 py-0.5 text-[11px] font-semibold text-success uppercase">
                    Nutri-score {food.nutriscore}
                  </span>
                )}
              </div>
            </div>
            <motion.button
              type="button"
              whileTap={{ scale: 0.85 }}
              transition={{ duration: 0.1 }}
              onClick={() => {
                haptic('light')
                void store
                  .toggleFavoriteFood({
                    name: food.name,
                    protein: Math.round(food.protein * scale * 10) / 10,
                    calories: Math.round(food.calories * scale),
                    fat: food.fat != null ? Math.round(food.fat * scale * 10) / 10 : null,
                    carbs: food.carbs != null ? Math.round(food.carbs * scale * 10) / 10 : null,
                    quantity: `${grams}g`,
                  })
                  .catch(() => {})
              }}
              className={`size-11 -mr-2 flex items-center justify-center shrink-0 ${isFav ? 'text-primary' : 'text-ink-3'}`}
              aria-label={isFav ? 'Remove favorite' : 'Favorite food'}
            >
              <Icon name="heart" size={20} fill={isFav ? 'currentColor' : 'none'} />
            </motion.button>
          </div>

          {/* Portion */}
          <div className="pb-6">
            <Overline>Portion</Overline>
            <div className="flex flex-col items-center gap-2.5">
              <Stepper value={grams} onChange={setGrams} step={10} min={5} suffix="g" />
              <div className="text-[12px] text-ink-3 tnum">
                {food.serving_label ? `${food.serving_label} = ` : 'Default portion = '}
                {Math.round(food.default_portion_grams)}g
              </div>
            </div>
          </div>

          {/* Macros for the chosen portion */}
          <div className="pb-2">
            <Overline>Per this portion</Overline>
            <div className="bg-surface-1 rounded-card overflow-hidden divide-y divide-divider/70">
              {MACRO_ROWS.map((row) => (
                <div key={row.key} className="flex items-center px-4 py-3">
                  <span className="text-[15px] text-ink">{row.label}</span>
                  <div className="flex-1" />
                  <span className={`text-[15px] font-semibold tnum ${row.cls}`}>
                    <AnimatedNumber value={per100[row.key] * scale} duration={0.4} />
                    <span className="text-[12px] ml-0.5 opacity-80">{row.unit}</span>
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </Sheet>
  )
}
