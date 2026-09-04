# iOS-feel web research

## Making a React PWA feel native-iOS with motion (v12, formerly Framer Motion) + CSS — copy-pasteable techniques tuned to UIKit/SwiftUI behavior. Stack-matched to pwa-prototype: motion ^12.42.0, import from 'motion/react', React 19, Tailwind v4.

### iOS-matched spring presets (visualDuration + bounce, the 1:1 SwiftUI mapping)
Motion v12's spring accepts { visualDuration, bounce } which maps directly onto SwiftUI's .spring(duration:bounce:). visualDuration is the time the motion *appears* to take (not the settle time), so values line up with how Apple specifies springs. Use bounce 0 for sheets/nav/layout (critically damped, no overshoot — what iOS sheets and push transitions actually do), bounce 0.15 for snappy selections/toggles, bounce 0.3 for playful emphasis. Keep one shared presets module and reference it everywhere so motion stays consistent like a system. If you'd rather think in stiffness/damping, convert from SwiftUI's (response, dampingFraction) with stiffness=(2π/response)²·mass and damping=(4π·dampingFraction/response)·mass. Wrap the app in <MotionConfig reducedMotion="user" transition={iosSpring.smooth}> so Reduce Motion is honored and the default transition is iOS-like everywhere.

```
// springs.ts — iOS-matched transitions for motion/react
// { visualDuration, bounce } maps 1:1 to SwiftUI .spring(duration:bounce:)
import type { Transition } from 'motion/react'

export const iosSpring = {
  smooth: { type: 'spring', visualDuration: 0.5,  bounce: 0    }, // .smooth — sheets, nav, layout
  snappy: { type: 'spring', visualDuration: 0.35, bounce: 0.15 }, // .snappy — toggles, selections
  bouncy: { type: 'spring', visualDuration: 0.5,  bounce: 0.3  }, // .bouncy — emphasis
  press:  { type: 'spring', visualDuration: 0.18, bounce: 0.2  }, // quick interactive settle
} satisfies Record<string, Transition>

// Prefer raw stiffness/damping? Convert from SwiftUI (response, dampingFraction):
export const fromSwiftUI = (response: number, damping: number, mass = 1): Transition => ({
  type: 'spring',
  stiffness: (2 * Math.PI / response) ** 2 * mass,
  damping: (4 * Math.PI * damping / response) * mass,
  mass,
})
// iOS sheet feel ≈ fromSwiftUI(0.5, 1.0);  legacy default ≈ fromSwiftUI(0.55, 0.825)

// App root:
// <MotionConfig reducedMotion="user" transition={iosSpring.smooth}>{children}</MotionConfig>
```

