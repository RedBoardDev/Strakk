import { useEffect, useState } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { Sheet } from '../../components/Sheet.tsx'
import { Button } from '../../components/Button.tsx'
import { Icon } from '../../components/Icon.tsx'
import { haptic, spring } from '../../lib/ios.ts'
import { useNav } from '../../nav.ts'
import { scannedProduct } from '../../data/mock.ts'

const SCAN_MS = 1600
const BARCODE = '7340197305001'

const factor = scannedProduct.serving.grams / 100
const SERVING = {
  calories: Math.round(scannedProduct.per100.calories * factor),
  protein: Math.round(scannedProduct.per100.protein * factor),
  fat: Math.round(scannedProduct.per100.fat * factor),
  carbs: Math.round(scannedProduct.per100.carbs * factor),
}

const MACROS: { label: string; value: string; color: string }[] = [
  { label: 'kcal', value: `${SERVING.calories}`, color: 'text-primary-light' },
  { label: 'Protein', value: `${SERVING.protein}g`, color: 'text-protein' },
  { label: 'Fat', value: `${SERVING.fat}g`, color: 'text-fat' },
  { label: 'Carbs', value: `${SERVING.carbs}g`, color: 'text-carbs' },
]

function Viewport({ found }: { found: boolean }) {
  const accent = found ? 'border-success' : 'border-primary'
  return (
    <div className="relative h-[340px] rounded-hero overflow-hidden bg-black">
      {/* Faux live-camera feed */}
      <div className="absolute inset-0 bg-gradient-to-b from-[#0B1228] via-[#04070F] to-[#0B1228]" />
      <div
        className="absolute inset-0 opacity-60"
        style={{ background: 'radial-gradient(120% 90% at 50% 40%, rgba(75,141,255,0.12), transparent 60%)' }}
      />

      {/* Live indicator */}
      <div className="absolute top-3 left-3 flex items-center gap-1.5 rounded-full bg-black/40 px-2 py-1 backdrop-blur-sm">
        <span className={`size-1.5 rounded-full ${found ? 'bg-success' : 'bg-error animate-pulse'}`} />
        <span className="text-[10px] font-semibold uppercase tracking-wide text-white/80">
          {found ? 'Captured' : 'Live'}
        </span>
      </div>
      <div className="absolute bottom-3 left-3 flex items-center gap-1.5 rounded-full bg-black/40 px-2 py-1 backdrop-blur-sm">
        <Icon name="camera" size={11} className="text-white/70" />
        <span className="text-[10px] font-medium text-white/70">PWA camera</span>
      </div>

      {/* Reticle */}
      <div className="absolute inset-0 flex items-center justify-center">
        <div className="relative h-40 w-64">
          <span className={`absolute top-0 left-0 size-6 rounded-tl-xl border-t-[3px] border-l-[3px] ${accent}`} />
          <span className={`absolute top-0 right-0 size-6 rounded-tr-xl border-t-[3px] border-r-[3px] ${accent}`} />
          <span className={`absolute bottom-0 left-0 size-6 rounded-bl-xl border-b-[3px] border-l-[3px] ${accent}`} />
          <span className={`absolute bottom-0 right-0 size-6 rounded-br-xl border-b-[3px] border-r-[3px] ${accent}`} />

          <div className="absolute inset-0 flex items-center justify-center">
            <Icon name="barcode" size={84} className="text-white/10" />
          </div>

          {!found && (
            <motion.div
              className="absolute left-1 right-1 h-[2px] rounded-full bg-gradient-to-r from-transparent via-primary to-transparent"
              style={{ boxShadow: '0 0 14px 2px rgba(255,122,61,0.55)' }}
              initial={{ y: 6 }}
              animate={{ y: 150 }}
              transition={{ duration: 1.3, repeat: Infinity, repeatType: 'reverse', ease: 'easeInOut' }}
            />
          )}

          {found && (
            <motion.div
              className="absolute inset-0 flex items-center justify-center"
              initial={{ scale: 0.5, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={spring.snappy}
            >
              <div className="size-14 rounded-full bg-success/20 flex items-center justify-center">
                <Icon name="check" size={28} className="text-success" />
              </div>
            </motion.div>
          )}
        </div>
      </div>
    </div>
  )
}

function ResultCard({ onAdd }: { onAdd: () => void }) {
  return (
    <motion.div
      key="result"
      initial={{ opacity: 0, y: 18 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0 }}
      transition={spring.page}
      className="bg-surface-1 rounded-card p-4"
    >
      <div className="flex items-center gap-1.5 text-[12px] font-semibold text-success">
        <Icon name="check" size={14} /> Product matched
      </div>
      <div className="mt-2.5 flex items-start gap-3">
        <div className="size-12 rounded-modal bg-surface-3 flex items-center justify-center shrink-0">
          <Icon name="barcode" size={22} className="text-primary" />
        </div>
        <div className="flex-1 min-w-0">
          <div className="text-[17px] font-semibold text-ink leading-tight">{scannedProduct.name}</div>
          <div className="mt-0.5 text-[12px] text-ink-3 truncate">
            {scannedProduct.brand} · {scannedProduct.serving.label} ({scannedProduct.serving.grams} g)
          </div>
        </div>
      </div>

      <div className="mt-3 grid grid-cols-4 gap-2">
        {MACROS.map((macro) => (
          <div key={macro.label} className="bg-surface-2 rounded-[10px] py-2 flex flex-col items-center gap-0.5">
            <span className={`text-[16px] font-bold tnum ${macro.color}`}>{macro.value}</span>
            <span className="text-[10px] text-ink-3">{macro.label}</span>
          </div>
        ))}
      </div>

      <div className="mt-3 flex items-center gap-1.5 text-[11px] text-ink-4 tnum">
        <Icon name="barcode" size={12} /> {BARCODE}
      </div>

      <div className="mt-4">
        <Button variant="primary" full glow onClick={onAdd}>
          Add to log
        </Button>
      </div>
    </motion.div>
  )
}

function ScanHint({ onManual }: { onManual: () => void }) {
  return (
    <motion.div
      key="hint"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.2 }}
      className="flex flex-col items-center gap-3 text-center"
    >
      <div className="text-[15px] text-ink-2">Point the camera at a barcode</div>
      <div className="text-[12px] text-ink-4 px-8">
        Yes — a PWA can read product barcodes straight from the device camera.
      </div>
      <button
        type="button"
        onClick={() => {
          haptic('light')
          onManual()
        }}
        className="mt-1 text-[13px] font-semibold text-primary"
      >
        Enter manually
      </button>
    </motion.div>
  )
}

export function ScanSheet({ open, onClose }: { open: boolean; onClose: () => void }) {
  const nav = useNav()
  const [found, setFound] = useState(false)

  // Restart the scan simulation every time the sheet opens.
  useEffect(() => {
    if (!open) {
      setFound(false)
      return
    }
    setFound(false)
    const timer = setTimeout(() => {
      setFound(true)
      haptic('medium')
    }, SCAN_MS)
    return () => clearTimeout(timer)
  }, [open])

  return (
    <Sheet open={open} onClose={onClose} title="Scan barcode" detents={['large']}>
      <div className="pt-2">
        <Viewport found={found} />
      </div>
      <div className="pt-6 pb-8">
        <AnimatePresence mode="wait">
          {found ? (
            <ResultCard onAdd={() => nav.open({ kind: 'foodDetail', food: scannedProduct, from: 'scan' })} />
          ) : (
            <ScanHint onManual={() => nav.open({ kind: 'search' })} />
          )}
        </AnimatePresence>
      </div>
    </Sheet>
  )
}
