import { useEffect, useState, type CSSProperties, type ReactNode } from 'react'

function useIsDesktop() {
  const [desktop, setDesktop] = useState(() =>
    typeof window === 'undefined' ? false : window.matchMedia('(min-width: 480px) and (min-height: 720px)').matches,
  )
  useEffect(() => {
    const mq = window.matchMedia('(min-width: 480px) and (min-height: 720px)')
    const on = () => setDesktop(mq.matches)
    mq.addEventListener('change', on)
    return () => mq.removeEventListener('change', on)
  }, [])
  return desktop
}

// Scale the 844px-tall frame down so it always fits the viewport height.
function useFitScale(active: boolean) {
  const [scale, setScale] = useState(1)
  useEffect(() => {
    if (!active) return
    const compute = () => setScale(Math.min(1, Math.max(0.55, (window.innerHeight - 24) / 868)))
    compute()
    window.addEventListener('resize', compute)
    return () => window.removeEventListener('resize', compute)
  }, [active])
  return scale
}

// Faux iOS status bar — only shown in the desktop device frame (on a real phone
// the OS draws the real one). 9:41, the canonical Apple demo time.
function StatusBar() {
  return (
    <div className="absolute top-0 inset-x-0 z-40 h-[54px] flex items-end justify-between px-8 pb-1.5 pointer-events-none">
      <span className="text-[15px] font-semibold text-ink tracking-tight">9:41</span>
      <div className="flex items-center gap-1.5 text-ink">
        <svg width="18" height="12" viewBox="0 0 18 12" fill="currentColor"><rect x="0" y="7" width="3" height="5" rx="1"/><rect x="5" y="4.5" width="3" height="7.5" rx="1"/><rect x="10" y="2" width="3" height="10" rx="1"/><rect x="15" y="0" width="3" height="12" rx="1"/></svg>
        <svg width="16" height="12" viewBox="0 0 16 12" fill="currentColor"><path d="M8 2.3c2 0 3.9.8 5.3 2.1l1.2-1.2C13 1.5 10.6.6 8 .6S3 1.5 1.5 3.2l1.2 1.2C4.1 3.1 6 2.3 8 2.3Z"/><path d="M8 5.6c1.1 0 2.1.4 2.9 1.2l1.2-1.2C11 4.5 9.6 4 8 4s-3 .5-4.1 1.6l1.2 1.2C5.9 6 6.9 5.6 8 5.6Z"/><circle cx="8" cy="9.5" r="1.6"/></svg>
        <div className="flex items-center gap-0.5">
          <div className="w-[22px] h-[11px] rounded-[3px] border border-ink/40 p-[1.5px]"><div className="h-full w-[80%] rounded-[1px] bg-ink"/></div>
        </div>
      </div>
    </div>
  )
}

export function DeviceFrame({ children }: { children: ReactNode }) {
  const desktop = useIsDesktop()
  const scale = useFitScale(desktop)

  if (!desktop) {
    return <div className="relative h-full w-full overflow-hidden bg-bg">{children}</div>
  }

  return (
    <div className="min-h-[100dvh] w-full flex items-center justify-center bg-[#0a0a0f] overflow-hidden">
      <div
        className="relative rounded-[56px] bg-black p-[10px] shadow-2xl shadow-black/60 ring-1 ring-white/10"
        style={{ transform: `scale(${scale})` }}
      >
        <div
          className="relative w-[390px] h-[844px] rounded-[46px] overflow-hidden bg-bg"
          style={{ '--safe-top': '54px', '--safe-bottom': '24px' } as CSSProperties}
        >
          <StatusBar />
          {/* Dynamic Island */}
          <div className="absolute top-2.5 left-1/2 -translate-x-1/2 z-50 w-[120px] h-[34px] rounded-full bg-black" />
          {children}
          {/* Home indicator */}
          <div className="absolute bottom-1.5 left-1/2 -translate-x-1/2 z-40 w-[134px] h-[5px] rounded-full bg-ink/60 pointer-events-none" />
        </div>
      </div>
    </div>
  )
}
