import { useState } from 'react'
import type { ReactNode } from 'react'
import { Card } from '../components/Card.tsx'
import { Icon, type IconName } from '../components/Icon.tsx'
import { ProgressBar } from '../components/ProgressBar.tsx'
import { Ring } from '../components/Ring.tsx'
import { Segmented } from '../components/Segmented.tsx'
import { SectionLabel } from '../components/SectionLabel.tsx'
import { colors } from '../theme/tokens.ts'
import { checkInStats, type MacroCompliance } from '../data/mock.ts'
import { SeriesChart } from './SeriesChart.tsx'

// Period segments — purely visual for the mock (data does not change).
type Period = '4w' | '12w' | 'all'
const PERIODS: { value: Period; label: string }[] = [
  { value: '4w', label: '4 wk' },
  { value: '12w', label: '12 wk' },
  { value: 'all', label: 'All' },
]

// Amber used for nutrition icon + mid-range compliance (matches spec strakkWarning).
const WARNING = '#E0A84D'

// Volume: ≥1000 kg renders as tonnes ("11.9t"), else "{n} kg".
function formatVolume(kg: number): string {
  if (kg >= 1000) return `${(kg / 1000).toFixed(1)}t`
  return `${Math.round(kg)} kg`
}

// 3120 min → "52h00"; 45 min → "45m".
function formatDuration(totalMin: number): string {
  const hrs = Math.floor(totalMin / 60)
  const mins = totalMin % 60
  if (hrs === 0) return `${mins}m`
  return `${hrs}h${mins.toString().padStart(2, '0')}`
}

// Compliance threshold colors (gauge): ≥80 green, ≥50 amber, else red.
function complianceColor(pct: number): string {
  if (pct >= 80) return colors.success
  if (pct >= 50) return WARNING
  return colors.error
}

// Per-macro fill color; ProgressBar auto-switches to success green at ≥100%.
const MACRO_COLOR: Record<MacroCompliance['name'], string> = {
  Calories: colors.primaryLight,
  Protein: colors.protein,
  Carbs: colors.carbs,
  Fat: colors.fat,
}

// ---- 2×2 overview card ----
function OverviewCard({
  icon,
  iconColor,
  label,
  children,
}: {
  icon: IconName
  iconColor: string
  label: string
  children: ReactNode
}) {
  return (
    <Card padding="p-4">
      <div className="flex flex-col gap-3">
        <div
          className="size-9 rounded-[8px] flex items-center justify-center"
          style={{ backgroundColor: `${iconColor}2E` }}
        >
          <Icon name={icon} size={16} color={iconColor} />
        </div>
        {children}
        <div className="text-[12px] text-ink-3">{label}</div>
      </div>
    </Card>
  )
}

// Weight overview: gain (delta>0) reads red ↑, loss reads green ↓, flat neutral.
function WeightTrendLabel({ delta }: { delta: number }) {
  if (Math.abs(delta) < 0.001) {
    return <span className="text-[12px] text-ink-3 tnum">0.0 kg</span>
  }
  const gained = delta > 0
  const color = gained ? 'text-error' : 'text-success'
  const sign = gained ? '+' : ''
  return (
    <span className={`flex items-center gap-1 text-[12px] font-semibold tnum ${color}`}>
      <Icon name={gained ? 'trend.up' : 'trend.down'} size={12} />
      {sign}
      {delta.toFixed(1)} kg
    </span>
  )
}

// ---- Macro / water compliance row ----
function MacroComplianceRow({ name, pct, color }: { name: string; pct: number; color: string }) {
  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center justify-between">
        <span className="text-[15px] text-ink-2">{name}</span>
        <span className="text-[12px] text-ink-3 tnum">{pct}%</span>
      </div>
      <ProgressBar value={pct / 100} color={color} height={6} />
    </div>
  )
}

function WaterRow({ avg, goal }: { avg: number; goal: number }) {
  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center justify-between">
        <span className="flex items-center gap-1.5 text-[15px] text-ink-2">
          <Icon name="drop.fill" size={12} color={colors.water} />
          Water
        </span>
        <span className="text-[12px] text-ink-3 tnum">
          {avg} / {goal} ml
        </span>
      </div>
      <ProgressBar value={avg / goal} color={colors.water} height={6} reached={false} />
    </div>
  )
}

