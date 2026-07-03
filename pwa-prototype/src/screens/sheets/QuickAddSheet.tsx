import { useEffect, useState } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { Sheet } from '../../components/Sheet.tsx'
import { Button } from '../../components/Button.tsx'
import { Icon } from '../../components/Icon.tsx'
import { ReviewItems, reviewTotals, type ReviewItem } from '../../components/ReviewItems.tsx'
import { useToast } from '../../components/Toast.tsx'
import { haptic, spring } from '../../lib/ios.ts'
import { useStore } from '../../store.tsx'
import { analyzeText } from '../../api/ai.ts'
import type { AnalyzedEntry, BreakdownItem } from '../../api/edge.ts'

// AI text-entry add: describe a meal in plain language → the backend grounds it
// into items. The result is fully reviewable (fix macros, adjust quantities,
// remove or add items) before saving as ONE entry with a breakdown.
let seq = 0

function toReviewItems(entry: AnalyzedEntry): ReviewItem[] {
  const source: BreakdownItem[] = entry.breakdown?.length ? entry.breakdown : [entry]
  return source.map((item) => ({
    id: `qa-${++seq}`,
    name: item.name,
    qty: item.quantity ?? '',
    kcal: Math.round(item.calories_kcal),
    p: Math.round(item.protein_g),
    f: Math.round(item.fat_g ?? 0),
    c: Math.round(item.carbs_g ?? 0),
  }))
}

export function QuickAddSheet({
  open,
  onClose,
  logDate,
}: {
  open: boolean
  onClose: () => void
  logDate?: string
}) {
  const store = useStore()
  const toast = useToast()
  const [text, setText] = useState('')
  const [analyzing, setAnalyzing] = useState(false)
  const [aiName, setAiName] = useState<string | null>(null)
  const [items, setItems] = useState<ReviewItem[]>([])
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (open) {
      setText('')
      setAnalyzing(false)
      setAiName(null)
      setItems([])
    }
  }, [open])

  const totals = reviewTotals(items)
  const analyzed = aiName !== null

  const analyze = async () => {
    haptic('medium')
    setAnalyzing(true)
    try {
      const entry = await analyzeText(text.trim().slice(0, 500))
      setAiName(entry.name)
      setItems(toReviewItems(entry))
    } catch (err) {
      toast.show(err instanceof Error ? err.message : 'Analysis failed')
    } finally {
      setAnalyzing(false)
    }
  }

  const addAll = async () => {
    haptic('medium')
    setSaving(true)
    try {
      const breakdown: BreakdownItem[] = items.map((item) => ({
        name: item.name,
        protein_g: item.p,
        calories_kcal: item.kcal,
        fat_g: item.f,
        carbs_g: item.c,
        quantity: item.qty || null,
      }))
      await store.addOrphanEntry(
        {
          name: aiName ?? items.map((i) => i.name).join(', ').slice(0, 100),
          protein: totals.protein,
          calories: totals.calories,
          fat: totals.fat,
          carbs: totals.carbs,
          quantity: null,
          source: 'text_ai',
          breakdown: breakdown.length > 1 ? breakdown : null,
        },
        logDate,
      )
      onClose()
      toast.show(`Added · ${totals.calories} kcal`)
    } catch {
      // toasted by the store
    } finally {
      setSaving(false)
    }
  }

  const footer = analyzed ? (
    <Button variant="primary" full glow disabled={items.length === 0 || saving} onClick={() => void addAll()}>
      {saving
        ? 'Adding…'
        : items.length > 0
          ? `Add ${items.length} ${items.length === 1 ? 'item' : 'items'} · ${totals.calories} kcal`
          : 'Nothing to add'}
    </Button>
  ) : (
    <Button variant="primary" full glow disabled={!text.trim() || analyzing} onClick={() => void analyze()}>
      <Icon name="sparkles" size={16} /> {analyzing ? 'Analyzing…' : 'Analyze with AI'}
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
          maxLength={500}
          className="w-full resize-none rounded-card bg-surface-1 border border-hair px-4 py-3 text-[16px] text-ink
            placeholder:text-ink-4 focus:border-primary focus:outline-none"
        />
        <div className="mt-2 flex items-center gap-1.5 text-[12px] text-ink-3">
          <Icon name="sparkles" size={13} className="text-primary" />
          AI reads your text and grounds it into tracked items.
        </div>

        {analyzing && (
          <div className="mt-8 flex flex-col items-center gap-3">
            <div className="size-6 rounded-full border-2 border-primary/30 border-t-primary animate-spin" />
            <span className="text-[13px] text-ink-2">Grounding your meal…</span>
          </div>
        )}

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
