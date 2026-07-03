import { useEffect, useState } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { Sheet } from '../../components/Sheet.tsx'
import { Button } from '../../components/Button.tsx'
import { Icon } from '../../components/Icon.tsx'
import { ReviewItems, reviewTotals, type ReviewItem } from '../../components/ReviewItems.tsx'
import { useToast } from '../../components/Toast.tsx'
import { haptic, spring } from '../../lib/ios.ts'
import { useStore } from '../../store.tsx'

// AI text-entry add (mock): type a meal in plain language → "AI" grounds it into
// items. The result is fully reviewable: fix macros, adjust quantities, remove
// or add items before saving.
const GROUNDED: ReviewItem[] = [
  { id: 'q1', name: 'Scrambled eggs', qty: '2 large', kcal: 180, p: 13, f: 13, c: 1 },
  { id: 'q2', name: 'Sourdough toast', qty: '2 slices', kcal: 160, p: 6, f: 2, c: 30 },
  { id: 'q3', name: 'Avocado', qty: '½', kcal: 120, p: 1, f: 11, c: 6 },
]

export function QuickAddSheet({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { dispatch } = useStore()
  const toast = useToast()
  const [text, setText] = useState('')
  const [analyzed, setAnalyzed] = useState(false)
  const [items, setItems] = useState<ReviewItem[]>([])

  useEffect(() => {
    if (open) {
      setText('')
      setAnalyzed(false)
      setItems([])
    }
  }, [open])

  const totals = reviewTotals(items)

  const addAll = () => {
    haptic('medium')
    dispatch({
      kind: 'meal/add',
      meal: {
        title: items.map((item) => item.name).join(', '),
        macros: totals,
        items: items.map((item) => (item.qty ? `${item.name} — ${item.qty}` : item.name)),
        source: 'ai',
      },
    })
    onClose()
    toast.show(`Added · ${totals.calories} kcal`)
  }

  const footer = analyzed ? (
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
      disabled={!text.trim()}
      onClick={() => {
        haptic('medium')
        setItems(GROUNDED.map((item) => ({ ...item })))
        setAnalyzed(true)
      }}
    >
      <Icon name="sparkles" size={16} /> Analyze with AI
    </Button>
  )

  return (
    <Sheet open={open} onClose={onClose} title="Quick add" detents={['large']} footer={footer}>
      <div className="pt-2">
        <textarea
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="Describe your meal — e.g. 2 scrambled eggs, 2 slices of toast, half an avocado"
          rows={3}
          className="w-full resize-none rounded-card bg-surface-1 border border-hair px-4 py-3 text-[16px] text-ink
            placeholder:text-ink-4 focus:border-primary focus:outline-none"
        />
        <div className="mt-2 flex items-center gap-1.5 text-[12px] text-ink-3">
          <Icon name="sparkles" size={13} className="text-primary" />
          AI reads your text and grounds it into tracked items.
        </div>

        <AnimatePresence>
          {analyzed && (
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={spring.page}
              className="mt-5 pb-4"
            >
              <div className="text-[11px] font-bold uppercase tracking-wider text-ink-3 mb-2">Detected items</div>
              <ReviewItems items={items} onChange={setItems} />
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </Sheet>
  )
}
