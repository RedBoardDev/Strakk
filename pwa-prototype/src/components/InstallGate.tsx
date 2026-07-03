import { useEffect, useState, type ReactNode } from 'react'
import { motion } from 'motion/react'
import { Share, SquarePlus, Check, X, ChevronDown } from 'lucide-react'
import { Button } from './Button.tsx'
import { haptic, spring } from '../lib/ios.ts'

type Platform = 'ios' | 'android' | 'desktop'

type Env = { platform: Platform; standalone: boolean; mobile: boolean }

function detectEnv(): Env {
  if (typeof navigator === 'undefined' || typeof window === 'undefined') {
    return { platform: 'desktop', standalone: false, mobile: false }
  }
  const ua = navigator.userAgent
  const ios = /iphone|ipad|ipod/i.test(ua) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)
  const android = /android/i.test(ua)
  const standalone =
    window.matchMedia('(display-mode: standalone)').matches ||
    (navigator as unknown as { standalone?: boolean }).standalone === true
  return { platform: ios ? 'ios' : android ? 'android' : 'desktop', standalone, mobile: ios || android }
}

// Minimal typing for the Chromium install prompt event.
type InstallPromptEvent = Event & { prompt: () => Promise<void>; userChoice: Promise<{ outcome: string }> }

function AppIcon() {
  return (
    <img
      src="/icons/icon-512.png"
      alt="Strakk"
      className="size-20 rounded-[22px] ring-1 ring-white/10 shadow-[0_14px_34px_-8px_rgba(0,0,0,0.7)]"
    />
  )
}

function Step({ n, children }: { n: number; children: ReactNode }) {
  return (
    <div className="flex items-center gap-3">
      <div className="size-6 rounded-full bg-surface-2 flex items-center justify-center shrink-0">
        <span className="text-[12px] font-bold text-ink-2 tnum">{n}</span>
      </div>
      <div className="flex-1 text-[15px] text-ink flex items-center gap-1.5 flex-wrap">{children}</div>
    </div>
  )
}

function Guide({ env, onDismiss }: { env: Env; onDismiss: () => void }) {
  const [deferred, setDeferred] = useState<InstallPromptEvent | null>(null)

  useEffect(() => {
    const onPrompt = (e: Event) => {
      e.preventDefault()
      setDeferred(e as InstallPromptEvent)
    }
    window.addEventListener('beforeinstallprompt', onPrompt)
    return () => window.removeEventListener('beforeinstallprompt', onPrompt)
  }, [])

  const androidInstall = async () => {
    if (!deferred) return
    haptic('medium')
    await deferred.prompt()
    setDeferred(null)
  }

  return (
    <div className="relative h-full w-full overflow-hidden bg-bg">
      <div className="h-full overflow-y-auto pt-safe pb-safe">
        <div className="min-h-full flex flex-col items-center justify-center px-7 py-12 text-center">
          <motion.div
            initial={{ scale: 0.8, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={spring.sheet}
          >
            <AppIcon />
          </motion.div>

          <h1 className="mt-6 text-[26px] font-bold tracking-tight text-ink">Install Strakk</h1>
          <p className="mt-2 text-[15px] leading-snug text-ink-2 max-w-[300px]">
            Add it to your Home Screen to use Strakk full-screen, like a native app — no browser bar.
          </p>

          {env.platform === 'ios' && (
            <div className="mt-8 w-full max-w-[340px] flex flex-col gap-3">
              <div className="flex items-start gap-2.5 rounded-card bg-warning/12 border border-warning/25 px-3.5 py-3 text-left">
                <Share size={16} className="text-warning mt-0.5 shrink-0" />
                <span className="text-[13px] text-ink leading-snug">
                  <span className="font-semibold text-warning">Safari only.</span> Chrome / Brave / Firefox on iPhone
                  can't install apps — open this page in <span className="font-semibold">Safari</span> first.
                </span>
              </div>
              <div className="bg-surface-1 rounded-card p-5 flex flex-col gap-4 text-left">
              <Step n={1}>
                Tap <Share size={17} className="text-primary" /> <span className="font-semibold">Share</span> in the
                Safari bar
              </Step>
              <Step n={2}>
                Choose <SquarePlus size={17} className="text-primary" />{' '}
                <span className="font-semibold">Add to Home Screen</span>
              </Step>
              <Step n={3}>
                Tap <Check size={17} className="text-primary" /> <span className="font-semibold">Add</span>
              </Step>
              </div>
            </div>
          )}

          {env.platform === 'android' && (
            <div className="mt-8 w-full max-w-[340px]">
              {deferred ? (
                <Button variant="primary" full glow onClick={androidInstall}>
                  Install Strakk
                </Button>
              ) : (
                <div className="bg-surface-1 rounded-card p-5 flex flex-col gap-4 text-left">
                  <Step n={1}>
                    Open the <span className="font-semibold">⋮ menu</span> (top right)
                  </Step>
                  <Step n={2}>
                    Tap <SquarePlus size={17} className="text-primary" />{' '}
                    <span className="font-semibold">Install app</span> / Add to Home screen
                  </Step>
                </div>
              )}
            </div>
          )}

          <button
            type="button"
            onClick={() => {
              haptic('light')
              onDismiss()
            }}
            className="mt-7 text-[14px] font-semibold text-ink-3 flex items-center gap-1"
          >
            <X size={15} /> Continue in browser
          </button>
        </div>

        {/* iOS hint arrow toward the Safari share button (bottom toolbar) */}
        {env.platform === 'ios' && (
          <motion.div
            aria-hidden
            className="pointer-events-none absolute inset-x-0 bottom-3 flex justify-center text-primary/70"
            animate={{ y: [0, 8, 0] }}
            transition={{ duration: 1.4, repeat: Infinity, ease: 'easeInOut' }}
          >
            <ChevronDown size={26} />
          </motion.div>
        )}
      </div>
    </div>
  )
}

// Shows the install guide when running in a mobile browser (not yet installed).
// Once added to the Home Screen (standalone), or on desktop, renders the app.
// `?install` forces the guide for previewing.
export function InstallGate({ children }: { children: ReactNode }) {
  const [env] = useState(detectEnv)
  const [dismissed, setDismissed] = useState(false)
  const forced = typeof window !== 'undefined' && new URLSearchParams(window.location.search).has('install')

  const showGuide = forced || (env.mobile && !env.standalone && !dismissed)
  if (!showGuide) return <>{children}</>
  return <Guide env={forced && env.platform === 'desktop' ? { ...env, platform: 'ios' } : env} onDismiss={() => setDismissed(true)} />
}
