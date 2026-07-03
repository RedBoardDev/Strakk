import { useState, type ReactNode } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import { ChevronLeft } from 'lucide-react'
import { Button } from '../../components/Button.tsx'
import { Stepper } from '../../components/Stepper.tsx'
import { MacroGrid } from '../../components/MacroCard.tsx'
import { ProgressBar } from '../../components/ProgressBar.tsx'
import { Icon, type IconName } from '../../components/Icon.tsx'
import { haptic, spring } from '../../lib/ios.ts'
import { colors } from '../../theme/tokens.ts'

// ---- shared bits --------------------------------------------------------
type Sex = 'male' | 'female' | 'na'
type Goal = 'lose' | 'build' | 'maintain' | 'track'
type Intensity = 'light' | 'moderate' | 'intense'
type Activity = 'sedentary' | 'moderate' | 'active'

type Data = {
  weight: number
  height: number
  sex: Sex | null
  goal: Goal | null
  sessions: number
  trainingTypes: string[]
  intensity: Intensity | null
  activity: Activity | null
  email: string
  password: string
  goals: { calories: number; protein: number; fat: number; carbs: number; water: number }
}

const INITIAL: Data = {
  weight: 75,
  height: 175,
  sex: null,
  goal: null,
  sessions: 3,
  trainingTypes: [],
  intensity: null,
  activity: null,
  email: '',
  password: '',
  goals: { calories: 2200, protein: 160, fat: 70, carbs: 240, water: 2500 },
}

const TRAINING_TYPES = ['Strength', 'Cardio', 'Team sport', 'Yoga / Mobility', 'Other']

function SelectCard({
  selected,
  title,
  subtitle,
  icon,
  onClick,
}: {
  selected: boolean
  title: string
  subtitle?: string
  icon?: IconName
  onClick: () => void
}) {
  return (
    <motion.button
      type="button"
      whileTap={{ scale: 0.98 }}
      onClick={() => {
        haptic('light')
        onClick()
      }}
      className={`w-full rounded-card px-4 py-3.5 flex items-center gap-3 text-left border ${
        selected ? 'bg-primary-faint border-primary/50' : 'bg-surface-1 border-transparent'
      }`}
    >
      {icon && (
        <div className={`size-9 rounded-[10px] flex items-center justify-center shrink-0 ${selected ? 'bg-primary/20' : 'bg-surface-3'}`}>
          <Icon name={icon} size={18} className={selected ? 'text-primary' : 'text-ink-2'} />
        </div>
      )}
      <div className="flex-1 min-w-0">
        <div className={`text-[15px] font-semibold ${selected ? 'text-ink' : 'text-ink'}`}>{title}</div>
        {subtitle && <div className="text-[12px] text-ink-3">{subtitle}</div>}
      </div>
      {selected && <Icon name="check" size={18} className="text-primary shrink-0" />}
    </motion.button>
  )
}

function Pill({ selected, label, onClick }: { selected: boolean; label: string; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={() => {
        haptic('light')
        onClick()
      }}
      className={`px-4 h-10 rounded-full text-[14px] font-medium border ${
        selected ? 'bg-primary text-white border-primary' : 'bg-surface-1 text-ink-2 border-divider/50'
      }`}
    >
      {label}
    </button>
  )
}

// Step scaffold: title + subtitle + body + sticky Continue.
function Step({
  title,
  subtitle,
  children,
  cta = 'Continue',
  ctaDisabled = false,
  onNext,
}: {
  title: string
  subtitle?: string
  children: ReactNode
  cta?: string
  ctaDisabled?: boolean
  onNext: () => void
}) {
  return (
    <div className="flex flex-col h-full">
      <div className="scroll-y no-scrollbar flex-1 min-h-0 px-6 pt-4 pb-6">
        <h1 className="text-[26px] font-bold tracking-tight text-ink leading-tight">{title}</h1>
        {subtitle && <p className="mt-2 text-[15px] leading-snug text-ink-2">{subtitle}</p>}
        <div className="mt-7">{children}</div>
      </div>
      <div className="shrink-0 px-6 pt-3 pb-safe-tight border-t border-divider/50 bg-bg">
        <Button variant="primary" full glow disabled={ctaDisabled} onClick={onNext}>
          {cta}
        </Button>
      </div>
    </div>
  )
}

