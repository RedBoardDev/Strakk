import type { ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { AnimatePresence, motion, useDragControls, type PanInfo } from 'motion/react'
import { ChevronLeft } from 'lucide-react'
import { spring, haptic } from '../lib/ios.ts'
import { useOverlayHost } from '../overlay.ts'

// A full-screen pushed page (iOS NavigationStack style): slides in from the
// right, back chevron, and an interactive LEFT-EDGE swipe-back gesture (drag the
// left edge → the page follows the finger; release past a threshold to go back).
// Content scrolls freely (drag only starts from the edge strip).
export function PushPage({
  open,
  onClose,
  title,
  trailing,
  children,
  footer,
}: {
  open: boolean
  onClose: () => void
  title: string
  trailing?: ReactNode
  children: ReactNode
  footer?: ReactNode
}) {
  const dragControls = useDragControls()
  const host = useOverlayHost()

  const onDragEnd = (_: unknown, info: PanInfo) => {
    if (info.offset.x > 90 || info.velocity.x > 550) {
      haptic('light')
      onClose()
    }
  }

  return createPortal(
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-50 bg-bg flex flex-col"
          initial={{ x: '100%' }}
          animate={{ x: 0 }}
          exit={{ x: '100%' }}
          transition={spring.page}
          drag="x"
          dragListener={false}
          dragControls={dragControls}
          dragConstraints={{ left: 0, right: 0 }}
          dragElastic={{ left: 0, right: 0.9 }}
          onDragEnd={onDragEnd}
        >
          {/* Left-edge swipe-back zone — the only place a drag starts */}
          <div
            className="absolute left-0 top-0 bottom-0 z-50 w-7 touch-none"
            onPointerDown={(event) => dragControls.start(event)}
          />

          {/* Nav bar */}
          <div className="shrink-0 pt-safe">
            <div className="relative h-11 flex items-center px-2">
              <button
                type="button"
                onClick={() => {
                  haptic('light')
                  onClose()
                }}
                className="relative z-10 -ml-1 h-11 px-1 flex items-center gap-0.5 text-primary"
              >
                <ChevronLeft size={24} />
                <span className="text-[16px]">Back</span>
              </button>
              <span className="absolute left-1/2 -translate-x-1/2 max-w-[55%] truncate text-[17px] font-semibold text-ink">
                {title}
              </span>
              {trailing && <div className="relative z-10 ml-auto pr-1">{trailing}</div>}
            </div>
          </div>

          {/* Content */}
          <div className="scroll-y no-scrollbar flex-1 min-h-0 px-5 pb-32">{children}</div>

          {footer && (
            <div className="shrink-0 px-5 pt-3 pb-safe-tight bg-bg border-t border-divider/50">
              {footer}
            </div>
          )}
        </motion.div>
      )}
    </AnimatePresence>,
    host ?? document.body,
  )
}
