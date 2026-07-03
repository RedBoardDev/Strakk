import { motion } from 'motion/react'
import { haptic, spring } from '../lib/ios.ts'

// iOS segmented control: pill track, sliding selected thumb.
export function Segmented<T extends string>({
  options,
  value,
  onChange,
}: {
  options: { value: T; label: string }[]
  value: T
  onChange: (v: T) => void
}) {
  return (
    <div className="relative flex p-1 rounded-full bg-surface-2">
      {options.map((opt) => {
        const on = opt.value === value
        return (
          <button
            key={opt.value}
            type="button"
            onClick={() => {
              if (!on) haptic('light')
              onChange(opt.value)
            }}
            className="relative flex-1 h-8 flex items-center justify-center"
          >
            {on && (
              <motion.div
                layoutId="segmented-thumb"
                transition={spring.snappy}
                className="absolute inset-0 rounded-full bg-surface-3"
              />
            )}
            <span className={`relative text-[13px] font-semibold ${on ? 'text-ink' : 'text-ink-2'}`}>
              {opt.label}
            </span>
          </button>
        )
      })}
    </div>
  )
}
