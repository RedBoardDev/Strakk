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
import type { Food, Macros } from '../../data/mock.ts'

// Macro rows, in the spec's order, each tinted with its accent.
const MACRO_ROWS: { key: keyof Macros; label: string; unit: string; cls: string }[] = [
  { key: 'protein', label: 'Protein', unit: 'g', cls: 'text-protein' },
  { key: 'calories', label: 'Calories', unit: 'kcal', cls: 'text-primary-light' },
  { key: 'fat', label: 'Fat', unit: 'g', cls: 'text-fat' },
  { key: 'carbs', label: 'Carbs', unit: 'g', cls: 'text-carbs' },
]

const Overline = ({ children }: { children: string }) => (
  <div className="text-[11px] font-bold uppercase tracking-wider text-ink-3 mb-3">{children}</div>
)

export function FoodDetailSheet({
  open,
  onClose,
  food,
}: {
  open: boolean
  onClose: () => void
  food: Food | null
}) {
  // Portion in grams. Reset to the food's default serving whenever the food
  // changes (the sheet stays mounted, so we adjust state during render rather
  // than relying on the initial value).
  const [grams, setGrams] = useState(100)
  const [trackedId, setTrackedId] = useState<string | null>(null)
  if (food && food.id !== trackedId) {
    setTrackedId(food.id)
    setGrams(food.serving.grams)
  }

  const scale = grams / 100
  const { state, dispatch } = useStore()
  const toast = useToast()
  const isFav = food ? state.favoriteFoodIds.includes(food.id) : false

  const addToLog = () => {
    if (!food) return
    haptic('medium')
    const macros = {
      calories: Math.round(food.per100.calories * scale),
      protein: Math.round(food.per100.protein * scale),
      fat: Math.round(food.per100.fat * scale),
      carbs: Math.round(food.per100.carbs * scale),
    }
    dispatch({
      kind: 'meal/add',
      meal: {
        title: food.name,
        macros,
        items: [`${food.name} — ${grams}g`],
        // Mock convention: scanned products carry a 'b' id prefix.
        source: food.id.startsWith('b') ? 'barcode' : 'search',
      },
    })
    onClose()
    toast.show(`Added · ${macros.calories} kcal`)
  }

  return (
    <Sheet
      open={open}
      onClose={onClose}
      title=""
      detents={['medium', 'large']}
      footer={
        food && (
          <Button variant="primary" full glow onClick={addToLog}>
            <Icon name="plus" size={17} /> Add to log
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
              {(food.brand || food.verified) && (
                <div className="flex flex-wrap items-center gap-x-2 gap-y-1.5 mt-2">
                  {food.brand && <span className="text-[13px] text-ink-2">{food.brand}</span>}
                  {food.verified && (
                    <span className="inline-flex items-center gap-1 rounded-full bg-success/15 px-2 py-0.5 text-[11px] font-semibold text-success">
                      <Icon name="check" size={10} /> Verified
                    </span>
                  )}
                </div>
              )}
            </div>
            <motion.button
              type="button"
              whileTap={{ scale: 0.85 }}
              transition={{ duration: 0.1 }}
              onClick={() => {
                haptic('light')
                dispatch({ kind: 'food/toggleFavorite', id: food.id })
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
                {food.serving.label} = {food.serving.grams}g
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
                    <AnimatedNumber value={food.per100[row.key] * scale} duration={0.4} />
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
