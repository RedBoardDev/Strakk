import { motion } from 'motion/react'
import type { ReactNode } from 'react'
import { haptic, spring } from '../lib/ios.ts'

type Variant = 'primary' | 'secondary' | 'destructive' | 'text'

const base = 'inline-flex items-center justify-center gap-2 font-semibold select-none disabled:opacity-100'

const variants: Record<Variant, string> = {
  primary: 'bg-primary text-white rounded-card h-[52px] px-5',
  secondary: 'bg-surface-2 text-ink rounded-card h-[52px] px-5',
  destructive: 'bg-error text-white rounded-card h-[52px] px-5',
  text: 'text-primary h-11 px-2',
}

export function Button({
  children,
  variant = 'primary',
  full = false,
  disabled = false,
  glow = false,
  onClick,
}: {
  children: ReactNode
  variant?: Variant
  full?: boolean
  disabled?: boolean
  glow?: boolean
  onClick?: () => void
}) {
  const disabledCls = disabled && variant !== 'text' ? '!bg-surface-2 !text-ink-3' : ''
  return (
    <motion.button
      type="button"
      disabled={disabled}
      whileTap={disabled ? undefined : { scale: 0.96 }}
      transition={spring.snappy}
      onClick={() => {
        if (disabled) return
        haptic('light')
        onClick?.()
      }}
      className={[
        base,
        variants[variant],
        full ? 'w-full' : '',
        glow && !disabled && variant === 'primary' ? 'glow-primary' : '',
        disabledCls,
        'text-[15px]',
      ].join(' ')}
    >
      {children}
    </motion.button>
  )
}
