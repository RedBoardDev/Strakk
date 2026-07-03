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
import { useToast } from '../components/Toast.tsx'
import { GoalsEditSheet } from './sheets/GoalsEditSheet.tsx'
import { haptic } from '../lib/ios.ts'
import { useNav } from '../nav.ts'
import { useStore } from '../store.tsx'
import { getHevyApiKey, saveHevyApiKey } from '../api/hevy.ts'

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

// Editable personal details (name lives in auth metadata; email is the key).
function ProfileSheet({ open, onClose }: { open: boolean; onClose: () => void }) {
  const store = useStore()
  const toast = useToast()
  const [name, setName] = useState(store.state.user.firstName)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (open) setName(store.state.user.firstName)
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
          disabled={name.trim() === '' || saving}
          onClick={() => {
            haptic('medium')
            setSaving(true)
            void store
              .setFirstName(name.trim())
              .then(() => {
                onClose()
                toast.show('Profile updated')
              })
              .catch(() => {})
              .finally(() => setSaving(false))
          }}
        >
          {saving ? 'Saving…' : 'Save'}
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
          <span className="flex-1 min-w-0 truncate text-[15px] text-ink-2 tnum">{store.state.user.email}</span>
        </div>
      </div>
    </Sheet>
  )
}

// Hevy integration — the API key is stored server-side in Vault via RPCs.
function HevySheet({ open, onClose }: { open: boolean; onClose: () => void }) {
  const store = useStore()
  const toast = useToast()
  const [key, setKey] = useState('')
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const connected = store.state.hevyConnected

  useEffect(() => {
    if (!open) return
    setKey('')
    setLoading(true)
    void getHevyApiKey()
      .then((existing) => store.setHevyConnected(Boolean(existing)))
      .catch(() => {})
      .finally(() => setLoading(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  const save = async () => {
    haptic('medium')
    setSaving(true)
    try {
      await saveHevyApiKey(key.trim())
      store.setHevyConnected(true)
      onClose()
      toast.show('Hevy connected')
    } catch (err) {
      toast.show(err instanceof Error ? err.message : 'Could not save the key')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Sheet open={open} onClose={onClose} title="Hevy" detents={['small']}>
      <div className="flex flex-col gap-4 pt-1 pb-4">
        <div className="bg-surface-1 rounded-card px-4 py-3.5 flex items-center gap-3">
          <div className="size-10 rounded-[12px] bg-surface-3 flex items-center justify-center shrink-0">
            <Icon name="dumbbell.fill" size={18} color="#34C7B5" />
          </div>
          <div className="flex-1 min-w-0">
            <div className="text-[15px] font-semibold text-ink">Hevy</div>
            <div className="text-[12px] text-ink-3">
              {loading ? 'Checking…' : connected ? 'API key configured' : 'Not connected'}
            </div>
          </div>
          {connected && <Icon name="check" size={16} className="text-success shrink-0" />}
        </div>
        <div className="bg-surface-1 rounded-card px-4 h-[54px] flex items-center gap-3">
          <span className="text-[13px] font-medium text-ink-3 shrink-0">API key</span>
          <input
            value={key}
            onChange={(event) => setKey(event.target.value)}
            placeholder={connected ? '•••••••• (replace)' : 'Paste your Hevy API key'}
            autoCapitalize="none"
            autoCorrect="off"
            className="flex-1 min-w-0 bg-transparent text-[15px] text-ink placeholder:text-ink-4 outline-none tnum"
          />
        </div>
        <Button variant="primary" full disabled={key.trim() === '' || saving} onClick={() => void save()}>
          {saving ? 'Saving…' : 'Save key'}
        </Button>
        <p className="px-1 text-[12px] leading-snug text-ink-4">
          Hevy → Settings → Developer → API key. Enables workout sync in check-ins and routine export.
        </p>
      </div>
    </Sheet>
  )
}

const PRIVACY_TEXT = [
  ['Your data', 'Meals, measurements and check-ins are stored in your account and never sold or shared with advertisers.'],
  ['Photos', 'Progress photos are private to your account and encrypted at rest.'],
  ['AI features', 'Meal photos and text sent for AI analysis are processed transiently and not used to train models.'],
  ['Delete anytime', 'You can export or permanently delete your data at any time.'],
] as const

export function SettingsScreen() {
  const nav = useNav()
  const store = useStore()
  const { user, goals, hevyConnected } = store.state
  const [goalsOpen, setGoalsOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const [hevyOpen, setHevyOpen] = useState(false)
  const [privacyOpen, setPrivacyOpen] = useState(false)
  const [confirmSignOut, setConfirmSignOut] = useState(false)

  // Reflect the Vault-stored key state once per screen mount.
  useEffect(() => {
    void getHevyApiKey()
      .then((existing) => store.setHevyConnected(Boolean(existing)))
      .catch(() => {})
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

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
            <span className="text-[18px] font-bold text-white">{user.firstName[0]?.toUpperCase() ?? '?'}</span>
          </div>
          <div className="flex-1 min-w-0">
            <div className="text-[16px] font-semibold text-ink truncate">{user.firstName}</div>
            <div className="text-[12px] text-ink-3 truncate">Personal details</div>
          </div>
          <Icon name="chevron.right" size={16} className="text-ink-3 shrink-0" />
          <span className="rowhair pointer-events-none absolute bottom-0 right-0 left-[68px] h-px bg-divider/60" />
        </motion.button>
        <Row title="Email" trailing={<span className="tnum text-ink-2">{user.email}</span>} />
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
            <span className={`text-[13px] font-medium ${hevyConnected ? 'text-success' : 'text-ink-3'}`}>
              {hevyConnected ? 'Connected' : 'Off'}
            </span>
          }
          chevron
          onClick={() => setHevyOpen(true)}
        />
      </RowGroup>

      {/* About */}
      <SectionLabel>About</SectionLabel>
      <RowGroup>
        <Row icon="info" title="Version" trailing={<span className="tnum text-ink-3">1.0.0</span>} />
        <Row icon="globe" title="Privacy policy" chevron onClick={() => setPrivacyOpen(true)} />
      </RowGroup>

      {/* Sign out */}
      <div className="pt-6">
        <SignOutRow onClick={() => setConfirmSignOut(true)} />
      </div>

      {/* Footer */}
      <div className="pt-7 flex flex-col items-center gap-1">
        <div className="text-[12px] text-ink-4">Strakk · v1.0.0</div>
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
