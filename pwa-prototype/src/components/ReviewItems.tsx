import { useState } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { Icon } from './Icon.tsx'
import { FoodPickerPanel } from './FoodPickerPanel.tsx'
import { MacroInputGrid } from './MacroInputGrid.tsx'
import { haptic } from '../lib/ios.ts'
import { sumMacros } from '../lib/macros.ts'
import type { CatalogFood } from '../api/foods.ts'
import type { Macros } from '../data/mock.ts'

// One reviewable item of an AI-detected meal (quick add / photo meal): the
// user can fix any macro, adjust the quantity label, remove the item, or add
// a missing food before saving — mirroring the native review step.
export type ReviewItem = {
  id: string
  name: string
  qty: string
  kcal: number
  p: number
  f: number
  c: number
}

let reviewSeq = 0

export function reviewItemFromFood(food: CatalogFood): ReviewItem {
  reviewSeq += 1
  // Recents carry absolute per-portion macros; catalog rows are per-100g.
  const isRecent = String(food.id).startsWith('recent-')
  const grams = Math.round(food.default_portion_grams) || 100
  const factor = isRecent ? 1 : grams / 100
  return {
    id: `rv-${food.id}-${reviewSeq}`,
    name: food.name,
    qty: isRecent ? (food.serving_label ?? '') : `${grams}g`,
    kcal: Math.round(food.calories * factor),
    p: Math.round(food.protein * factor),
    f: Math.round((food.fat ?? 0) * factor),
    c: Math.round((food.carbs ?? 0) * factor),
  }
}

// ReviewItem keeps compact field names; adapt to Macros for shared helpers.
function toMacros(item: ReviewItem): Macros {
  return { calories: item.kcal, protein: item.p, fat: item.f, carbs: item.c }
}

export function reviewTotals(items: ReviewItem[]): Macros {
  return sumMacros(items.map(toMacros))
}

function ItemEditor({
  item,
  onChange,
  onRemove,
}: {
  item: ReviewItem
  onChange: (patch: Partial<ReviewItem>) => void
  onRemove: () => void
}) {
  return (
    <div className="px-4 pb-3.5">
      <MacroInputGrid
        values={toMacros(item)}
        onChange={(m) => onChange({ kcal: m.calories, p: m.protein, f: m.fat, c: m.carbs })}
        ariaPrefix={item.name}
        variant="inset"
      />
      <div className="mt-2 flex items-center gap-2">
        <input
          value={item.qty}
          onChange={(event) => onChange({ qty: event.target.value.slice(0, 30) })}
          placeholder="Quantity"
          aria-label={`${item.name} quantity`}
          className="flex-1 bg-surface-2 rounded-[10px] px-3 h-9 text-[13px] text-ink placeholder:text-ink-4 outline-none"
        />
        <button
          type="button"
          onClick={() => {
            haptic('light')
            onRemove()
          }}
          className="h-9 px-3 rounded-[10px] bg-error/15 text-error text-[13px] font-semibold flex items-center gap-1.5"
          aria-label={`Remove ${item.name}`}
        >
          <Icon name="trash" size={13} /> Remove
        </button>
      </div>
    </div>
  )
}

export function ReviewItems({
  items,
  onChange,
}: {
  items: ReviewItem[]
  onChange: (items: ReviewItem[]) => void
}) {
  const [expandedId, setExpandedId] = useState<string | null>(null)
  const [adding, setAdding] = useState(false)

  const patch = (id: string, changes: Partial<ReviewItem>) =>
    onChange(items.map((item) => (item.id === id ? { ...item, ...changes } : item)))

  return (
    <div>
      <div className="bg-surface-1 rounded-card overflow-hidden divide-y divide-divider/70">
        {items.map((item) => {
          const expanded = expandedId === item.id
          return (
            <div key={item.id}>
              <button
                type="button"
                onClick={() => {
                  haptic('light')
                  setExpandedId(expanded ? null : item.id)
                }}
                className="w-full flex items-center gap-3 px-4 py-3 text-left"
              >
                <div className="flex-1 min-w-0">
                  <div className="text-[15px] text-ink truncate">{item.name}</div>
                  <div className="text-[12px] text-ink-3">
                    <span className="text-protein">{item.p}P</span> · <span className="text-fat">{item.f}F</span> ·{' '}
                    <span className="text-carbs">{item.c}C</span>
                  </div>
                </div>
                <div className="text-right shrink-0">
                  <div className="text-[14px] font-semibold text-ink tnum">{item.kcal}</div>
                  <div className="text-[11px] text-ink-3 tnum">{item.qty}</div>
                </div>
                <Icon name={expanded ? 'chevron.up' : 'chevron.down'} size={14} className="text-ink-4 shrink-0" />
              </button>
              <AnimatePresence initial={false}>
                {expanded && (
                  <motion.div
                    initial={{ height: 0, opacity: 0 }}
                    animate={{ height: 'auto', opacity: 1 }}
                    exit={{ height: 0, opacity: 0 }}
                    transition={{ duration: 0.18 }}
                    className="overflow-hidden"
                  >
                    <ItemEditor
                      item={item}
                      onChange={(changes) => patch(item.id, changes)}
                      onRemove={() => {
                        setExpandedId(null)
                        onChange(items.filter((it) => it.id !== item.id))
                      }}
                    />
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          )
        })}
        {items.length === 0 && (
          <div className="px-4 py-6 text-center text-[13px] text-ink-3">No items — add one below.</div>
        )}
      </div>

      <div className="mt-3">
        {adding ? (
          <FoodPickerPanel
            onPick={(food) => onChange([...items, reviewItemFromFood(food)])}
            onCancel={() => setAdding(false)}
          />
        ) : (
          <button
            type="button"
            onClick={() => {
              haptic('light')
              setAdding(true)
            }}
            className="w-full h-11 rounded-card border border-dashed border-divider-strong/60 flex items-center justify-center gap-2 text-ink-2 text-[14px] font-medium"
          >
            <Icon name="plus" size={15} /> Add item
          </button>
        )}
      </div>
      <div className="mt-2 px-1 text-[11px] text-ink-4">Tap an item to fix its macros or quantity.</div>
    </div>
  )
}
