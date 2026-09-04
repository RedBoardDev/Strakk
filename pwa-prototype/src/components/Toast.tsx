import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { Icon } from './Icon.tsx'
import { spring } from '../lib/ios.ts'

// Lightweight confirmation toast (iOS-style capsule, top center). One at a
// time; auto-dismisses. Gives every store mutation visible feedback.
type ToastContextValue = { show: (message: string) => void }

const ToastContext = createContext<ToastContextValue | null>(null)

export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used within ToastProvider')
  return ctx
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [message, setMessage] = useState<string | null>(null)
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)

  const show = useCallback((next: string) => {
    if (timer.current) clearTimeout(timer.current)
    setMessage(next)
    timer.current = setTimeout(() => setMessage(null), 1800)
  }, [])

  return (
    <ToastContext.Provider value={{ show }}>
      {children}
      <div className="pointer-events-none absolute inset-x-0 z-[70] flex justify-center" style={{ top: 'calc(var(--safe-top, env(safe-area-inset-top)) + 10px)' }}>
        <AnimatePresence>
          {message && (
            <motion.div
              initial={{ opacity: 0, y: -16, scale: 0.92 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: -12, scale: 0.95 }}
              transition={spring.snappy}
              className="flex items-center gap-2 rounded-full bg-surface-2/95 backdrop-blur-xl px-4 h-10 shadow-[0_8px_24px_rgba(0,0,0,0.45)] ring-1 ring-white/10"
            >
              <Icon name="check" size={15} className="text-success" />
              <span className="text-[14px] font-medium text-ink">{message}</span>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </ToastContext.Provider>
  )
}
