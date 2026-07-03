import { useState, type ReactNode } from 'react'
import { motion } from 'motion/react'
import { ScreenScroll } from '../components/ScreenScroll.tsx'
import { PushPage } from '../components/PushPage.tsx'
import { MacroGrid } from '../components/MacroCard.tsx'
import { ProgressBar } from '../components/ProgressBar.tsx'
import { Button } from '../components/Button.tsx'
import { Icon } from '../components/Icon.tsx'
import { SectionLabel } from '../components/SectionLabel.tsx'
import { haptic, spring } from '../lib/ios.ts'
import { useNav } from '../nav.ts'
import { colors } from '../theme/tokens.ts'
import { calendarDays, goals, meals, type DayLog, type MealEntry } from '../data/mock.ts'

const WEEKDAY_LABELS = ['M', 'T', 'W', 'T', 'F', 'S', 'S']
const WEEKDAYS_FULL = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']
const MONTHS_FULL = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
]
// The month the mock dataset describes; "today" only lights up here.
const DATA_YEAR = 2026
const DATA_MONTH = 5 // June (0-indexed)
const TODAY_DATE = 30

type Selection = { year: number; month: number; date: number }

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
  log,
  isToday,
  delay,
  onSelect,
}: {
  date: number
  log: DayLog | undefined
  isToday: boolean
  delay: number
  onSelect: () => void
}) {
  const reached = !!log && log.kcal >= log.goal
  const numCls = isToday ? 'text-primary font-semibold' : log ? 'text-ink' : 'text-ink-4'
  return (
    <motion.button
      type="button"
      whileTap={{ scale: 0.9 }}
      transition={spring.snappy}
      onClick={onSelect}
      aria-label={`Day ${date}${reached ? ', goal reached' : log ? ', logged' : ''}`}
      className={[
        'relative h-12 rounded-full flex flex-col items-center justify-center gap-1.5',
        isToday ? 'ring-1 ring-inset ring-primary/35' : '',
      ].join(' ')}
    >
      <span className={`text-[15px] tnum ${numCls}`}>{date}</span>
      {/* Light adherence indicator: a single dot instead of a per-day ring */}
      <span className="flex h-1.5 items-center justify-center">
        {log && (
          <motion.span
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ ...spring.gentle, delay }}
            className="size-1.5 rounded-full"
            style={{ backgroundColor: reached ? colors.success : colors.primary, opacity: reached ? 1 : 0.6 }}
          />
        )}
      </span>
    </motion.button>
  )
}


function SheetMealRow({ meal, onTap }: { meal: MealEntry; onTap: () => void }) {
  return (
    <motion.button
      type="button"
      whileTap={{ scale: 0.99, backgroundColor: '#151B38' }}
      transition={{ duration: 0.15 }}
      onClick={onTap}
      className="w-full bg-surface-1 rounded-card px-3.5 py-3 flex items-center gap-2.5 text-left"
    >
      <span className="tnum w-11 shrink-0 text-[12px] font-semibold text-ink-3">{meal.time}</span>
      <div className="w-4 shrink-0 flex justify-center">
        <Icon name="fork" size={13} className="text-ink-2" />
      </div>
      <div className="flex-1 min-w-0">
        <div className="text-[15px] font-semibold text-ink truncate">{meal.type}</div>
        <div className="text-[12px] text-ink-2 truncate">
          {meal.items.length} items · {meal.macros.calories} kcal
        </div>
      </div>
      <Icon name="chevron.right" size={14} className="text-ink-4 shrink-0" />
    </motion.button>
  )
}

function dayMacros(log: DayLog | undefined) {
  if (!log) {
    return {
      protein: { consumed: 0, goal: goals.protein },
      calories: { consumed: 0, goal: goals.calories },
      fat: { consumed: 0, goal: goals.fat },
      carbs: { consumed: 0, goal: goals.carbs },
    }
  }
  const ratio = log.goal > 0 ? log.kcal / log.goal : 0
  // Subtle deterministic per-macro wobble so the four bars don't read identical.
  const wob = (seed: number) => 1 + (((log.date * seed) % 9) - 4) / 100
  const scale = (base: number, seed: number) => Math.max(0, Math.round(base * ratio * wob(seed)))
  return {
    protein: { consumed: scale(goals.protein, 7), goal: goals.protein },
    calories: { consumed: log.kcal, goal: goals.calories },
    fat: { consumed: scale(goals.fat, 3), goal: goals.fat },
    carbs: { consumed: scale(goals.carbs, 5), goal: goals.carbs },
  }
}

function statusLabel(log: DayLog | undefined): string {
  if (!log) return 'No meals logged'
  const ratio = log.kcal / log.goal
  if (ratio >= 1) return 'Goal reached'
  if (ratio >= 0.85) return 'On track'
  return 'Under goal'
}

