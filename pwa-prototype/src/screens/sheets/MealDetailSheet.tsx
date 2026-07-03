import { useEffect, useState } from 'react'
import { motion } from 'motion/react'
import { Sheet } from '../../components/Sheet.tsx'
import { MacroGrid } from '../../components/MacroCard.tsx'
import { Button } from '../../components/Button.tsx'
import { Stepper } from '../../components/Stepper.tsx'
import { ConfirmDialog } from '../../components/ConfirmDialog.tsx'
import { Icon } from '../../components/Icon.tsx'
import { MacroInputGrid } from '../../components/MacroInputGrid.tsx'
import { useToast } from '../../components/Toast.tsx'
import { haptic } from '../../lib/ios.ts'
import { scaleMacros } from '../../lib/macros.ts'
import { useStore } from '../../store.tsx'
import type { Macros, MealEntry } from '../../data/mock.ts'

// Items are stored as "Name — 200g"; split the trailing quantity off the name,
// then parse the leading number + unit so the quantity can be edited.
type EditItem = { id: string; name: string; qty: number; unit: string; raw: string }

function parseItems(items: string[]): EditItem[] {
  return items.map((item, index) => {
    const dash = item.indexOf('—')
    const name = dash === -1 ? item.trim() : item.slice(0, dash).trim()
    const qtyStr = dash === -1 ? '' : item.slice(dash + 1).trim()
    const num = parseFloat(qtyStr)
    const unit = qtyStr.replace(/[\d.\s]/g, '') || 'g'
    return { id: `${index}-${name}`, name, qty: Number.isNaN(num) ? NaN : num, unit, raw: qtyStr }
  })
}

function qtyLabel(item: EditItem): string {
  return Number.isNaN(item.qty) ? item.raw : `${Math.round(item.qty)}${item.unit}`
}

// Total known grams across items (NaN quantities don't count).
function totalGrams(items: EditItem[]): number {
  return items.reduce((sum, item) => (Number.isNaN(item.qty) ? sum : sum + item.qty), 0)
}

function itemsToStrings(items: EditItem[]): string[] {
  return items.map((item) => {
    const qty = Number.isNaN(item.qty) ? item.raw : `${Math.round(item.qty)}${item.unit}`
    return qty ? `${item.name} — ${qty}` : item.name
  })
}

function FavoriteHeart({ mealId }: { mealId: string }) {
  const { state, dispatch } = useStore()
  const fav = state.favoriteMealIds.includes(mealId)
  return (
    <motion.button
      type="button"
      whileTap={{ scale: 0.85 }}
      transition={{ duration: 0.1 }}
      onClick={() => {
        haptic('light')
        dispatch({ kind: 'meal/toggleFavorite', id: mealId })
      }}
      className={`size-11 -mr-2 flex items-center justify-center shrink-0 ${fav ? 'text-primary' : 'text-ink-3'}`}
      aria-label={fav ? 'Remove favorite meal' : 'Favorite meal'}
    >
      <Icon name="heart" size={20} fill={fav ? 'currentColor' : 'none'} />
    </motion.button>
  )
}

function MealHeader({ meal, count }: { meal: MealEntry; count: number }) {
  return (
    <div className="pt-1 pb-6 flex items-start gap-3">
      <div className="flex-1 min-w-0">
        <h2 className="text-[22px] font-bold text-ink leading-tight">{meal.type}</h2>
        <div className="flex items-center gap-1.5 mt-1.5 text-[12px] text-ink-2">
          <Icon name="clock" size={12} className="text-ink-3" />
          <span className="tnum">{meal.time}</span>
          <span className="text-ink-4">·</span>
          <span>
            {count} {count > 1 ? 'items' : 'item'}
          </span>
        </div>
        <div className="text-[13px] text-ink-3 mt-1 truncate">{meal.title}</div>
      </div>
      <FavoriteHeart mealId={meal.id} />
    </div>
  )
}

