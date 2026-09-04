import { useEffect, useRef, useState } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { Sheet } from '../../components/Sheet.tsx'
import { Button } from '../../components/Button.tsx'
import { Icon } from '../../components/Icon.tsx'
import { ReviewItems, reviewTotals, type ReviewItem } from '../../components/ReviewItems.tsx'
import { useToast } from '../../components/Toast.tsx'
import { haptic, spring } from '../../lib/ios.ts'
import { useStore } from '../../store.tsx'
import { scanMeal } from '../../api/ai.ts'
import { compressImage, uploadMealPhoto } from '../../api/photos.ts'
import type { NewEntryInput } from '../../api/meals.ts'

type Stage = 'pick' | 'scanning' | 'review'

let seq = 0

// Meal name from item names, capped like the backend's 60-char limit.
function mealNameFrom(items: ReviewItem[]): string {
  const joined = items.map((i) => i.name).join(', ')
  return joined.length > 57 ? `${joined.slice(0, 57)}…` : joined || 'Photo meal'
}

export function PhotoMealSheet({
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
  const fileRef = useRef<HTMLInputElement>(null)
  const [stage, setStage] = useState<Stage>('pick')
  const [preview, setPreview] = useState<string | null>(null)
  const [photoPath, setPhotoPath] = useState<string | null>(null)
  const [items, setItems] = useState<ReviewItem[]>([])
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (open) {
      setStage('pick')
      setItems([])
      setPhotoPath(null)
      setPreview((old) => {
        if (old) URL.revokeObjectURL(old)
        return null
      })
    }
  }, [open])

  const scan = async (file: File) => {
    setStage('scanning')
    setPreview(URL.createObjectURL(file))
    try {
      const blob = await compressImage(file)
      const path = await uploadMealPhoto(blob)
      setPhotoPath(path)
      const result = await scanMeal([path])
      setItems(
        result.items.map((item) => ({
          id: `pm-${++seq}`,
          name: item.prediction.name,
          qty: `${Math.round(item.macros.grams)}g`,
          kcal: Math.round(item.macros.kcal),
          p: Math.round(item.macros.protein),
          f: Math.round(item.macros.fat),
          c: Math.round(item.macros.carbs),
        })),
      )
      setStage('review')
      haptic('success')
    } catch (err) {
      toast.show(err instanceof Error ? err.message : 'Scan failed')
      setStage('pick')
    }
  }

  const totals = reviewTotals(items)

  const addAll = async () => {
    haptic('medium')
    setSaving(true)
    try {
      const inputs: NewEntryInput[] = items.map((item, index) => ({
        name: item.name,
        protein: item.p,
        calories: item.kcal,
        fat: item.f,
        carbs: item.c,
        quantity: item.qty || null,
        source: 'photo_ai',
        photo_path: index === 0 ? photoPath : null, // photo attached to the first entry
      }))
      await store.createMeal(mealNameFrom(items), inputs, logDate)
      onClose()
      toast.show(`Added · ${totals.calories} kcal`)
    } catch {
      // toasted by the store
    } finally {
      setSaving(false)
    }
  }

  const footer =
    stage === 'review' ? (
      <Button variant="primary" full glow disabled={items.length === 0 || saving} onClick={() => void addAll()}>
        {saving
          ? 'Adding…'
          : items.length > 0
            ? `Add ${items.length} ${items.length === 1 ? 'item' : 'items'} · ${totals.calories} kcal`
            : 'Nothing to add'}
      </Button>
    ) : (
      <Button variant="primary" full glow disabled={stage === 'scanning'} onClick={() => fileRef.current?.click()}>
        <Icon name="camera" size={16} /> {stage === 'scanning' ? 'Analyzing…' : 'Take photo'}
      </Button>
    )

  return (
    <Sheet open={open} onClose={onClose} title="Photo meal" detents={['large']} footer={footer}>
      <input
        ref={fileRef}
        type="file"
        accept="image/*"
        capture="environment"
        className="hidden"
        onChange={(e) => {
          const file = e.target.files?.[0]
          if (file) void scan(file)
          e.target.value = ''
        }}
      />

      <div className="pt-2">
        {/* Photo preview / placeholder */}
        <div className="relative h-[300px] rounded-hero overflow-hidden bg-black">
          {preview ? (
            <img src={preview} alt="Your plate" className="absolute inset-0 h-full w-full object-cover" />
          ) : (
            <div className="absolute inset-0 bg-[#0A0F1E]" />
          )}

          <div className="absolute inset-0 flex items-center justify-center">
            {stage === 'scanning' ? (
              <div className="flex flex-col items-center gap-3 rounded-2xl bg-black/50 backdrop-blur-sm px-6 py-5">
                <div className="size-6 rounded-full border-2 border-primary/40 border-t-primary animate-spin" />
                <span className="text-[13px] text-white/85 flex items-center gap-1.5">
                  <Icon name="sparkles" size={13} className="text-primary" /> Analyzing your plate…
                </span>
              </div>
            ) : stage === 'pick' ? (
              <div className="size-48 rounded-full border-2 border-dashed border-white/25 flex items-center justify-center">
                <Icon name="fork" size={40} className="text-white/15" />
              </div>
            ) : null}
          </div>

          {stage === 'review' && (
            <div className="absolute top-3 left-3 flex items-center gap-1.5 rounded-full bg-black/50 px-2 py-1 backdrop-blur-sm">
              <span className="size-1.5 rounded-full bg-success" />
              <span className="text-[10px] font-semibold uppercase tracking-wide text-white/85">Analyzed</span>
            </div>
          )}
        </div>

        <AnimatePresence>
          {stage === 'review' && (
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
