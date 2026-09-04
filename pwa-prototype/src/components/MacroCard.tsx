import { Icon, type IconName } from './Icon.tsx'
import { ProgressBar } from './ProgressBar.tsx'
import { AnimatedNumber } from './AnimatedNumber.tsx'
import { macroColor, type Macro } from '../theme/tokens.ts'

const META: Record<Macro, { icon: IconName; label: string; unit: string }> = {
  protein: { icon: 'dumbbell.fill', label: 'Protein', unit: 'g' },
  calories: { icon: 'flame.fill', label: 'Calories', unit: 'kcal' },
  fat: { icon: 'drop.fill', label: 'Fat', unit: 'g' },
  carbs: { icon: 'leaf.fill', label: 'Carbs', unit: 'g' },
}

// DESIGN.md §5 Macro card: icon tile (28x28 tinted) + label + big value + bar.
export function MacroCard({ macro, consumed, goal }: { macro: Macro; consumed: number; goal?: number }) {
  const { icon, label, unit } = META[macro]
  const color = macroColor[macro]
  const hasGoal = !!goal && goal > 0
  const reached = hasGoal && consumed >= goal

  return (
    <div className="bg-surface-1 rounded-card p-4 flex flex-col gap-1.5">
      <div className="flex items-center gap-2">
        <div
          className="size-7 rounded-[8px] flex items-center justify-center shrink-0"
          style={{ backgroundColor: `${color}2E`, boxShadow: `inset 0 0 0 1px ${color}33` }}
        >
          <Icon name={icon} size={13} color={color} />
        </div>
        <span className="text-[13px] text-ink-2 truncate">{label}</span>
      </div>

      <div className="flex items-baseline gap-1">
        <AnimatedNumber value={consumed} className="text-[22px] font-bold text-ink leading-none" />
        <span className="text-[12px] text-ink-3">{hasGoal ? `/ ${goal}${unit}` : unit}</span>
      </div>

      {/* No goal (e.g. a meal's macro totals) → no empty track */}
      {hasGoal && <ProgressBar value={consumed / goal} color={color} reached={reached} />}
    </div>
  )
}

export function MacroGrid({
  protein,
  calories,
  fat,
  carbs,
}: {
  protein: { consumed: number; goal?: number }
  calories: { consumed: number; goal?: number }
  fat: { consumed: number; goal?: number }
  carbs: { consumed: number; goal?: number }
}) {
  return (
    <div className="grid grid-cols-2 gap-3">
      <MacroCard macro="protein" {...protein} />
      <MacroCard macro="calories" {...calories} />
      <MacroCard macro="fat" {...fat} />
      <MacroCard macro="carbs" {...carbs} />
    </div>
  )
}
