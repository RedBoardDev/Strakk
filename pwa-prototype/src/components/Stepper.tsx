import { motion } from 'motion/react'
import { Icon } from './Icon.tsx'
import { haptic } from '../lib/ios.ts'

// Quantity stepper. `decimals={1}` renders one decimal (0.1 kg / 0.5 cm steps
// for check-in measurements); the default rounds to integers.
export function Stepper({
  value,
  onChange,
  step = 10,
  min = 0,
  suffix = 'g',
  decimals = 0,
}: {
  value: number
  onChange: (v: number) => void
  step?: number
  min?: number
  suffix?: string
  decimals?: 0 | 1
}) {
  const set = (v: number) => {
    haptic('light')
    const clamped = Math.max(min, v)
    onChange(decimals === 1 ? Math.round(clamped * 10) / 10 : Math.round(clamped))
  }
  return (
    <div className="flex items-center gap-3 bg-surface-2 rounded-full p-1">
      <motion.button
        type="button"
        whileTap={{ scale: 0.9 }}
        onClick={() => set(value - step)}
        className="size-9 rounded-full bg-surface-3 flex items-center justify-center text-ink"
        aria-label="Decrease"
      >
        <Icon name="minus" size={18} />
      </motion.button>
      <span className="tnum min-w-16 text-center text-[17px] font-semibold text-ink">
        {decimals === 1 ? value.toFixed(1) : Math.round(value)}
        <span className="text-ink-3 text-[13px] ml-0.5">{suffix}</span>
      </span>
      <motion.button
        type="button"
        whileTap={{ scale: 0.9 }}
        onClick={() => set(value + step)}
        className="size-9 rounded-full bg-primary flex items-center justify-center text-white"
        aria-label="Increase"
      >
        <Icon name="plus" size={18} />
      </motion.button>
    </div>
  )
}
