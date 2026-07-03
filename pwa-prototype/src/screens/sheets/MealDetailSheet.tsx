import { useEffect, useState } from 'react'
import { Sheet } from '../../components/Sheet.tsx'
import { MacroGrid } from '../../components/MacroCard.tsx'
import { Button } from '../../components/Button.tsx'
import { ConfirmDialog } from '../../components/ConfirmDialog.tsx'
import { FoodPickerPanel } from '../../components/FoodPickerPanel.tsx'
import { Icon } from '../../components/Icon.tsx'
import { MacroInputGrid } from '../../components/MacroInputGrid.tsx'
import { useToast } from '../../components/Toast.tsx'
import { haptic } from '../../lib/ios.ts'
import { timeOf } from '../../lib/dates.ts'
import { entryMacros, mealMacros, useStore } from '../../store.tsx'
import { parseBreakdown, type Entry, type EntryPatch, type Meal } from '../../api/meals.ts'
import { reviewItemFromFood } from '../../components/ReviewItems.tsx'
import type { CatalogFood } from '../../api/foods.ts'
import type { Macros } from '../../data/viewTypes.ts'

// Detail sheet for BOTH a meal (grouped entries) and an orphan entry — matching
// the two timeline row kinds. Edit mode mutates entry rows server-first.

type EditRow = { entry: Entry; name: string; qty: string; macros: Macros; removed: boolean }

function toEditRows(entries: Entry[]): EditRow[] {
  return entries.map((entry) => ({
    entry,
    name: entry.name ?? '',
    qty: entry.quantity ?? '',
    macros: entryMacros(entry),
    removed: false,
  }))
}

function EntryLine({ entry, index }: { entry: Entry; index: number }) {
  const kcal = entryMacros(entry).calories
  return (
    <div className={`flex items-center gap-3 px-4 py-3 ${index > 0 ? 'border-t border-divider/70' : ''}`}>
      <div className="size-6 rounded-full bg-surface-3 flex items-center justify-center shrink-0">
        <span className="text-[11px] font-semibold text-ink-3 tnum">{index + 1}</span>
      </div>
      <span className="flex-1 min-w-0 text-[15px] text-ink truncate">{entry.name ?? 'Item'}</span>
      {entry.quantity && <span className="text-[13px] text-ink-3 tnum shrink-0">{entry.quantity}</span>}
      <span className="text-[13px] font-semibold text-ink tnum shrink-0">{kcal} kcal</span>
    </div>
  )
}

