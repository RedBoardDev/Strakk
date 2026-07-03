import { useState } from 'react'
import { motion } from 'motion/react'
import { Icon } from './Icon.tsx'
import { Sheet } from './Sheet.tsx'
import { Stepper } from './Stepper.tsx'
import { Button } from './Button.tsx'
import { useToast } from './Toast.tsx'
import { haptic, spring } from '../lib/ios.ts'

// Water intake card (used on Today and in the calendar day detail): −/+ step
// buttons plus a custom-amount sheet. Every change flows through onDelta
// (positive = add, negative = remove); removals are clamped to the current
// total so the day can never go below zero, whatever was custom-added before.
const STEP = 100

export function WaterCard({
  totalMl,
  goalMl,
  onDelta,
}: {
  totalMl: number
  goalMl: number
  onDelta: (deltaMl: number) => void
}) {
  const toast = useToast()
  const [sheetOpen, setSheetOpen] = useState(false)
  const [customMl, setCustomMl] = useState(200)

  return (
    <>
      <div className="bg-surface-1 rounded-card px-4 py-3 flex items-center gap-3">
        <div className="size-9 rounded-lg bg-surface-3 flex items-center justify-center shrink-0">
          <Icon name="drop.fill" size={15} className="text-water" />
        </div>
        <div className="text-[18px] font-bold text-ink tnum">
          <motion.span
            key={totalMl}
            initial={{ scale: 1.15 }}
            animate={{ scale: 1 }}
            transition={spring.snappy}
            className="inline-block origin-left"
          >
            {(totalMl / 1000).toFixed(1)}
          </motion.span>{' '}
          L<span className="text-ink-3 font-semibold"> / {(goalMl / 1000).toFixed(1)} L</span>
        </div>
        <div className="flex-1" />
        <button
          type="button"
          disabled={totalMl === 0}
          onClick={() => {
            haptic('light')
            onDelta(-Math.min(STEP, totalMl))
          }}
          className="size-10 rounded-[12px] bg-surface-2 flex items-center justify-center text-ink disabled:text-ink-4"
          aria-label={`Remove ${STEP} mL`}
        >
          <Icon name="minus" size={16} />
        </button>
        <button
          type="button"
          onClick={() => {
            haptic('light')
            onDelta(STEP)
          }}
          className="size-10 rounded-[12px] bg-surface-2 flex items-center justify-center text-water"
          aria-label={`Add ${STEP} mL`}
        >
          <Icon name="plus" size={16} />
        </button>
        <button
          type="button"
          onClick={() => {
            haptic('light')
            setCustomMl(200)
            setSheetOpen(true)
          }}
          className="size-10 rounded-[12px] bg-surface-2 flex items-center justify-center text-ink-2"
          aria-label="Custom amount"
        >
          <Icon name="pencil" size={15} />
        </button>
      </div>

      {/* Custom-amount sheet — add OR remove an arbitrary amount */}
      <Sheet open={sheetOpen} onClose={() => setSheetOpen(false)} title="Custom amount" detents={['small']}>
        <div className="flex flex-col items-center gap-5 py-3">
          <Stepper value={customMl} onChange={setCustomMl} step={STEP} min={STEP} suffix="mL" />
          <div className="w-full flex gap-3">
            <Button
              variant="secondary"
              full
              disabled={totalMl === 0}
              onClick={() => {
                const removed = Math.min(customMl, totalMl)
                setSheetOpen(false)
                onDelta(-removed)
                toast.show(`Removed ${removed} mL`)
              }}
            >
              Remove
            </Button>
            <Button
              variant="primary"
              full
              onClick={() => {
                setSheetOpen(false)
                onDelta(customMl)
                toast.show(`Added ${customMl} mL`)
              }}
            >
              Add water
            </Button>
          </div>
        </div>
      </Sheet>
    </>
  )
}
