import { motion } from 'motion/react'
import type { ReactNode } from 'react'
import { haptic, spring } from '../lib/ios.ts'

// surface-1 card, radius 12, padding 16 (DESIGN.md §5 Cards). No border/shadow —
// the surface contrast does the separation. Optional tap (brief surface-2 flash).
export function Card({
  children,
  className = '',
  onClick,
  padding = 'p-4',
}: {
  children: ReactNode
  className?: string
  onClick?: () => void
  padding?: string
}) {
  const interactive = !!onClick
  return (
    <motion.div
      whileTap={interactive ? { scale: 0.985, backgroundColor: '#151B38' } : undefined}
      transition={spring.snappy}
      onClick={() => {
        if (!interactive) return
        haptic('light')
        onClick?.()
      }}
      className={`bg-surface-1 rounded-card ${padding} ${interactive ? 'cursor-pointer' : ''} ${className}`}
    >
      {children}
    </motion.div>
  )
}