export function CalendarScreen() {
  const nav = useNav()
  const [viewYear, setViewYear] = useState(DATA_YEAR)
  const [viewMonth, setViewMonth] = useState(DATA_MONTH)
  const [selected, setSelected] = useState<Selection | null>(null)

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
  const dayLogs = Array.from({ length: daysInMonth }, (_, i) => calendarDays[i] as DayLog | undefined)
  const logged = dayLogs.filter((d): d is DayLog => !!d)
  const reachedCount = logged.filter((d) => d.kcal >= d.goal).length
  const avgAdherence = logged.length
    ? logged.reduce((sum, d) => sum + d.kcal / d.goal, 0) / logged.length
    : 0

  const detailLog = selected ? (calendarDays[selected.date - 1] as DayLog | undefined) : undefined
  const detailRatio = detailLog ? detailLog.kcal / detailLog.goal : 0
  const detailWater = Math.min(goals.water, Math.round(detailRatio * goals.water))
  const detailTitle = selected
    ? `${WEEKDAYS_FULL[new Date(selected.year, selected.month, selected.date).getDay()]} `
      + `${selected.date} ${MONTHS_FULL[selected.month]}`
    : undefined

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

      {/* Month adherence summary */}
      <div className="mt-4 bg-surface-1 rounded-card p-3.5 flex items-center gap-3.5">
        <DayRing progress={avgAdherence} reached={avgAdherence >= 1} size={48} stroke={4}>
          <span className="text-[12px] font-bold text-ink tnum">{Math.round(avgAdherence * 100)}%</span>
        </DayRing>
        <div className="min-w-0">
          <div className="text-[15px] font-semibold text-ink">
            {reachedCount} of {logged.length} days hit goal
          </div>
          <div className="text-[12px] text-ink-2">{Math.round(avgAdherence * 100)}% average adherence this month</div>
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
        {dayLogs.map((log, i) => {
          const date = i + 1
          const isToday = viewYear === DATA_YEAR && viewMonth === DATA_MONTH && date === TODAY_DATE
          return (
            <DayCell
              key={date}
              date={date}
              log={log}
              isToday={isToday}
              delay={Math.min(date * 0.01, 0.3)}
              onSelect={() => {
                haptic('light')
                setSelected({ year: viewYear, month: viewMonth, date })
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
            // The picker opens ON TOP — the day page stays underneath.
            onClick={() => nav.open({ kind: 'add' })}
          >
            <Icon name="plus" size={16} /> Add for this day
          </Button>
        }
      >
        {selected && (
          <div>
            {/* Day summary */}
            <div className="pt-3 flex items-center gap-3.5">
              <DayRing progress={detailRatio} reached={!!detailLog && detailLog.kcal >= detailLog.goal} size={56} stroke={5}>
                <span className="text-[13px] font-bold text-ink tnum">{Math.round(detailRatio * 100)}%</span>
              </DayRing>
              <div className="min-w-0">
                <div className="text-[20px] font-bold text-ink tnum">
                  {(detailLog?.kcal ?? 0).toLocaleString()}
                  <span className="text-[13px] text-ink-3 font-semibold"> / {goals.calories.toLocaleString()} kcal</span>
                </div>
                <div className="text-[13px] text-ink-2">{statusLabel(detailLog)}</div>
              </div>
            </div>

            <SectionLabel>Nutrition</SectionLabel>
            <MacroGrid {...dayMacros(detailLog)} />

            <SectionLabel>Water</SectionLabel>
            <div className="bg-surface-1 rounded-card px-4 py-3 flex items-center gap-3">
              <div className="size-9 rounded-lg bg-surface-3 flex items-center justify-center shrink-0">
                <Icon name="drop.fill" size={15} className="text-water" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-[16px] font-bold text-ink tnum">
                  {(detailWater / 1000).toFixed(1)} L
                  <span className="text-ink-3 font-semibold"> / {(goals.water / 1000).toFixed(1)} L</span>
                </div>
                <div className="pt-2">
                  <ProgressBar value={detailWater / goals.water} color={colors.water} />
                </div>
              </div>
            </div>

            {detailLog && (
              <>
                <SectionLabel>Meals</SectionLabel>
                <div className="flex flex-col gap-1.5">
                  {meals.map((meal) => (
                    <SheetMealRow
                      key={meal.id}
                      meal={meal}
                      onTap={() => {
                        haptic('light')
                        // Meal drawer slides over the day page; closing it
                        // returns here instead of kicking back to the grid.
                        nav.open({ kind: 'mealDetail', meal })
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