export function MealDetailSheet({
  open,
  onClose,
  meal: mealProp,
  entry: entryProp,
}: {
  open: boolean
  onClose: () => void
  meal: Meal | null
  entry: Entry | null
}) {
  const store = useStore()
  const toast = useToast()

  // Always render live data (props are snapshots taken at open time).
  const meal = mealProp ? (store.state.meals.find((m) => m.id === mealProp.id) ?? mealProp) : null
  const orphan = entryProp
    ? (store.state.orphans.find((e) => e.id === entryProp.id) ?? entryProp)
    : null

  const [editing, setEditing] = useState(false)
  const [rows, setRows] = useState<EditRow[]>([])
  const [mealName, setMealName] = useState('')
  const [addingFood, setAddingFood] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (open) {
      setEditing(false)
      setConfirmDelete(false)
      setAddingFood(false)
    }
  }, [open, mealProp, entryProp])

  const startEdit = () => {
    if (meal) {
      setRows(toEditRows(meal.meal_entries))
      setMealName(meal.name)
    } else if (orphan) {
      setRows(toEditRows([orphan]))
    }
    setEditing(true)
  }

  const saveEdit = async () => {
    haptic('medium')
    setSaving(true)
    try {
      if (meal && mealName.trim() && mealName.trim() !== meal.name) {
        await store.renameMeal(meal.id, mealName.trim())
      }
      for (const row of rows) {
        if (row.removed) {
          await store.deleteEntry(row.entry.id)
          continue
        }
        const patch: EntryPatch = {
          name: row.name.trim() || row.entry.name,
          protein: row.macros.protein,
          calories: row.macros.calories,
          fat: row.macros.fat,
          carbs: row.macros.carbs,
          quantity: row.qty.trim() || null,
        }
        const before = entryMacros(row.entry)
        const changed =
          patch.name !== row.entry.name ||
          patch.quantity !== row.entry.quantity ||
          patch.calories !== before.calories ||
          patch.protein !== before.protein ||
          patch.fat !== before.fat ||
          patch.carbs !== before.carbs
        if (changed) await store.updateEntry(row.entry.id, patch)
      }
      setEditing(false)
      toast.show(meal ? 'Meal updated' : 'Entry updated')
      // A meal whose last entry was removed collapses to nothing — close it.
      if (meal && rows.every((r) => r.removed)) {
        await store.deleteMeal(meal.id)
        onClose()
      }
      if (orphan && rows.every((r) => r.removed)) onClose()
    } catch {
      // toasted by the store
    } finally {
      setSaving(false)
    }
  }

  const doDelete = async () => {
    setConfirmDelete(false)
    try {
      if (meal) await store.deleteMeal(meal.id)
      else if (orphan) await store.deleteEntry(orphan.id)
      onClose()
      toast.show(meal ? 'Meal deleted' : 'Entry deleted')
    } catch {
      // toasted by the store
    }
  }

  const addFoodToMeal = async (food: CatalogFood) => {
    if (!meal) return
    const item = reviewItemFromFood(food)
    try {
      await store.addEntryToMeal(meal.id, {
        name: item.name,
        protein: item.p,
        calories: item.kcal,
        fat: item.f,
        carbs: item.c,
        quantity: item.qty || null,
        source: 'search',
      })
      toast.show(`Added ${item.name}`)
    } catch {
      // toasted by the store
    }
  }

  const title = meal ? meal.name : (orphan?.name ?? '')
  const createdAt = meal?.created_at ?? orphan?.created_at
  const macros = meal ? mealMacros(meal) : orphan ? entryMacros(orphan) : null
  const breakdown = orphan ? parseBreakdown(orphan) : null
  const present = meal ?? orphan

  const footer = present
    ? editing
      ? (
        <div className="flex gap-3">
          <Button variant="secondary" full disabled={saving} onClick={() => setEditing(false)}>
            Cancel
          </Button>
          <Button variant="primary" full disabled={saving} onClick={() => void saveEdit()}>
            {saving ? 'Saving…' : 'Save'}
          </Button>
        </div>
      )
      : (
        <div className="flex gap-3">
          <Button variant="secondary" full onClick={startEdit}>
            <Icon name="pencil" size={16} /> Edit
          </Button>
          <button
            type="button"
            onClick={() => {
              haptic('light')
              setConfirmDelete(true)
            }}
            className="h-[52px] w-[52px] rounded-card bg-surface-2 flex items-center justify-center text-error shrink-0"
            aria-label={meal ? 'Delete meal' : 'Delete entry'}
          >
            <Icon name="trash" size={18} />
          </button>
        </div>
      )
    : undefined

  return (
    <>
      <Sheet open={open} onClose={onClose} title="" detents={['medium', 'large']} footer={footer}>
        {present && !editing && (
          <div className="pb-2">
            {/* Header */}
            <div className="pt-1 pb-6">
              <h2 className="text-[22px] font-bold text-ink leading-tight">{title}</h2>
              <div className="flex items-center gap-1.5 mt-1.5 text-[12px] text-ink-2">
                <Icon name="clock" size={12} className="text-ink-3" />
                {createdAt && <span className="tnum">{timeOf(createdAt)}</span>}
                {meal && (
                  <>
                    <span className="text-ink-4">·</span>
                    <span>
                      {meal.meal_entries.length} {meal.meal_entries.length === 1 ? 'item' : 'items'}
                    </span>
                  </>
                )}
                {orphan?.quantity && (
                  <>
                    <span className="text-ink-4">·</span>
                    <span>{orphan.quantity}</span>
                  </>
                )}
              </div>
            </div>

            {/* Total macros */}
            {macros && (
              <div className="pb-6">
                <MacroGrid
                  protein={{ consumed: macros.protein }}
                  calories={{ consumed: macros.calories }}
                  fat={{ consumed: macros.fat }}
                  carbs={{ consumed: macros.carbs }}
                />
              </div>
            )}

            {/* Items */}
            {meal && meal.meal_entries.length > 0 && (
              <div className="pb-2">
                <div className="text-[11px] font-bold uppercase tracking-wider text-ink-3 mb-2">Items</div>
                <div className="bg-surface-1 rounded-card overflow-hidden">
                  {meal.meal_entries.map((entry, i) => (
                    <EntryLine key={entry.id} entry={entry} index={i} />
                  ))}
                </div>
              </div>
            )}

            {/* Breakdown of an AI orphan entry */}
            {breakdown && breakdown.length > 0 && (
              <div className="pb-2">
                <div className="text-[11px] font-bold uppercase tracking-wider text-ink-3 mb-2">Detected items</div>
                <div className="bg-surface-1 rounded-card overflow-hidden divide-y divide-divider/70">
                  {breakdown.map((item) => (
                    <div key={item.name} className="flex items-center justify-between gap-3 px-4 py-3">
                      <span className="text-[15px] text-ink truncate">{item.name}</span>
                      <span className="text-[13px] font-semibold text-ink tnum shrink-0">
                        {Math.round(item.calories_kcal)} kcal
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {present && editing && (
          <div className="pb-2">
            <div className="pt-1 pb-5">
              {meal ? (
                <input
                  value={mealName}
                  onChange={(event) => setMealName(event.target.value.slice(0, 60))}
                  aria-label="Meal name"
                  className="w-full bg-transparent text-[22px] font-bold text-ink outline-none"
                />
              ) : (
                <h2 className="text-[22px] font-bold text-ink leading-tight">Edit entry</h2>
              )}
              <div className="text-[13px] text-ink-2 mt-1">Fix macros, quantities, or remove items.</div>
            </div>

            <div className="flex flex-col gap-2.5">
              {rows.map((row, index) =>
                row.removed ? null : (
                  <div key={row.entry.id} className="bg-surface-1 rounded-card p-3.5">
                    <div className="flex items-center gap-2.5">
                      <button
                        type="button"
                        onClick={() => {
                          haptic('light')
                          setRows((prev) => prev.map((r, i) => (i === index ? { ...r, removed: true } : r)))
                        }}
                        className="size-7 rounded-full bg-error/15 flex items-center justify-center text-error shrink-0"
                        aria-label={`Remove ${row.name || 'item'}`}
                      >
                        <Icon name="minus" size={16} />
                      </button>
                      <input
                        value={row.name}
                        onChange={(event) =>
                          setRows((prev) => prev.map((r, i) => (i === index ? { ...r, name: event.target.value.slice(0, 100) } : r)))
                        }
                        aria-label="Item name"
                        className="flex-1 min-w-0 bg-transparent text-[15px] font-medium text-ink outline-none"
                      />
                      <input
                        value={row.qty}
                        onChange={(event) =>
                          setRows((prev) => prev.map((r, i) => (i === index ? { ...r, qty: event.target.value.slice(0, 50) } : r)))
                        }
                        placeholder="qty"
                        aria-label="Item quantity"
                        className="w-20 bg-surface-2 rounded-[8px] px-2 h-8 text-[13px] text-ink placeholder:text-ink-4 outline-none text-right"
                      />
                    </div>
                    <div className="mt-2.5">
                      <MacroInputGrid
                        values={row.macros}
                        onChange={(macros) => setRows((prev) => prev.map((r, i) => (i === index ? { ...r, macros } : r)))}
                        ariaPrefix={meal ? row.name || 'Item' : 'Meal'}
                        variant="inset"
                      />
                    </div>
                  </div>
                ),
              )}
            </div>

            {/* Add another item to a meal */}
            {meal && (
              <div className="mt-4">
                {addingFood ? (
                  <FoodPickerPanel
                    onPick={(food) => {
                      void addFoodToMeal(food).then(() => {
                        // refresh edit rows with the live meal (new entry included)
                        const live = store.state.meals.find((m) => m.id === meal.id)
                        if (live) setRows(toEditRows(live.meal_entries))
                      })
                    }}
                    onCancel={() => setAddingFood(false)}
                  />
                ) : (
                  <button
                    type="button"
                    onClick={() => {
                      haptic('light')
                      setAddingFood(true)
                    }}
                    className="w-full h-11 rounded-card border border-dashed border-divider-strong/60 flex items-center justify-center gap-2 text-ink-2 text-[14px] font-medium"
                  >
                    <Icon name="plus" size={15} /> Add item
                  </button>
                )}
              </div>
            )}
          </div>
        )}
      </Sheet>
      <ConfirmDialog
        open={confirmDelete}
        title={meal ? `Delete ${meal.name}?` : 'Delete entry?'}
        message="This will be removed from the day's log."
        confirmLabel="Delete"
        onCancel={() => setConfirmDelete(false)}
        onConfirm={() => void doDelete()}
      />
    </>
  )
}
