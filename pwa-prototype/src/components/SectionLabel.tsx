import type { ReactNode } from 'react'

// One overline spec across all screens (was defined 3 different ways).
export function SectionLabel({ children }: { children: ReactNode }) {
  return (
    <div className="px-1 pt-6 pb-2 text-[11px] font-semibold uppercase tracking-[0.06em] text-ink-3">{children}</div>
  )
}
