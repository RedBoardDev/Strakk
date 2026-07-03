import { useState } from 'react'
import { Sheet } from '../../components/Sheet.tsx'
import { Button } from '../../components/Button.tsx'
import { SectionLabel } from '../../components/SectionLabel.tsx'
import { Toggle } from '../../components/Toggle.tsx'
import { Icon } from '../../components/Icon.tsx'
import { haptic } from '../../lib/ios.ts'

// 13 export toggles, grouped — mirrors the native PdfExportOptionsSheet.
type OptKey =
  | 'photos' | 'measurements' | 'feelings'
  | 'calories' | 'protein' | 'carbs' | 'fat' | 'water' | 'averages' | 'perDay'
  | 'trainingSummary' | 'muscleVolume' | 'sessionDetails'

const GROUPS: { title: string; opts: { key: OptKey; label: string }[] }[] = [
  {
    title: 'General',
    opts: [
      { key: 'photos', label: 'Progress photos' },
      { key: 'measurements', label: 'Body measurements' },
      { key: 'feelings', label: 'Feelings' },
    ],
  },
  {
    title: 'Nutrition',
    opts: [
      { key: 'calories', label: 'Calories' },
      { key: 'protein', label: 'Protein' },
      { key: 'carbs', label: 'Carbs' },
      { key: 'fat', label: 'Fat' },
      { key: 'water', label: 'Water' },
      { key: 'averages', label: 'Averages' },
      { key: 'perDay', label: 'Per-day breakdown' },
    ],
  },
  {
    title: 'Training',
    opts: [
      { key: 'trainingSummary', label: 'Training summary' },
      { key: 'muscleVolume', label: 'Muscle volume' },
      { key: 'sessionDetails', label: 'Session details' },
    ],
  },
]

const DEFAULTS: Record<OptKey, boolean> = {
  photos: true, measurements: true, feelings: true,
  calories: true, protein: true, carbs: true, fat: true, water: true, averages: true, perDay: false,
  trainingSummary: true, muscleVolume: true, sessionDetails: false,
}

export function PdfExportOptionsSheet({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [opts, setOpts] = useState<Record<OptKey, boolean>>(DEFAULTS)
  const set = (key: OptKey, value: boolean) => setOpts((prev) => ({ ...prev, [key]: value }))
  const count = Object.values(opts).filter(Boolean).length

  return (
    <Sheet
      open={open}
      onClose={onClose}
      title="Export PDF"
      detents={['large']}
      footer={
        <Button
          variant="primary"
          full
          glow
          disabled={count === 0}
          onClick={() => {
            haptic('medium')
            onClose()
          }}
        >
          <Icon name="share" size={16} /> Generate PDF
        </Button>
      }
    >
      <div className="pb-2">
        <p className="pt-1 text-[13px] text-ink-2">Choose what to include in the shared report.</p>
        {GROUPS.map((group) => (
          <div key={group.title}>
            <SectionLabel>{group.title}</SectionLabel>
            <div className="bg-surface-1 rounded-card overflow-hidden">
              {group.opts.map((opt, i) => (
                <div key={opt.key} className="relative flex items-center gap-3 px-4 min-h-[52px]">
                  <span className="flex-1 text-[15px] text-ink">{opt.label}</span>
                  <Toggle on={opts[opt.key]} onChange={(value) => set(opt.key, value)} label={opt.label} />
                  {i < group.opts.length - 1 && (
                    <span className="pointer-events-none absolute bottom-0 right-0 left-4 h-px bg-divider/60" />
                  )}
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </Sheet>
  )
}
