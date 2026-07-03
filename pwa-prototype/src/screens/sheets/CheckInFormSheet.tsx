import { useEffect, useState } from 'react'
import { motion } from 'motion/react'
import { PushPage } from '../../components/PushPage.tsx'
import { Button } from '../../components/Button.tsx'
import { Stepper } from '../../components/Stepper.tsx'
import { Icon } from '../../components/Icon.tsx'
import { SectionLabel } from '../../components/SectionLabel.tsx'
import { useToast } from '../../components/Toast.tsx'
import { haptic } from '../../lib/ios.ts'
import { useStore } from '../../store.tsx'
import { MEASUREMENT_FIELDS, type CheckIn, type FeelingTag, type Measurements } from '../../data/mock.ts'

// Representative feeling-tag vocabulary (positive AND negative) the user toggles.
type FormTag = { id: string; label: string; positive: boolean }
const FEELING_TAGS: FormTag[] = [
  { id: 'good_energy', label: 'Good energy', positive: true },
  { id: 'motivated', label: 'Motivated', positive: true },
  { id: 'strong_training', label: 'Strong training', positive: true },
  { id: 'good_sleep', label: 'Slept well', positive: true },
  { id: 'focused', label: 'Focused', positive: true },
  { id: 'disciplined', label: 'Disciplined', positive: true },
  { id: 'tired', label: 'Tired', positive: false },
  { id: 'stress', label: 'Stress', positive: false },
  { id: 'sore', label: 'Sore', positive: false },
  { id: 'hungry', label: 'Hungry', positive: false },
  { id: 'poor_sleep', label: 'Poor sleep', positive: false },
]

function FeelingChip({ tag, selected, onToggle }: { tag: FormTag; selected: boolean; onToggle: () => void }) {
  const tone = tag.positive
    ? selected
      ? 'bg-success/20 text-success ring-1 ring-inset ring-success/40'
      : 'bg-surface-2 text-ink-2'
    : selected
      ? 'bg-warning/20 text-warning ring-1 ring-inset ring-warning/40'
      : 'bg-surface-2 text-ink-2'
  return (
    <motion.button
      type="button"
      whileTap={{ scale: 0.94 }}
      transition={{ duration: 0.12 }}
      onClick={() => {
        haptic('light')
        onToggle()
      }}
      className={`px-3 h-9 rounded-full text-[13px] font-semibold transition-colors ${tone}`}
    >
      {tag.label}
    </motion.button>
  )
}

function MeasurementRow({
  label,
  unit,
  value,
  step,
  onChange,
}: {
  label: string
  unit: string
  value: number
  step: number
  onChange: (next: number) => void
}) {
  return (
    <div className="relative flex items-center gap-3 px-4 min-h-[56px]">
      <span className="flex-1 text-[15px] text-ink">{label}</span>
      <Stepper value={value} step={step} suffix={unit} decimals={1} onChange={onChange} />
      <span className="rowhair pointer-events-none absolute bottom-0 right-0 left-4 h-px bg-divider/60" />
    </div>
  )
}

function NoteField({
  label,
  placeholder,
  value,
  onChange,
}: {
  label: string
  placeholder: string
  value: string
  onChange: (next: string) => void
}) {
  return (
    <div className="bg-surface-1 rounded-card p-4">
      <div className="text-[12px] font-semibold uppercase tracking-[0.05em] text-ink-3">{label}</div>
      <textarea
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        rows={3}
        className="mt-2 w-full resize-none bg-transparent text-[16px] leading-snug text-ink
          placeholder:text-ink-4 outline-none"
      />
    </div>
  )
}

