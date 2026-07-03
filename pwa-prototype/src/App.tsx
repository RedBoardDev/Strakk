import { useState, type ComponentType } from 'react'
import { DeviceFrame } from './components/DeviceFrame.tsx'
import { InstallGate } from './components/InstallGate.tsx'
import { TabBar, type TabKey } from './components/TabBar.tsx'
import { ToastProvider } from './components/Toast.tsx'
import { NavContext, type Flow } from './nav.ts'
import { OverlayContext } from './overlay.ts'
import { StoreProvider } from './store.tsx'
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
import type { Food, MealEntry } from './data/mock.ts'

type Phase = 'app' | 'auth' | 'onboarding'

// The app starts signed-in (a mock convenience for testing). Sign-out drops to
// the login screen; "create account" enters onboarding; both return to the app.
// `?auth` / `?onboarding` jump straight to a flow for previewing.
function initialPhase(): Phase {
  if (typeof window === 'undefined') return 'app'
  const params = new URLSearchParams(window.location.search)
  if (params.has('auth')) return 'auth'
  if (params.has('onboarding')) return 'onboarding'
  return 'app'
}

const SCREENS: Record<TabKey, ComponentType> = {
  today: TodayScreen,
  calendar: CalendarScreen,
  checkins: CheckInScreen,
  settings: SettingsScreen,
}

function MainApp({ onSignOut }: { onSignOut: () => void }) {
  const [tab, setTab] = useState<TabKey>('today')
  const [flow, setFlow] = useState<Flow | null>(null)
  const [food, setFood] = useState<Food | null>(null)
  const [meal, setMeal] = useState<MealEntry | null>(null)
  const [overlayHost, setOverlayHost] = useState<HTMLElement | null>(null)

  const nav = {
    open: (f: Flow) => {
      if (f.kind === 'foodDetail') setFood(f.food)
      if (f.kind === 'mealDetail') setMeal(f.meal)
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
          <AddSheet open={flow?.kind === 'add'} onClose={nav.close} />
          <SearchSheet open={flow?.kind === 'search'} onClose={nav.close} />
          <ScanSheet open={flow?.kind === 'scan'} onClose={nav.close} />
          <ManualEntrySheet open={flow?.kind === 'manual'} onClose={nav.close} />
          <MealBuilderSheet open={flow?.kind === 'mealBuilder'} onClose={nav.close} />
          <QuickAddSheet open={flow?.kind === 'quickAdd'} onClose={nav.close} />
          <PhotoMealSheet open={flow?.kind === 'photoMeal'} onClose={nav.close} />
          <FoodDetailSheet
            open={flow?.kind === 'foodDetail'}
            food={food}
            // Dismissing the detail returns to the search it came from (so you
            // can keep adding foods); other origins just close.
            onClose={() => {
              const from = flow?.kind === 'foodDetail' ? flow.from : undefined
              if (from === 'search') nav.open({ kind: 'search' })
              else nav.close()
            }}
          />
          <MealDetailSheet open={flow?.kind === 'mealDetail'} onClose={nav.close} meal={meal} />

          {/* Portal host for full-screen pushed pages (see overlay.ts) */}
          <div ref={setOverlayHost} />
        </div>
      </OverlayContext.Provider>
    </NavContext.Provider>
  )
}

export function App() {
  const [phase, setPhase] = useState<Phase>(initialPhase)

  return (
    <DeviceFrame>
      <InstallGate>
        <StoreProvider>
          <ToastProvider>
            {phase === 'auth' ? (
              <LoginScreen onSignIn={() => setPhase('app')} onCreateAccount={() => setPhase('onboarding')} />
            ) : phase === 'onboarding' ? (
              <OnboardingFlow onComplete={() => setPhase('app')} onBackToLogin={() => setPhase('auth')} />
            ) : (
              <MainApp onSignOut={() => setPhase('auth')} />
            )}
          </ToastProvider>
        </StoreProvider>
      </InstallGate>
    </DeviceFrame>
  )
}
