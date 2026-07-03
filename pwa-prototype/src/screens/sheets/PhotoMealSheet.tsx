import { useEffect, useState } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { Sheet } from '../../components/Sheet.tsx'
import { Button } from '../../components/Button.tsx'
import { Icon } from '../../components/Icon.tsx'
import { ReviewItems, reviewTotals, type ReviewItem } from '../../components/ReviewItems.tsx'
import { useToast } from '../../components/Toast.tsx'
import { haptic, spring } from '../../lib/ios.ts'
import { useStore } from '../../store.tsx'

type Stage = 'camera' | 'analyzing' | 'done'

const PLATE: ReviewItem[] = [
  { id: 'ph1', name: 'Grilled chicken', qty: '160g', kcal: 280, p: 50, f: 8, c: 0 },
  { id: 'ph2', name: 'Basmati rice', qty: '180g', kcal: 240, p: 5, f: 1, c: 52 },
  { id: 'ph3', name: 'Steamed broccoli', qty: '150g', kcal: 55, p: 4, f: 1, c: 7 },
]

// AI photo-meal add (mock): snap a plate → AI detects the items. A live-camera
// look (plate framing, NOT a barcode reticle), so it's clearly distinct.
export function PhotoMealSheet({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { dispatch } = useStore()
  const toast = useToast()
  const [stage, setStage] = useState<Stage>('camera')
  const [items, setItems] = useState<ReviewItem[]>([])

  const totals = reviewTotals(items)

  const addAll = () => {
    haptic('medium')
    dispatch({
      kind: 'meal/add',
      meal: {
        title: items.map((item) => item.name).join(', '),
        macros: totals,
        items: items.map((item) => (item.qty ? `${item.name} — ${item.qty}` : item.name)),
        source: 'photo',
      },
    })
    onClose()
    toast.show(`Added · ${totals.calories} kcal`)
  }

  useEffect(() => {
    if (open) {
      setStage('camera')
      setItems([])
    }
  }, [open])

  useEffect(() => {
    if (stage !== 'analyzing') return
    const timer = setTimeout(() => {
      setStage('done')
      setItems(PLATE.map((item) => ({ ...item })))
      haptic('success')
    }, 1700)
    return () => clearTimeout(timer)
  }, [stage])

  const footer =
    stage === 'done' ? (
      <Button variant="primary" full glow disabled={items.length === 0} onClick={addAll}>
        {items.length > 0
          ? `Add ${items.length} ${items.length === 1 ? 'item' : 'items'} · ${totals.calories} kcal`
          : 'Nothing to add'}
      </Button>
    ) : (
      <Button
        variant="primary"
        full
        glow
        disabled={stage === 'analyzing'}
        onClick={() => {
          haptic('medium')
          setStage('analyzing')
        }}
      >
        <Icon name="camera" size={16} /> {stage === 'analyzing' ? 'Analyzing…' : 'Take photo'}
      </Button>
    )

  return (
    <Sheet open={open} onClose={onClose} title="Photo meal" detents={['large']} footer={footer}>
      <div className="pt-2">
        {/* Faux live-camera viewport with a plate framing */}
        <div className="relative h-[300px] rounded-hero overflow-hidden bg-black">
          <div className="absolute inset-0 bg-gradient-to-b from-[#11182B] via-[#06090F] to-[#11182B]" />
          <div
            className="absolute inset-0 opacity-60"
            style={{ background: 'radial-gradient(120% 90% at 50% 45%, rgba(255,122,61,0.12), transparent 60%)' }}
          />
          <div className="absolute top-3 left-3 flex items-center gap-1.5 rounded-full bg-black/40 px-2 py-1 backdrop-blur-sm">
            <span className={`size-1.5 rounded-full ${stage === 'done' ? 'bg-success' : 'bg-error animate-pulse'}`} />
            <span className="text-[10px] font-semibold uppercase tracking-wide text-white/80">
              {stage === 'done' ? 'Captured' : 'Live'}
            </span>
          </div>

          {/* Plate framing circle */}
          <div className="absolute inset-0 flex items-center justify-center">
            <motion.div
              className="size-48 rounded-full border-2 border-dashed border-white/25 flex items-center justify-center"
              animate={stage === 'analyzing' ? { scale: [1, 0.96, 1] } : { scale: 1 }}
              transition={{ duration: 1.2, repeat: stage === 'analyzing' ? Infinity : 0 }}
            >
              {stage === 'done' ? (
                <motion.div initial={{ scale: 0.5, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} transition={spring.snappy}>
                  <div className="size-14 rounded-full bg-success/20 flex items-center justify-center">
                    <Icon name="check" size={28} className="text-success" />
                  </div>
                </motion.div>
              ) : (
                <Icon name="fork" size={40} className="text-white/15" />
              )}
            </motion.div>
          </div>

          {stage === 'analyzing' && (
            <div className="absolute bottom-3 inset-x-0 flex items-center justify-center gap-1.5 text-[12px] text-white/80">
              <Icon name="sparkles" size={13} className="text-primary" /> Analyzing your plate…
            </div>
          )}
        </div>

        <AnimatePresence>
          {stage === 'done' && (
            <motion.div
              initial={{ opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              transition={spring.page}
              className="mt-5 pb-4"
            >
              <div className="flex items-center gap-1.5 text-[12px] font-semibold text-success mb-2">
                <Icon name="check" size={14} /> Detected on your plate
              </div>
              <ReviewItems items={items} onChange={setItems} />
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </Sheet>
  )
}
