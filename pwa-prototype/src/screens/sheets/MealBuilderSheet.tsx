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
import type { Food, Macros } from '../../data/mock.ts'

// A composed meal: several food items with adjustable portions. Mirrors the
// native multi-item meal draft (start → add items → review → confirm).
type DraftItem = { id: string; name: string; grams: number; per100: Macros }

let seq = 0

function itemFromFood(food: Food): DraftItem {
  seq += 1
  return { id: `${food.id}-${seq}`, name: food.name, grams: food.serving.grams, per100: food.per100 }
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


export function MealBuilderSheet({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { dispatch } = useStore()
  const toast = useToast()
  const [name, setName] = useState('New meal')
  const [items, setItems] = useState<DraftItem[]>([])
  const [adding, setAdding] = useState(false)
  const [confirmCancel, setConfirmCancel] = useState(false)

  useEffect(() => {
    if (open) {
      setName('New meal')
      setItems([])
      setAdding(true)
      setConfirmCancel(false)
    }
  }, [open])

  const total = items.reduce((sum, item) => sum + kcalOf(item), 0)

  const requestClose = () => {
    if (items.length > 0) setConfirmCancel(true)
    else onClose()
  }

  return (
    <>
      <Sheet
        open={open}
        onClose={requestClose}
        title="New meal"
        detents={['large']}
        footer={
          <Button
            variant="primary"
            full
            glow
            disabled={items.length === 0}
            onClick={() => {
              haptic('medium')
              dispatch({
                kind: 'meal/add',
                meal: {
                  title: name.trim() || 'New meal',
                  macros: totalMacros(items),
                  items: items.map((item) => `${item.name} — ${Math.round(item.grams)}g`),
                  source: 'search',
                },
              })
              onClose()
              toast.show(`Meal saved · ${total} kcal`)
            }}
          >
            {items.length > 0 ? `Save meal · ${total} kcal` : 'Add items to save'}
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
