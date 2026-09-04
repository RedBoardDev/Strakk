import type { ReactNode } from 'react'
import { motion, useMotionValue, useTransform } from 'motion/react'

// iOS large-title screen. At the top the big title shows; the moment you scroll
// a FILLED (opaque) top bar snaps in with the inline title + hairline so content
// never bleeds through it. Trailing accessory stays pinned in the bar.
export function ScreenScroll({
  title,
  trailing,
  children,
}: {
  title: string
  trailing?: ReactNode
  children: ReactNode
}) {
  const y = useMotionValue(0)
  const barOpacity = useTransform(y, [4, 20], [0, 1])
  const largeOpacity = useTransform(y, [0, 14], [1, 0])

  return (
    <div className="relative h-full">
      {/* Top bar — the filled background covers the FULL top inset (notch +
          bar) so nothing bleeds through the safe area while scrolling. */}
      <div className="absolute inset-x-0 top-0 z-20">
        <motion.div
          style={{ opacity: barOpacity }}
          className="absolute inset-0 bg-bg-elevated/95 backdrop-blur-xl backdrop-saturate-150 border-b border-divider/60"
        />
        <div className="pt-safe">
          <div className="relative h-11 flex items-center justify-center px-5">
            <motion.span style={{ opacity: barOpacity }} className="relative text-[17px] font-semibold text-ink">
              {title}
            </motion.span>
            {trailing && <div className="absolute right-3 z-10">{trailing}</div>}
          </div>
        </div>
      </div>

      {/* Scroll content */}
      <div className="scroll-y no-scrollbar h-full" onScroll={(e) => y.set((e.target as HTMLElement).scrollTop)}>
        <div className="pt-safe">
          <div className="h-11" />
          <div className="px-5 pt-1 pb-2">
            <motion.h1 style={{ opacity: largeOpacity }} className="text-[34px] font-bold tracking-tight text-ink">
              {title}
            </motion.h1>
          </div>
          <div className="px-5 pb-32">{children}</div>
        </div>
      </div>
    </div>
  )
}
