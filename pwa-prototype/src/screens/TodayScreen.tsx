import { useState } from 'react'
import { motion } from 'motion/react'
import { Icon, type IconName } from '../components/Icon.tsx'
import { MacroGrid } from '../components/MacroCard.tsx'
import { ScreenScroll } from '../components/ScreenScroll.tsx'
import { WaterCard } from '../components/WaterCard.tsx'
import { haptic } from '../lib/ios.ts'
import { timeOf } from '../lib/dates.ts'
import { useNav } from '../nav.ts'
import { entryMacros, mealMacros, timelineOf, useStore, type TimelineItem } from '../store.tsx'
import type { EntrySource } from '../api/meals.ts'
import { HevyExportSheet } from './sheets/HevyExportSheet.tsx'

const SOURCE_ICON: Record<EntrySource, IconName> = {
  photo_ai: 'camera',
  search: 'search',
  barcode: 'barcode',
  manual: 'pencil',
  text_ai: 'sparkles',
  frequent: 'clock',
}

function todayLabel(): string {
  return new Date().toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' })
}

function TimelineRow({ item, onTap }: { item: TimelineItem; onTap: () => void }) {
  const isMeal = item.kind === 'meal'
  const title = isMeal ? item.meal.name : (item.entry.name ?? 'Entry')
  const kcal = isMeal ? mealMacros(item.meal).calories : entryMacros(item.entry).calories
  const subtitle = isMeal
    ? `${item.meal.meal_entries.length} ${item.meal.meal_entries.length === 1 ? 'item' : 'items'} · ${kcal} kcal`
    : `${item.entry.quantity ? `${item.entry.quantity} · ` : ''}${kcal} kcal`
  const icon: IconName = isMeal ? 'fork' : SOURCE_ICON[item.entry.source]

  return (
    <motion.button
      type="button"
      layout
      initial={{ opacity: 0, y: -8 }}
      animate={{ opacity: 1, y: 0 }}
      whileTap={{ scale: 0.99, backgroundColor: '#151B38' }}
      transition={{ duration: 0.15 }}
      onClick={() => {
        haptic('light')
        onTap()
      }}
      className="w-full bg-surface-1 rounded-card px-4 py-3 flex items-center gap-2.5 text-left"
    >
      <span className="tnum w-11 shrink-0 text-[12px] font-semibold text-ink-3">{timeOf(item.createdAt)}</span>
      <div className="w-4 shrink-0 flex justify-center">
        <Icon name={icon} size={13} className="text-ink-2" />
      </div>
      <div className="flex-1 min-w-0">
        <div className="text-[15px] font-semibold text-ink truncate">{title}</div>
        <div className="text-[12px] text-ink-2 truncate">{subtitle}</div>
      </div>
      <Icon name="chevron.right" size={14} className="text-ink-3 shrink-0" />
    </motion.button>
  )
}

export function TodayScreen() {
  const nav = useNav()
  const store = useStore()
  const { state, consumed, waterMl } = store
  const { goals, ready } = state

  const [hevyOpen, setHevyOpen] = useState(false)

  const timeline = timelineOf(state)

  return (
    <div className="relative h-full">
      <ScreenScroll
        title="Today"
        trailing={
          <div className="flex items-center gap-1">
            <span className="text-[13px] text-ink-2">{todayLabel()}</span>
            <button
              type="button"
              onClick={() => {
                haptic('light')
                setHevyOpen(true)
              }}
              className="size-9 flex items-center justify-center text-ink-2"
              aria-label="Export to Hevy"
            >
              <Icon name="dumbbell.fill" size={18} />
            </button>
          </div>
        }
      >
        {/* Macro grid */}
        <div className="pt-5">
          <MacroGrid
            protein={{ consumed: consumed.protein, goal: goals.protein }}
            calories={{ consumed: consumed.calories, goal: goals.calories }}
            fat={{ consumed: consumed.fat, goal: goals.fat }}
            carbs={{ consumed: consumed.carbs, goal: goals.carbs }}
          />
        </div>

        {/* Water */}
        <div className="pt-6">
          <WaterCard
            totalMl={waterMl}
            goalMl={goals.water}
            onDelta={(delta) => void store.addWater(delta).catch(() => {})}
          />
        </div>

        {/* Timeline */}
        <div className="pt-6 flex flex-col gap-2">
          {!ready ? (
            <div className="py-12 flex justify-center">
              <div className="size-5 rounded-full border-2 border-primary/30 border-t-primary animate-spin" />
            </div>
          ) : timeline.length === 0 ? (
            <div className="py-12 flex flex-col items-center gap-2 text-center">
              <Icon name="fork" size={26} className="text-ink-4" />
              <div className="text-[15px] text-ink-2">Nothing logged yet</div>
              <div className="text-[12px] text-ink-4">Add your first meal with the buttons below.</div>
            </div>
          ) : (
            timeline.map((item) => (
              <TimelineRow
                key={item.kind === 'meal' ? item.meal.id : item.entry.id}
                item={item}
                onTap={() =>
                  nav.open(item.kind === 'meal' ? { kind: 'mealDetail', meal: item.meal } : { kind: 'entryDetail', entry: item.entry })
                }
              />
            ))
          )}
        </div>

        {/* Clearance for the floating action bar + tab bar */}
        <div className="h-28" />
      </ScreenScroll>

      {/* Action bar — floats just above the glass tab bar; timeline fades into it */}
      <div
        className="absolute inset-x-0 z-10 px-5 pt-8 pb-2 pointer-events-none bg-gradient-to-t from-bg from-45% to-transparent"
        style={{ bottom: 'calc(var(--safe-bottom, env(safe-area-inset-bottom)) + 60px)' }}
      >
        <div className="flex gap-3 pointer-events-auto">
          <button
            type="button"
            onClick={() => {
              haptic('light')
              nav.open({ kind: 'add' })
            }}
            className="flex-1 h-14 rounded-[14px] bg-surface-2 flex items-center justify-center gap-2 text-ink font-semibold text-[15px]"
          >
            <Icon name="fork" size={16} /> Meal
          </button>
          <motion.button
            type="button"
            whileTap={{ scale: 0.97 }}
            onClick={() => {
              haptic('medium')
              // Quick = the AI text shortcut; Meal opens the full picker.
              nav.open({ kind: 'quickAdd' })
            }}
            className="flex-1 h-14 rounded-[14px] bg-primary glow-primary flex items-center justify-center gap-2 text-white font-semibold text-[15px]"
          >
            <Icon name="sparkles" size={16} /> Quick
          </motion.button>
        </div>
      </div>


      <HevyExportSheet open={hevyOpen} onClose={() => setHevyOpen(false)} />
    </div>
  )
}
