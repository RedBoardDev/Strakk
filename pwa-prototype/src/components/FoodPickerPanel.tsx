import { useMemo, useState } from 'react'
import { motion } from 'motion/react'
import { Icon } from './Icon.tsx'
import { haptic } from '../lib/ios.ts'
import { recentFoods, searchResults, type Food } from '../data/mock.ts'

// Compact inline food picker (recent + catalog, text-filtered) — shared by the
// meal builder and the AI review lists so items can be added without leaving
// the current sheet.
export function FoodPickerPanel({ onPick, onCancel }: { onPick: (food: Food) => void; onCancel: () => void }) {
  const [query, setQuery] = useState('')
  const needle = query.trim().toLowerCase()
  const results = useMemo(() => {
    const pool = [...recentFoods, ...searchResults]
    const seen = new Set<string>()
    const unique = pool.filter((food) => (seen.has(food.id) ? false : (seen.add(food.id), true)))
    if (!needle) return unique.slice(0, 8)
    return unique.filter((food) => food.name.toLowerCase().includes(needle)).slice(0, 12)
  }, [needle])

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
        {results.map((food) => (
          <motion.button
            key={food.id}
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
              <Icon name="fork" size={14} className="text-ink-2" />
            </div>
            <div className="flex-1 min-w-0">
              <div className="text-[14px] font-medium text-ink truncate">{food.name}</div>
              <div className="text-[12px] text-ink-3 truncate">
                {food.brand ? `${food.brand} · ` : ''}
                {food.serving.grams} g · {Math.round((food.per100.calories * food.serving.grams) / 100)} kcal
              </div>
            </div>
            <Icon name="plus" size={16} className="text-primary shrink-0" />
          </motion.button>
        ))}
      </div>
    </div>
  )
}
