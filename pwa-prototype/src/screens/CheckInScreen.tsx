import { useState } from 'react'
import { motion } from 'motion/react'
import { ScreenScroll } from '../components/ScreenScroll.tsx'
import { PushPage } from '../components/PushPage.tsx'
import { Icon, type IconName } from '../components/Icon.tsx'
import { ProgressBar } from '../components/ProgressBar.tsx'
import { MacroGrid } from '../components/MacroCard.tsx'
import { SectionLabel } from '../components/SectionLabel.tsx'
import { Card } from '../components/Card.tsx'
import { CheckInFormSheet } from './sheets/CheckInFormSheet.tsx'
import { PdfExportOptionsSheet } from './sheets/PdfExportOptionsSheet.tsx'
import { Sheet } from '../components/Sheet.tsx'
import { ConfirmDialog } from '../components/ConfirmDialog.tsx'
import { CheckInStatsView } from './CheckInStatsView.tsx'
import { SeriesChart } from './SeriesChart.tsx'
import { haptic } from '../lib/ios.ts'
import { colors } from '../theme/tokens.ts'
import { useStore } from '../store.tsx'
import { useToast } from '../components/Toast.tsx'
import { toCheckInView, weightTrendOf, type CheckInView } from '../lib/checkinView.ts'
import type { CheckinRow } from '../api/checkins.ts'
import { MEASUREMENT_FIELDS, type CheckIn, type Measurements } from '../data/mock.ts'

type Trend = { color: string; icon: IconName | null }

// Weight loss reads as success (green ↓), gain as error (red ↑), flat as neutral.
function trendOf(delta: number): Trend {
  if (delta < -0.001) return { color: 'text-success', icon: 'trend.down' }
  if (delta > 0.001) return { color: 'text-error', icon: 'trend.up' }
  return { color: 'text-ink-3', icon: null }
}

// A measurement's delta is "good" or "bad" depending on lowerIsBetter, not on
// its raw sign — a +0.3 cm chest is progress, a +0.3 cm waist is not.
function measurementTrend(delta: number, lowerIsBetter: boolean): Trend {
  if (Math.abs(delta) < 0.001) return { color: 'text-ink-3', icon: null }
  const improving = lowerIsBetter ? delta < 0 : delta > 0
  const icon: IconName = delta < 0 ? 'trend.down' : 'trend.up'
  return { color: improving ? 'text-success' : 'text-error', icon }
}

const FEELING: Record<CheckIn['feeling'], { color: string; bg: string }> = {
  'On track': { color: 'text-primary', bg: 'bg-primary/15' },
  Strong: { color: 'text-success', bg: 'bg-success/15' },
  Tired: { color: 'text-warning', bg: 'bg-warning/15' },
}

const fmtSigned = (delta: number, digits = 1) => `${delta > 0 ? '+' : ''}${delta.toFixed(digits)}`

// QUICK STATS delta string — neutral grey per spec: "=" / "↑ +0.2" / "↓ -0.6" / "—".
function quickDelta(delta: number | undefined): string {
  if (delta === undefined) return '—'
  if (Math.abs(delta) < 0.001) return '='
  return `${delta > 0 ? '↑ +' : '↓ -'}${Math.abs(delta).toFixed(1)}`
}

// 312 min → "5h 12m"; 45 min → "45m"
function fmtDuration(totalMin: number): string {
  const hrs = Math.floor(totalMin / 60)
  const mins = totalMin % 60
  if (hrs === 0) return `${mins}m`
  return `${hrs}h ${mins.toString().padStart(2, '0')}m`
}

// 48250 kg → "48.3k"; 980 → "980"
function fmtVolume(kg: number): string {
  if (kg >= 1000) return `${(kg / 1000).toFixed(1)}k`
  return String(kg)
}

// Inline weight curve — thin wrapper over the shared SeriesChart, feeding it a
// plain number[] (weekly weights) as an unlabelled orange line+area.
function WeightSparkline({ data, height }: { data: number[]; height: number }) {
  return <SeriesChart series={data.map((value, i) => ({ week: `W${i}`, value }))} color={colors.primary} height={height} />
}

