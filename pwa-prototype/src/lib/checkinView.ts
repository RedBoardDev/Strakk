import { dateRangeLabel } from './dates.ts'
import type { CheckinRow } from '../api/checkins.ts'
import type { CheckIn, FeelingTag, MacroCompliance, Measurements, SeriesPoint } from '../data/viewTypes.ts'
import type { Goals } from '../store.tsx'

// Adapts an API checkins row to the view model the check-in screens render.
// Keys: UI measurement name ↔ DB column.
export const MEASUREMENT_COLUMNS: Record<keyof Measurements, keyof CheckinRow> = {
  weight: 'weight_kg',
  shoulders: 'shoulders_cm',
  chest: 'chest_cm',
  armLeft: 'arm_left_cm',
  armRight: 'arm_right_cm',
  waist: 'waist_cm',
  hips: 'hips_cm',
  thighLeft: 'thigh_left_cm',
  thighRight: 'thigh_right_cm',
}

// Known tag vocabulary (ids stored in feeling_tags).
export const TAG_LABELS: Record<string, { label: string; positive: boolean }> = {
  good_energy: { label: 'Good energy', positive: true },
  motivated: { label: 'Motivated', positive: true },
  strong_training: { label: 'Strong training', positive: true },
  good_sleep: { label: 'Slept well', positive: true },
  focused: { label: 'Focused', positive: true },
  disciplined: { label: 'Disciplined', positive: true },
  good_recovery: { label: 'Good recovery', positive: true },
  good_mood: { label: 'Good mood', positive: true },
  energy_stable: { label: 'Stable energy', positive: true },
  light_body: { label: 'Light body', positive: true },
  tired: { label: 'Tired', positive: false },
  stress: { label: 'Stress', positive: false },
  sore: { label: 'Sore', positive: false },
  hungry: { label: 'Hungry', positive: false },
  poor_sleep: { label: 'Poor sleep', positive: false },
  low_motivation: { label: 'Low motivation', positive: false },
}

function tagOf(id: string): FeelingTag {
  const known = TAG_LABELS[id]
  return {
    id,
    label: known?.label ?? id.replaceAll('_', ' ').replace(/^./, (c) => c.toUpperCase()),
    positive: known?.positive ?? true,
  }
}

export function measurementsOf(row: CheckinRow): Measurements {
  const out = {} as Measurements
  for (const [key, column] of Object.entries(MEASUREMENT_COLUMNS) as [keyof Measurements, keyof CheckinRow][]) {
    out[key] = (row[column] as number | null) ?? 0
  }
  return out
}

function deltasOf(row: CheckinRow, previous: CheckinRow | undefined): Partial<Measurements> {
  if (!previous) return {}
  const current = measurementsOf(row)
  const prior = measurementsOf(previous)
  const deltas: Partial<Measurements> = {}
  for (const key of Object.keys(MEASUREMENT_COLUMNS) as (keyof Measurements)[]) {
    if (current[key] === 0 || prior[key] === 0) continue
    const diff = Math.round((current[key] - prior[key]) * 10) / 10
    if (diff !== 0) deltas[key] = diff
  }
  return deltas
}

function feelingOf(tags: FeelingTag[]): CheckIn['feeling'] {
  const positive = tags.filter((t) => t.positive).length
  const negative = tags.length - positive
  if (negative > positive) return 'Tired'
  if (positive >= 4) return 'Strong'
  return 'On track'
}

// "2026-W27" → "Week 27"
export function weekTitle(weekLabel: string): string {
  const num = parseInt(weekLabel.split('-W')[1] ?? '', 10)
  return Number.isNaN(num) ? weekLabel : `Week ${num}`
}

export type CheckInView = CheckIn & { row: CheckinRow }

export function toCheckInView(row: CheckinRow, previous: CheckinRow | undefined, calorieGoal: number): CheckInView {
  const tags = (row.feeling_tags ?? []).map(tagOf)
  const training = row.training_stats
  return {
    row,
    id: row.id,
    weekLabel: weekTitle(row.week_label),
    dateRange: dateRangeLabel(row.covered_dates),
    createdAt: new Date(row.created_at).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }),
    feeling: feelingOf(tags),
    adherence: row.avg_calories && calorieGoal > 0 ? Math.min(1, row.avg_calories / calorieGoal) : 0,
    measurements: measurementsOf(row),
    deltas: deltasOf(row, previous),
    feelingTags: tags,
    mentalFeeling: row.mental_feeling ?? '',
    physicalFeeling: row.physical_feeling ?? '',
    nutrition: {
      avgCalories: Math.round(row.avg_calories ?? 0),
      avgProtein: Math.round(row.avg_protein ?? 0),
      avgFat: Math.round(row.avg_fat ?? 0),
      avgCarbs: Math.round(row.avg_carbs ?? 0),
      avgWater: row.avg_water ?? 0,
      days: row.nutrition_days ?? 0,
      aiSummary: row.ai_summary ?? '',
    },
    training: {
      sessions: training?.total_sessions ?? 0,
      durationMin: training?.total_duration_minutes ?? 0,
      volumeKg: training?.total_volume_kg ?? 0,
      avgRpe: training?.avg_rpe ?? 0,
    },
    photoCount: row.checkin_photos.length,
  }
}

