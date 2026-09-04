import { motion } from 'motion/react'
import { colors } from '../theme/tokens.ts'

// Capsule progress bar (DESIGN.md §5). Track surface-2, animated ease-out fill.
// Fill switches to success green when goal reached.
export function ProgressBar({
  value,
  color,
  height = 4,
  reached,
}: {
  value: number // 0..1
  color: string
  height?: number
  reached?: boolean
}) {
  const clamped = Math.max(0, Math.min(1, value))
  const fill = reached ?? clamped >= 1 ? colors.success : color
  return (
    <div className="w-full rounded-full bg-surface-2 overflow-hidden" style={{ height }}>
      <motion.div
        className="h-full rounded-full"
        style={{ backgroundColor: fill }}
        initial={{ width: 0 }}
        animate={{ width: `${clamped * 100}%` }}
        transition={{ duration: 0.45, ease: [0.32, 0.72, 0, 1] }}
      />
    </div>
  )
}
