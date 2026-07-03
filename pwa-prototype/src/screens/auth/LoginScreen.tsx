import { useState } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import { Eye, EyeOff, Lock, Mail } from 'lucide-react'
import { Button } from '../../components/Button.tsx'
import { haptic, spring } from '../../lib/ios.ts'
import * as authApi from '../../api/auth.ts'

// Shares the onboarding Welcome screen's visual language (top glow, haloed app
// mark, card fields) so auth and onboarding read as one product.

function Field({
  icon,
  type = 'text',
  value,
  onChange,
  placeholder,
  ariaLabel,
  trailing,
}: {
  icon: React.ReactNode
  type?: string
  value: string
  onChange: (value: string) => void
  placeholder: string
  ariaLabel: string
  trailing?: React.ReactNode
}) {
  return (
    <div
      className="flex h-[56px] items-center gap-3 rounded-card bg-surface-1 px-4 ring-1 ring-inset
        ring-white/[0.05] transition-shadow focus-within:ring-primary/40"
    >
      <span className="shrink-0 text-ink-3">{icon}</span>
      <input
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        aria-label={ariaLabel}
        autoCapitalize="none"
        autoCorrect="off"
        className="min-w-0 flex-1 bg-transparent text-[16px] text-ink placeholder:text-ink-4 outline-none"
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
        <div className="flex min-h-full flex-col px-7 pt-16 pb-10">
          <motion.div
            initial={{ opacity: 0, y: 14 }}
            animate={{ opacity: 1, y: 0 }}
            transition={spring.gentle}
            className="flex flex-col items-center text-center"
          >
            <img
              src="/icons/icon-512.png"
              alt="Strakk"
              className="size-[76px] rounded-[21px] ring-1 ring-white/10 shadow-[0_14px_34px_-8px_rgba(0,0,0,0.7)]"
            />
            <h1 className="mt-6 text-[28px] font-bold tracking-tight text-ink">
              {mode === 'login' ? 'Welcome back' : 'Reset password'}
            </h1>
            <p className="mt-2 max-w-[280px] text-[15px] leading-snug text-ink-2">
              {mode === 'login'
                ? 'Sign in to pick up where you left off.'
                : 'We’ll email you a link to set a new password.'}
            </p>
          </motion.div>

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
                <Field
                  icon={<Mail size={18} />}
                  type="email"
                  value={email}
                  onChange={setEmail}
                  placeholder="you@example.com"
                  ariaLabel="Email"
                />
                <Field
                  icon={<Lock size={18} />}
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={setPassword}
                  placeholder="Password"
                  ariaLabel="Password"
                  trailing={
                    <button
                      type="button"
                      onClick={() => setShowPassword((v) => !v)}
                      className="shrink-0 text-ink-3"
                      aria-label={showPassword ? 'Hide password' : 'Show password'}
                    >
                      {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                  }
                />
                {error && <div className="px-1 text-[13px] text-error">{error}</div>}
                <button
                  type="button"
                  onClick={() => {
                    haptic('light')
                    setMode('reset')
                    setResetSent(false)
                    setError(null)
                  }}
                  className="self-end pt-0.5 text-[13px] font-medium text-primary"
                >
                  Forgot password?
                </button>
                <div className="pt-2">
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

                <div className="flex items-center gap-3 py-2">
                  <span className="h-px flex-1 bg-divider/60" />
                  <span className="text-[12px] font-medium uppercase tracking-wide text-ink-4">or</span>
                  <span className="h-px flex-1 bg-divider/60" />
                </div>

                <Button
                  variant="secondary"
                  full
                  onClick={() => {
                    haptic('light')
                    onCreateAccount()
                  }}
                >
                  Create an account
                </Button>
                <p className="pt-1 text-center text-[12px] leading-snug text-ink-4">
                  New here? We’ll set your nutrition targets in about a minute.
                </p>
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
                  <div className="rounded-card border border-success/25 bg-success/12 px-4 py-4 text-center">
                    <div className="text-[15px] font-semibold text-success">Check your inbox</div>
                    <div className="mt-1 text-[13px] text-ink-2">
                      If an account exists for {email || 'that address'}, a reset link is on its way.
                    </div>
                  </div>
                ) : (
                  <>
                    <Field
                      icon={<Mail size={18} />}
                      type="email"
                      value={email}
                      onChange={setEmail}
                      placeholder="you@example.com"
                      ariaLabel="Email"
                    />
                    {error && <div className="px-1 text-[13px] text-error">{error}</div>}
                    <div className="pt-2">
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
                  className="self-center pt-2 text-[14px] font-medium text-primary"
                >
                  Back to sign in
                </button>
              </motion.div>
            )}
          </AnimatePresence>

          <div className="flex-1" />
        </div>
      </div>
    </div>
  )
}