// Photo slot — tap to "capture" (mock fills the tile), tap again to remove.
function PhotoTile({ filled, onToggle }: { filled: boolean; onToggle: () => void }) {
  return (
    <motion.button
      type="button"
      whileTap={{ scale: 0.94 }}
      transition={{ duration: 0.12 }}
      onClick={() => {
        haptic('light')
        onToggle()
      }}
      className={`size-20 shrink-0 rounded-card flex flex-col items-center justify-center gap-1 ring-1 ring-inset ${
        filled ? 'bg-primary/15 ring-primary/40' : 'bg-surface-2 ring-hair'
      }`}
      aria-label={filled ? 'Remove progress photo' : 'Add progress photo'}
    >
      <Icon name={filled ? 'photo' : 'camera'} size={20} className={filled ? 'text-primary' : 'text-ink-3'} />
      <span className={`text-[11px] font-medium ${filled ? 'text-primary' : 'text-ink-4'}`}>
        {filled ? 'Added' : 'Add'}
      </span>
    </motion.button>
  )
}

// ---- save helpers ----------------------------------------------------------

let checkInSeq = 100

function computeDeltas(next: Measurements, prev: Measurements | undefined): Partial<Measurements> {
  if (!prev) return {}
  const deltas: Partial<Measurements> = {}
  for (const field of MEASUREMENT_FIELDS) {
    const diff = Math.round((next[field.key] - prev[field.key]) * 10) / 10
    if (diff !== 0) deltas[field.key] = diff
  }
  return deltas
}

function feelingFromTags(tags: FeelingTag[]): CheckIn['feeling'] {
  const positive = tags.filter((tag) => tag.positive).length
  const negative = tags.length - positive
  if (negative > positive) return 'Tired'
  if (positive >= 4) return 'Strong'
  return 'On track'
}

function nextWeekMeta(latest: CheckIn | undefined): { label: string; range: string } {
  const num = latest ? parseInt(latest.weekLabel.replace(/\D/g, ''), 10) + 1 : 1
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 6)
  const fmt = (d: Date) => d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
  return { label: `Week ${num}`, range: `${fmt(start)} – ${fmt(end)}` }
}

