import { useId } from 'react'
import { motion } from 'motion/react'
import { spring } from '../lib/ios.ts'
import type { SeriesPoint } from '../data/viewTypes.ts'

// Catmull-Rom smoothed SVG line + filled area, in a 0..100 × 0..height viewBox
// stretched to fill its container (preserveAspectRatio none). Shared by the list
// weight curve and the Trends weight/volume charts.
function buildPath(values: number[], height: number) {
  const xPad = 2
  const yPad = 8
  const min = Math.min(...values)
  const max = Math.max(...values)
  const span = max - min || 1
  const innerW = 100 - xPad * 2
  const innerH = height - yPad * 2
  const pts = values.map((value, i) => ({
    x: xPad + (values.length === 1 ? innerW / 2 : (i / (values.length - 1)) * innerW),
    y: yPad + (1 - (value - min) / span) * innerH,
  }))
  const round = (num: number) => Math.round(num * 100) / 100
  let line = `M ${round(pts[0].x)} ${round(pts[0].y)}`
  for (let i = 0; i < pts.length - 1; i++) {
    const p0 = pts[i - 1] ?? pts[i]
    const p1 = pts[i]
    const p2 = pts[i + 1]
    const p3 = pts[i + 2] ?? p2
    const c1x = p1.x + (p2.x - p0.x) / 6
    const c1y = p1.y + (p2.y - p0.y) / 6
    const c2x = p2.x - (p3.x - p1.x) / 6
    const c2y = p2.y - (p3.y - p1.y) / 6
    line += ` C ${round(c1x)} ${round(c1y)}, ${round(c2x)} ${round(c2y)}, ${round(p2.x)} ${round(p2.y)}`
  }
  const last = pts[pts.length - 1]
  const area = `${line} L ${round(last.x)} ${height} L ${round(pts[0].x)} ${height} Z`
  return { line, area, lastX: last.x, lastY: last.y }
}

// Sparse X-axis week labels (~5 evenly-spaced ticks) drawn under the chart.
function tickIndices(count: number, ticks = 5): number[] {
  if (count <= ticks) return Array.from({ length: count }, (_, i) => i)
  const step = (count - 1) / (ticks - 1)
  return Array.from({ length: ticks }, (_, i) => Math.round(i * step))
}

export function SeriesChart({
  series,
  color,
  height,
  labels = false,
}: {
  series: SeriesPoint[]
  color: string
  height: number
  labels?: boolean
}) {
  const gid = `chart-${useId().replace(/:/g, '')}`
  const { line, area, lastX, lastY } = buildPath(
    series.map((point) => point.value),
    height,
  )
  return (
    <div>
      <div className="relative w-full" style={{ height }}>
        <svg width="100%" height={height} viewBox={`0 0 100 ${height}`} preserveAspectRatio="none" className="block">
          <defs>
            <linearGradient id={gid} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor={color} stopOpacity={0.25} />
              <stop offset="100%" stopColor={color} stopOpacity={0} />
            </linearGradient>
          </defs>
          <motion.path
            d={area}
            fill={`url(#${gid})`}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.6, delay: 0.2 }}
          />
          <motion.path
            d={line}
            fill="none"
            stroke={color}
            strokeWidth={2.5}
            strokeLinecap="round"
            strokeLinejoin="round"
            vectorEffect="non-scaling-stroke"
            initial={{ pathLength: 0 }}
            animate={{ pathLength: 1 }}
            transition={{ duration: 0.9, ease: [0.32, 0.72, 0, 1] }}
          />
        </svg>
        <div className="absolute -translate-x-1/2 -translate-y-1/2" style={{ left: `${lastX}%`, top: lastY }}>
          <motion.div
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ delay: 0.7, ...spring.gentle }}
            className="size-2.5 rounded-full"
            style={{ backgroundColor: color, boxShadow: `0 0 0 4px ${color}33` }}
          />
        </div>
      </div>
      {labels && (
        <div className="mt-2 flex justify-between text-[11px] text-ink-4 tnum">
          {tickIndices(series.length).map((idx) => (
            <span key={series[idx].week}>{series[idx].week}</span>
          ))}
        </div>
      )}
    </div>
  )
}
