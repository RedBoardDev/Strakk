import { useEffect, useState } from 'react'
import { motion } from 'motion/react'
import { ScreenScroll } from '../components/ScreenScroll.tsx'
import { RowGroup, Row } from '../components/Row.tsx'
import { SectionLabel } from '../components/SectionLabel.tsx'
import { Icon, type IconName } from '../components/Icon.tsx'
import { ConfirmDialog } from '../components/ConfirmDialog.tsx'
import { Sheet } from '../components/Sheet.tsx'
import { Button } from '../components/Button.tsx'
import { PushPage } from '../components/PushPage.tsx'
import { Toggle } from '../components/Toggle.tsx'
import { useToast } from '../components/Toast.tsx'
import { GoalsEditSheet } from './sheets/GoalsEditSheet.tsx'
import { haptic } from '../lib/ios.ts'
import { useNav } from '../nav.ts'
import { useStore } from '../store.tsx'

const EMAIL = 'ott.thomas68@gmail.com'

type GoalRow = { label: string; icon: IconName; color: string }

// Glyphs match MacroCard's macro icons exactly (one glyph per concept app-wide).
const GOAL_META: GoalRow[] = [
  { label: 'Calories', icon: 'flame.fill', color: '#FF9A55' },
  { label: 'Protein', icon: 'dumbbell.fill', color: '#34C7B5' },
  { label: 'Fat', icon: 'drop.fill', color: '#FFC84D' },
  { label: 'Carbs', icon: 'leaf.fill', color: '#637CFF' },
  { label: 'Water', icon: 'drops', color: '#4B8DFF' },
]

function SignOutRow({ onClick }: { onClick: () => void }) {
  return (
    <RowGroup>
      <motion.button
        type="button"
        whileTap={{ backgroundColor: '#151B38' }}
        onClick={() => {
          haptic('medium')
          onClick()
        }}
        className="w-full flex items-center gap-3 px-4 min-h-[52px] text-left"
      >
        <div
          className="size-7 rounded-[7px] flex items-center justify-center shrink-0"
          style={{ backgroundColor: '#E052521F' }}
        >
          <Icon name="logout" size={15} color="#E05252" />
        </div>
        <span className="flex-1 text-[15px] text-error">Sign out</span>
      </motion.button>
    </RowGroup>
  )
}

