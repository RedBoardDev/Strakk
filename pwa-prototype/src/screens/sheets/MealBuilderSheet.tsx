import { useEffect, useState } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import { Sheet } from '../../components/Sheet.tsx'
import { Button } from '../../components/Button.tsx'
import { Stepper } from '../../components/Stepper.tsx'
import { Icon } from '../../components/Icon.tsx'
import { ConfirmDialog } from '../../components/ConfirmDialog.tsx'
import { FoodPickerPanel } from '../../components/FoodPickerPanel.tsx'
import { useToast } from '../../components/Toast.tsx'
import { haptic } from '../../lib/ios.ts'
import { scaleMacros, sumMacros } from '../../lib/macros.ts'
import { useStore } from '../../store.tsx'
import type { CatalogFood } from '../../api/foods.ts'
import type { NewEntryInput } from '../../api/meals.ts'
import type { Macros } from '../../data/mock.ts'

// A composed meal: several foods with adjustable portions, committed to the
// backend as one meal + its entries.
type DraftItem = { id: string; name: string; grams: number; per100: Macros; fromRecent: boolean }

let seq = 0

function itemFromFood(food: CatalogFood): DraftItem {
  seq += 1
  const isRecent = String(food.id).startsWith('recent-')
  const grams = Math.round(food.default_portion_grams) || 100
  const per100: Macros = isRecent
    ? // Recents are absolute per-portion values — treat the portion as 100g so
      // the stepper still scales sensibly.
      { calories: food.calories, protein: food.protein, fat: food.fat ?? 0, carbs: food.carbs ?? 0 }
    : { calories: food.calories, protein: food.protein, fat: food.fat ?? 0, carbs: food.carbs ?? 0 }
  return { id: `${food.id}-${seq}`, name: food.name, grams: isRecent ? 100 : grams, per100, fromRecent: isRecent }
}

function macrosOf(item: DraftItem): Macros {
  return scaleMacros(item.per100, item.grams / 100)
}

function kcalOf(item: DraftItem): number {
  return macrosOf(item).calories
}

function totalMacros(items: DraftItem[]): Macros {
  return sumMacros(items.map(macrosOf))
}

export function MealBuilderSheet({
  open,
  onClose,
  logDate,
}: {
  open: boolean
  onClose: () => void
  logDate?: string
}) {
  const store = useStore()
  const toast = useToast()
  const [name, setName] = useState('New meal')
  const [items, setItems] = useState<DraftItem[]>([])
  const [adding, setAdding] = useState(false)
  const [confirmCancel, setConfirmCancel] = useState(false)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (open) {
      setName('New meal')
      setItems([])
      setAdding(true)
      setConfirmCancel(false)
    }
  }, [open])

  const total = totalMacros(items).calories

  const requestClose = () => {
    if (items.length > 0) setConfirmCancel(true)
    else onClose()
  }

  const save = async () => {
    haptic('medium')
    setSaving(true)
    try {
      const inputs: NewEntryInput[] = items.map((item) => {
        const m = macrosOf(item)
        return {
          name: item.name,
          protein: m.protein,
          calories: m.calories,
          fat: m.fat,
          carbs: m.carbs,
          quantity: `${Math.round(item.grams)}g`,
          source: 'search',
        }
      })
      await store.createMeal(name.trim() || 'New meal', inputs, logDate)
      onClose()
      toast.show(`Meal saved · ${total} kcal`)
    } catch {
      // toasted by the store
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <Sheet
        open={open}
        onClose={requestClose}
        title="New meal"
        detents={['large']}
        footer={
          <Button variant="primary" full glow disabled={items.length === 0 || saving} onClick={() => void save()}>
            {saving ? 'Saving…' : items.length > 0 ? `Save meal · ${total} kcal` : 'Add items to save'}
          </Button>
        }
      >
        <div className="pb-2">
          {/* Editable meal name */}
          <input
            value={name}
            onChange={(event) => setName(event.target.value.slice(0, 60))}
            aria-label="Meal name"
            className="w-full bg-transparent text-[22px] font-bold text-ink outline-none pt-1"
          />
          <div className="text-[13px] text-ink-2 mt-0.5">
            {items.length} {items.length === 1 ? 'item' : 'items'} · {total} kcal
          </div>

          {/* Items */}
          {items.length > 0 && (
            <div className="mt-4 flex flex-col gap-2.5">
              {items.map((item) => (
                <div key={item.id} className="bg-surface-1 rounded-card p-3.5">
                  <div className="flex items-center gap-2.5">
                    <button
                      type="button"
                      onClick={() => {
                        haptic('light')
                        setItems((prev) => prev.filter((it) => it.id !== item.id))
                      }}
                      className="size-7 rounded-full bg-error/15 flex items-center justify-center text-error shrink-0"
                      aria-label={`Remove ${item.name}`}
                    >
                      <Icon name="minus" size={16} />
                    </button>
                    <div className="flex-1 min-w-0">
                      <div className="text-[15px] font-medium text-ink truncate">{item.name}</div>
                      <div className="text-[12px] text-ink-3 tnum">{kcalOf(item)} kcal</div>
                    </div>
                  </div>
                  <div className="mt-2.5 flex justify-end">
                    <Stepper
                      value={item.grams}
                      onChange={(grams) => setItems((prev) => prev.map((it) => (it.id === item.id ? { ...it, grams } : it)))}
                      step={10}
                      min={0}
                      suffix="g"
                    />
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Add-food panel or trigger */}
          <div className="mt-4">
            <AnimatePresence mode="wait" initial={false}>
              {adding ? (
                <motion.div
                  key="panel"
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0 }}
                  transition={{ duration: 0.15 }}
                >
                  <FoodPickerPanel onPick={(food) => setItems((prev) => [...prev, itemFromFood(food)])} onCancel={() => setAdding(false)} />
                </motion.div>
              ) : (
                <motion.button
                  key="trigger"
                  type="button"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  whileTap={{ scale: 0.99 }}
                  onClick={() => {
                    haptic('light')
                    setAdding(true)
                  }}
                  className="w-full h-12 rounded-card border border-dashed border-divider-strong/60 flex items-center justify-center gap-2 text-ink-2 text-[15px] font-medium"
                >
                  <Icon name="plus" size={16} /> Add food
                </motion.button>
              )}
            </AnimatePresence>
          </div>
        </div>
      </Sheet>
      <ConfirmDialog
        open={confirmCancel}
        title="Discard meal?"
        message="The items you added won't be saved."
        confirmLabel="Discard"
        onCancel={() => setConfirmCancel(false)}
        onConfirm={() => {
          setConfirmCancel(false)
          onClose()
        }}
      />
    </>
  )
}
