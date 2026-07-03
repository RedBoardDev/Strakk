import { useEffect, useState } from 'react'
import { Sheet } from '../../components/Sheet.tsx'
import { Button } from '../../components/Button.tsx'
import { SectionLabel } from '../../components/SectionLabel.tsx'
import { useToast } from '../../components/Toast.tsx'
import { haptic } from '../../lib/ios.ts'
import { sanitizeDecimal } from '../../lib/numbers.ts'
import { useStore } from '../../store.tsx'

// Type a food's macros by hand — mirrors the native ManualEntryView. Mock:
// "Add" just closes; the real app would submit the entry.
type NumField = { key: 'calories' | 'protein' | 'fat' | 'carbs'; label: string; unit: string; max: number; required?: boolean }

const NUM_FIELDS: NumField[] = [
  { key: 'calories', label: 'Calories', unit: 'kcal', max: 5000, required: true },
  { key: 'protein', label: 'Protein', unit: 'g', max: 500, required: true },
  { key: 'fat', label: 'Fat', unit: 'g', max: 500 },
  { key: 'carbs', label: 'Carbs', unit: 'g', max: 500 },
]

function NumRow({
  field,
  value,
  onChange,
  last,
}: {
  field: NumField
  value: string
  onChange: (value: string) => void
  last: boolean
}) {
  return (
    <div className="relative flex items-center gap-3 px-4 min-h-[54px]">
      <span className="flex-1 text-[15px] text-ink">
        {field.label}
        {field.required && <span className="text-primary"> *</span>}
      </span>
      <input
        value={value}
        onChange={(event) => onChange(sanitizeDecimal(event.target.value, field.max))}
        inputMode="decimal"
        placeholder="0"
        aria-label={field.label}
        className="w-20 bg-transparent text-right text-[16px] font-semibold text-ink placeholder:text-ink-4 outline-none tnum"
      />
      <span className="w-9 text-[13px] text-ink-3">{field.unit}</span>
      {!last && <span className="pointer-events-none absolute bottom-0 right-0 left-4 h-px bg-divider/60" />}
    </div>
  )
}

export function ManualEntrySheet({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { dispatch } = useStore()
  const toast = useToast()
  const [name, setName] = useState('')
  const [quantity, setQuantity] = useState('')
  const [nums, setNums] = useState<Record<NumField['key'], string>>({
    calories: '',
    protein: '',
    fat: '',
    carbs: '',
  })

  useEffect(() => {
    if (open) {
      setName('')
      setQuantity('')
      setNums({ calories: '', protein: '', fat: '', carbs: '' })
    }
  }, [open])

  const valid = name.trim().length > 0 && nums.calories !== '' && nums.protein !== ''

  return (
    <Sheet
      open={open}
      onClose={onClose}
      title="Manual entry"
      detents={['large']}
      footer={
        <Button
          variant="primary"
          full
          glow
          disabled={!valid}
          onClick={() => {
            haptic('medium')
            const macros = {
              calories: Math.round(parseFloat(nums.calories) || 0),
              protein: Math.round(parseFloat(nums.protein) || 0),
              fat: Math.round(parseFloat(nums.fat) || 0),
              carbs: Math.round(parseFloat(nums.carbs) || 0),
            }
            dispatch({
              kind: 'meal/add',
              meal: {
                title: name.trim(),
                macros,
                items: [quantity.trim() ? `${name.trim()} — ${quantity.trim()}` : name.trim()],
                source: 'manual',
              },
            })
            onClose()
            toast.show(`Added · ${macros.calories} kcal`)
          }}
        >
          Add to log
        </Button>
      }
    >
      <div className="pb-2">
        <SectionLabel>Food</SectionLabel>
        <div className="bg-surface-1 rounded-card overflow-hidden">
          <div className="relative flex items-center gap-3 px-4 min-h-[54px]">
            <span className="text-[15px] text-ink shrink-0">
              Name<span className="text-primary"> *</span>
            </span>
            <input
              value={name}
              onChange={(event) => setName(event.target.value.slice(0, 100))}
              placeholder="e.g. Homemade granola"
              aria-label="Food name"
              className="flex-1 bg-transparent text-right text-[16px] text-ink placeholder:text-ink-4 outline-none"
            />
            <span className="pointer-events-none absolute bottom-0 right-0 left-4 h-px bg-divider/60" />
          </div>
          <div className="flex items-center gap-3 px-4 min-h-[54px]">
            <span className="text-[15px] text-ink shrink-0">Quantity</span>
            <input
              value={quantity}
              onChange={(event) => setQuantity(event.target.value.slice(0, 50))}
              placeholder="e.g. 150 g, 1 bowl"
              aria-label="Quantity"
              className="flex-1 bg-transparent text-right text-[16px] text-ink placeholder:text-ink-4 outline-none"
            />
          </div>
        </div>

        <SectionLabel>Macros</SectionLabel>
        <div className="bg-surface-1 rounded-card overflow-hidden">
          {NUM_FIELDS.map((field, i) => (
            <NumRow
              key={field.key}
              field={field}
              value={nums[field.key]}
              onChange={(value) => setNums((prev) => ({ ...prev, [field.key]: value }))}
              last={i === NUM_FIELDS.length - 1}
            />
          ))}
        </div>
        <div className="px-1 pt-2 text-[12px] text-ink-4">
          <span className="text-primary">*</span> required · protein & calories at minimum.
        </div>
      </div>
    </Sheet>
  )
}
