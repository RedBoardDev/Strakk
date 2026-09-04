import { useEffect, useRef } from 'react'
import { haptic } from '../lib/ios.ts'

// iOS-style vertical wheel picker. Native momentum scroll + CSS snap does the
// physics; a rAF pass fades/scales rows by distance from center and fires a
// light haptic on each detent. No dependencies, buttery on touch.
const ITEM_H = 44

export function WheelPicker({
  value,
  onChange,
  min,
  max,
  step = 1,
  suffix,
  rows = 5,
  className = '',
}: {
  value: number
  onChange: (next: number) => void
  min: number
  max: number
  step?: number
  suffix?: string
  rows?: 3 | 5 | 7
  className?: string
}) {
  const scrollRef = useRef<HTMLDivElement>(null)
  const rafRef = useRef(0)
  const currentRef = useRef(value)
  const count = Math.floor((max - min) / step) + 1
  const height = ITEM_H * rows
  const pad = (height - ITEM_H) / 2

  // Depth styling: rows fade and shrink as they leave the center detent.
  const paint = (el: HTMLDivElement) => {
    const center = el.scrollTop + height / 2
    el.querySelectorAll<HTMLElement>('[data-wheel-item]').forEach((item) => {
      const mid = item.offsetTop + ITEM_H / 2
      const dist = Math.min(Math.abs(mid - center) / ITEM_H, rows / 2)
      item.style.opacity = String(Math.max(0.15, 1 - dist * 0.38))
      item.style.transform = `scale(${Math.max(0.78, 1 - dist * 0.09)})`
    })
  }

  useEffect(() => {
    const el = scrollRef.current
    if (!el) return
    el.scrollTop = Math.round((value - min) / step) * ITEM_H
    paint(el)
    // position once on mount — afterwards the wheel is the source of truth
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const onScroll = () => {
    cancelAnimationFrame(rafRef.current)
    rafRef.current = requestAnimationFrame(() => {
      const el = scrollRef.current
      if (!el) return
      paint(el)
      const idx = Math.max(0, Math.min(count - 1, Math.round(el.scrollTop / ITEM_H)))
      const next = min + idx * step
      if (next !== currentRef.current) {
        currentRef.current = next
        haptic('light')
        onChange(next)
      }
    })
  }

  return (
    <div className={`relative ${className}`} style={{ height }}>
      {/* center detent highlight */}
      <div
        className="pointer-events-none absolute inset-x-0 rounded-[12px] bg-surface-2/70 ring-1 ring-inset ring-white/[0.06]"
        style={{ top: pad, height: ITEM_H }}
      />
      <div
        ref={scrollRef}
        onScroll={onScroll}
        className="no-scrollbar h-full overflow-y-auto overscroll-contain"
        style={{
          scrollSnapType: 'y mandatory',
          paddingTop: pad,
          paddingBottom: pad,
          maskImage: 'linear-gradient(to bottom, transparent, black 24%, black 76%, transparent)',
          WebkitMaskImage: 'linear-gradient(to bottom, transparent, black 24%, black 76%, transparent)',
        }}
        role="listbox"
        aria-label={suffix ? `Value in ${suffix}` : 'Value'}
      >
        {Array.from({ length: count }, (_, i) => {
          const v = min + i * step
          return (
            <div
              key={v}
              data-wheel-item
              className="flex items-baseline justify-center gap-1"
              style={{ height: ITEM_H, scrollSnapAlign: 'center' }}
              role="option"
              aria-selected={v === value}
            >
              <span className="text-[22px] font-semibold text-ink tnum leading-[44px]">{v}</span>
              {suffix && <span className="text-[12px] font-medium text-ink-3">{suffix}</span>}
            </div>
          )
        })}
      </div>
    </div>
  )
}