// ---- Trends header card (Screen A) ----
function TrendsCard({ data }: { data: number[] }) {
  const current = data[data.length - 1]
  const start = data[0]
  const delta = current - start
  const tr = trendOf(delta)
  return (
    <Card padding="p-4">
      <div className="flex items-center justify-between">
        <span className="text-[11px] font-semibold uppercase tracking-wide text-ink-3">Weight trend</span>
        <span className="text-[11px] text-ink-4">Last {data.length} weeks</span>
      </div>
      <div className="mt-2 flex items-baseline gap-1.5">
        <span className="text-[28px] font-bold text-ink tnum leading-none">{current.toFixed(1)}</span>
        <span className="text-[15px] font-semibold text-ink-3">kg</span>
        <span className="flex-1" />
        <span className={`flex items-center gap-1 ${tr.color}`}>
          {tr.icon && <Icon name={tr.icon} size={14} />}
          <span className="text-[13px] font-semibold tnum">{Math.abs(delta).toFixed(1)} kg</span>
        </span>
      </div>
      <div className="mt-3">
        <WeightSparkline data={data} height={64} />
      </div>
      <div className="mt-1 flex justify-between text-[11px] text-ink-4 tnum">
        <span>{start.toFixed(1)}</span>
        <span>{current.toFixed(1)}</span>
      </div>
    </Card>
  )
}

// ---- Check-in list row ----
function CheckInRow({ checkIn, onTap }: { checkIn: CheckIn; onTap: () => void }) {
  const deltaWeight = checkIn.deltas.weight ?? 0
  const tr = trendOf(deltaWeight)
  const feel = FEELING[checkIn.feeling]
  const pct = Math.round(checkIn.adherence * 100)
  return (
    <motion.button
      type="button"
      whileTap={{ scale: 0.99, backgroundColor: colors.surface2 }}
      transition={{ duration: 0.15 }}
      onClick={() => {
        haptic('light')
        onTap()
      }}
      className="w-full bg-surface-1 rounded-card p-4 text-left"
    >
      <div className="flex items-center gap-3">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <span className="text-[15px] font-semibold text-ink">{checkIn.weekLabel}</span>
            <span className={`px-2 py-0.5 rounded-full text-[11px] font-semibold ${feel.color} ${feel.bg}`}>
              {checkIn.feeling}
            </span>
          </div>
          <div className="mt-0.5 text-[12px] text-ink-3 tnum">{checkIn.dateRange}</div>
        </div>
        <div className="text-right">
          <div className="text-[17px] font-semibold text-ink tnum leading-none">
            {checkIn.measurements.weight.toFixed(1)}
            <span className="text-[12px] text-ink-3 font-medium"> kg</span>
          </div>
          <div className={`mt-1 flex items-center justify-end gap-1 ${tr.color}`}>
            {tr.icon && <Icon name={tr.icon} size={12} />}
            <span className="text-[12px] font-semibold tnum">{Math.abs(deltaWeight).toFixed(1)}</span>
          </div>
        </div>
        <Icon name="chevron.right" size={15} className="text-ink-4 shrink-0" />
      </div>
      <div className="mt-3 flex items-center gap-2.5">
        <div className="flex-1">
          <ProgressBar value={checkIn.adherence} color={colors.primary} height={5} />
        </div>
        <span className="text-[11px] font-semibold text-ink-2 tnum">{pct}%</span>
      </div>
      <div className="mt-2.5 flex items-center gap-3 text-[12px] text-ink-3">
        <span className="flex items-center gap-1">
          <Icon name="camera" size={13} />
          <span className="tnum">{checkIn.photoCount}</span>
        </span>
        {checkIn.nutrition.aiSummary && (
          <span className="flex items-center gap-1 text-primary font-semibold">
            <Icon name="sparkles" size={13} />
            AI
          </span>
        )}
      </div>
    </motion.button>
  )
}

