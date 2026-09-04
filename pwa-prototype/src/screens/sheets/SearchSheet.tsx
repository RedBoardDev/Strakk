import { useEffect, useRef, useState } from 'react'
import { motion } from 'motion/react'
import { Sheet } from '../../components/Sheet.tsx'
import { Segmented } from '../../components/Segmented.tsx'
import { Icon } from '../../components/Icon.tsx'
import { useToast } from '../../components/Toast.tsx'
import { haptic } from '../../lib/ios.ts'
import { useNav } from '../../nav.ts'
import { useStore } from '../../store.tsx'
import { fetchRecentFoods, searchFoods, type CatalogFood, type RecentFood } from '../../api/foods.ts'

type Tab = 'mine' | 'all'

// A saved/recent food — absolute macros for one logged portion.
type MineFood = {
  key: string
  name: string
  protein: number
  calories: number
  fat: number | null
  carbs: number | null
  quantity: string | null
  favorite: boolean
}

function CatalogRow({ food, onTap }: { food: CatalogFood; onTap: () => void }) {
  const factor = (food.default_portion_grams || 100) / 100
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
          {food.nutriscore && ['a', 'b'].includes(food.nutriscore) && (
            <Icon name="check" size={12} className="text-success shrink-0" />
          )}
        </div>
        <div className="text-[12px] text-ink-3 truncate">
          {food.brand ? `${food.brand} · ` : ''}
          {food.serving_label ?? 'portion'} · {Math.round(food.default_portion_grams)} g
        </div>
        <div className="mt-1 flex items-center gap-1.5 text-[12px] tnum">
          <span className="font-semibold text-primary-light">{Math.round(food.calories * factor)} kcal</span>
          <span className="text-ink-4">·</span>
          <span className="text-protein">{Math.round(food.protein * factor)} P</span>
          <span className="text-fat">{Math.round((food.fat ?? 0) * factor)} F</span>
          <span className="text-carbs">{Math.round((food.carbs ?? 0) * factor)} C</span>
        </div>
      </div>
      <Icon name="chevron.right" size={14} className="text-ink-4 shrink-0" />
    </motion.button>
  )
}

function MineRow({ food, onAdd }: { food: MineFood; onAdd: () => void }) {
  return (
    <motion.button
      type="button"
      whileTap={{ backgroundColor: '#151B38' }}
      transition={{ duration: 0.15 }}
      onClick={() => {
        haptic('light')
        onAdd()
      }}
      className="w-full px-2 py-3 flex items-center gap-3 text-left border-b border-hair last:border-0"
    >
      <div className="size-9 rounded-[10px] bg-surface-2 flex items-center justify-center shrink-0">
        <Icon name={food.favorite ? 'heart' : 'clock'} size={15} className={food.favorite ? 'text-primary' : 'text-ink-2'} />
      </div>
      <div className="flex-1 min-w-0">
        <div className="text-[15px] font-semibold text-ink truncate">{food.name}</div>
        <div className="mt-0.5 flex items-center gap-1.5 text-[12px] tnum">
          <span className="font-semibold text-primary-light">{Math.round(food.calories)} kcal</span>
          {food.quantity && (
            <>
              <span className="text-ink-4">·</span>
              <span className="text-ink-3">{food.quantity}</span>
            </>
          )}
        </div>
      </div>
      <Icon name="plus" size={16} className="text-primary shrink-0" />
    </motion.button>
  )
}

function EmptyState({ label, sub }: { label: string; sub: string }) {
  return (
    <div className="flex flex-col items-center gap-2 py-14 text-center">
      <Icon name="search" size={28} className="text-ink-4" />
      <div className="text-[15px] text-ink-2">{label}</div>
      <div className="text-[12px] text-ink-4 px-10">{sub}</div>
    </div>
  )
}