### Draggable bottom sheet with medium/large detents, velocity dismiss, rubber-band past top
Drive one external motion value `y` (the sheet's translateY). Present and dismiss imperatively with animate(y, target, iosSpring) so AnimatePresence timing stays simple and the same value the user drags is the one that animates. drag="y" + useDragControls with dragListener={false} means only the grabber handle starts the drag (mirrors iOS where you grab the sheet). dragConstraints clamp the free range between the large detent (top) and screen bottom; dragElastic on top gives the rubber-band resistance when pulled above the large detent. On release, snap with the canonical iOS UIScrollView projection — project(velocity) = (v/1000)·(decel/(1-decel)) with decel 0.998 ≈ v·0.5 px — to predict where a flick would land, then pick the nearest detent or dismiss. A hard velocity threshold (>1200 px/s downward) also dismisses, matching a fast flick-to-close.

```
import { motion, AnimatePresence, useMotionValue, useDragControls, animate, type PanInfo } from 'motion/react'
import { useEffect } from 'react'

const project = (v: number, decel = 0.998) => (v / 1000) * (decel / (1 - decel)) // iOS projection

export function BottomSheet({ open, onClose, children }: {
  open: boolean; onClose: () => void; children: React.ReactNode
}) {
  return (
    <AnimatePresence>
      {open && <Sheet key="sheet" onClose={onClose}>{children}</Sheet>}
    </AnimatePresence>
  )
}

function Sheet({ onClose, children }: { onClose: () => void; children: React.ReactNode }) {
  const h = window.innerHeight
  const large = h * 0.10, medium = h * 0.55, dismissAt = h * 0.9
  const y = useMotionValue(h)
  const controls = useDragControls()

  useEffect(() => { animate(y, medium, { type: 'spring', visualDuration: 0.5, bounce: 0 }) }, [])
  const dismiss = () => animate(y, h, { type: 'spring', visualDuration: 0.35, bounce: 0, onComplete: onClose })

  const onEnd = (_: PointerEvent, info: PanInfo) => {
    const predicted = y.get() + project(info.velocity.y)
    if (predicted > dismissAt || (info.velocity.y > 1200 && y.get() > medium)) return void dismiss()
    animate(y, predicted < (large + medium) / 2 ? large : medium, { type: 'spring', visualDuration: 0.5, bounce: 0 })
  }

  return (
    <>
      <motion.div className="fixed inset-0 z-40 bg-black/40"
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={dismiss} />
      <motion.div
        className="fixed inset-x-0 top-0 z-50 rounded-t-[20px] bg-neutral-900"
        style={{ y, height: h }}
        drag="y" dragControls={controls} dragListener={false}
        dragConstraints={{ top: large, bottom: h }}
        dragElastic={{ top: 0.06, bottom: 0.5 }}   // rubber-band when pulled above the large detent
        onDragEnd={onEnd}
      >
        <div className="mx-auto mt-2 h-1.5 w-9 rounded-full bg-white/30"
          style={{ touchAction: 'none' }} onPointerDown={(e) => controls.start(e)} />
        {children}
      </motion.div>
    </>
  )
}
```

### NavigationStack push/pop transition with left-edge back-swipe gesture
iOS push = incoming page slides in from x:100% while the page underneath parallaxes to about x:-25% and dims to ~0.6 brightness; pop reverses it. Render the stack so the top page is an overlay and the page beneath gets the parallax/dim driven by whether something is on top. For the interactive back-swipe, replicate UIKit's screenEdgePan: an invisible left-edge strip (~20px) is the only thing that starts the drag (useDragControls + dragListener={false}), so vertical scrolls and mid-screen drags are never hijacked. drag="x" with dragConstraints left:0 and dragElastic that frees the right direction lets the page follow the finger; on release, use the same projection (offset + velocity·0.5) — past 40% of width or a fast flick pops, otherwise it springs back. This is the exact heuristic iOS uses.

```
import { motion, useMotionValue, useDragControls, animate, type PanInfo } from 'motion/react'
import { iosSpring } from './springs'

// Top page in the stack. The page BENEATH should render with
// animate={{ x: '-25%', filter: 'brightness(0.6)' }} while this is mounted.
export function NavPage({ children, onBack }: { children: React.ReactNode; onBack?: () => void }) {
  const x = useMotionValue(0)
  const controls = useDragControls()

  const onEnd = (_: PointerEvent, info: PanInfo) => {
    const w = window.innerWidth
    const predicted = info.offset.x + info.velocity.x * 0.5 // iOS projection
    if (onBack && predicted > w * 0.4) {
      animate(x, w, { type: 'spring', visualDuration: 0.35, bounce: 0, onComplete: onBack })
    } else {
      animate(x, 0, iosSpring.smooth)
    }
  }

  return (
    <motion.div
      className="absolute inset-0 bg-neutral-950 shadow-[-8px_0_24px_rgba(0,0,0,0.35)] will-change-transform"
      style={{ x }}
      initial={{ x: '100%' }} animate={{ x: 0 }} exit={{ x: '100%' }}  // push in / pop out
      transition={iosSpring.smooth}
      drag={onBack ? 'x' : false} dragControls={controls} dragListener={false}
      dragConstraints={{ left: 0, right: 0 }} dragElastic={{ left: 0, right: 1 }} // free to the right only
      onDragEnd={onEnd}
    >
      {/* left-edge strip is the ONLY back-swipe initiator, like UIScreenEdgePanGestureRecognizer */}
      {onBack && (
        <div className="absolute left-0 top-0 z-50 h-full w-5"
          style={{ touchAction: 'pan-y' }} onPointerDown={(e) => controls.start(e)} />
      )}
      {children}
    </motion.div>
  )
}
```

### Numeric odometer / digit-roll for changing counters
A real iOS-style rolling counter animates each digit independently as a vertical strip of 0–9, translating to the active digit — not a single crossfade of the whole number. Each digit column is 1em tall with overflow-hidden and leading-none; the inner stack of ten glyphs translates by -digit em with a snappy spring. Only the digits that actually change roll, so 199→200 rolls three columns by different amounts, just like the native odometer. Use tabular-nums (the project already has a `.tnum` class) so widths don't jitter. This complements the existing crossfade AnimatedNumber.tsx in src/components — use the roll for prominent counters (calories, streak), the crossfade for fast-ticking values.

```
import { motion } from 'motion/react'
import { iosSpring } from './springs'

function Digit({ place, value }: { place: number; value: number }) {
  const digit = Math.floor(value / place) % 10
  return (
    <span className="relative inline-block h-[1em] w-[1ch] overflow-hidden leading-none tabular-nums">
      <motion.span className="absolute inset-x-0 top-0 flex flex-col items-center"
        animate={{ y: `${-digit}em` }} transition={iosSpring.snappy}>
        {Array.from({ length: 10 }, (_, n) => (
          <span key={n} className="flex h-[1em] items-center justify-center">{n}</span>
        ))}
      </motion.span>
    </span>
  )
}

export function Odometer({ value, places = [100, 10, 1] }: { value: number; places?: number[] }) {
  return (
    <span className="inline-flex leading-none">
      {places.map((p) => <Digit key={p} place={p} value={Math.max(0, Math.floor(value))} />)}
    </span>
  )
}
```

### iOS tab-bar switching with crossfade (no horizontal slide)
Native iOS tab switching is an instant crossfade in place — it does NOT slide horizontally (that's a paging container, a different gesture). Reproduce it with AnimatePresence mode="popLayout" keyed on the active tab so the outgoing view fades out while the incoming view is already laid out underneath. initial={false} prevents the first tab from animating on mount. A barely-there scale (0.98→1) adds the subtle iOS depth pop without reading as a slide. Keep the fade short (~0.22s) on the project's iOS ease [0.32,0.72,0,1] so it feels instant but not harsh. Pair each tab button with the Pressable from the press technique for the icon tap feel.

```
import { motion, AnimatePresence } from 'motion/react'

export function TabView({ tab, children }: { tab: string; children: React.ReactNode }) {
  return (
    <div className="relative flex-1 overflow-hidden">
      <AnimatePresence mode="popLayout" initial={false}>
        <motion.div
          key={tab}
          className="absolute inset-0"
          initial={{ opacity: 0, scale: 0.98 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 1.01 }}
          transition={{ duration: 0.22, ease: [0.32, 0.72, 0, 1] }}
        >
          {children}
        </motion.div>
      </AnimatePresence>
    </div>
  )
}
```

### Press scale 0.97 over ~100ms with haptic-like feedback on touch-down
whileTap={{ scale: 0.97 }} with a 0.1s tween on the iOS ease gives the exact UIKit button-down compression. Fire feedback on onTapStart (press-down), not onTap (release), because UIKit triggers UIImpactFeedbackGenerator the instant the finger lands. For actual haptics: navigator.vibrate works on Android Chrome but iOS Safari ignores it — the only known iOS web haptic is the Safari 17.4+ trick of programmatically clicking a hidden <input type="checkbox" switch> inside a user gesture, which emits a real haptic tick when the user has system haptics enabled. Wrap both in one haptic() helper that degrades gracefully. Add touch-action: manipulation and WebkitTapHighlightColor: transparent so taps feel instant and don't flash the mobile-browser highlight.

```
import { motion } from 'motion/react'

let hapticEl: HTMLLabelElement | null = null
export function haptic(ms = 8) {
  if (typeof navigator !== 'undefined' && 'vibrate' in navigator) navigator.vibrate(ms) // Android
  if (typeof document === 'undefined') return
  if (!hapticEl) {                       // iOS Safari 17.4+ best-effort: hidden <input switch>
    hapticEl = document.createElement('label')
    hapticEl.setAttribute('aria-hidden', 'true')
    hapticEl.style.cssText = 'position:fixed;top:-9999px;opacity:0;pointer-events:none'
    hapticEl.innerHTML = '<input type="checkbox" switch>'
    document.body.appendChild(hapticEl)
  }
  ;(hapticEl.firstChild as HTMLInputElement).click() // must run inside a user gesture
}

export function Pressable({ onPress, children, className = '' }: {
  onPress?: () => void; children: React.ReactNode; className?: string
}) {
  return (
    <motion.button
      type="button"
      className={className}
      style={{ WebkitTapHighlightColor: 'transparent', touchAction: 'manipulation' }}
      whileTap={{ scale: 0.97 }}
      transition={{ duration: 0.1, ease: [0.32, 0.72, 0, 1] }}
      onTapStart={() => haptic(8)}   // fire on touch-down, like UIImpactFeedbackGenerator
      onTap={onPress}
    >
      {children}
    </motion.button>
  )
}
```

## Making a React PWA feel native and installable in standalone mode on iOS Safari + Android Chrome

### viewport-fit=cover + env(safe-area-inset-*) for nav bar and tab bar
Add viewport-fit=cover to the viewport meta so the layout extends edge-to-edge under the notch and home indicator; this is the precondition that makes the env(safe-area-inset-*) variables resolve to non-zero values. Then pad fixed app-chrome (top nav, bottom tab bar) with calc(designPadding + env(safe-area-inset-*)) so content clears the notch and home indicator. Wrap each inset in max(..., 0px) so non-notch devices and Android don't get NaN/negative values. With apple-mobile-web-app-status-bar-style=black-translucent the web content draws UNDER the iOS status bar, so safe-area-inset-top is mandatory on the top bar or the title collides with the clock. The project already exposes pt-safe/pb-safe utilities in index.css — extend that pattern for the bars.

```
<!-- index.html: viewport-fit=cover is what activates the env() insets -->
<meta name="viewport"
      content="width=device-width, initial-scale=1, viewport-fit=cover, maximum-scale=1, user-scalable=no" />

/* index.css @layer utilities — fixed top nav + bottom tab bar */
.app-topbar {
  position: fixed; inset: 0 0 auto 0;
  /* content sits under the translucent status bar -> pad it down */
  padding-top: max(env(safe-area-inset-top), 0px);
  height: calc(52px + max(env(safe-area-inset-top), 0px));
  background: color-mix(in srgb, var(--color-bg) 82%, transparent);
  -webkit-backdrop-filter: saturate(160%) blur(20px);
  backdrop-filter: saturate(160%) blur(20px);
}
.app-tabbar {
  position: fixed; inset: auto 0 0 0;
  /* lift the row above the home indicator */
  padding-bottom: max(env(safe-area-inset-bottom), 8px);
  height: calc(56px + max(env(safe-area-inset-bottom), 8px));
}
/* React usage with Tailwind 4 arbitrary values */
// <nav className="app-tabbar flex items-end justify-around">...</nav>
// or inline: <div className="pb-[max(env(safe-area-inset-bottom),8px)]" />
```

### 100dvh / 100svh full-height shell that survives the iOS URL bar
100vh on iOS Safari equals the LARGE viewport (URL bar collapsed), so it overflows by ~the toolbar height when the bar is showing, causing the bottom tab bar to sit off-screen until you scroll. Use the cascade height:100vh -> 100dvh -> 100svh: dvh tracks the dynamic viewport, svh is the smallest (stable) viewport. For a native app feel, do NOT let the body scroll — lock a fixed app shell to 100svh (so the tab bar never shifts when the URL bar animates) and put overflow scrolling on an inner content region only. svh avoids the reflow jank that pure dvh causes while the URL bar slides. Keep the existing html/body/#root { height:100% } as the non-iOS fallback.

```
/* index.css — robust full-height app shell */
.app-shell {
  height: 100vh;        /* legacy fallback */
  height: 100svh;       /* smallest viewport: tab bar never jumps */
  display: flex;
  flex-direction: column;
  overflow: hidden;     /* the SHELL never scrolls */
}
.app-content {
  flex: 1 1 auto;
  min-height: 0;        /* critical: lets the flex child actually scroll */
  overflow-y: auto;
}

/* React layout */
function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="app-shell">
      <header className="app-topbar" />
      <main className="app-content scroll-y">{children}</main>
      <nav className="app-tabbar" />
    </div>
  )
}

/* If you need an exact pixel value in JS for an animated sheet height,
   prefer visualViewport over innerHeight on iOS: */
const vh = () => window.visualViewport?.height ?? window.innerHeight
```

### Momentum scrolling + killing body rubber-band while keeping it in scroll areas
Set overscroll-behavior:none on body to stop the whole-page rubber-band/bounce and pull-to-refresh that breaks the app illusion (already present in this project's body rule). On each inner scroll region use overscroll-behavior-y:contain so its bounce stays local and does not chain to the body, plus -webkit-overflow-scrolling:touch for iOS momentum (kept for older iOS; modern iOS has momentum by default but it is harmless). The project's existing .scroll-y utility already does this — apply it to every scrollable pane. Important caveat: overscroll-behavior on body does NOT prevent the bounce when the page itself is the scroller on iOS < 16; that's another reason to use the fixed app-shell pattern where body has overflow:hidden and only inner panes scroll.

```
/* index.css — already in this repo, this is the canonical setup */
body { overscroll-behavior: none; }   /* no global bounce / pull-to-refresh */

@layer utilities {
  .scroll-y {
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;   /* iOS momentum */
    overscroll-behavior-y: contain;      /* bounce stays inside this pane */
  }
}

/* A modal/bottom-sheet that scrolls internally without scrolling the page behind it */
.sheet-body {
  max-height: 80svh;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;          /* both axes: no scroll-chaining */
}
// <div className="sheet-body">{rows}</div>
```

### Preventing iOS input zoom + disabling tap-highlight / selection / callout for chrome
iOS Safari auto-zooms when you focus an input whose font-size is < 16px. Set font-size:16px (or 1rem with root >=16px) on input/textarea/select — this repo already does it globally. maximum-scale=1, user-scalable=no in the viewport also blocks the zoom but disables user pinch-zoom (an accessibility tradeoff; the 16px rule is the accessible fix and should be the primary mechanism). For app-chrome feel: -webkit-tap-highlight-color:transparent removes the grey tap flash, -webkit-touch-callout:none stops the long-press image/link callout, and user-select:none stops accidental text selection on buttons/labels — then re-enable selection only where users genuinely read/copy content via an opt-in [data-selectable] hook. This is exactly the layered approach already in index.css; keep new interactive elements inside it.

```
/* index.css @layer base — keep inputs at 16px so iOS never zooms */
input, textarea, select, button { font-family: inherit; font-size: 16px; }

/* App-chrome: no tap flash, no long-press callout, no stray selection */
* { -webkit-tap-highlight-color: transparent; -webkit-touch-callout: none; }
body { user-select: none; -webkit-user-select: none; }

/* Opt back IN for real content the user reads/copies */
input, textarea, [data-selectable] { user-select: text; -webkit-user-select: text; }

// Usage: <p data-selectable>{meal.notes}</p>
// Buttons/labels inherit user-select:none automatically -> feel like native controls.
// NOTE: user-scalable=no aids the no-zoom goal but hurts a11y; the 16px rule is the
// real fix, so never rely on the meta alone for accessibility.
```

### Web app manifest fields for installability (standalone, theme/background, maskable icons)
display:standalone (this repo's value) removes browser UI so the launched app has no address bar. theme_color sets the Android status-bar / task-switcher tint and should match the top chrome; background_color paints the splash screen behind the icon while JS boots, so set it to the app background (#050918 here) to avoid a white flash. Provide 192px and 512px PNG icons for Android install + splash, and AT LEAST one icon with purpose:maskable (a 512 with ~20% safe-zone padding) so Android's adaptive icon mask doesn't clip the logo. Add scope and start_url so the standalone app stays inside your origin. Chrome's install criteria require: served over HTTPS, a registered service worker with a fetch handler, name/short_name, a 192 and a 512 icon, and a display of standalone/fullscreen/minimal-ui. This project's vite-plugin-pwa manifest already satisfies all of these.

```
// vite.config.ts -> VitePWA({ manifest: { ... } }) — installable-complete
manifest: {
  name: 'Strakk',
  short_name: 'Strakk',
  description: 'Premium nutrition & weekly check-in coach',
  start_url: '/',
  scope: '/',
  display: 'standalone',          // also try 'minimal-ui' if you want a reload affordance
  orientation: 'portrait',
  background_color: '#050918',    // splash bg == app bg -> no white flash
  theme_color: '#050918',         // Android status bar / task switcher tint
  categories: ['health', 'fitness', 'lifestyle'],
  icons: [
    { src: 'icons/icon-192.png', sizes: '192x192', type: 'image/png', purpose: 'any' },
    { src: 'icons/icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'any' },
    // maskable MUST keep the logo inside the inner ~80% safe zone
    { src: 'icons/maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
  ],
}
// Tip: a separate 'any' icon avoids Android shrinking your real logo to fit the mask.
```

### apple-mobile-web-app-capable + status-bar-style (iOS standalone behavior)
iOS does not read the web manifest's display field for home-screen launch behavior — it reads apple-mobile-web-app-capable=yes (plus the newer cross-browser mobile-web-app-capable=yes) to launch chromeless/standalone. apple-mobile-web-app-status-bar-style controls the status bar: default (black text on the page's own background — no overlay), black (black bar), or black-translucent (the page draws UNDER a transparent status bar — what this repo uses, which is why safe-area-inset-top padding on the top bar is required). apple-mobile-web-app-title sets the home-screen label. apple-touch-icon supplies the iOS home-screen icon (iOS ignores manifest maskable icons). To detect standalone at runtime on iOS use navigator.standalone; cross-browser use the display-mode media query.

```
<!-- index.html <head> — iOS standalone (this repo's setup, annotated) -->
<meta name="apple-mobile-web-app-capable" content="yes" />
<meta name="mobile-web-app-capable" content="yes" />
<!-- black-translucent => content under the status bar => pad with safe-area-inset-top -->
<meta name="apple-mobile-web-app-status-bar-style" content="black-translucent" />
<meta name="apple-mobile-web-app-title" content="Strakk" />
<link rel="apple-touch-icon" href="/icons/icon-192.png" />
<meta name="theme-color" content="#050918" />

// Runtime: are we installed/standalone?
const isStandalone =
  window.matchMedia('(display-mode: standalone)').matches ||
  (navigator as any).standalone === true   // iOS Safari legacy flag
// e.g. only show a custom in-app back/close chrome when isStandalone.
```

### Minimal service worker via vite-plugin-pwa (autoUpdate + offline shell)
Use vite-plugin-pwa's generateSW mode (the default) with registerType:'autoUpdate' so a new build silently activates on next load — no update prompt UI needed. Add workbox.globPatterns so the build precaches the app shell (JS/CSS/HTML/icons) for offline launch, navigateFallback so deep links resolve to index.html in standalone, and cleanupOutdatedCaches to drop stale precaches. registerType:'autoUpdate' auto-injects the registration script, so you don't hand-write one. For runtime data (e.g. an API), add a runtimeCaching rule (NetworkFirst for freshness, CacheFirst for immutable assets). If you outgrow generateSW, switch strategy:'injectManifest' and ship your own src/sw.ts. The repo's current workbox:{} block is empty — fill it as below to actually precache and work offline.

```
// vite.config.ts
import { VitePWA } from 'vite-plugin-pwa'

VitePWA({
  registerType: 'autoUpdate',          // new SW activates on next load, auto-injected
  includeAssets: ['icons/icon-192.png', 'icons/icon-512.png', 'icons/maskable-512.png'],
  manifest: { /* ...as above... */ },
  workbox: {
    globPatterns: ['**/*.{js,css,html,svg,png,woff2}'],  // precache the shell
    navigateFallback: '/',             // SPA deep links resolve offline in standalone
    cleanupOutdatedCaches: true,
    clientsClaim: true,
    skipWaiting: true,
    runtimeCaching: [{
      urlPattern: ({ url }) => url.pathname.startsWith('/api/'),
      handler: 'NetworkFirst',
      options: {
        cacheName: 'api',
        expiration: { maxEntries: 64, maxAgeSeconds: 60 * 60 * 24 },
        cacheableResponse: { statuses: [0, 200] },
      },
    }],
  },
  devOptions: { enabled: true },        // exercise the SW in `vite dev`
})

// Optional explicit React registration (instead of auto-inject):
// import { useRegisterSW } from 'virtual:pwa-register/react'
// const { needRefresh: [need], updateServiceWorker } = useRegisterSW()
```