// ---- Quick-stat card (Screen A header) ----
function QuickStatCard({ title, value, unit, delta }: { title: string; value: number; unit: string; delta: number }) {
  return (
    <div className="flex-1 bg-surface-1 rounded-card p-3 flex flex-col gap-2">
      <div className="text-[11px] font-semibold uppercase tracking-[0.06em] text-ink-3 truncate">{title}</div>
      <div className="flex items-baseline gap-0.5">
        <span className="text-[17px] font-semibold text-ink tnum leading-none">{value.toFixed(1)}</span>
        <span className="text-[12px] text-ink-2">{unit}</span>
      </div>
      <div className="text-[12px] text-ink-2 tnum">{quickDelta(delta)}</div>
    </div>
  )
}

function QuickStatsSection({ checkIn, onOpenStats }: { checkIn: CheckIn; onOpenStats: () => void }) {
  const { measurements, deltas } = checkIn
  const avgArms = (measurements.armLeft + measurements.armRight) / 2
  const avgArmsDelta = ((deltas.armLeft ?? 0) + (deltas.armRight ?? 0)) / 2
  return (
    <>
      <SectionLabel>Quick stats</SectionLabel>
      <div className="flex gap-2">
        <QuickStatCard title="Weight" value={measurements.weight} unit="kg" delta={deltas.weight ?? 0} />
        <QuickStatCard title="Avg. arms" value={avgArms} unit="cm" delta={avgArmsDelta} />
        <QuickStatCard title="Waist" value={measurements.waist} unit="cm" delta={deltas.waist ?? 0} />
      </div>
      <button
        type="button"
        onClick={() => {
          haptic('light')
          onOpenStats()
        }}
        className="mt-3 flex items-center gap-1.5 text-primary"
      >
        <span className="text-[12px] font-semibold">View detailed stats</span>
        <Icon name="chevron.right" size={11} />
      </button>
    </>
  )
}

// ---- Square stat tile (measurements + training grids) ----
function StatTile({
  label,
  value,
  unit,
  delta,
}: {
  label: string
  value: string
  unit?: string
  delta?: { text: string; trend: Trend }
}) {
  return (
    <div className="aspect-square bg-surface-1 rounded-card p-3.5 flex flex-col">
      <div className="text-[12px] text-ink-3 truncate">{label}</div>
      <div className="flex-1 flex items-end">
        <div className="flex items-baseline gap-1 min-w-0">
          <span className="text-[20px] font-bold text-ink tnum leading-none">{value}</span>
          {unit && <span className="text-[12px] text-ink-3 font-medium shrink-0">{unit}</span>}
        </div>
      </div>
      {delta ? (
        <div className={`mt-2 flex items-center gap-1 ${delta.trend.color}`}>
          {delta.trend.icon && <Icon name={delta.trend.icon} size={12} />}
          <span className="text-[12px] font-semibold tnum">{delta.text}</span>
        </div>
      ) : (
        <div className="mt-2 h-[15px]" />
      )}
    </div>
  )
}

// ---- Pushed detail view content (Screen B) ----
function HeroCard({ checkIn }: { checkIn: CheckIn }) {
  const deltaWeight = checkIn.deltas.weight ?? 0
  const tr = trendOf(deltaWeight)
  const feel = FEELING[checkIn.feeling]
  const pct = Math.round(checkIn.adherence * 100)
  return (
    <Card padding="p-5">
      <div className="flex items-end justify-between">
        <div className="min-w-0">
          <div className="text-[11px] font-semibold uppercase tracking-wide text-ink-3">Weight</div>
          <div className="mt-1.5 text-[40px] font-bold text-ink tnum leading-none">
            {checkIn.measurements.weight.toFixed(1)}
            <span className="text-[18px] text-ink-3 font-semibold"> kg</span>
          </div>
        </div>
        <div className={`mb-1 flex items-center gap-1.5 shrink-0 ${tr.color}`}>
          {tr.icon && <Icon name={tr.icon} size={18} />}
          <span className="text-[18px] font-bold tnum">{fmtSigned(deltaWeight)} kg</span>
        </div>
      </div>
      <div className="mt-4 flex items-center gap-2">
        <span className={`px-2.5 py-1 rounded-full text-[12px] font-semibold ${feel.color} ${feel.bg}`}>
          {checkIn.feeling}
        </span>
        <span className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-surface-2 text-[12px] font-semibold text-ink-2">
          <Icon name="check" size={12} className="text-success" />
          <span className="tnum">{pct}%</span> adherence
        </span>
      </div>
    </Card>
  )
}