export function SearchSheet({
  open,
  onClose,
  logDate,
}: {
  open: boolean
  onClose: () => void
  logDate?: string
}) {
  const nav = useNav()
  const toast = useToast()
  const store = useStore()
  const [tab, setTab] = useState<Tab>('mine')
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<CatalogFood[]>([])
  const [recents, setRecents] = useState<RecentFood[]>([])
  const [searching, setSearching] = useState(false)
  const searchSeq = useRef(0)

  // Fresh state + load recents every time the sheet is presented.
  useEffect(() => {
    if (open) {
      setTab('mine')
      setQuery('')
      setResults([])
      void fetchRecentFoods().then(setRecents)
    }
  }, [open])

  // Debounced server search (catalog RPC + Open Food Facts live, merged).
  useEffect(() => {
    const q = query.trim()
    if (q.length < 2) {
      setResults([])
      setSearching(false)
      return
    }
    setSearching(true)
    const seq = ++searchSeq.current
    const timer = setTimeout(() => {
      void searchFoods(q).then((items) => {
        if (searchSeq.current !== seq) return // stale response
        setResults(items)
        setSearching(false)
      })
    }, 350)
    return () => clearTimeout(timer)
  }, [query])

  const needle = query.trim().toLowerCase()
  const mine: MineFood[] = [
    ...store.state.favoriteFoods.map((f) => ({
      key: `fav-${f.id}`,
      name: f.name,
      protein: f.protein,
      calories: f.calories,
      fat: f.fat,
      carbs: f.carbs,
      quantity: f.quantity,
      favorite: true,
    })),
    ...recents
      .filter((r) => !store.state.favoriteFoods.some((f) => f.name_normalized === r.name_normalized))
      .map((r) => ({
        key: `rec-${r.name_normalized}`,
        name: r.name,
        protein: r.protein,
        calories: r.calories,
        fat: r.fat,
        carbs: r.carbs,
        quantity: r.quantity,
        favorite: false,
      })),
  ].filter((f) => !needle || f.name.toLowerCase().includes(needle))

  const addMine = async (food: MineFood) => {
    try {
      await store.addOrphanEntry(
        {
          name: food.name,
          protein: food.protein,
          calories: food.calories,
          fat: food.fat,
          carbs: food.carbs,
          quantity: food.quantity,
          source: 'frequent',
        },
        logDate,
      )
      toast.show(`Added · ${Math.round(food.calories)} kcal`)
    } catch {
      // toasted by the store
    }
  }

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
              { value: 'all', label: 'Catalog' },
            ]}
            value={tab}
            onChange={setTab}
          />
        </div>
      </div>

      <div className="pb-8">
        {tab === 'mine' ? (
          mine.length === 0 ? (
            <EmptyState
              label={needle ? `No match for “${query.trim()}”` : 'No saved foods yet'}
              sub="Foods you log or favorite show up here for one-tap re-logging."
            />
          ) : (
            <>
              <div className="px-2 pt-2 pb-1 flex items-center justify-between">
                <span className="text-[11px] font-semibold uppercase tracking-[0.06em] text-ink-3">Favorites & recents</span>
                <span className="text-[11px] text-ink-4 tnum">{mine.length}</span>
              </div>
              {mine.map((food) => (
                <MineRow key={food.key} food={food} onAdd={() => void addMine(food)} />
              ))}
            </>
          )
        ) : searching ? (
          <div className="py-14 flex justify-center">
            <div className="size-5 rounded-full border-2 border-primary/30 border-t-primary animate-spin" />
          </div>
        ) : results.length === 0 ? (
          <EmptyState
            label={needle.length >= 2 ? `No results for “${query.trim()}”` : 'Search the catalog'}
            sub={needle.length >= 2 ? 'Try a different spelling or a simpler name.' : 'CIQUAL + Open Food Facts, live.'}
          />
        ) : (
          <>
            <div className="px-2 pt-2 pb-1 flex items-center justify-between">
              <span className="text-[11px] font-semibold uppercase tracking-[0.06em] text-ink-3">Catalog</span>
              <span className="text-[11px] text-ink-4 tnum">{results.length}</span>
            </div>
            {results.map((food) => (
              <CatalogRow
                key={`${food.source}-${food.id}`}
                food={food}
                onTap={() => nav.open({ kind: 'foodDetail', food, from: 'search', logDate })}
              />
            ))}
          </>
        )}
      </div>
    </Sheet>
  )
}
