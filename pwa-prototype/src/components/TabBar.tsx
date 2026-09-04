import { motion } from 'motion/react'
import { Icon, type IconName } from './Icon.tsx'
import { haptic } from '../lib/ios.ts'

export type TabKey = 'today' | 'calendar' | 'checkins' | 'settings'

const TABS: { key: TabKey; label: string; icon: IconName }[] = [
  { key: 'today', label: 'Today', icon: 'house.fill' },
  { key: 'calendar', label: 'Calendar', icon: 'calendar' },
  { key: 'checkins', label: 'Check-ins', icon: 'chart.bar' },
  { key: 'settings', label: 'Settings', icon: 'gearshape.fill' },
]

// Frosted-glass tab bar — the cross-platform-honest material (iOS Safari can't
// do live SVG refraction, so we don't fake it). Strong backdrop blur over a
// near-opaque tint plus a single specular hairline reads as native chrome;
// content scrolls underneath so the blur stays alive.
export function TabBar({ active, onChange }: { active: TabKey; onChange: (t: TabKey) => void }) {
  return (
    <div className="absolute inset-x-0 bottom-0 z-30 glass-chrome">
      {/* single specular hairline (no border — doubling reads muddy) */}
      <div className="pointer-events-none absolute inset-x-0 top-0 h-px bg-gradient-to-r from-white/5 via-white/20 to-white/5" />
      <div className="pb-safe-tight">
        <div className="relative flex items-stretch justify-around h-[49px]">
          {TABS.map((t) => {
            const on = t.key === active
            return (
              <button
                key={t.key}
                type="button"
                onClick={() => {
                  if (!on) haptic('light')
                  onChange(t.key)
                }}
                aria-current={on ? 'page' : undefined}
                className="flex-1 flex flex-col items-center justify-center gap-[3px] pt-1"
              >
                <motion.div
                  animate={{ scale: on ? 1 : 0.94, y: on ? 0 : 1 }}
                  transition={{ type: 'spring', stiffness: 560, damping: 32 }}
                >
                  <Icon
                    name={t.icon}
                    size={23}
                    className={`transition-colors duration-200 ${on ? 'text-primary' : 'text-ink-3'}`}
                  />
                </motion.div>
                <span
                  className={`text-[10px] font-medium tracking-tight transition-colors duration-200 ${
                    on ? 'text-primary' : 'text-ink-3'
                  }`}
                >
                  {t.label}
                </span>
              </button>
            )
          })}
        </div>
      </div>
    </div>
  )
}