function MeasurementsGrid({ measurements, deltas }: { measurements: Measurements; deltas: Partial<Measurements> }) {
  return (
    <div className="grid grid-cols-3 gap-2.5">
      {MEASUREMENT_FIELDS.map((field) => {
        const value = measurements[field.key]
        const delta = deltas[field.key]
        const tr = delta !== undefined ? measurementTrend(delta, field.lowerIsBetter) : { color: 'text-ink-3', icon: null }
        return (
          <StatTile
            key={field.key}
            label={field.label}
            value={value.toFixed(1)}
            unit={field.unit}
            delta={delta !== undefined ? { text: fmtSigned(delta), trend: tr } : undefined}
          />
        )
      })}
    </div>
  )
}

function FeelingsSection({ checkIn }: { checkIn: CheckIn }) {
  return (
    <>
      <div className="flex flex-wrap gap-2">
        {checkIn.feelingTags.map((tag) => (
          <span
            key={tag.id}
            className={`px-3 h-8 inline-flex items-center rounded-full text-[13px] font-semibold ${
              tag.positive ? 'bg-success/15 text-success' : 'bg-warning/15 text-warning'
            }`}
          >
            {tag.label}
          </span>
        ))}
      </div>
      <div className="mt-3 flex flex-col gap-2.5">
        <Card padding="p-4">
          <div className="text-[12px] font-semibold uppercase tracking-[0.05em] text-ink-3">Mental</div>
          <p className="mt-1.5 text-[15px] leading-snug text-ink-2">{checkIn.mentalFeeling}</p>
        </Card>
        <Card padding="p-4">
          <div className="text-[12px] font-semibold uppercase tracking-[0.05em] text-ink-3">Physical</div>
          <p className="mt-1.5 text-[15px] leading-snug text-ink-2">{checkIn.physicalFeeling}</p>
        </Card>
      </div>
    </>
  )
}

function NutritionSection({ checkIn }: { checkIn: CheckIn }) {
  const { nutrition } = checkIn
  return (
    <>
      <MacroGrid
        protein={{ consumed: nutrition.avgProtein }}
        calories={{ consumed: nutrition.avgCalories }}
        fat={{ consumed: nutrition.avgFat }}
        carbs={{ consumed: nutrition.avgCarbs }}
      />
      <div className="mt-2.5 grid grid-cols-2 gap-2.5">
        <div className="bg-surface-1 rounded-card p-4 flex items-center gap-3">
          <div
            className="size-9 rounded-[10px] flex items-center justify-center shrink-0"
            style={{ backgroundColor: `${colors.water}2E`, boxShadow: `inset 0 0 0 1px ${colors.water}33` }}
          >
            <Icon name="drop.fill" size={15} color={colors.water} />
          </div>
          <div className="min-w-0">
            <div className="text-[18px] font-bold text-ink tnum leading-none">
              {(nutrition.avgWater / 1000).toFixed(1)}
              <span className="text-[12px] text-ink-3 font-medium"> L</span>
            </div>
            <div className="text-[12px] text-ink-3 mt-1">Avg water</div>
          </div>
        </div>
        <div className="bg-surface-1 rounded-card p-4 flex items-center gap-3">
          <div className="size-9 rounded-[10px] bg-surface-2 flex items-center justify-center shrink-0">
            <Icon name="calendar" size={15} className="text-ink-2" />
          </div>
          <div className="min-w-0">
            <div className="text-[18px] font-bold text-ink tnum leading-none">{nutrition.days} / 7</div>
            <div className="text-[12px] text-ink-3 mt-1">Days logged</div>
          </div>
        </div>
      </div>
      <div className="mt-2.5 rounded-card p-4 bg-primary/10 ring-1 ring-inset ring-primary/20">
        <div className="flex items-center gap-2">
          <Icon name="sparkles" size={15} className="text-primary" />
          <span className="text-[12px] font-semibold uppercase tracking-[0.05em] text-primary">Coach note</span>
        </div>
        <p className="mt-2 text-[15px] leading-snug text-ink">{nutrition.aiSummary}</p>
      </div>
    </>
  )
}

