// Date conventions shared with the KMP client: all `date`/`log_date` values
// are `yyyy-MM-dd` in the DEVICE's local timezone; check-in weeks are ISO-8601
// (Mon–Sun) labelled `YYYY-Www` and compared as plain strings.

export function toIsoDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

export function todayIso(): string {
  return toIsoDate(new Date())
}

export function addDays(iso: string, days: number): string {
  const [y, m, d] = iso.split('-').map(Number)
  const date = new Date(y, m - 1, d + days)
  return toIsoDate(date)
}

// Monday of the ISO week containing `d`.
export function mondayOf(d: Date): Date {
  const copy = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  const day = copy.getDay() // 0=Sun
  const diff = day === 0 ? -6 : 1 - day
  copy.setDate(copy.getDate() + diff)
  return copy
}

// ISO week number via the Thursday rule.
export function isoWeekLabel(d: Date): string {
  const thursday = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  const day = thursday.getDay() || 7
  thursday.setDate(thursday.getDate() + 4 - day)
  const year = thursday.getFullYear()
  const jan1 = new Date(year, 0, 1)
  const week = Math.ceil(((thursday.getTime() - jan1.getTime()) / 86400000 + 1) / 7)
  return `${year}-W${String(week).padStart(2, '0')}`
}

// The 7 yyyy-MM-dd dates (Mon..Sun) of the ISO week containing `d`.
export function weekDates(d: Date): string[] {
  const monday = mondayOf(d)
  return Array.from({ length: 7 }, (_, i) => {
    const day = new Date(monday.getFullYear(), monday.getMonth(), monday.getDate() + i)
    return toIsoDate(day)
  })
}

// "Jun 24 – Jun 30" style label for a list of covered dates.
export function dateRangeLabel(coveredDates: string[]): string {
  if (coveredDates.length === 0) return ''
  const sorted = [...coveredDates].sort()
  const fmt = (iso: string) => {
    const [y, m, d] = iso.split('-').map(Number)
    return new Date(y, m - 1, d).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
  }
  return `${fmt(sorted[0])} – ${fmt(sorted[sorted.length - 1])}`
}

// "HH:MM" local time from an ISO timestamp.
export function timeOf(createdAt: string): string {
  const d = new Date(createdAt)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
