import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { motion } from 'motion/react'
import { ScreenScroll } from '../components/ScreenScroll.tsx'
import { PushPage } from '../components/PushPage.tsx'
import { MacroGrid } from '../components/MacroCard.tsx'
import { ProgressBar } from '../components/ProgressBar.tsx'
import { Button } from '../components/Button.tsx'
import { Icon } from '../components/Icon.tsx'
import { SectionLabel } from '../components/SectionLabel.tsx'
import { haptic, spring } from '../lib/ios.ts'
import { toIsoDate, todayIso, timeOf } from '../lib/dates.ts'
import { useNav } from '../nav.ts'
import { colors } from '../theme/tokens.ts'
import { useStore, entryMacros, mealMacros } from '../store.tsx'
import { sumMacros, ZERO_MACROS } from '../lib/macros.ts'
import {
  fetchActiveDates,
  fetchMealsForDate,
  fetchOrphanEntriesForDate,
  type Entry,
  type Meal,
} from '../api/meals.ts'
import { fetchWaterForDate } from '../api/water.ts'

const WEEKDAY_LABELS = ['M', 'T', 'W', 'T', 'F', 'S', 'S']
const WEEKDAYS_FULL = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']
const MONTHS_FULL = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
]

type Selection = { year: number; month: number; date: number; iso: string }

type DayDetail = { meals: Meal[]; orphans: Entry[]; waterMl: number }

// A mini activity ring — adherence (kcal / goal), green once the goal is met.
function DayRing({
  progress,
  reached,
  size,
  stroke,
  delay = 0,
  children,
}: {
  progress: number
  reached: boolean
  size: number
  stroke: number
  delay?: number
  children?: ReactNode
}) {
  const radius = (size - stroke) / 2
  const circ = 2 * Math.PI * radius
  const clamped = Math.max(0, Math.min(1, progress))
  return (
    <div className="relative shrink-0" style={{ width: size, height: size }}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="-rotate-90">
        <circle cx={size / 2} cy={size / 2} r={radius} fill="none" stroke={colors.surface3} strokeWidth={stroke} />
        <motion.circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke={reached ? colors.success : colors.primary}
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={circ}
          initial={{ strokeDashoffset: circ }}
          animate={{ strokeDashoffset: circ * (1 - clamped) }}
          transition={{ ...spring.gentle, delay }}
        />
      </svg>
      {children && <div className="absolute inset-0 flex items-center justify-center">{children}</div>}
    </div>
  )
}

function DayCell({
  date,
  logged,
  isToday,
  delay,
  onSelect,
}: {
  date: number
  logged: boolean
  isToday: boolean
  delay: number
  onSelect: () => void
}) {
  const numCls = isToday ? 'text-primary font-semibold' : logged ? 'text-ink' : 'text-ink-4'
  return (
    <motion.button
      type="button"
      whileTap={{ scale: 0.9 }}
      transition={spring.snappy}
      onClick={onSelect}
      aria-label={`Day ${date}${logged ? ', logged' : ''}`}
      className={[
        'relative h-12 rounded-full flex flex-col items-center justify-center gap-1.5',
        isToday ? 'ring-1 ring-inset ring-primary/35' : '',
      ].join(' ')}
    >
      <span className={`text-[15px] tnum ${numCls}`}>{date}</span>
      <span className="flex h-1.5 items-center justify-center">
        {logged && (
          <motion.span
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ ...spring.gentle, delay }}
            className="size-1.5 rounded-full"
            style={{ backgroundColor: colors.primary, opacity: 0.7 }}
          />
        )}
      </span>
    </motion.button>
  )
}

function MealSheetRow({ title, subtitle, time, onTap }: { title: string; subtitle: string; time: string; onTap: () => void }) {
  return (
    <motion.button
      type="button"
      whileTap={{ scale: 0.99, backgroundColor: '#151B38' }}
      transition={{ duration: 0.15 }}
      onClick={onTap}
      className="w-full bg-surface-1 rounded-card px-3.5 py-3 flex items-center gap-2.5 text-left"
    >
      <span className="tnum w-11 shrink-0 text-[12px] font-semibold text-ink-3">{time}</span>
      <div className="w-4 shrink-0 flex justify-center">
        <Icon name="fork" size={13} className="text-ink-2" />
      </div>
      <div className="flex-1 min-w-0">
        <div className="text-[15px] font-semibold text-ink truncate">{title}</div>
        <div className="text-[12px] text-ink-2 truncate">{subtitle}</div>
      </div>
      <Icon name="chevron.right" size={14} className="text-ink-4 shrink-0" />
    </motion.button>
  )
}

