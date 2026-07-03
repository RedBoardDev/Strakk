import { createPortal } from 'react-dom'
import { AnimatePresence, motion } from 'motion/react'
import { spring, haptic } from '../lib/ios.ts'
import { useOverlayHost } from '../overlay.ts'

// iOS-style centered confirmation alert (used for destructive actions). Sits
// above sheets/pages (z-60). Portaled to the shell overlay host so it is never
// trapped inside a scroll container.
export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = 'Delete',
  destructive = true,
  onConfirm,
  onCancel,
}: {
  open: boolean
  title: string
  message?: string
  confirmLabel?: string
  destructive?: boolean
  onConfirm: () => void
  onCancel: () => void
}) {
  const host = useOverlayHost()
  return createPortal(
    <AnimatePresence>
      {open && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center px-10">
          <motion.div
            className="absolute inset-0 bg-black/60"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            onClick={onCancel}
          />
          <motion.div
            className="relative w-full max-w-[300px] rounded-modal bg-surface-2 overflow-hidden"
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.95, opacity: 0 }}
            transition={spring.snappy}
          >
            <div className="px-5 pt-5 pb-4 text-center">
              <div className="text-[17px] font-semibold text-ink">{title}</div>
              {message && <div className="mt-1.5 text-[13px] leading-snug text-ink-2">{message}</div>}
            </div>
            <div className="grid grid-cols-2 border-t border-divider/60">
              <button
                type="button"
                onClick={onCancel}
                className="h-12 text-[16px] font-medium text-ink-2 border-r border-divider/60 active:bg-surface-3"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  haptic('medium')
                  onConfirm()
                }}
                className={`h-12 text-[16px] font-semibold active:bg-surface-3 ${destructive ? 'text-error' : 'text-primary'}`}
              >
                {confirmLabel}
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>,
    host ?? document.body,
  )
}
