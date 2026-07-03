import { useEffect, useMemo, useState } from 'react'
import { motion } from 'motion/react'
import { Sheet } from '../../components/Sheet.tsx'
import { Segmented } from '../../components/Segmented.tsx'
import { Icon } from '../../components/Icon.tsx'
import { haptic } from '../../lib/ios.ts'
import { useNav } from '../../nav.ts'
import { recentFoods, searchResults, type Food } from '../../data/mock.ts'

type Tab = 'mine' | 'all'

function perServing(food: Food) {
  const factor = food.serving.grams / 100
  return {
    calories: Math.round(food.per100.calories * factor),
    protein: Math.round(food.per100.protein * factor),
    fat: Math.round(food.per100.fat * factor),
    carbs: Math.round(food.per100.carbs * factor),
  }
}

function FoodRow({ food, onTap }: { food: Food; onTap: () => void }) {
  const macros = perServing(food)
  return (
    <motion.button
      type="button"
      whileTap={{ backgroundColor: '#151B38' }}
      transition={{ duration: 0.15 }}
      onClick={() => {
        haptic('light')
        onTap()
      }}
      className="w-full px-2 py-3 flex items-center gap-3 text-left border-b border-hair last:border-0"
    >
      <div className="size-9 rounded-[10px] bg-surface-2 flex items-center justify-center shrink-0">
        <Icon name="fork" size={15} className="text-ink-2" />
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-1.5">
          <span className="text-[15px] font-semibold text-ink truncate">{food.name}</span>
          {food.verified && <Icon name="check" size={12} className="text-success shrink-0" />}
        </div>
        <div className="text-[12px] text-ink-3 truncate">
          {food.brand ? `${food.brand} · ` : ''}
          {food.serving.label} · {food.serving.grams} g
        </div>
        <div className="mt-1 flex items-center gap-1.5 text-[12px] tnum">
          <span className="font-semibold text-primary-light">{macros.calories} kcal</span>
          <span className="text-ink-4">·</span>
          <span className="text-protein">{macros.protein} P</span>
          <span className="text-fat">{macros.fat} F</span>
          <span className="text-carbs">{macros.carbs} C</span>
        </div>
      </div>
      <Icon name="chevron.right" size={14} className="text-ink-4 shrink-0" />
    </motion.button>
  )
}

function EmptyResults({ query }: { query: string }) {
  return (
    <div className="flex flex-col items-center gap-2 py-14 text-center">
      <Icon name="search" size={28} className="text-ink-4" />
      <div className="text-[15px] text-ink-2">No results for &ldquo;{query}&rdquo;</div>
      <div className="text-[12px] text-ink-4 px-10">Try a different name, or switch tabs.</div>
    </div>
  )
}

export function SearchSheet({ open, onClose }: { open: boolean; onClose: () => void }) {
  const nav = useNav()
  const [tab, setTab] = useState<Tab>('mine')
  const [query, setQuery] = useState('')

  // Fresh state every time the sheet is presented.
  useEffect(() => {
    if (open) {
      setTab('mine')
      setQuery('')
    }
  }, [open])

  const source = tab === 'mine' ? recentFoods : searchResults
  const needle = query.trim().toLowerCase()
  const results = useMemo(() => {
    if (!needle) return source
    return source.filter(
      (food) =>
        food.name.toLowerCase().includes(needle) || (food.brand?.toLowerCase().includes(needle) ?? false),
    )
  }, [source, needle])

  return (
    <Sheet open={open} onClose={onClose} title="Search a food" detents={['large']}>
      <div className="sticky top-0 z-10 bg-bg pt-1 pb-3">
        <div className="flex items-center gap-2 h-11 px-3 rounded-[12px] bg-surface-1">
          <Icon name="search" size={16} className="text-ink-3 shrink-0" />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder={tab === 'mine' ? 'Search my foods' : 'Apple, chicken…'}
            className="flex-1 bg-transparent text-[16px] text-ink placeholder:text-ink-4 outline-none"
            aria-label="Search foods"
            autoCorrect="off"
            autoCapitalize="none"
          />
          {query.length > 0 && (
            <button
              type="button"
              onClick={() => {
                haptic('light')
                setQuery('')
              }}
              className="size-6 flex items-center justify-center text-ink-3"
              aria-label="Clear search"
            >
              <Icon name="xmark" size={14} />
            </button>
          )}
        </div>
        <div className="mt-3">
          <Segmented<Tab>
            options={[
              { value: 'mine', label: 'My Foods' },
              { value: 'all', label: 'All foods' },
            ]}
            value={tab}
            onChange={setTab}
          />
        </div>
      </div>

      <div className="px-2 pt-2 pb-1 flex items-center justify-between">
        <span className="text-[11px] font-bold uppercase tracking-wider text-ink-3">
          {tab === 'mine' ? 'Recent foods' : 'All foods'}
        </span>
        <span className="text-[11px] text-ink-4 tnum">{results.length}</span>
      </div>

      <div className="pb-8">
        {results.length === 0 ? (
          <EmptyResults query={query.trim()} />
        ) : (
          results.map((food) => (
            <FoodRow key={food.id} food={food} onTap={() => nav.open({ kind: 'foodDetail', food, from: 'search' })} />
          ))
        )}
      </div>
    </Sheet>
  )
}