function TrainingGrid({ checkIn }: { checkIn: CheckIn }) {
  const { training } = checkIn
  return (
    <div className="grid grid-cols-2 gap-2.5">
      <StatTile label="Sessions" value={String(training.sessions)} />
      <StatTile label="Time trained" value={fmtDuration(training.durationMin)} />
      <StatTile label="Total volume" value={fmtVolume(training.volumeKg)} unit="kg" />
      <StatTile label="Avg RPE" value={training.avgRpe.toFixed(1)} />
    </div>
  )
}

function PhotosRow({ count }: { count: number }) {
  return (
    <div className="flex gap-2.5">
      {Array.from({ length: count }, (_, i) => (
        <div
          key={i}
          className="flex-1 aspect-square rounded-card bg-surface-2 flex items-center justify-center"
        >
          <Icon name="photo" size={22} className="text-ink-4" />
        </div>
      ))}
    </div>
  )
}

// Detail content rendered INSIDE <PushPage> (PushPage provides nav bar + the
// scroll region `px-5 pb-32`, so sections here add only vertical rhythm).
function CheckInDetailContent({ checkIn, data }: { checkIn: CheckIn; data: number[] }) {
  const feel = FEELING[checkIn.feeling]
  return (
    <>
      <div className="pt-1 pb-2">
        <h1 className="text-[28px] font-bold tracking-tight text-ink">{checkIn.weekLabel}</h1>
        <p className="mt-0.5 text-[14px] text-ink-3 tnum">
          {checkIn.dateRange} · <span className={feel.color}>{checkIn.feeling}</span>
        </p>
      </div>

      <div className="pt-2">
        <HeroCard checkIn={checkIn} />
      </div>

      <SectionLabel>Measurements</SectionLabel>
      <MeasurementsGrid measurements={checkIn.measurements} deltas={checkIn.deltas} />

      {data.length >= 2 && (
        <>
          <SectionLabel>Weight trend</SectionLabel>
          <Card padding="p-4">
            <WeightSparkline data={data} height={140} />
            <div className="mt-2 flex justify-between text-[11px] text-ink-4 tnum">
              <span>{data[0].toFixed(1)} kg</span>
              <span>{data[data.length - 1].toFixed(1)} kg</span>
            </div>
          </Card>
        </>
      )}

      <SectionLabel>How you felt</SectionLabel>
      <FeelingsSection checkIn={checkIn} />

      <SectionLabel>Nutrition · weekly average</SectionLabel>
      <NutritionSection checkIn={checkIn} />

      <SectionLabel>Training</SectionLabel>
      <TrainingGrid checkIn={checkIn} />

      <SectionLabel>Progress photos</SectionLabel>
      <PhotosRow count={checkIn.photoCount} />
    </>
  )
}

// A row inside the check-in detail actions sheet.
function ActionRow({
  icon,
  label,
  destructive = false,
  onClick,
}: {
  icon: IconName
  label: string
  destructive?: boolean
  onClick: () => void
}) {
  return (
    <motion.button
      type="button"
      whileTap={{ scale: 0.99, backgroundColor: '#151B38' }}
      transition={{ duration: 0.12 }}
      onClick={() => {
        haptic('light')
        onClick()
      }}
      className="w-full bg-surface-1 rounded-card h-[54px] px-4 flex items-center gap-3.5 text-left"
    >
      <Icon name={icon} size={19} className={destructive ? 'text-error' : 'text-primary'} />
      <span className={`text-[16px] font-medium ${destructive ? 'text-error' : 'text-ink'}`}>{label}</span>
    </motion.button>
  )
}

