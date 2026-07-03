import { useEffect, useRef, useState } from 'react'
import { motion } from 'motion/react'
import { PushPage } from '../../components/PushPage.tsx'
import { Button } from '../../components/Button.tsx'
import { Icon } from '../../components/Icon.tsx'
import { SectionLabel } from '../../components/SectionLabel.tsx'
import { Stepper } from '../../components/Stepper.tsx'
import { useToast } from '../../components/Toast.tsx'
import { haptic } from '../../lib/ios.ts'
import { dateRangeLabel, isoWeekLabel, weekDates } from '../../lib/dates.ts'
import { MEASUREMENT_COLUMNS, measurementsOf, weekTitle, TAG_LABELS } from '../../lib/checkinView.ts'
import { useStore } from '../../store.tsx'
import { computeNutritionSummary, type CheckinInput, type CheckinRow } from '../../api/checkins.ts'
import { aggregateTrainingStats, fetchHevyWorkouts } from '../../api/hevy.ts'
import { generateCheckinSummary } from '../../api/ai.ts'
import { compressImage } from '../../api/photos.ts'
import { MEASUREMENT_FIELDS, type Measurements } from '../../data/mock.ts'

const FEELING_TAGS = Object.entries(TAG_LABELS)
  .slice(0, 11)
  .map(([id, meta]) => ({ id, ...meta }))

function FeelingChip({
  id,
  label,
  positive,
  selected,
  onToggle,
}: {
  id: string
  label: string
  positive: boolean
  selected: boolean
  onToggle: () => void
}) {
  const tone = positive
    ? selected
      ? 'bg-success/20 text-success ring-1 ring-inset ring-success/40'
      : 'bg-surface-2 text-ink-2'
    : selected
      ? 'bg-warning/20 text-warning ring-1 ring-inset ring-warning/40'
      : 'bg-surface-2 text-ink-2'
  return (
    <motion.button
      key={id}
      type="button"
      whileTap={{ scale: 0.94 }}
      transition={{ duration: 0.12 }}
      onClick={() => {
        haptic('light')
        onToggle()
      }}
      className={`px-3 h-9 rounded-full text-[13px] font-semibold transition-colors ${tone}`}
    >
      {label}
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
        onChange={(event) => onChange(event.target.value.slice(0, 1000))}
        placeholder={placeholder}
        rows={3}
        className="mt-2 w-full resize-none bg-transparent text-[16px] leading-snug text-ink
          placeholder:text-ink-4 outline-none"
      />
    </div>
  )
}

type PhotoSlot = { file: Blob; preview: string } | null

