import { useCallback, useEffect, useState, type ComponentType } from 'react'
import { DeviceFrame } from './components/DeviceFrame.tsx'
import { InstallGate } from './components/InstallGate.tsx'
import { TabBar, type TabKey } from './components/TabBar.tsx'
import { ToastProvider } from './components/Toast.tsx'
import { NavContext, type Flow } from './nav.ts'
import { OverlayContext } from './overlay.ts'
import { StoreProvider } from './store.tsx'
import { supabase } from './api/supabase.ts'
import * as authApi from './api/auth.ts'
import { fetchProfile } from './api/profile.ts'
import { TodayScreen } from './screens/TodayScreen.tsx'
import { CalendarScreen } from './screens/CalendarScreen.tsx'
import { CheckInScreen } from './screens/CheckInScreen.tsx'
import { SettingsScreen } from './screens/SettingsScreen.tsx'
import { AddSheet } from './screens/sheets/AddSheet.tsx'
import { SearchSheet } from './screens/sheets/SearchSheet.tsx'
import { ScanSheet } from './screens/sheets/ScanSheet.tsx'
import { ManualEntrySheet } from './screens/sheets/ManualEntrySheet.tsx'
import { MealBuilderSheet } from './screens/sheets/MealBuilderSheet.tsx'
import { QuickAddSheet } from './screens/sheets/QuickAddSheet.tsx'
import { PhotoMealSheet } from './screens/sheets/PhotoMealSheet.tsx'
import { FoodDetailSheet } from './screens/sheets/FoodDetailSheet.tsx'
import { MealDetailSheet } from './screens/sheets/MealDetailSheet.tsx'
import { LoginScreen } from './screens/auth/LoginScreen.tsx'
import { OnboardingFlow } from './screens/onboarding/OnboardingFlow.tsx'
import type { CatalogFood } from './api/foods.ts'
import type { Meal, Entry } from './api/meals.ts'

type Phase = 'boot' | 'auth' | 'onboarding' | 'app'

const SCREENS: Record<TabKey, ComponentType> = {
  today: TodayScreen,
  calendar: CalendarScreen,
  checkins: CheckInScreen,
  settings: SettingsScreen,
}

function Splash() {
  return (
    <div className="h-full w-full flex flex-col items-center justify-center bg-bg gap-4">
      <img src="/icons/icon-512.png" alt="Strakk" className="size-16 rounded-[18px] ring-1 ring-white/10" />
      <div className="size-5 rounded-full border-2 border-primary/30 border-t-primary animate-spin" />
    </div>
  )
}

function MainApp({ onSignOut }: { onSignOut: () => void }) {
  const [tab, setTab] = useState<TabKey>('today')
  const [flow, setFlow] = useState<Flow | null>(null)
  const [food, setFood] = useState<CatalogFood | null>(null)
  const [meal, setMeal] = useState<Meal | null>(null)
  const [entry, setEntry] = useState<Entry | null>(null)
  const [overlayHost, setOverlayHost] = useState<HTMLElement | null>(null)

  const nav = {
    open: (f: Flow) => {
      if (f.kind === 'foodDetail') setFood(f.food)
      if (f.kind === 'mealDetail') {
        setMeal(f.meal)
        setEntry(null)
      }
      if (f.kind === 'entryDetail') {
        setEntry(f.entry)
        setMeal(null)
      }
      setFlow(f)
    },
    close: () => setFlow(null),
    setTab,
    signOut: () => {
      setFlow(null)
      onSignOut()
    },
  }

  const Screen = SCREENS[tab]
  const logDate = flow && 'logDate' in flow ? flow.logDate : undefined

  return (
    <NavContext.Provider value={nav}>
      <OverlayContext.Provider value={overlayHost}>
        <div className="relative h-full w-full overflow-hidden bg-bg">
          {/* Tab content fills the shell; it scrolls UNDER the floating glass
              tab bar (each screen pads its own bottom to clear it). */}
          <div className="absolute inset-0">
            {/* iOS tab switches are instant (no crossfade); remount resets scroll. */}
            <div key={tab} className="h-full">
              <Screen />
            </div>
          </div>

          <TabBar active={tab} onChange={setTab} />

          {/* Modal flows (one active at a time) */}
          <AddSheet open={flow?.kind === 'add'} onClose={nav.close} logDate={logDate} />
          <SearchSheet open={flow?.kind === 'search'} onClose={nav.close} logDate={logDate} />
          <ScanSheet open={flow?.kind === 'scan'} onClose={nav.close} logDate={logDate} />
          <ManualEntrySheet open={flow?.kind === 'manual'} onClose={nav.close} logDate={logDate} />
          <MealBuilderSheet open={flow?.kind === 'mealBuilder'} onClose={nav.close} logDate={logDate} />
          <QuickAddSheet open={flow?.kind === 'quickAdd'} onClose={nav.close} logDate={logDate} />
          <PhotoMealSheet open={flow?.kind === 'photoMeal'} onClose={nav.close} logDate={logDate} />
          <FoodDetailSheet
            open={flow?.kind === 'foodDetail'}
            food={food}
            logDate={logDate}
            // Dismissing the detail returns to the search it came from (so you
            // can keep adding foods); other origins just close.
            onClose={() => {
              const from = flow?.kind === 'foodDetail' ? flow.from : undefined
              if (from === 'search') nav.open({ kind: 'search', logDate })
              else nav.close()
            }}
          />
          <MealDetailSheet
            open={flow?.kind === 'mealDetail' || flow?.kind === 'entryDetail'}
            onClose={nav.close}
            meal={meal}
            entry={entry}
          />

          {/* Portal host for full-screen pushed pages (see overlay.ts) */}
          <div ref={setOverlayHost} />
        </div>
      </OverlayContext.Provider>
    </NavContext.Provider>
  )
}

export function App() {
  const [phase, setPhase] = useState<Phase>('boot')

  // A signed-in user lands on the app when onboarded, otherwise resumes
  // onboarding (profile row exists from the signup trigger).
  const resolveSignedIn = useCallback(async () => {
    try {
      const profile = await fetchProfile()
      setPhase(profile?.onboarding_completed ? 'app' : 'onboarding')
    } catch {
      setPhase('app') // profile fetch hiccup — let the app load and retry inside
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    void authApi.getSession().then((session) => {
      if (cancelled) return
      if (session) void resolveSignedIn()
      else setPhase('auth')
    })
    const { data: sub } = supabase.auth.onAuthStateChange((event) => {
      if (event === 'SIGNED_OUT') setPhase('auth')
    })
    return () => {
      cancelled = true
      sub.subscription.unsubscribe()
    }
  }, [resolveSignedIn])

  const signOut = useCallback(() => {
    void authApi.signOut() // listener flips the phase
  }, [])

  return (
    <DeviceFrame>
      <InstallGate>
        <ToastProvider>
          {phase === 'boot' ? (
            <Splash />
          ) : phase === 'auth' ? (
            <LoginScreen onSignedIn={resolveSignedIn} onCreateAccount={() => setPhase('onboarding')} />
          ) : phase === 'onboarding' ? (
            <OnboardingFlow onComplete={() => setPhase('app')} onBackToLogin={() => setPhase('auth')} />
          ) : (
            <StoreProvider>
              <MainApp onSignOut={signOut} />
            </StoreProvider>
          )}
        </ToastProvider>
      </InstallGate>
    </DeviceFrame>
  )
}