// ---- Inline summary stat split by vertical dividers ----
function StatInlineItem({ value, label }: { value: string; label: string }) {
  return (
    <div className="flex-1 flex flex-col items-center gap-1">
      <span className="text-[15px] font-semibold text-ink tnum">{value}</span>
      <span className="text-[12px] text-ink-3">{label}</span>
    </div>
  )
}

// Stats content rendered INSIDE <PushPage> (PushPage provides nav bar + the
// scroll region `px-5 pb-32`, so sections here add only vertical rhythm).
export function CheckInStatsView() {
  const [period, setPeriod] = useState<Period>('12w')
  const stats = checkInStats
  const consistencyPct = Math.round((stats.consistency.weeks / stats.consistency.total) * 100)

  return (
    <>
      <div className="pt-1 pb-2">
        <h1 className="text-[28px] font-bold tracking-tight text-ink">Trends</h1>
      </div>

      <div className="pt-2">
        <Segmented options={PERIODS} value={period} onChange={setPeriod} />
      </div>

      {/* 2×2 overview grid */}
      <div className="pt-4 grid grid-cols-2 gap-3">
        <OverviewCard icon="scale" iconColor={colors.primary} label="Weight">
          <div className="flex flex-col gap-1">
            <span className="text-[22px] font-bold text-ink tnum leading-none">{stats.weightKg.toFixed(1)} kg</span>
            <WeightTrendLabel delta={stats.weightDelta} />
          </div>
        </OverviewCard>

        <OverviewCard icon="dumbbell.fill" iconColor={colors.carbs} label="Avg volume / wk">
          <span className="text-[22px] font-bold text-ink tnum leading-none">
            {formatVolume(stats.avgWeeklyVolumeKg)}
          </span>
        </OverviewCard>

        <OverviewCard icon="chart.bar" iconColor={colors.success} label="Sessions / wk">
          <span className="text-[22px] font-bold text-ink tnum leading-none">{stats.sessionsPerWeek.toFixed(1)}</span>
        </OverviewCard>

        <OverviewCard icon="fork" iconColor={WARNING} label="Nutrition">
          <div className="flex items-center gap-2">
            <span className="text-[22px] font-bold text-ink tnum leading-none">{stats.nutritionCompliance}%</span>
            <Ring
              progress={stats.nutritionCompliance / 100}
              size={30}
              stroke={4}
              color={complianceColor(stats.nutritionCompliance)}
            />
          </div>
        </OverviewCard>
      </div>

      {/* Weight chart */}
      <SectionLabel>Weight</SectionLabel>
      <Card padding="p-4">
        <SeriesChart series={stats.weightSeries} color={colors.primary} height={150} labels />
      </Card>

      {/* Training */}
      <SectionLabel>Training</SectionLabel>
      <Card padding="p-4">
        <SeriesChart series={stats.volumeSeries} color={colors.carbs} height={120} />
        <div className="mt-4 flex items-stretch">
          <StatInlineItem value={String(stats.training.totalSessions)} label="Sessions" />
          <div className="w-px self-center h-8 bg-divider/60" />
          <StatInlineItem value={formatDuration(stats.training.totalDurationMin)} label="Duration" />
          <div className="w-px self-center h-8 bg-divider/60" />
          <StatInlineItem value={formatVolume(stats.training.totalVolumeKg)} label="Volume" />
        </div>
      </Card>

      {/* Nutrition */}
      <SectionLabel>Nutrition</SectionLabel>
      <Card padding="p-4">
        <div className="flex flex-col gap-4">
          {stats.macros.map((macro) => (
            <MacroComplianceRow key={macro.name} name={macro.name} pct={macro.pct} color={MACRO_COLOR[macro.name]} />
          ))}
          <WaterRow avg={stats.water.avg} goal={stats.water.goal} />
        </div>
      </Card>

      {/* Consistency */}
      <SectionLabel>Consistency</SectionLabel>
      <Card padding="p-4">
        <div className="flex items-center justify-between">
          <span className="text-[15px] font-semibold text-ink tnum">
            {stats.consistency.weeks}/{stats.consistency.total} weeks
          </span>
          <span className="text-[17px] font-semibold text-primary tnum">{consistencyPct}%</span>
        </div>
        <div className="mt-3">
          <ProgressBar
            value={stats.consistency.weeks / stats.consistency.total}
            color={colors.primary}
            height={8}
            reached={false}
          />
        </div>
      </Card>
    </>
  )
}
