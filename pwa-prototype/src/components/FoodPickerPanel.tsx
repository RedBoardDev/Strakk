import { useEffect, useRef, useState } from 'react'
import { motion } from 'motion/react'
import { Icon } from './Icon.tsx'
import { haptic } from '../lib/ios.ts'
import { fetchRecentFoods, searchFoods, type CatalogFood } from '../api/foods.ts'

// Compact inline food picker (server search; recents when the query is empty) —
// shared by the meal builder and the AI review lists so items can be added
// without leaving the current sheet.
export function FoodPickerPanel({ onPick, onCancel }: { onPick: (food: CatalogFood) => void; onCancel: () => void }) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<CatalogFood[]>([])
  const [recents, setRecents] = useState<CatalogFood[]>([])
  const [searching, setSearching] = useState(false)
  const seqRef = useRef(0)

  useEffect(() => {
    // Recents presented as pseudo-catalog rows (absolute macros ≈ per-portion).
    void fetchRecentFoods().then((rows) =>
      setRecents(
        rows.slice(0, 8).map((r) => ({
          id: `recent-${r.name_normalized}`,
          source: 'recent',
          name: r.name,
          brand: null,
          protein: r.protein,
          calories: r.calories,
          fat: r.fat,
          carbs: r.carbs,
          default_portion_grams: 100,
          serving_label: r.quantity,
          nutriscore: null,
          nova_group: null,
          barcode: null,
          image_url: null,
          rank: 1,
        })),
      ),
    )
  }, [])

  useEffect(() => {
    const q = query.trim()
    if (q.length < 2) {
      setResults([])
      setSearching(false)
      return
    }
    setSearching(true)
    const seq = ++seqRef.current
    const timer = setTimeout(() => {
      void searchFoods(q, 12).then((items) => {
        if (seqRef.current !== seq) return
        setResults(items)
        setSearching(false)
      })
    }, 350)
    return () => clearTimeout(timer)
  }, [query])

  const shown = query.trim().length >= 2 ? results : recents

  return (
    <div className="pb-2">
      <div className="flex items-center gap-2 h-11 px-3 rounded-[12px] bg-surface-1">
        <Icon name="search" size={16} className="text-ink-3 shrink-0" />
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Add a food…"
          aria-label="Search food to add"
          autoCorrect="off"
          autoCapitalize="none"
          className="flex-1 bg-transparent text-[16px] text-ink placeholder:text-ink-4 outline-none"
        />
        <button type="button" onClick={onCancel} className="text-[14px] font-medium text-ink-2 px-1">
          Done
        </button>
      </div>
      <div className="mt-2">
        {searching ? (
          <div className="py-6 flex justify-center">
            <div className="size-5 rounded-full border-2 border-primary/30 border-t-primary animate-spin" />
          </div>
        ) : shown.length === 0 ? (
          <div className="py-5 text-center text-[13px] text-ink-4">
            {query.trim().length >= 2 ? 'No results — try a simpler name.' : 'Type to search the catalog.'}
          </div>
        ) : (
          shown.map((food) => {
            const isRecent = String(food.id).startsWith('recent-')
            const factor = isRecent ? 1 : (food.default_portion_grams || 100) / 100
            return (
              <motion.button
                key={`${food.source}-${food.id}`}
                type="button"
                whileTap={{ backgroundColor: '#151B38' }}
                transition={{ duration: 0.12 }}
                onClick={() => {
                  haptic('light')
                  onPick(food)
                }}
                className="w-full px-2 py-2.5 flex items-center gap-3 text-left border-b border-hair last:border-0"
              >
                <div className="size-8 rounded-[9px] bg-surface-2 flex items-center justify-center shrink-0">
                  <Icon name={isRecent ? 'clock' : 'fork'} size={14} className="text-ink-2" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="text-[14px] font-medium text-ink truncate">{food.name}</div>
                  <div className="text-[12px] text-ink-3 truncate">
                    {food.brand ? `${food.brand} · ` : ''}
                    {isRecent ? (food.serving_label ?? 'last portion') : `${Math.round(food.default_portion_grams)} g`} ·{' '}
                    {Math.round(food.calories * factor)} kcal
                  </div>
                </div>
                <Icon name="plus" size={16} className="text-primary shrink-0" />
              </motion.button>
            )
          })
        )}
      </div>
    </div>
  )
}
