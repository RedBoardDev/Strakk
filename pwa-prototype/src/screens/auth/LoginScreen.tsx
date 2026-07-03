import { useState } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import { Eye, EyeOff } from 'lucide-react'
import { Button } from '../../components/Button.tsx'
import { haptic, spring } from '../../lib/ios.ts'
import * as authApi from '../../api/auth.ts'

function AppMark() {
  return (
    <img
      src="/icons/icon-512.png"
      alt="Strakk"
      className="size-[72px] rounded-[20px] ring-1 ring-white/10 shadow-[0_14px_34px_-8px_rgba(0,0,0,0.7)]"
    />
  )
}

function Field({
  label,
  type = 'text',
  value,
  onChange,
  placeholder,
  trailing,
}: {
  label: string
  type?: string
  value: string
  onChange: (value: string) => void
  placeholder?: string
  trailing?: React.ReactNode
}) {
  return (
    <div className="bg-surface-1 rounded-card px-4 h-[54px] flex items-center gap-3">
      <span className="text-[13px] font-medium text-ink-3 w-[74px] shrink-0">{label}</span>
      <input
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        autoCapitalize="none"
        autoCorrect="off"
        className="flex-1 min-w-0 bg-transparent text-[16px] text-ink placeholder:text-ink-4 outline-none"
      />
      {trailing}
    </div>
  )
}

export function LoginScreen({
  onSignedIn,
  onCreateAccount,
}: {
  onSignedIn: () => void
  onCreateAccount: () => void
}) {
  const [mode, setMode] = useState<'login' | 'reset'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [resetSent, setResetSent] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const signIn = async () => {
    haptic('medium')
    setBusy(true)
    setError(null)
    try {
      await authApi.signIn(email.trim(), password)
      onSignedIn()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Sign-in failed')
    } finally {
      setBusy(false)
    }
  }

  const sendReset = async () => {
    haptic('medium')
    setBusy(true)
    setError(null)
    try {
      await authApi.resetPassword(email.trim())
      setResetSent(true)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not send the reset email')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="relative h-full w-full overflow-hidden bg-bg">
      <div className="h-full overflow-y-auto pt-safe pb-safe">
        <div className="min-h-full flex flex-col px-7 pt-14 pb-10">
          <div className="flex flex-col items-center text-center">
            <AppMark />
            <h1 className="mt-6 text-[28px] font-bold tracking-tight text-ink">
              {mode === 'login' ? 'Welcome back' : 'Reset password'}
            </h1>
            <p className="mt-2 text-[15px] leading-snug text-ink-2 max-w-[280px]">
              {mode === 'login'
                ? 'Sign in to pick up where you left off.'
                : 'We’ll email you a link to set a new password.'}
            </p>
          </div>

          <AnimatePresence mode="wait" initial={false}>
            {mode === 'login' ? (
              <motion.div
                key="login"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -8 }}
                transition={spring.gentle}
                className="mt-9 flex flex-col gap-3"
              >
                <Field label="Email" type="email" value={email} onChange={setEmail} placeholder="you@example.com" />
                <Field
                  label="Password"
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={setPassword}
                  placeholder="••••••••"
                  trailing={
                    <button
                      type="button"
                      onClick={() => setShowPassword((value) => !value)}
                      className="text-ink-3 shrink-0"
                      aria-label={showPassword ? 'Hide password' : 'Show password'}
                    >
                      {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                  }
                />
                {error && <div className="text-[13px] text-error px-1">{error}</div>}
                <button
                  type="button"
                  onClick={() => {
                    haptic('light')
                    setMode('reset')
                    setResetSent(false)
                    setError(null)
                  }}
                  className="self-end text-[13px] font-medium text-primary pt-0.5"
                >
                  Forgot password?
                </button>
                <div className="pt-3">
                  <Button
                    variant="primary"
                    full
                    glow
                    disabled={email.trim() === '' || password === '' || busy}
                    onClick={() => void signIn()}
                  >
                    {busy ? 'Signing in…' : 'Sign in'}
                  </Button>
                </div>
              </motion.div>
            ) : (
              <motion.div
                key="reset"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -8 }}
                transition={spring.gentle}
                className="mt-9 flex flex-col gap-3"
              >
                {resetSent ? (
                  <div className="bg-success/12 border border-success/25 rounded-card px-4 py-4 text-center">
                    <div className="text-[15px] font-semibold text-success">Check your inbox</div>
                    <div className="mt-1 text-[13px] text-ink-2">
                      If an account exists for {email || 'that address'}, a reset link is on its way.
                    </div>
                  </div>
                ) : (
                  <>
                    <Field label="Email" type="email" value={email} onChange={setEmail} placeholder="you@example.com" />
                    {error && <div className="text-[13px] text-error px-1">{error}</div>}
                    <div className="pt-3">
                      <Button
                        variant="primary"
                        full
                        glow
                        disabled={email.trim() === '' || busy}
                        onClick={() => void sendReset()}
                      >
                        {busy ? 'Sending…' : 'Send reset link'}
                      </Button>
                    </div>
                  </>
                )}
                <button
                  type="button"
                  onClick={() => {
                    haptic('light')
                    setMode('login')
                    setError(null)
                  }}
                  className="self-center text-[14px] font-medium text-primary pt-2"
                >
                  Back to sign in
                </button>
              </motion.div>
            )}
          </AnimatePresence>

          <div className="flex-1" />

          {mode === 'login' && (
            <button
              type="button"
              onClick={() => {
                haptic('light')
                onCreateAccount()
              }}
              className="mt-8 text-center text-[14px] text-ink-2"
            >
              New here? <span className="font-semibold text-primary">Create an account</span>
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