// Ordered oldest → newest weights for the trend curve.
export function weightTrendOf(rows: CheckinRow[]): number[] {
  return rows
    .filter((r) => r.weight_kg != null)
    .sort((a, b) => a.week_label.localeCompare(b.week_label))
    .map((r) => r.weight_kg as number)
}

// "2026-W27" → "W27" for chart axis labels.
function shortWeek(weekLabel: string): string {
  const part = weekLabel.split('-')[1]
  return part ?? weekLabel
}

// Compliance % of an average against its goal (capped at 100).
function compliance(avg: number | null, goal: number): number {
  if (!avg || goal <= 0) return 0
  return Math.min(100, Math.round((avg / goal) * 100))
}

export type CheckInStats = {
  weightKg: number
  weightDelta: number
  avgWeeklyVolumeKg: number
  sessionsPerWeek: number
  nutritionCompliance: number
  weightSeries: SeriesPoint[]
  volumeSeries: SeriesPoint[]
  training: { totalSessions: number; totalDurationMin: number; totalVolumeKg: number }
  macros: MacroCompliance[]
  water: { avg: number; goal: number }
  consistency: { weeks: number; total: number }
  hasData: boolean
}

// Aggregates the given check-in rows into the Trends view model. `rows` come
// newest-first (as stored); this sorts oldest→newest for the series.
export function computeStats(rows: CheckinRow[], goals: Goals, periodWeeks: number): CheckInStats {
  const asc = [...rows].sort((a, b) => a.week_label.localeCompare(b.week_label))
  const scoped = asc.slice(Math.max(0, asc.length - periodWeeks))

  const weights = scoped.filter((r) => r.weight_kg != null)
  const weightSeries: SeriesPoint[] = weights.map((r) => ({ week: shortWeek(r.week_label), value: r.weight_kg as number }))
  const volumeSeries: SeriesPoint[] = scoped.map((r) => ({
    week: shortWeek(r.week_label),
    value: r.training_stats?.total_volume_kg ?? 0,
  }))

  const withTraining = scoped.filter((r) => r.training_stats && r.training_stats.total_sessions > 0)
  const totalSessions = withTraining.reduce((sum, r) => sum + (r.training_stats?.total_sessions ?? 0), 0)
  const totalDurationMin = withTraining.reduce((sum, r) => sum + (r.training_stats?.total_duration_minutes ?? 0), 0)
  const totalVolumeKg = withTraining.reduce((sum, r) => sum + (r.training_stats?.total_volume_kg ?? 0), 0)

  const nutritionRows = scoped.filter((r) => (r.nutrition_days ?? 0) > 0)
  const avg = (pick: (r: CheckinRow) => number | null) =>
    nutritionRows.length ? nutritionRows.reduce((sum, r) => sum + (pick(r) ?? 0), 0) / nutritionRows.length : 0
  const avgCalories = avg((r) => r.avg_calories)
  const avgProtein = avg((r) => r.avg_protein)
  const avgCarbs = avg((r) => r.avg_carbs)
  const avgFat = avg((r) => r.avg_fat)
  const avgWater = avg((r) => r.avg_water)

  const macros: MacroCompliance[] = [
    { name: 'Calories', pct: compliance(avgCalories, goals.calories) },
    { name: 'Protein', pct: compliance(avgProtein, goals.protein) },
    { name: 'Carbs', pct: compliance(avgCarbs, goals.carbs) },
    { name: 'Fat', pct: compliance(avgFat, goals.fat) },
  ]
  const nutritionCompliance = macros.length
    ? Math.round(macros.reduce((sum, macro) => sum + macro.pct, 0) / macros.length)
    : 0

  return {
    weightKg: weights.length ? (weights[weights.length - 1].weight_kg as number) : 0,
    weightDelta:
      weights.length >= 2
        ? Math.round(((weights[weights.length - 1].weight_kg as number) - (weights[0].weight_kg as number)) * 10) / 10
        : 0,
    avgWeeklyVolumeKg: withTraining.length ? Math.round(totalVolumeKg / withTraining.length) : 0,
    sessionsPerWeek: scoped.length ? Math.round((totalSessions / scoped.length) * 10) / 10 : 0,
    nutritionCompliance,
    weightSeries,
    volumeSeries,
    training: { totalSessions, totalDurationMin, totalVolumeKg },
    macros,
    water: { avg: Math.round(avgWater), goal: goals.water },
    consistency: { weeks: scoped.length, total: periodWeeks },
    hasData: scoped.length > 0,
  }
}
