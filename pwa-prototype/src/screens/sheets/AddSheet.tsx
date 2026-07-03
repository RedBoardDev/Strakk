import { motion } from 'motion/react'
import { Sheet } from '../../components/Sheet.tsx'
import { Icon, type IconName } from '../../components/Icon.tsx'
import { haptic, spring } from '../../lib/ios.ts'
import { useNav, type Flow } from '../../nav.ts'

type OptionKind = Extract<Flow['kind'], 'mealBuilder' | 'search' | 'scan' | 'manual' | 'quickAdd' | 'photoMeal'>

type Option = {
  id: string
  icon: IconName
  label: string
  sub: string
  kind: OptionKind
}

// Entry-point picker. Every logging path is available to everyone (accounts
// are all Pro for now — quota gating stays server-side).
const OPTIONS: Option[] = [
  { id: 'build', icon: 'fork', label: 'Build a meal', sub: 'Combine several foods', kind: 'mealBuilder' },
  { id: 'search', icon: 'search', label: 'Search food', sub: 'Saved foods & full catalog', kind: 'search' },
  { id: 'scan', icon: 'barcode', label: 'Scan barcode', sub: 'Point your camera at a product', kind: 'scan' },
  { id: 'manual', icon: 'pencil', label: 'Manual entry', sub: 'Type the macros yourself', kind: 'manual' },
  { id: 'quick', icon: 'sparkles', label: 'Quick add', sub: 'Describe a meal in plain text', kind: 'quickAdd' },
  { id: 'photo', icon: 'camera', label: 'Photo meal', sub: 'Snap a plate, AI logs it', kind: 'photoMeal' },
]

export function AddSheet({
  open,
  onClose,
  logDate,
}: {
  open: boolean
  onClose: () => void
  logDate?: string
}) {
  const nav = useNav()
  return (
    <Sheet open={open} onClose={onClose} title="Add food" detents={['large']}>
      <div className="flex flex-col gap-2.5 pt-2 pb-4">
        {OPTIONS.map((opt, i) => (
          <motion.button
            key={opt.id}
            type="button"
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ ...spring.snappy, delay: 0.04 * i }}
            whileTap={{ scale: 0.98, backgroundColor: '#151B38' }}
            onClick={() => {
              haptic('light')
              nav.open({ kind: opt.kind, logDate })
            }}
            className="w-full bg-surface-1 rounded-card h-[68px] px-4 flex items-center gap-3.5 text-left"
          >
            <div className="size-10 rounded-[12px] bg-surface-3 flex items-center justify-center shrink-0">
              <Icon name={opt.icon} size={18} className="text-primary" />
            </div>
            <div className="flex-1 min-w-0">
              <div className="text-[15px] font-semibold text-ink">{opt.label}</div>
              <div className="text-[12px] text-ink-3 truncate">{opt.sub}</div>
            </div>
            <Icon name="chevron.right" size={14} className="text-ink-4 shrink-0" />
          </motion.button>
        ))}
      </div>
    </Sheet>
  )
}