export function CheckInScreen() {
  const store = useStore()
  const toast = useToast()
  const rows = store.state.checkins
  const calorieGoal = store.state.goals.calories
  const [view, setView] = useState<'list' | 'detail' | 'stats'>('list')
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<CheckinRow | null>(null)
  const [actionsOpen, setActionsOpen] = useState(false)
  const [pdfOpen, setPdfOpen] = useState(false)
  const [confirmDeleteCheckIn, setConfirmDeleteCheckIn] = useState(false)

  // API rows adapted to the view model the sections render (deltas vs previous).
  const checkIns: CheckInView[] = rows.map((row, i) => toCheckInView(row, rows[i + 1], calorieGoal))
  const weightTrend = weightTrendOf(rows)

  // Always render the live check-in (stays fresh after an edit).
  const selected = selectedId ? (checkIns.find((c) => c.id === selectedId) ?? null) : null

  const openDetail = (checkIn: CheckInView) => {
    setSelectedId(checkIn.id)
    setView('detail')
  }

  return (
    <div className="relative h-full overflow-hidden">
      <ScreenScroll
        title="Check-ins"
        trailing={
          <button
            type="button"
            aria-label="New check-in"
            onClick={() => {
              haptic('medium')
              setEditTarget(null)
              setFormOpen(true)
            }}
            className="size-11 flex items-center justify-center text-primary"
          >
            <Icon name="plus" size={22} />
          </button>
        }
      >
        {checkIns.length > 0 && (
          <QuickStatsSection checkIn={checkIns[0]} onOpenStats={() => setView('stats')} />
        )}

        {weightTrend.length >= 2 && (
          <div className="pt-3">
            <TrendsCard data={weightTrend} />
          </div>
        )}

        <SectionLabel>Recent check-ins</SectionLabel>
        <div className="flex flex-col gap-2.5">
          {checkIns.length === 0 && (
            <div className="py-10 flex flex-col items-center gap-2 text-center">
              <Icon name="chart.bar" size={26} className="text-ink-4" />
              <div className="text-[15px] text-ink-2">No check-ins yet</div>
              <div className="text-[12px] text-ink-4">Tap + to log your first weekly check-in.</div>
            </div>
          )}
          {checkIns.map((checkIn) => (
            <CheckInRow key={checkIn.id} checkIn={checkIn} onTap={() => openDetail(checkIn)} />
          ))}
        </div>
      </ScreenScroll>

      <CheckInFormSheet open={formOpen} onClose={() => setFormOpen(false)} editing={editTarget} />

      <PushPage
        open={view === 'detail' && selected !== null}
        onClose={() => setView('list')}
        title={selected?.weekLabel ?? ''}
        trailing={
          <button
            type="button"
            aria-label="Check-in options"
            onClick={() => {
              haptic('light')
              setActionsOpen(true)
            }}
            className="size-11 flex items-center justify-center text-primary"
          >
            <Icon name="ellipsis" size={22} />
          </button>
        }
      >
        {selected && <CheckInDetailContent checkIn={selected} data={weightTrend} />}
      </PushPage>

      {/* Detail actions */}
      <Sheet open={actionsOpen} onClose={() => setActionsOpen(false)} title="" detents={['small']}>
        <div className="flex flex-col gap-2 pt-1 pb-3">
          <ActionRow
            icon="pencil"
            label="Edit check-in"
            onClick={() => {
              setActionsOpen(false)
              setEditTarget(selected?.row ?? null)
              setFormOpen(true)
            }}
          />
          <ActionRow
            icon="share"
            label="Export as PDF"
            onClick={() => {
              setActionsOpen(false)
              setPdfOpen(true)
            }}
          />
          <ActionRow
            icon="trash"
            label="Delete check-in"
            destructive
            onClick={() => {
              setActionsOpen(false)
              setConfirmDeleteCheckIn(true)
            }}
          />
        </div>
      </Sheet>

      <PdfExportOptionsSheet open={pdfOpen} onClose={() => setPdfOpen(false)} />
      <ConfirmDialog
        open={confirmDeleteCheckIn}
        title={selected ? `Delete ${selected.weekLabel}?` : 'Delete check-in?'}
        message="This check-in and its photos will be removed. This can't be undone."
        confirmLabel="Delete"
        onCancel={() => setConfirmDeleteCheckIn(false)}
        onConfirm={() => {
          setConfirmDeleteCheckIn(false)
          const row = selected?.row
          setView('list')
          if (row) {
            void store
              .deleteCheckin(row)
              .then(() => toast.show('Check-in deleted'))
              .catch(() => {})
          }
        }}
      />

      <PushPage open={view === 'stats'} onClose={() => setView('list')} title="Trends">
        <CheckInStatsView />
      </PushPage>
    </div>
  )
}