export function CheckInFormSheet({
  open,
  onClose,
  editing = null,
}: {
  open: boolean
  onClose: () => void
  editing?: CheckIn | null
}) {
  const { state, dispatch, consumed } = useStore()
  const toast = useToast()

  const latest = state.checkIns[0] as CheckIn | undefined
  const [meas, setMeas] = useState<Measurements>(
    editing?.measurements ?? latest?.measurements ?? ({} as Measurements),
  )
  const [tags, setTags] = useState<Set<string>>(new Set())
  const [mental, setMental] = useState('')
  const [physical, setPhysical] = useState('')
  const [photos, setPhotos] = useState<boolean[]>([false, false, false])

  // Fresh state each presentation (prefilled when editing).
  useEffect(() => {
    if (!open) return
    if (editing) {
      setMeas(editing.measurements)
      setTags(new Set(editing.feelingTags.map((tag) => tag.id)))
      setMental(editing.mentalFeeling)
      setPhysical(editing.physicalFeeling)
      setPhotos([0, 1, 2].map((i) => i < editing.photoCount))
    } else {
      setMeas(latest?.measurements ?? ({} as Measurements))
      setTags(new Set())
      setMental('')
      setPhysical('')
      setPhotos([false, false, false])
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, editing])

  const setField = (key: keyof Measurements, next: number) =>
    setMeas((prev) => ({ ...prev, [key]: Math.round(next * 10) / 10 }))

  const toggleTag = (id: string) =>
    setTags((prev) => {
      const copy = new Set(prev)
      if (copy.has(id)) copy.delete(id)
      else copy.add(id)
      return copy
    })

  const selectedTags: FeelingTag[] = FEELING_TAGS.filter((tag) => tags.has(tag.id))
  const photoCount = photos.filter(Boolean).length
  const meta = editing
    ? { label: editing.weekLabel, range: editing.dateRange }
    : nextWeekMeta(latest)

  const save = () => {
    haptic('success')
    if (editing) {
      // Deltas recompute against the check-in just before the edited one.
      const index = state.checkIns.findIndex((c) => c.id === editing.id)
      const previous = state.checkIns[index + 1]
      dispatch({
        kind: 'checkin/update',
        id: editing.id,
        patch: {
          measurements: meas,
          deltas: computeDeltas(meas, previous?.measurements),
          feelingTags: selectedTags,
          mentalFeeling: mental,
          physicalFeeling: physical,
          feeling: feelingFromTags(selectedTags),
          photoCount,
        },
      })
      toast.show('Check-in updated')
    } else {
      checkInSeq += 1
      const checkIn: CheckIn = {
        id: `c${checkInSeq}`,
        weekLabel: meta.label,
        dateRange: meta.range,
        createdAt: new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }),
        feeling: feelingFromTags(selectedTags),
        adherence: latest?.adherence ?? 0.85,
        measurements: meas,
        deltas: computeDeltas(meas, latest?.measurements),
        feelingTags: selectedTags,
        mentalFeeling: mental,
        physicalFeeling: physical,
        // This week's nutrition summary comes from today's live totals (mock).
        nutrition: {
          avgCalories: consumed.calories,
          avgProtein: consumed.protein,
          avgFat: consumed.fat,
          avgCarbs: consumed.carbs,
          avgWater: state.waterMl,
          days: 7,
          aiSummary: 'Week logged. AI summary will be generated once the API is connected.',
        },
        training: latest?.training ?? { sessions: 0, durationMin: 0, volumeKg: 0, avgRpe: 0 },
        photoCount,
      }
      dispatch({ kind: 'checkin/add', checkIn })
      toast.show(`${meta.label} saved`)
    }
    onClose()
  }

  return (
    <PushPage
      open={open}
      onClose={onClose}
      title={editing ? 'Edit check-in' : 'New check-in'}
      footer={
        <Button variant="primary" full glow onClick={save}>
          <Icon name="check" size={16} /> {editing ? 'Save changes' : 'Save check-in'}
        </Button>
      }
    >
      <SectionLabel>This week</SectionLabel>
      <div className="bg-surface-1 rounded-card px-4 py-3.5 flex items-center gap-3">
        <div className="size-9 rounded-[10px] bg-primary/15 flex items-center justify-center shrink-0">
          <Icon name="calendar" size={16} className="text-primary" />
        </div>
        <div className="min-w-0">
          <div className="text-[16px] font-semibold text-ink">{meta.label}</div>
          <div className="text-[12px] text-ink-3 tnum">{meta.range}</div>
        </div>
      </div>

      <SectionLabel>Measurements</SectionLabel>
      <div className="bg-surface-1 rounded-card overflow-hidden [&>*:last-child_.rowhair]:hidden">
        {MEASUREMENT_FIELDS.map((field) => (
          <MeasurementRow
            key={field.key}
            label={field.label}
            unit={field.unit}
            value={meas[field.key] ?? 0}
            step={field.key === 'weight' ? 0.1 : 0.5}
            onChange={(next) => setField(field.key, next)}
          />
        ))}
      </div>

      <SectionLabel>Progress photos</SectionLabel>
      <div className="flex gap-2.5">
        {photos.map((filled, i) => (
          <PhotoTile
            key={i}
            filled={filled}
            onToggle={() => setPhotos((prev) => prev.map((p, j) => (j === i ? !p : p)))}
          />
        ))}
      </div>

      <SectionLabel>How did you feel?</SectionLabel>
      <div className="flex flex-wrap gap-2">
        {FEELING_TAGS.map((tag) => (
          <FeelingChip key={tag.id} tag={tag} selected={tags.has(tag.id)} onToggle={() => toggleTag(tag.id)} />
        ))}
      </div>

      <SectionLabel>Notes</SectionLabel>
      <div className="flex flex-col gap-2.5">
        <NoteField
          label="Mental"
          placeholder="Mood, motivation, stress, focus this week…"
          value={mental}
          onChange={setMental}
        />
        <NoteField
          label="Physical"
          placeholder="Energy, strength, soreness, sleep, any niggles…"
          value={physical}
          onChange={setPhysical}
        />
      </div>
    </PushPage>
  )
}
