import { motion } from 'motion/react'
import { haptic, spring } from '../lib/ios.ts'

// iOS-style switch.
export function Toggle({ on, onChange, label }: { on: boolean; onChange: (on: boolean) => void; label?: string }) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={on}
      aria-label={label}
      onClick={() => {
        haptic('light')
        onChange(!on)
      }}
      className={`relative h-[31px] w-[51px] shrink-0 rounded-full transition-colors ${on ? 'bg-success' : 'bg-surface-3'}`}
    >
      <motion.span
        layout
        transition={spring.snappy}
        className="absolute top-[2px] size-[27px] rounded-full bg-white shadow-[0_2px_4px_rgba(0,0,0,0.35)]"
        style={{ left: on ? 22 : 2 }}
      />
    </button>
  )
}
