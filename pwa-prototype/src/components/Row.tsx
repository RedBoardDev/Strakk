import type { ReactNode } from 'react'
import { motion } from 'motion/react'
import { Icon, type IconName } from './Icon.tsx'
import { haptic } from '../lib/ios.ts'

// Grouped iOS list row. Use inside <RowGroup>. Dividers are inset to the text
// origin (iOS style) — RowGroup hides the last row's hairline.
export function Row({
  icon,
  iconColor,
  title,
  subtitle,
  trailing,
  chevron = false,
  onClick,
}: {
  icon?: IconName
  iconColor?: string
  title: string
  subtitle?: string
  trailing?: ReactNode
  chevron?: boolean
  onClick?: () => void
}) {
  const interactive = !!onClick
  return (
    <motion.div
      whileTap={interactive ? { backgroundColor: '#151B38' } : undefined}
      onClick={() => {
        if (!interactive) return
        haptic('light')
        onClick?.()
      }}
      className={`relative flex items-center gap-3 px-4 min-h-[52px] ${interactive ? 'cursor-pointer' : ''}`}
    >
      {icon && (
        <div
          className="size-7 rounded-[8px] flex items-center justify-center shrink-0"
          style={{
            backgroundColor: iconColor ? `${iconColor}2E` : '#1A2142',
            boxShadow: iconColor ? `inset 0 0 0 1px ${iconColor}33` : undefined,
          }}
        >
          <Icon name={icon} size={15} color={iconColor ?? '#9CA1B8'} />
        </div>
      )}
      <div className="flex-1 min-w-0">
        <div className="text-[15px] text-ink truncate">{title}</div>
        {subtitle && <div className="text-[12px] text-ink-3 truncate">{subtitle}</div>}
      </div>
      {trailing && <div className="text-[15px] text-ink-2 shrink-0">{trailing}</div>}
      {chevron && <Icon name="chevron.right" size={16} className="text-ink-3 shrink-0" />}
      <span
        className="rowhair pointer-events-none absolute bottom-0 right-0 h-px bg-divider/60"
        style={{ left: icon ? 52 : 16 }}
      />
    </motion.div>
  )
}

export function RowGroup({ children }: { children: ReactNode }) {
  return (
    <div className="bg-surface-1 rounded-card overflow-hidden [&>*:last-child_.rowhair]:hidden">{children}</div>
  )
}