export function CalendarScreen() {
  const nav = useNav()
  const store = useStore()
  const goals = store.state.goals
  const now = useMemo(() => new Date(), [])
  const today = todayIso()
  const [viewYear, setViewYear] = useState(now.getFullYear())
  const [viewMonth, setViewMonth] = useState(now.getMonth())
  const [activeDates, setActiveDates] = useState<Set<string>>(new Set())
  const [selected, setSelected] = useState<Selection | null>(null)
  const [detail, setDetail] = useState<DayDetail | null>(null)
  const [loadingDetail, setLoadingDetail] = useState(false)

  const prevMonth = () => {
    if (viewMonth === 0) {
      setViewMonth(11)
      setViewYear((yr) => yr - 1)
    } else {
      setViewMonth((mo) => mo - 1)
    }
  }
  const nextMonth = () => {
    if (viewMonth === 11) {
      setViewMonth(0)
      setViewYear((yr) => yr + 1)
    } else {
      setViewMonth((mo) => mo + 1)
    }
  }

  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate()
  const leading = (new Date(viewYear, viewMonth, 1).getDay() + 6) % 7 // Monday-first offset

  // Which days of the visible month have any logged data.
  useEffect(() => {
    const monthStart = toIsoDate(new Date(viewYear, viewMonth, 1))
    const monthEnd = toIsoDate(new Date(viewYear, viewMonth, daysInMonth))
    let cancelled = false
    void fetchActiveDates(monthStart, monthEnd)
      .then((dates) => {
        if (!cancelled) setActiveDates(new Set(dates))
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [viewYear, viewMonth, daysInMonth])

  // Load the selected day's meals, orphans and water.
  useEffect(() => {
    if (!selected) {
      setDetail(null)
      return
    }
    let cancelled = false
    setLoadingDetail(true)
    void Promise.all([
      fetchMealsForDate(selected.iso).catch(() => []),
      fetchOrphanEntriesForDate(selected.iso).catch(() => []),
      fetchWaterForDate(selected.iso).catch(() => []),
    ])
      .then(([meals, orphans, water]) => {
        if (cancelled) return
        setDetail({ meals, orphans, waterMl: water.reduce((sum, w) => sum + w.amount, 0) })
        setLoadingDetail(false)
      })
      .catch(() => {
        if (!cancelled) setLoadingDetail(false)
      })
    return () => {
      cancelled = true
    }
  }, [selected])

  const loggedCount = Array.from({ length: daysInMonth }, (_, i) =>
    activeDates.has(toIsoDate(new Date(viewYear, viewMonth, i + 1))),
  ).filter(Boolean).length

  const consumed = detail
    ? sumMacros([...detail.meals.map(mealMacros), ...detail.orphans.map(entryMacros)])
    : ZERO_MACROS
  const detailRatio = goals.calories > 0 ? consumed.calories / goals.calories : 0
  const detailTitle = selected
    ? `${WEEKDAYS_FULL[new Date(selected.year, selected.month, selected.date).getDay()]} `
      + `${selected.date} ${MONTHS_FULL[selected.month]}`
    : undefined

  const statusLabel = () => {
    if (!detail || (detail.meals.length === 0 && detail.orphans.length === 0)) return 'No meals logged'
    if (detailRatio >= 1) return 'Goal reached'
    if (detailRatio >= 0.85) return 'On track'
    return 'Under goal'
  }

  return (
    <ScreenScroll title="Calendar">
      {/* Month navigator */}
      <div className="pt-2 flex items-center">
        <button
          type="button"
          onClick={prevMonth}
          aria-label="Previous month"
          className="-ml-2 size-11 flex items-center justify-center text-ink"
        >
          <Icon name="chevron.left" size={18} />
        </button>
        <div className="flex-1 text-center text-[22px] font-bold text-ink">
          {MONTHS_FULL[viewMonth]} {viewYear}
        </div>
        <button
          type="button"
          onClick={nextMonth}
          aria-label="Next month"
          className="-mr-2 size-11 flex items-center justify-center text-ink"
        >
          <Icon name="chevron.right" size={18} />
        </button>
      </div>

      {/* Month summary */}
      <div className="mt-4 bg-surface-1 rounded-card p-3.5 flex items-center gap-3.5">
        <div className="size-12 rounded-full bg-primary/15 flex items-center justify-center shrink-0">
          <Icon name="calendar" size={20} className="text-primary" />
        </div>
        <div className="min-w-0">
          <div className="text-[15px] font-semibold text-ink">
            {loggedCount} {loggedCount === 1 ? 'day' : 'days'} logged
          </div>
          <div className="text-[12px] text-ink-2">Tap a day to see its meals and macros.</div>
        </div>
      </div>

      {/* Weekday header */}
      <div className="grid grid-cols-7 gap-x-1 pt-5 pb-1.5">
        {WEEKDAY_LABELS.map((label, i) => (
          <div key={i} className="text-center text-[11px] font-bold uppercase text-ink-3">
            {label}
          </div>
        ))}
      </div>

      {/* Calendar grid */}
      <div key={`${viewYear}-${viewMonth}`} className="grid grid-cols-7 gap-x-1 gap-y-2">
        {Array.from({ length: leading }, (_, i) => (
          <div key={`lead-${i}`} className="h-12" />
        ))}
        {Array.from({ length: daysInMonth }, (_, i) => {
          const date = i + 1
          const iso = toIsoDate(new Date(viewYear, viewMonth, date))
          return (
            <DayCell
              key={date}
              date={date}
              logged={activeDates.has(iso)}
              isToday={iso === today}
              delay={Math.min(date * 0.01, 0.3)}
              onSelect={() => {
                haptic('light')
                setSelected({ year: viewYear, month: viewMonth, date, iso })
              }}
            />
          )
        })}
      </div>

      {/* Day detail page */}
      <PushPage
        open={!!selected}
        onClose={() => setSelected(null)}
        title={detailTitle ?? ''}
        footer={
          <Button
            variant="primary"
            full
            glow
            onClick={() => selected && nav.open({ kind: 'add', logDate: selected.iso })}
          >
            <Icon name="plus" size={16} /> Add for this day
          </Button>
        }
      >
        {selected && (
          <div>
            {/* Day summary */}
            <div className="pt-3 flex items-center gap-3.5">
              <DayRing progress={detailRatio} reached={detailRatio >= 1} size={56} stroke={5}>
                <span className="text-[13px] font-bold text-ink tnum">{Math.round(detailRatio * 100)}%</span>
              </DayRing>
              <div className="min-w-0">
                <div className="text-[20px] font-bold text-ink tnum">
                  {consumed.calories.toLocaleString()}
                  <span className="text-[13px] text-ink-3 font-semibold"> / {goals.calories.toLocaleString()} kcal</span>
                </div>
                <div className="text-[13px] text-ink-2">{statusLabel()}</div>
              </div>
            </div>

            <SectionLabel>Nutrition</SectionLabel>
            <MacroGrid
              protein={{ consumed: consumed.protein, goal: goals.protein }}
              calories={{ consumed: consumed.calories, goal: goals.calories }}
              fat={{ consumed: consumed.fat, goal: goals.fat }}
              carbs={{ consumed: consumed.carbs, goal: goals.carbs }}
            />

            <SectionLabel>Water</SectionLabel>
            <div className="bg-surface-1 rounded-card px-4 py-3 flex items-center gap-3">
              <div className="size-9 rounded-lg bg-surface-3 flex items-center justify-center shrink-0">
                <Icon name="drop.fill" size={15} className="text-water" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-[16px] font-bold text-ink tnum">
                  {((detail?.waterMl ?? 0) / 1000).toFixed(1)} L
                  <span className="text-ink-3 font-semibold"> / {(goals.water / 1000).toFixed(1)} L</span>
                </div>
                <div className="pt-2">
                  <ProgressBar value={goals.water > 0 ? (detail?.waterMl ?? 0) / goals.water : 0} color={colors.water} />
                </div>
              </div>
            </div>

            {loadingDetail && (
              <div className="py-10 flex justify-center">
                <div className="size-5 rounded-full border-2 border-primary/30 border-t-primary animate-spin" />
              </div>
            )}

            {detail && (detail.meals.length > 0 || detail.orphans.length > 0) && (
              <>
                <SectionLabel>Logged</SectionLabel>
                <div className="flex flex-col gap-1.5">
                  {detail.meals.map((meal) => (
                    <MealSheetRow
                      key={meal.id}
                      title={meal.name}
                      subtitle={`${meal.meal_entries.length} items · ${mealMacros(meal).calories} kcal`}
                      time={timeOf(meal.created_at)}
                      onTap={() => {
                        haptic('light')
                        nav.open({ kind: 'mealDetail', meal })
                      }}
                    />
                  ))}
                  {detail.orphans.map((entry) => (
                    <MealSheetRow
                      key={entry.id}
                      title={entry.name ?? 'Item'}
                      subtitle={`${entryMacros(entry).calories} kcal`}
                      time={timeOf(entry.created_at)}
                      onTap={() => {
                        haptic('light')
                        nav.open({ kind: 'entryDetail', entry })
                      }}
                    />
                  ))}
                </div>
              </>
            )}
          </div>
        )}
      </PushPage>
    </ScreenScroll>
  )
}