export function CheckInFormSheet({
  open,
  onClose,
  editing = null,
}: {
  open: boolean
  onClose: () => void
  editing?: CheckinRow | null
}) {
  const store = useStore()
  const toast = useToast()
  const fileRef = useRef<HTMLInputElement>(null)
  const pendingSlot = useRef(0)

  const latest = store.state.checkins[0] as CheckinRow | undefined
  const [meas, setMeas] = useState<Measurements>({} as Measurements)
  const [tags, setTags] = useState<Set<string>>(new Set())
  const [mental, setMental] = useState('')
  const [physical, setPhysical] = useState('')
  const [photos, setPhotos] = useState<PhotoSlot[]>([null, null, null])
  const [saving, setSaving] = useState(false)

  // Fresh state each presentation (prefilled when editing).
  useEffect(() => {
    if (!open) return
    if (editing) {
      setMeas(measurementsOf(editing))
      setTags(new Set(editing.feeling_tags ?? []))
      setMental(editing.mental_feeling ?? '')
      setPhysical(editing.physical_feeling ?? '')
    } else {
      setMeas(latest ? measurementsOf(latest) : ({ weight: 75, shoulders: 0, chest: 0, armLeft: 0, armRight: 0, waist: 0, hips: 0, thighLeft: 0, thighRight: 0 } as Measurements))
      setTags(new Set())
      setMental('')
      setPhysical('')
    }
    setPhotos([null, null, null])
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

  const now = new Date()
  const meta = editing
    ? { label: weekTitle(editing.week_label), range: dateRangeLabel(editing.covered_dates) }
    : { label: weekTitle(isoWeekLabel(now)), range: dateRangeLabel(weekDates(now)) }

  const pickPhoto = (slot: number) => {
    pendingSlot.current = slot
    fileRef.current?.click()
  }

  const onFile = async (file: File) => {
    const blob = await compressImage(file, 1600, 0.85)
    const preview = URL.createObjectURL(blob)
    setPhotos((prev) => prev.map((p, i) => (i === pendingSlot.current ? { file: blob, preview } : p)))
  }

  // Measurements → DB columns; zero means "not measured" (NULL).
  const measurementColumns = (): CheckinInput['measurements'] => {
    const out: Record<string, number> = {}
    for (const [key, column] of Object.entries(MEASUREMENT_COLUMNS)) {
      const value = meas[key as keyof Measurements]
      if (value > 0) out[column as string] = value
    }
    return out as CheckinInput['measurements']
  }

  const save = async () => {
    haptic('success')
    setSaving(true)
    try {
      const base = {
        measurements: measurementColumns(),
        feeling_tags: [...tags],
        mental_feeling: mental.trim() || null,
        physical_feeling: physical.trim() || null,
      }
      if (editing) {
        await store.updateCheckin(editing.id, base)
        toast.show('Check-in updated')
        onClose()
      } else {
        const coveredDates = weekDates(now)
        const nutrition = await computeNutritionSummary(coveredDates).catch(() => null)
        // Pull the week's Hevy workouts (if connected) into training_stats.
        const trainingStats = store.state.hevyConnected
          ? await fetchHevyWorkouts(coveredDates[0], coveredDates[coveredDates.length - 1])
              .then(aggregateTrainingStats)
              .catch(() => null)
          : null
        const row = await store.createCheckin(
          {
            week_label: isoWeekLabel(now),
            covered_dates: coveredDates,
            training_stats: trainingStats,
            ...base,
            ...(nutrition
              ? {
                  nutrition: {
                    avg_protein: nutrition.avg_protein,
                    avg_calories: nutrition.avg_calories,
                    avg_fat: nutrition.avg_fat,
                    avg_carbs: nutrition.avg_carbs,
                    avg_water: nutrition.avg_water,
                    nutrition_days: nutrition.nutrition_days,
                  },
                }
              : {}),
          },
          photos.filter((p): p is NonNullable<PhotoSlot> => p !== null).map((p) => p.file),
        )
        toast.show(`${meta.label} saved`)
        onClose()
        // AI summary in the background — appears on the check-in once ready.
        if (nutrition) {
          void generateCheckinSummary({
            avg_protein: nutrition.avg_protein,
            avg_calories: nutrition.avg_calories,
            avg_fat: nutrition.avg_fat,
            avg_carbs: nutrition.avg_carbs,
            avg_water: nutrition.avg_water,
            nutrition_days: nutrition.nutrition_days,
            weight_kg: meas.weight > 0 ? meas.weight : undefined,
            feeling_tags: [...tags],
            mental_feeling: mental.trim() || undefined,
            physical_feeling: physical.trim() || undefined,
            goals: {
              protein_goal: store.state.goals.protein,
              calorie_goal: store.state.goals.calories,
              water_goal: store.state.goals.water,
            },
          })
            .then((summary) => store.attachAiSummary(row.id, summary))
            .then(() => toast.show('AI summary ready'))
            .catch(() => {})
        }
      }
    } catch {
      // toasted by the store
    } finally {
      setSaving(false)
    }
  }

  return (
    <PushPage
      open={open}
      onClose={onClose}
      title={editing ? 'Edit check-in' : 'New check-in'}
      footer={
        <Button variant="primary" full glow disabled={saving} onClick={() => void save()}>
          <Icon name="check" size={16} /> {saving ? 'Saving…' : editing ? 'Save changes' : 'Save check-in'}
        </Button>
      }
    >
      <input
        ref={fileRef}
        type="file"
        accept="image/*"
        capture="environment"
        className="hidden"
        onChange={(e) => {
          const file = e.target.files?.[0]
          if (file) void onFile(file)
          e.target.value = ''
        }}
      />

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

      {!editing && (
        <>
          <SectionLabel>Progress photos</SectionLabel>
          <div className="flex gap-2.5">
            {photos.map((photo, i) => (
              <motion.button
                key={i}
                type="button"
                whileTap={{ scale: 0.94 }}
                transition={{ duration: 0.12 }}
                onClick={() => {
                  haptic('light')
                  if (photo) {
                    URL.revokeObjectURL(photo.preview)
                    setPhotos((prev) => prev.map((p, j) => (j === i ? null : p)))
                  } else {
                    pickPhoto(i)
                  }
                }}
                className={`relative size-20 shrink-0 rounded-card overflow-hidden flex flex-col items-center justify-center gap-1 ring-1 ring-inset ${
                  photo ? 'ring-primary/40' : 'bg-surface-2 ring-hair'
                }`}
                aria-label={photo ? 'Remove progress photo' : 'Add progress photo'}
              >
                {photo ? (
                  <img src={photo.preview} alt="" className="absolute inset-0 h-full w-full object-cover" />
                ) : (
                  <>
                    <Icon name="camera" size={20} className="text-ink-3" />
                    <span className="text-[11px] font-medium text-ink-4">Add</span>
                  </>
                )}
              </motion.button>
            ))}
          </div>
        </>
      )}

      <SectionLabel>How did you feel?</SectionLabel>
      <div className="flex flex-wrap gap-2">
        {FEELING_TAGS.map((tag) => (
          <FeelingChip
            key={tag.id}
            id={tag.id}
            label={tag.label}
            positive={tag.positive}
            selected={tags.has(tag.id)}
            onToggle={() => toggleTag(tag.id)}
          />
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
