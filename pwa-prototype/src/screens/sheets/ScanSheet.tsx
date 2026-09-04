import { useEffect, useRef, useState } from 'react'
import { Sheet } from '../../components/Sheet.tsx'
import { Icon } from '../../components/Icon.tsx'
import { haptic } from '../../lib/ios.ts'
import { useNav } from '../../nav.ts'
import { lookupBarcode } from '../../api/foods.ts'

// Real barcode scanning: live camera feed + the native BarcodeDetector where
// the browser has one (Chrome/Android). Browsers without it (iOS Safari, for
// now) get the camera preview and a clear fallback to search.
type BarcodeDetectorLike = { detect: (source: CanvasImageSource) => Promise<{ rawValue: string }[]> }
type BarcodeDetectorCtor = new (options?: { formats?: string[] }) => BarcodeDetectorLike

function getDetector(): BarcodeDetectorLike | null {
  const ctor = (globalThis as { BarcodeDetector?: BarcodeDetectorCtor }).BarcodeDetector
  if (!ctor) return null
  try {
    return new ctor({ formats: ['ean_13', 'ean_8', 'upc_a', 'upc_e', 'code_128'] })
  } catch {
    return null
  }
}

type Status = 'starting' | 'scanning' | 'looking-up' | 'unsupported' | 'no-camera'

export function ScanSheet({
  open,
  onClose,
  logDate,
}: {
  open: boolean
  onClose: () => void
  logDate?: string
}) {
  const nav = useNav()
  const videoRef = useRef<HTMLVideoElement>(null)
  const [status, setStatus] = useState<Status>('starting')
  const [lastMiss, setLastMiss] = useState<string | null>(null)

  useEffect(() => {
    if (!open) return
    let stream: MediaStream | null = null
    let stopped = false
    let interval: ReturnType<typeof setInterval> | null = null
    setStatus('starting')
    setLastMiss(null)

    const detector = getDetector()

    void (async () => {
      try {
        stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: 'environment' },
          audio: false,
        })
        if (stopped) {
          stream.getTracks().forEach((t) => t.stop())
          return
        }
        const video = videoRef.current
        if (video) {
          video.srcObject = stream
          await video.play().catch(() => {})
        }
        if (!detector) {
          setStatus('unsupported')
          return
        }
        setStatus('scanning')
        let busy = false
        interval = setInterval(() => {
          const v = videoRef.current
          if (!v || busy || v.readyState < 2) return
          busy = true
          void detector
            .detect(v)
            .then(async (codes) => {
              const code = codes[0]?.rawValue
              if (!code) return
              haptic('medium')
              setStatus('looking-up')
              const food = await lookupBarcode(code)
              if (food) {
                nav.open({ kind: 'foodDetail', food, from: 'scan', logDate })
              } else {
                setLastMiss(code)
                setStatus('scanning')
              }
            })
            .catch(() => {})
            .finally(() => {
              busy = false
            })
        }, 350)
      } catch {
        setStatus('no-camera')
      }
    })()

    return () => {
      stopped = true
      if (interval) clearInterval(interval)
      stream?.getTracks().forEach((t) => t.stop())
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  const scanning = status === 'scanning' || status === 'looking-up'

  return (
    <Sheet open={open} onClose={onClose} title="Scan barcode" detents={['large']}>
      <div className="pt-2">
        <div className="relative h-[340px] rounded-hero overflow-hidden bg-black">
          <video ref={videoRef} playsInline muted className="absolute inset-0 h-full w-full object-cover" />

          {/* Live indicator */}
          <div className="absolute top-3 left-3 flex items-center gap-1.5 rounded-full bg-black/40 px-2 py-1 backdrop-blur-sm">
            <span className={`size-1.5 rounded-full ${scanning ? 'bg-error animate-pulse' : 'bg-ink-4'}`} />
            <span className="text-[10px] font-semibold uppercase tracking-wide text-white/80">
              {status === 'looking-up' ? 'Looking up…' : scanning ? 'Live' : 'Camera'}
            </span>
          </div>

          {/* Reticle */}
          {scanning && (
            <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
              <div className="relative h-40 w-64">
                <span className="absolute top-0 left-0 size-6 rounded-tl-xl border-t-[3px] border-l-[3px] border-primary" />
                <span className="absolute top-0 right-0 size-6 rounded-tr-xl border-t-[3px] border-r-[3px] border-primary" />
                <span className="absolute bottom-0 left-0 size-6 rounded-bl-xl border-b-[3px] border-l-[3px] border-primary" />
                <span className="absolute bottom-0 right-0 size-6 rounded-br-xl border-b-[3px] border-r-[3px] border-primary" />
              </div>
            </div>
          )}

          {(status === 'unsupported' || status === 'no-camera') && (
            <div className="absolute inset-0 flex items-center justify-center p-8">
              <div className="rounded-2xl bg-black/60 backdrop-blur-sm px-5 py-4 text-center">
                <Icon name="barcode" size={28} className="text-white/40 mx-auto" />
                <div className="mt-2 text-[14px] text-white/85">
                  {status === 'no-camera'
                    ? 'Camera unavailable — allow camera access and retry.'
                    : 'Barcode scanning isn’t supported by this browser yet.'}
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="pt-6 pb-8 flex flex-col items-center gap-3 text-center">
          {lastMiss && (
            <div className="text-[13px] text-warning">
              Barcode {lastMiss} isn’t in the catalog yet — try searching it.
            </div>
          )}
          <div className="text-[15px] text-ink-2">Point the camera at a product barcode</div>
          <button
            type="button"
            onClick={() => {
              haptic('light')
              nav.open({ kind: 'search', logDate })
            }}
            className="text-[13px] font-semibold text-primary"
          >
            Search instead
          </button>
          {status === 'looking-up' && (
            <div className="size-5 rounded-full border-2 border-primary/30 border-t-primary animate-spin" />
          )}
        </div>
      </div>
    </Sheet>
  )
}
