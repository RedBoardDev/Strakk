import { useEffect, useRef, useState } from 'react'
import { animate, useMotionValue } from 'motion/react'

// Animates a number when it CHANGES (like SwiftUI .contentTransition(.numericText())) —
// not a count-up-from-zero on every mount. Tabular figures keep width stable.
export function AnimatedNumber({
  value,
  className = '',
  format = (n) => Math.round(n).toString(),
  duration = 0.45,
}: {
  value: number
  className?: string
  format?: (n: number) => string
  duration?: number
}) {
  const mv = useMotionValue(value)
  const [display, setDisplay] = useState(() => format(value))
  const prev = useRef(value)

  useEffect(() => {
    if (prev.current === value) return
    const controls = animate(mv, value, {
      duration,
      ease: [0.32, 0.72, 0, 1],
      onUpdate: (v) => setDisplay(format(v)),
    })
    prev.current = value
    return () => controls.stop()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value])

  return <span className={`tnum ${className}`}>{display}</span>
}