// Editable personal details (name; email is the account key, read-only).
function ProfileSheet({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { state, dispatch } = useStore()
  const toast = useToast()
  const [name, setName] = useState(state.user.firstName)

  useEffect(() => {
    if (open) setName(state.user.firstName)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  return (
    <Sheet
      open={open}
      onClose={onClose}
      title="Personal details"
      detents={['small']}
      footer={
        <Button
          variant="primary"
          full
          disabled={name.trim() === ''}
          onClick={() => {
            haptic('medium')
            dispatch({ kind: 'user/setName', firstName: name.trim() })
            onClose()
            toast.show('Profile updated')
          }}
        >
          Save
        </Button>
      }
    >
      <div className="flex flex-col gap-3 pt-1 pb-3">
        <div className="bg-surface-1 rounded-card px-4 h-[54px] flex items-center gap-3">
          <span className="text-[13px] font-medium text-ink-3 w-[64px] shrink-0">Name</span>
          <input
            value={name}
            onChange={(event) => setName(event.target.value.slice(0, 40))}
            aria-label="First name"
            className="flex-1 min-w-0 bg-transparent text-[16px] text-ink outline-none"
          />
        </div>
        <div className="bg-surface-1 rounded-card px-4 h-[54px] flex items-center gap-3">
          <span className="text-[13px] font-medium text-ink-3 w-[64px] shrink-0">Email</span>
          <span className="flex-1 min-w-0 truncate text-[15px] text-ink-2 tnum">{EMAIL}</span>
        </div>
      </div>
    </Sheet>
  )
}

// Hevy integration status + connect/disconnect (mock of the native API-key screen).
function HevySheet({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { state, dispatch } = useStore()
  const toast = useToast()
  const connected = state.integrations.hevy

  return (
    <Sheet open={open} onClose={onClose} title="Hevy" detents={['small']}>
      <div className="flex flex-col gap-4 pt-1 pb-4">
        <div className="bg-surface-1 rounded-card px-4 py-3.5 flex items-center gap-3">
          <div className="size-10 rounded-[12px] bg-surface-3 flex items-center justify-center shrink-0">
            <Icon name="dumbbell.fill" size={18} color="#34C7B5" />
          </div>
          <div className="flex-1 min-w-0">
            <div className="text-[15px] font-semibold text-ink">Hevy</div>
            <div className="text-[12px] text-ink-3">Workout sync</div>
          </div>
          <Toggle
            on={connected}
            label="Hevy connection"
            onChange={() => {
              dispatch({ kind: 'integration/toggle', name: 'hevy' })
              toast.show(connected ? 'Hevy disconnected' : 'Hevy connected')
            }}
          />
        </div>
        <p className="px-1 text-[12px] leading-snug text-ink-4">
          Syncs your workout sessions so check-ins include training volume and RPE.
        </p>
      </div>
    </Sheet>
  )
}

const PRIVACY_TEXT = [
  ['Your data', 'Meals, measurements and check-ins are stored in your account and never sold or shared with advertisers.'],
  ['Photos', 'Progress photos are private to your account and encrypted at rest.'],
  ['AI features', 'Meal photos and text sent for AI analysis are processed transiently and not used to train models.'],
  ['Delete anytime', 'You can export or permanently delete your data from this screen at any time.'],
] as const

export function SettingsScreen() {
  const nav = useNav()
  const { state } = useStore()
  const { user, goals, integrations } = state
  const [goalsOpen, setGoalsOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const [hevyOpen, setHevyOpen] = useState(false)
  const [privacyOpen, setPrivacyOpen] = useState(false)
  const [confirmSignOut, setConfirmSignOut] = useState(false)

  const goalValues: Record<string, string> = {
    Calories: `${goals.calories} kcal`,
    Protein: `${goals.protein} g`,
    Fat: `${goals.fat} g`,
    Carbs: `${goals.carbs} g`,
    Water: `${(goals.water / 1000).toFixed(1)} L`,
  }

  return (
    <ScreenScroll title="Settings">
      {/* Account */}
      <SectionLabel>Account</SectionLabel>
      <RowGroup>
        <motion.button
          type="button"
          whileTap={{ backgroundColor: '#151B38' }}
          onClick={() => {
            haptic('light')
            setProfileOpen(true)
          }}
          className="relative w-full flex items-center gap-3 px-4 py-3 text-left"
        >
          <div className="size-11 rounded-full bg-gradient-to-br from-primary to-primary-light flex items-center justify-center shrink-0">
            <span className="text-[18px] font-bold text-white">{user.firstName[0] ?? '?'}</span>
          </div>
          <div className="flex-1 min-w-0">
            <div className="text-[16px] font-semibold text-ink truncate">{user.firstName}</div>
            <div className="text-[12px] text-ink-3 truncate">Personal details</div>
          </div>
          <Icon name="chevron.right" size={16} className="text-ink-3 shrink-0" />
          <span className="rowhair pointer-events-none absolute bottom-0 right-0 left-[68px] h-px bg-divider/60" />
        </motion.button>
        <Row title="Email" trailing={<span className="tnum text-ink-2">{EMAIL}</span>} />
      </RowGroup>

      {/* Daily goals */}
      <div className="flex items-center justify-between pr-1">
        <SectionLabel>Daily goals</SectionLabel>
        <button
          type="button"
          onClick={() => {
            haptic('light')
            setGoalsOpen(true)
          }}
          className="text-[13px] font-semibold text-primary pt-6 pb-2"
        >
          Edit
        </button>
      </div>
      <RowGroup>
        {GOAL_META.map((g) => (
          <Row
            key={g.label}
            icon={g.icon}
            iconColor={g.color}
            title={g.label}
            trailing={<span className="tnum text-ink-2 font-medium">{goalValues[g.label]}</span>}
          />
        ))}
      </RowGroup>

      {/* Integrations */}
      <SectionLabel>Integrations</SectionLabel>
      <RowGroup>
        <Row
          icon="dumbbell.fill"
          title="Hevy"
          subtitle="Workout sync"
          trailing={
            <span className={`text-[13px] font-medium ${integrations.hevy ? 'text-success' : 'text-ink-3'}`}>
              {integrations.hevy ? 'Connected' : 'Off'}
            </span>
          }
          chevron
          onClick={() => setHevyOpen(true)}
        />
      </RowGroup>

      {/* About */}
      <SectionLabel>About</SectionLabel>
      <RowGroup>
        <Row icon="info" title="Version" trailing={<span className="tnum text-ink-3">1.0.0 (mock)</span>} />
        <Row icon="globe" title="Privacy policy" chevron onClick={() => setPrivacyOpen(true)} />
      </RowGroup>

      {/* Sign out */}
      <div className="pt-6">
        <SignOutRow onClick={() => setConfirmSignOut(true)} />
      </div>

      {/* Footer */}
      <div className="pt-7 flex flex-col items-center gap-1">
        <div className="text-[12px] text-ink-4">Strakk · v1.0.0 (mock)</div>
        <div className="text-[11px] text-ink-4">Food data: Open Food Facts (ODbL) · CIQUAL</div>
      </div>

      <GoalsEditSheet open={goalsOpen} onClose={() => setGoalsOpen(false)} />
      <ProfileSheet open={profileOpen} onClose={() => setProfileOpen(false)} />
      <HevySheet open={hevyOpen} onClose={() => setHevyOpen(false)} />
      <PushPage open={privacyOpen} onClose={() => setPrivacyOpen(false)} title="Privacy policy">
        <div className="pt-2 flex flex-col gap-2.5">
          {PRIVACY_TEXT.map(([title, body]) => (
            <div key={title} className="bg-surface-1 rounded-card p-4">
              <div className="text-[14px] font-semibold text-ink">{title}</div>
              <p className="mt-1 text-[13px] leading-snug text-ink-2">{body}</p>
            </div>
          ))}
          <p className="px-1 pt-1 text-[11px] text-ink-4">Full policy ships with the production release.</p>
        </div>
      </PushPage>
      <ConfirmDialog
        open={confirmSignOut}
        title="Sign out?"
        message="You'll need to sign back in to access your data."
        confirmLabel="Sign out"
        onCancel={() => setConfirmSignOut(false)}
        onConfirm={() => {
          setConfirmSignOut(false)
          nav.signOut()
        }}
      />
    </ScreenScroll>
  )
}
