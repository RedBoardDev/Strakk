import { useEffect, useState } from 'react'
import { Sheet } from '../../components/Sheet.tsx'
import { Button } from '../../components/Button.tsx'
import { Stepper } from '../../components/Stepper.tsx'
import { SectionLabel } from '../../components/SectionLabel.tsx'
import { useToast } from '../../components/Toast.tsx'
import { haptic } from '../../lib/ios.ts'
import { useStore, type Goals } from '../../store.tsx'

type GoalKey = keyof Goals
const FIELDS: { key: GoalKey; label: string; step: number; suffix: string }[] = [
  { key: 'calories', label: 'Calories', step: 50, suffix: 'kcal' },
  { key: 'protein', label: 'Protein', step: 5, suffix: 'g' },
  { key: 'fat', label: 'Fat', step: 5, suffix: 'g' },
  { key: 'carbs', label: 'Carbs', step: 5, suffix: 'g' },
  { key: 'water', label: 'Water', step: 100, suffix: 'mL' },
]

// Edit daily targets — saving persists to the profile row; Today's macro grid
// and the Settings rows reflect the new goals immediately.
export function GoalsEditSheet({ open, onClose }: { open: boolean; onClose: () => void }) {
  const store = useStore()
  const toast = useToast()
  const [vals, setVals] = useState<Goals>(store.state.goals)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (open) setVals(store.state.goals)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  return (
    <Sheet
      open={open}
      onClose={onClose}
      title="Daily goals"
      detents={['large']}
      footer={
        <Button
          variant="primary"
          full
          glow
          disabled={saving}
          onClick={() => {
            haptic('medium')
            setSaving(true)
            void store
              .updateGoals({ ...vals })
              .then(() => {
                onClose()
                toast.show('Goals updated')
              })
              .catch(() => {})
              .finally(() => setSaving(false))
          }}
        >
          {saving ? 'Saving…' : 'Save goals'}
        </Button>
      }
    >
      <div className="pb-2">
        <SectionLabel>Targets</SectionLabel>
        <div className="flex flex-col gap-2.5">
          {FIELDS.map((field) => (
            <div key={field.key} className="bg-surface-1 rounded-card px-4 py-3 flex items-center gap-3">
              <span className="flex-1 text-[15px] text-ink">{field.label}</span>
              <Stepper
                value={vals[field.key]}
                onChange={(value) => setVals((prev) => ({ ...prev, [field.key]: value }))}
                step={field.step}
                min={0}
                suffix={field.suffix}
              />
            </div>
          ))}
        </div>
      </div>
    </Sheet>
  )
}