// Read-only meal view.
function MealBody({ meal, items }: { meal: MealEntry; items: EditItem[] }) {
  return (
    <div className="pb-2">
      <MealHeader meal={meal} count={items.length} />
      <div className="pb-6">
        <MacroGrid
          protein={{ consumed: meal.macros.protein }}
          calories={{ consumed: meal.macros.calories }}
          fat={{ consumed: meal.macros.fat }}
          carbs={{ consumed: meal.macros.carbs }}
        />
      </div>
      {items.length > 0 && (
        <div className="pb-2">
          <div className="text-[11px] font-bold uppercase tracking-wider text-ink-3 mb-2">Items</div>
          <div className="bg-surface-1 rounded-card overflow-hidden">
            {items.map((item, i) => (
              <div
                key={item.id}
                className={`flex items-center gap-3 px-4 py-3 ${i > 0 ? 'border-t border-divider/70' : ''}`}
              >
                <div className="size-6 rounded-full bg-surface-3 flex items-center justify-center shrink-0">
                  <span className="text-[11px] font-semibold text-ink-3 tnum">{i + 1}</span>
                </div>
                <span className="flex-1 min-w-0 text-[15px] text-ink truncate">{item.name}</span>
                <span className="text-[13px] text-ink-3 tnum shrink-0">{qtyLabel(item)}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

// Edit view — adjust quantities, fix macros directly, or remove items.
function MealEditBody({
  items,
  macros,
  onChangeQty,
  onChangeMacros,
  onRemove,
}: {
  items: EditItem[]
  macros: Macros
  onChangeQty: (id: string, qty: number) => void
  onChangeMacros: (macros: Macros) => void
  onRemove: (id: string) => void
}) {
  return (
    <div className="pb-2">
      <div className="pt-1 pb-5">
        <h2 className="text-[22px] font-bold text-ink leading-tight">Edit meal</h2>
        <div className="text-[13px] text-ink-2 mt-1">Adjust quantities, fix macros, or remove items.</div>
      </div>

      {/* Macros — editable; quantities rescale them automatically, manual edits win. */}
      <div className="pb-5">
        <div className="text-[11px] font-bold uppercase tracking-wider text-ink-3 mb-2">Macros</div>
        <MacroInputGrid values={macros} onChange={onChangeMacros} ariaPrefix="Meal" />
      </div>
      {items.length === 0 ? (
        <div className="py-12 text-center text-[14px] text-ink-3">No items left — close to discard.</div>
      ) : (
        <div className="flex flex-col gap-2.5">
          {items.map((item) => (
            <div key={item.id} className="bg-surface-1 rounded-card p-3.5">
              <div className="flex items-center gap-2.5">
                <button
                  type="button"
                  onClick={() => {
                    haptic('light')
                    onRemove(item.id)
                  }}
                  className="size-7 rounded-full bg-error/15 flex items-center justify-center text-error shrink-0"
                  aria-label={`Remove ${item.name}`}
                >
                  <Icon name="minus" size={16} />
                </button>
                <span className="flex-1 min-w-0 text-[15px] font-medium text-ink truncate">{item.name}</span>
              </div>
              {!Number.isNaN(item.qty) && (
                <div className="mt-2.5 flex justify-end">
                  <Stepper
                    value={item.qty}
                    onChange={(value) => onChangeQty(item.id, value)}
                    step={10}
                    min={0}
                    suffix={item.unit}
                  />
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export function MealDetailSheet({
  open,
  onClose,
  meal: mealProp,
}: {
  open: boolean
  onClose: () => void
  meal: MealEntry | null
}) {
  const { state, dispatch } = useStore()
  const toast = useToast()
  // Always render the LIVE meal (the prop is a snapshot taken at open time and
  // goes stale after an edit).
  const meal = mealProp ? (state.meals.find((m) => m.id === mealProp.id) ?? mealProp) : null

  const [editing, setEditing] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [items, setItems] = useState<EditItem[]>([])
  const [editMacros, setEditMacros] = useState<Macros>({ calories: 0, protein: 0, fat: 0, carbs: 0 })

  // Quantity changes rescale the macros from the meal's original values; the
  // fields stay directly editable so the user can fix any number by hand.
  const applyItems = (next: EditItem[]) => {
    setItems(next)
    if (meal) {
      const before = totalGrams(parseItems(meal.items))
      const after = totalGrams(next)
      setEditMacros(scaleMacros(meal.macros, before > 0 ? after / before : 1))
    }
  }

  // Reset to a clean read-only view every time a meal is presented.
  useEffect(() => {
    if (open && mealProp) {
      setItems(parseItems(mealProp.items))
      setEditing(false)
      setConfirmDelete(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, mealProp])

  const changeQty = (id: string, qty: number) =>
    applyItems(items.map((it) => (it.id === id ? { ...it, qty } : it)))
  const removeItem = (id: string) => applyItems(items.filter((it) => it.id !== id))

  const footer = meal
    ? editing
      ? (
        <div className="flex gap-3">
          <Button
            variant="secondary"
            full
            onClick={() => {
              haptic('light')
              setItems(parseItems(meal.items))
              setEditing(false)
            }}
          >
            Cancel
          </Button>
          <Button
            variant="primary"
            full
            onClick={() => {
              haptic('medium')
              dispatch({
                kind: 'meal/update',
                id: meal.id,
                patch: { items: itemsToStrings(items), macros: editMacros },
              })
              setEditing(false)
              toast.show('Meal updated')
            }}
          >
            Save
          </Button>
        </div>
      )
      : (
        <div className="flex gap-3">
          <Button
            variant="secondary"
            full
            onClick={() => {
              setEditMacros(meal.macros)
              setEditing(true)
            }}
          >
            <Icon name="pencil" size={16} /> Edit
          </Button>
          <motion.button
            type="button"
            whileTap={{ scale: 0.97 }}
            transition={{ duration: 0.1 }}
            onClick={() => {
              haptic('light')
              setConfirmDelete(true)
            }}
            className="h-[52px] w-[52px] rounded-card bg-surface-2 flex items-center justify-center text-error shrink-0"
            aria-label="Delete meal"
          >
            <Icon name="trash" size={18} />
          </motion.button>
        </div>
      )
    : undefined

  return (
    <>
      <Sheet open={open} onClose={onClose} title="" detents={['medium', 'large']} footer={footer}>
        {meal &&
          (editing ? (
            <MealEditBody
              items={items}
              macros={editMacros}
              onChangeQty={changeQty}
              onChangeMacros={setEditMacros}
              onRemove={removeItem}
            />
          ) : (
            <MealBody meal={meal} items={items} />
          ))}
      </Sheet>
      <ConfirmDialog
        open={confirmDelete}
        title={meal ? `Delete ${meal.type}?` : 'Delete meal?'}
        message="This meal will be removed from today's log."
        confirmLabel="Delete"
        onCancel={() => setConfirmDelete(false)}
        onConfirm={() => {
          setConfirmDelete(false)
          if (meal) dispatch({ kind: 'meal/delete', id: meal.id })
          onClose()
          toast.show('Meal deleted')
        }}
      />
    </>
  )
}
