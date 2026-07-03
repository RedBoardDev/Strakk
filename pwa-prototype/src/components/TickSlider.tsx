import { useRef, useState } from 'react'
import { haptic } from '../lib/ios.ts'

// Horizontal graduated slider that snaps to whole steps — drag anywhere on the
// track or tap a position. Ticks light up as the fill passes them; the active
// value's label pops in primary.
export function TickSlider({
  value,
  onChange,
  min,
  max,
  format = (v: number) => String(v),
  ariaLabel,
}: {
  value: number
  onChange: (next: number) => void
  min: number
  max: number
  format?: (v: number) => string
  ariaLabel: string
}) {
  const trackRef = useRef<HTMLDivElement>(null)
  const [dragging, setDragging] = useState(false)
  const steps = max - min
  const pct = ((value - min) / steps) * 100

  const setFromX = (clientX: number) => {
    const rect = trackRef.current?.getBoundingClientRect()
    if (!rect || rect.width === 0) return
    const ratio = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width))
    const next = min + Math.round(ratio * steps)
    if (next !== value) {
      haptic('light')
      onChange(next)
    }
  }

  return (
    <div className="select-none px-1">
      <div
        ref={trackRef}
        role="slider"
        aria-label={ariaLabel}
        aria-valuemin={min}
        aria-valuemax={max}
        aria-valuenow={value}
        tabIndex={0}
        onKeyDown={(event) => {
          if (event.key === 'ArrowRight' && value < max) onChange(value + 1)
          if (event.key === 'ArrowLeft' && value > min) onChange(value - 1)
        }}
        className="relative h-11 touch-none cursor-pointer outline-none"
        onPointerDown={(event) => {
          event.currentTarget.setPointerCapture(event.pointerId)
          setDragging(true)
          setFromX(event.clientX)
        }}
        onPointerMove={(event) => dragging && setFromX(event.clientX)}
        onPointerUp={() => setDragging(false)}
        onPointerCancel={() => setDragging(false)}
      >
        {/* rail */}
        <div className="absolute inset-x-0 top-1/2 h-[4px] -translate-y-1/2 rounded-full bg-surface-3" />
        {/* fill */}
        <div
          className="absolute top-1/2 h-[4px] -translate-y-1/2 rounded-full bg-gradient-to-r from-primary/60 to-primary"
          style={{ width: `${pct}%`, transition: dragging ? 'none' : 'width 0.18s ease' }}
        />
        {/* graduations */}
        {Array.from({ length: steps + 1 }, (_, i) => (
          <div
            key={i}
            className={`absolute top-1/2 h-[12px] w-[2px] -translate-y-1/2 rounded-full ${
              min + i <= value ? 'bg-primary/60' : 'bg-ink-4/35'
            }`}
            style={{ left: `calc(${(i / steps) * 100}% - 1px)` }}
          />
        ))}
        {/* thumb */}
        <div
          className="absolute top-1/2 size-7 rounded-full bg-primary ring-4 ring-bg shadow-[0_2px_14px_rgba(255,122,61,0.55)]"
          style={{
            left: `${pct}%`,
            transform: `translate(-50%, -50%) scale(${dragging ? 1.18 : 1})`,
            transition: dragging ? 'transform 0.12s ease' : 'left 0.18s ease, transform 0.12s ease',
          }}
        />
      </div>
      {/* step labels */}
      <div className="relative h-5 mt-0.5">
        {Array.from({ length: steps + 1 }, (_, i) => {
          const v = min + i
          return (
            <span
              key={v}
              className={`absolute -translate-x-1/2 text-[12px] tnum transition-colors ${
                v === value ? 'font-bold text-primary' : 'text-ink-4'
              }`}
              style={{ left: `${(i / steps) * 100}%` }}
            >
              {format(v)}
            </span>
          )
        })}
      </div>
    </div>
  )
}