// ---- flow ---------------------------------------------------------------
export function OnboardingFlow({ onComplete, onBackToLogin }: { onComplete: () => void; onBackToLogin: () => void }) {
  const [step, setStep] = useState(0)
  const [data, setData] = useState<Data>(INITIAL)
  const set = <K extends keyof Data>(key: K, value: Data[K]) => setData((prev) => ({ ...prev, [key]: value }))

  const next = () => {
    haptic('light')
    setStep((current) => current + 1)
  }
  const back = () => {
    haptic('light')
    if (step === 0) onBackToLogin()
    else setStep((current) => current - 1)
  }

  const toggleTraining = (type: string) =>
    set(
      'trainingTypes',
      data.trainingTypes.includes(type) ? data.trainingTypes.filter((item) => item !== type) : [...data.trainingTypes, type],
    )

  // Progress bar spans the data-collection steps (Weight..SignUp = 1..6).
  const TOTAL = 9
  const showProgress = step >= 1 && step <= 6
  const progress = (step + 1) / TOTAL

  const renderStep = () => {
    switch (step) {
      case 0:
        return (
          <div className="flex flex-col h-full px-6 pt-4 pb-safe-tight">
            <div className="flex-1 flex flex-col items-center justify-center text-center">
              <img src="/icons/icon-512.png" alt="Strakk" className="size-20 rounded-[22px] ring-1 ring-white/10 shadow-[0_14px_34px_-8px_rgba(0,0,0,0.7)]" />
              <h1 className="mt-7 text-[32px] font-bold tracking-tight text-ink">Strakk</h1>
              <p className="mt-3 text-[16px] leading-snug text-ink-2 max-w-[300px]">
                Dial in your nutrition and track weekly progress — built to feel effortless.
              </p>
            </div>
            <div className="flex flex-col gap-3 pt-4">
              <Button variant="primary" full glow onClick={next}>Get started</Button>
              <button type="button" onClick={onBackToLogin} className="text-center text-[14px] text-ink-2 py-1">
                Already have an account? <span className="font-semibold text-primary">Sign in</span>
              </button>
            </div>
          </div>
        )
      case 1:
        return (
          <Step title="What’s your current weight?" subtitle="We use it to calibrate your targets." onNext={next}>
            <div className="flex flex-col items-center gap-6 pt-6">
              <div className="text-[56px] font-bold text-ink tnum leading-none">
                {data.weight}
                <span className="text-[20px] text-ink-3 font-semibold ml-1">kg</span>
              </div>
              <Stepper value={data.weight} onChange={(value) => set('weight', value)} step={1} min={30} suffix="kg" />
            </div>
          </Step>
        )
      case 2:
        return (
          <Step title="A bit about you" subtitle="Optional — improves accuracy. Skip if you’d rather not." cta={data.sex ? 'Continue' : 'Skip'} onNext={next}>
            <div className="flex flex-col gap-6">
              <div>
                <div className="text-[13px] font-semibold text-ink-3 mb-2">Height</div>
                <div className="bg-surface-1 rounded-card px-4 py-3 flex items-center gap-3">
                  <span className="flex-1 text-[18px] font-bold text-ink tnum">{data.height} <span className="text-[13px] text-ink-3 font-semibold">cm</span></span>
                  <Stepper value={data.height} onChange={(value) => set('height', value)} step={1} min={100} suffix="cm" />
                </div>
              </div>
              <div>
                <div className="text-[13px] font-semibold text-ink-3 mb-2">Biological sex</div>
                <div className="flex gap-2 flex-wrap">
                  <Pill selected={data.sex === 'male'} label="Male" onClick={() => set('sex', data.sex === 'male' ? null : 'male')} />
                  <Pill selected={data.sex === 'female'} label="Female" onClick={() => set('sex', data.sex === 'female' ? null : 'female')} />
                  <Pill selected={data.sex === 'na'} label="Prefer not to say" onClick={() => set('sex', data.sex === 'na' ? null : 'na')} />
                </div>
              </div>
            </div>
          </Step>
        )
      case 3:
        return (
          <Step title="What’s your goal?" ctaDisabled={!data.goal} onNext={next}>
            <div className="flex flex-col gap-2.5">
              <SelectCard selected={data.goal === 'lose'} title="Lose fat" subtitle="Sustainable deficit" icon="flame.fill" onClick={() => set('goal', 'lose')} />
              <SelectCard selected={data.goal === 'build'} title="Build muscle" subtitle="Lean surplus" icon="dumbbell.fill" onClick={() => set('goal', 'build')} />
              <SelectCard selected={data.goal === 'maintain'} title="Maintain" subtitle="Stay where you are" icon="scale" onClick={() => set('goal', 'maintain')} />
              <SelectCard selected={data.goal === 'track'} title="Just track" subtitle="Log without a target" icon="chart.bar" onClick={() => set('goal', 'track')} />
            </div>
          </Step>
        )
      case 4:
        return (
          <Step title="How do you train?" subtitle="Sessions per week and the kind of training you do." onNext={next}>
            <div className="flex flex-col gap-6">
              <div>
                <div className="text-[13px] font-semibold text-ink-3 mb-2">Sessions / week</div>
                <div className="flex gap-2 flex-wrap">
                  {[0, 1, 2, 3, 4, 5, 6, 7].map((n) => (
                    <Pill key={n} selected={data.sessions === n} label={`${n}`} onClick={() => set('sessions', n)} />
                  ))}
                </div>
              </div>
              <div>
                <div className="text-[13px] font-semibold text-ink-3 mb-2">Training types</div>
                <div className="flex gap-2 flex-wrap">
                  {TRAINING_TYPES.map((type) => (
                    <Pill key={type} selected={data.trainingTypes.includes(type)} label={type} onClick={() => toggleTraining(type)} />
                  ))}
                </div>
              </div>
            </div>
          </Step>
        )
      case 5:
        return (
          <Step title="Daily activity" subtitle="Outside of training." ctaDisabled={!data.intensity || !data.activity} onNext={next}>
            <div className="flex flex-col gap-6">
              <div>
                <div className="text-[13px] font-semibold text-ink-3 mb-2">Training intensity</div>
                <div className="flex gap-2 flex-wrap">
                  <Pill selected={data.intensity === 'light'} label="Light" onClick={() => set('intensity', 'light')} />
                  <Pill selected={data.intensity === 'moderate'} label="Moderate" onClick={() => set('intensity', 'moderate')} />
                  <Pill selected={data.intensity === 'intense'} label="Intense" onClick={() => set('intensity', 'intense')} />
                </div>
              </div>
              <div>
                <div className="text-[13px] font-semibold text-ink-3 mb-2">Daily activity level</div>
                <div className="flex flex-col gap-2.5">
                  <SelectCard selected={data.activity === 'sedentary'} title="Sedentary" subtitle="Desk job, little movement" onClick={() => set('activity', 'sedentary')} />
                  <SelectCard selected={data.activity === 'moderate'} title="Moderately active" subtitle="On your feet part of the day" onClick={() => set('activity', 'moderate')} />
                  <SelectCard selected={data.activity === 'active'} title="Very active" subtitle="Physical job, always moving" onClick={() => set('activity', 'active')} />
                </div>
              </div>
            </div>
          </Step>
        )
      case 6:
        return (
          <Step
            title="Create your account"
            subtitle="So your data syncs across devices."
            cta="Create account"
            ctaDisabled={data.email.trim() === '' || data.password.length < 6}
            onNext={next}
          >
            <div className="flex flex-col gap-3">
              <input
                type="email"
                value={data.email}
                onChange={(event) => set('email', event.target.value)}
                placeholder="you@example.com"
                autoCapitalize="none"
                autoCorrect="off"
                className="bg-surface-1 rounded-card px-4 h-[54px] text-[16px] text-ink placeholder:text-ink-4 outline-none"
              />
              <input
                type="password"
                value={data.password}
                onChange={(event) => set('password', event.target.value)}
                placeholder="Password (min. 6 characters)"
                className="bg-surface-1 rounded-card px-4 h-[54px] text-[16px] text-ink placeholder:text-ink-4 outline-none"
              />
            </div>
          </Step>
        )
      case 7:
        return (
          <Step title="Your nutrition targets" subtitle="Generated from your profile — tweak anything." cta="Looks good" onNext={next}>
            <div className="flex flex-col gap-2.5">
              <div className="bg-primary-faint border border-primary/25 rounded-card px-4 py-3 flex items-center gap-3">
                <Icon name="sparkles" size={18} className="text-primary shrink-0" />
                <span className="text-[13px] text-ink">Calculated with AI from your goal, weight & activity.</span>
              </div>
              {([
                { key: 'calories', label: 'Calories', step: 50, suffix: 'kcal' },
                { key: 'protein', label: 'Protein', step: 5, suffix: 'g' },
                { key: 'fat', label: 'Fat', step: 5, suffix: 'g' },
                { key: 'carbs', label: 'Carbs', step: 5, suffix: 'g' },
                { key: 'water', label: 'Water', step: 100, suffix: 'mL' },
              ] as const).map((field) => (
                <div key={field.key} className="bg-surface-1 rounded-card px-4 py-3 flex items-center gap-3">
                  <span className="flex-1 text-[15px] text-ink">{field.label}</span>
                  <Stepper
                    value={data.goals[field.key]}
                    onChange={(value) => set('goals', { ...data.goals, [field.key]: value })}
                    step={field.step}
                    min={0}
                    suffix={field.suffix}
                  />
                </div>
              ))}
            </div>
          </Step>
        )
      case 8:
      default:
        return (
          <Step title="Here’s your day" subtitle="This is what a fresh day looks like against your targets." cta="Start tracking" onNext={onComplete}>
            <MacroGrid
              protein={{ consumed: 0, goal: data.goals.protein }}
              calories={{ consumed: 0, goal: data.goals.calories }}
              fat={{ consumed: 0, goal: data.goals.fat }}
              carbs={{ consumed: 0, goal: data.goals.carbs }}
            />
            <div className="mt-3 bg-surface-1 rounded-card px-4 py-3 flex items-center gap-3">
              <div className="size-9 rounded-lg bg-surface-3 flex items-center justify-center shrink-0">
                <Icon name="drop.fill" size={15} className="text-water" />
              </div>
              <div className="flex-1">
                <div className="text-[15px] font-bold text-ink tnum">0.0 L <span className="text-ink-3 font-semibold">/ {(data.goals.water / 1000).toFixed(1)} L</span></div>
                <div className="pt-2"><ProgressBar value={0} color={colors.water} /></div>
              </div>
            </div>
          </Step>
        )
    }
  }

  return (
    <div className="relative h-full w-full overflow-hidden bg-bg">
      {/* Top bar: back + progress */}
      <div className="pt-safe">
        <div className="h-12 px-3 flex items-center gap-3">
          <button type="button" onClick={back} aria-label="Back" className="size-9 flex items-center justify-center text-ink">
            <ChevronLeft size={24} />
          </button>
          {showProgress && (
            <div className="flex-1 pr-3">
              <ProgressBar value={progress} color={colors.primary} />
            </div>
          )}
        </div>
      </div>
      {/* Steps fill the rest */}
      <div className="absolute inset-x-0 bottom-0" style={{ top: 'calc(var(--safe-top, env(safe-area-inset-top)) + 48px)' }}>
        <AnimatePresence mode="wait" initial={false}>
          <motion.div
            key={step}
            initial={{ opacity: 0, x: 24 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -24 }}
            transition={spring.page}
            className="h-full"
          >
            {renderStep()}
          </motion.div>
        </AnimatePresence>
      </div>
    </div>
  )
}
