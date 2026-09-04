import { useEffect, useState, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { AnimatePresence, motion, useMotionValue, animate, useDragControls, type PanInfo } from 'motion/react'
import { spring, haptic } from '../lib/ios.ts'
import { useOverlayHost } from '../overlay.ts'
import { Icon } from './Icon.tsx'

type Detent = 'small' | 'medium' | 'large'

function useViewportHeight() {
  const [vh, setVh] = useState(() => (typeof window === 'undefined' ? 800 : window.innerHeight))
  useEffect(() => {
    const on = () => setVh(window.innerHeight)
    window.addEventListener('resize', on)
    return () => window.removeEventListener('resize', on)
  }, [])
  return vh
}

// Auto-height bottom sheet: the panel is exactly as tall as its content (so there
// is NEVER empty space and the footer always hugs the content), capped at a max
// so tall content scrolls instead. `detents[0]` sets that cap. Drag the grabber
// down (or flick) to dismiss; tap the backdrop to close.
export function Sheet({
  open,
  onClose,
  title,
  detents = ['large'],
  children,
  footer,
}: {
  open: boolean
  onClose: () => void
  title?: string
  detents?: Detent[]
  children: ReactNode
  footer?: ReactNode
}) {
  const vh = useViewportHeight()
  const cap = detents[0]
  const maxHeight = (cap === 'large' ? 0.92 : cap === 'medium' ? 0.72 : 0.5) * vh
  const closedY = vh

  const y = useMotionValue(closedY)
  const dragControls = useDragControls()
  const host = useOverlayHost()

  useEffect(() => {
    if (open) {
      animate(y, 0, spring.sheet)
      haptic('light')
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  // Ask the parent to close. Never animate out here: the parent may VETO the
  // close (e.g. MealBuilder's "Discard meal?" confirmation) and keep the sheet
  // open — pre-animating would leave a dead dimmed backdrop with the panel
  // off-screen (app feels frozen). The exit animation runs on real unmount
  // via AnimatePresence.
  const close = () => onClose()

  const onDragEnd = (_: unknown, info: PanInfo) => {
    if (info.velocity.y > 650 || info.offset.y > 120) {
      onClose()
      // Spring back in case the parent vetoes; on a real close the exit
      // animation takes over from the current drag position.
      animate(y, 0, spring.sheet)
    } else {
      animate(y, 0, spring.snappy)
    }
  }

  return createPortal(
    <AnimatePresence>
      {open && (
        <div className="fixed inset-0 z-50">
          <motion.div
            className="absolute inset-0 bg-black/70"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.25 }}
            onClick={close}
          />
          <motion.div
            className="absolute inset-x-0 bottom-0 bg-bg rounded-t-sheet flex flex-col"
            style={{ maxHeight, y }}
            exit={{ y: closedY }}
            transition={spring.sheet}
            drag="y"
            dragListener={false}
            dragControls={dragControls}
            dragConstraints={{ top: 0, bottom: closedY }}
            dragElastic={{ top: 0.03, bottom: 0.2 }}
            onDragEnd={onDragEnd}
          >
            <div className="pointer-events-none absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-white/15 to-transparent" />
            {/* grabber + nav = the drag handle (so the body scrolls freely) */}
            <div className="shrink-0 cursor-grab touch-none" onPointerDown={(event) => dragControls.start(event)}>
              <div className="pt-2.5 pb-1 flex justify-center">
                <div className="h-1.5 w-9 rounded-full bg-ink-4/60" />
              </div>
              {title !== undefined && (
                <div className="relative h-11 flex items-center justify-center px-3">
                  <button
                    type="button"
                    onClick={close}
                    className="absolute left-2 size-11 flex items-center justify-center text-ink-2"
                    aria-label="Close"
                  >
                    <Icon name="xmark" size={20} />
                  </button>
                  <span className="text-[17px] font-semibold text-ink">{title}</span>
                </div>
              )}
            </div>
            {/* body grows with content, scrolls only when the sheet hits its cap */}
            <div className="scroll-y no-scrollbar min-h-0 px-5">{children}</div>
            {footer && (
              <div className="shrink-0 px-5 pt-3 pb-safe-tight bg-bg border-t border-divider/50">
                {footer}
              </div>
            )}
          </motion.div>
        </div>
      )}
    </AnimatePresence>,
    host ?? document.body,
  )
}
