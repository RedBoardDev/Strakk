import { motion } from 'motion/react'
import type { ReactNode } from 'react'
import { colors } from '../theme/tokens.ts'

// Progress ring (DESIGN.md §5). Track surface-2, fill primary, success when
// reached. Animated stroke with iOS ease-out. Used for the hero calorie counter.
export function Ring({
  progress,
  size = 168,
  stroke = 12,
  color = colors.primary,
  reached,
  children,
}: {
  progress: number // 0..1
  size?: number
  stroke?: number
  color?: string
  reached?: boolean
  children?: ReactNode
}) {
  const clamped = Math.max(0, Math.min(1, progress))
  const r = (size - stroke) / 2
  const c = 2 * Math.PI * r
  const fill = reached ?? clamped >= 1 ? colors.success : color

  return (
    <div className="relative" style={{ width: size, height: size }}>
      <svg width={size} height={size} className="-rotate-90">
        <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke={colors.surface2} strokeWidth={stroke} />
        <motion.circle
          cx={size / 2}
          cy={size / 2}
          r={r}
          fill="none"
          stroke={fill}
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={c}
          initial={{ strokeDashoffset: c }}
          animate={{ strokeDashoffset: c * (1 - clamped) }}
          transition={{ duration: 0.9, ease: [0.32, 0.72, 0, 1] }}
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">{children}</div>
    </div>
  )
}
