# Strakk — Design System

> Deep navy, warm orange accent. Premium nutrition coach, not a gym timer.
> This file is the single source of truth for every UI decision across iOS (SwiftUI) and Android (Jetpack Compose).
> The shipped iOS implementation is canonical. Any future iOS or Android change must align here first.

---

## 1. Visual Theme

**Personality:** A supportive coach. Calm, grounded, confident. Motivates without guilt.
**Aesthetic:** Premium nutrition / lifestyle product. Deep midnight surfaces with a single warm orange accent for action and progress.
**Mode:** Dark only. Optimized for one-handed use, low-light gym/kitchen contexts, and quick glances.
**Feel:** Data-rich without clutter. The user opens it daily because the data is legible and the interactions feel native.

**Reference benchmarks:**
- Revolut: information density done right, calm hierarchy.
- Whoop / Oura: dark calm, restraint with color, focus on numbers.
- Apple Fitness: native iOS controls, generous touch targets, soft haptics.

**Anti-references:**
- Neon gym-bro aesthetic.
- Generic AI-looking dashboards with stacked centered icon cards.
- Glassy gradients and purple-violet color floods.

---

## 2. Colors

All colors live in `iosApp/iosApp/Theme/StrakkColors.swift` and the equivalent Android tokens. Never use hex literals in views.

### Core palette

| Token | Hex | Usage |
|-------|-----|-------|
| `background` | `#050918` | App background, base layer behind every screen |
| `background-elevated` | `#080D1F` | Tab bar background, top nav background, persistent chrome |
| `background-edge` | `#0B1028` | Edge gradients and subtle separation |
| `surface-1` | `#10162F` | Cards, list rows, input fields, default elevated content |
| `surface-1-gradient-top` / `bottom` | `#121833` / `#0C1127` | Optional vertical gradient inside `surface-1` for hero cards |
| `surface-2` | `#151B38` | Hover/press, secondary buttons, button-track for progress |
| `surface-3` | `#1A2142` | Modals, dialogs, floating menus |

### Text

| Token | Hex | Usage |
|-------|-----|-------|
| `text-primary` | `#F4F6FF` | Headings, body, primary numbers |
| `text-secondary` | `#9CA1B8` | Captions, supporting text, inactive labels |
| `text-tertiary` | `#6F748C` | Hints, placeholders, time stamps |
| `text-disabled` | `#50566F` | Disabled controls, very low-emphasis text |

### Accents

| Token | Hex | Usage |
|-------|-----|-------|
| `primary` / `accent-orange` | `#FF7A3D` | CTAs, active progress, brand accent, protein |
| `primary-light` | `#FF9A55` | Hover/secondary highlight, calorie color |
| `accent-orange-glow` | `rgba(#FF7A3D, 0.35)` | Soft glow under primary CTAs (use sparingly) |
| `accent-orange-faint` | `rgba(#FF7A3D, 0.08)` | Tinted backgrounds for primary chips/badges |
| `accent-orange-border` | `rgba(#FF7A3D, 0.18)` | Outlines on tinted primary backgrounds |
| `water` / `accent-blue` | `#4B8DFF` | Hydration only |
| `accent-blue-light` | `#67B7FF` | Hover/highlight on water controls |
| `accent-yellow` | `#FFC84D` | Fat (lipids) — used inside macro cards/charts only |
| `accent-indigo` | `#637CFF` | Carbs (glucides) — used inside macro cards/charts only |
| `success` | `#4DAE6A` | Goal reached, positive deltas |
| `error` | `#E05252` | Destructive actions, validation errors |
| `warning` | `#E0A84D` | Approaching limits, attention needed |

### Borders & dividers

| Token | Hex (rgba) | Usage |
|-------|------------|-------|
| `border-subtle` | `rgba(#7D89BE, 0.25)` | Default outlines on tinted/translucent surfaces |
| `border-faint` | `rgba(#858FBE, 0.18)` | Hairline borders on input fields |
| `divider-strong` | `rgba(#969DC8, 0.22)` | Section separators inside dense surfaces |
| `divider-weak` | `rgba(#FFFFFF, 0.12)` | Minimal separators (rare) |

### Color rules

- The accent (`primary`) is used **sparingly** — CTAs, active state, key progress indicators. Never floods the screen.
- Most surfaces are neutral. Color comes from data, not chrome.
- Depth is communicated by **surface stepping** (`background → surface-1 → surface-2 → surface-3`), not drop shadows.
- `accent-yellow` and `accent-indigo` are **macro-only**. They never appear on toolbars, primary buttons, headers, badges, or general UI.
- `accent-blue` is **water-only**. No other surface uses blue as a primary visual cue.
- **Maximum 2 accent colors per screen** outside of the macro grid. The macro grid is the documented exception.

---

## 3. Typography

### Font

- **iOS:** SF Pro (system default) via SwiftUI `Font.system(...)`. Always with a semantic `TextStyle` to honor Dynamic Type.
- **Android:** System default (Roboto / Google Sans).
- Consistency comes from the weight/size hierarchy, not typeface variety.

### Scale

The Swift implementation lives in `iosApp/iosApp/Theme/StrakkTypography.swift` and uses semantic text styles so all text scales with Dynamic Type.

| Token | Swift mapping | Approx weight | Usage |
|-------|---------------|---------------|-------|
| `display-hero` | `.largeTitle` heavy | 800 | Hero counter (e.g. daily kcal total at top of Today) |
| `display` | `.largeTitle` bold | 700 | Hero numbers, big stats |
| `heading-1` | `.title` bold | 700 | Screen titles |
| `heading-2` | `.title2` bold | 700 | Section headers, large card titles |
| `heading-3` | `.headline` semibold | 600 | List item titles, card titles |
| `body-large` | `.body` medium | 500 | Emphasized body |
| `body` | `.subheadline` regular | 400 | Default body |
| `body-bold` | `.subheadline` semibold | 600 | Inline emphasis (totals, values) |
| `caption` | `.caption` regular | 400 | Secondary info, timestamps |
| `caption-bold` | `.caption` semibold | 600 | Labels, badge content, field labels |
| `overline` | `.caption2` bold + 1pt kerning | 700 | Section overlines (uppercase labels like "NUTRITION") |

### Typography rules

- Numbers use `display` / `display-hero` / `heading-2` with `.monospacedDigit()` to keep figures aligned.
- Use `.contentTransition(.numericText())` on changing numbers (totals, counters).
- Headings are left-aligned. Center alignment is only allowed in empty/loading/error states.
- Body line height: 1.4×. Heading line height: 1.2×.
- Max line width for body copy: ~65 characters.

---

## 4. Spacing & Layout

### Spacing scale (base unit: 4pt)

The Swift enum is `iosApp/iosApp/Theme/StrakkSpacing.swift`. **The Swift token names override the prior 4-step naming.**

| Token | Value | Usage |
|-------|-------|-------|
| `xxs` | 4pt | Icon-to-label, micro gaps |
| `xs` | 8pt | Inside compact components |
| `sm` | 12pt | Default inner padding for tight cards/badges |
| `md` | 16pt | Default card inner padding, between related items |
| `lg` | 20pt | **Screen horizontal margin**, between sub-sections |
| `xl` | 24pt | Between sections |
| `xxl` | 32pt | Screen top/bottom padding, major separations |
| `xxxl` | 40pt | Empty-state vertical breathing room |

### Layout rules

- **Screen margins:** `lg` (20pt) horizontal.
- **Card padding:** `md` (16pt) default; `sm` (12pt) for compact rows.
- **Section spacing:** `xl` (24pt) between sections.
- **Single-column layout** as a rule. Side-by-side only for the macro summary grid.
- **Macro summary grid:** 2×2, equal width, equal height.

### Corner radius

The Swift enum is `iosApp/iosApp/Theme/StrakkRadius.swift`. The implementation runs on a deeper radius scale than the prior reference.

| Token | Value | Usage |
|-------|-------|-------|
| `sm` | 12pt | Cards, inputs, default buttons |
| `md` | 18pt | Modal/sheet inner cards, primary CTAs in marketing surfaces |
| `lg` | 24pt | Bottom sheet outer corners |
| `xl` | 32pt | Hero/feature surfaces |
| `xxl` | 36pt | Welcome / paywall hero containers |
| `xxxl` | 56pt | Decorative/hero badges only |
| `full` | `.capsule` | Pills, chips, FABs, progress bars |

### Elevation through surface stepping

Depth is communicated by background color stepping, not drop shadows:

| Level | Surface | Usage |
|-------|---------|-------|
| Base | `background` | Screen background |
| L1 | `surface-1` | Cards, list rows |
| L2 | `surface-2` | Pressed state, secondary buttons, progress track |
| L3 | `surface-3` | Modals, popovers |

Drop shadows are minimal to none. If used, only on Liquid Glass / floating bars.

---

## 5. Components

### Buttons

The Swift implementations live in `iosApp/iosApp/Theme/StrakkButton.swift`.

**Primary (CTA) — `StrakkPrimaryButton`:**
- Background: `primary`.
- Text: `#FFFFFF`, `body-bold`.
- Radius: `sm` (12pt).
- Height: 52pt (large touch target).
- Disabled background: `surface-2`.
- Disabled text: `text-tertiary`.
- Press: scale 0.97 + slight darken, ~100ms.
- Full width on forms; fitted on toolbars.

**Secondary — `StrakkSecondaryButton`:**
- Background: `surface-2`.
- Text: `text-primary`, `body-bold`.
- Same radius and height as primary.
- Press: scale 0.97 + lift to `surface-3`.

**Destructive — `StrakkDestructiveButton`:**
- Background: `error`.
- Text: `#FFFFFF`, `body-bold`.
- Used only for destructive confirmations.

**Text/link — `StrakkTextButton`:**
- No background.
- Text: `primary`, `body-bold` for emphasis or `body` for de-emphasis.
- Used for "Skip", "See all", inline links inside text blocks.

**Close — `StrakkCloseButton`:**
- SF Symbol `xmark` (plain, not filled).
- Tint: `text-secondary`.
- Tap target: 44pt minimum (iOS HIG).
- Accessibility label localized as `"Close"`.

**Pro badge — `ProBadge`:**
- Pill, `accent-orange-faint` background, `primary` text.
- Used to mark gated capabilities.

### Cards

- Background: `surface-1`.
- Radius: `sm` (12pt).
- Padding: `md` (16pt).
- No border, no shadow — the surface contrast separates from the background.
- Content: left-aligned.

### Input fields

- Background: `surface-1`.
- Text: `text-primary`. Placeholder: `text-tertiary`.
- Border: 1pt `border-faint`. On error: `error`. On focus: `primary`.
- Radius: `sm` (12pt).
- Height: 48pt.
- Label above the field, `caption-bold` `text-secondary`.

### Sheets

The Swift sheet pattern lives in `iosApp/iosApp/Theme/StrakkSheet.swift`.

- **Default close pattern:** leading `StrakkCloseButton` (plain `xmark`) with localized accessibility label.
- **Title:** centered inline `navigationTitle`.
- **Background:** `background`.
- **Drag indicator:** visible.
- **Detents:**
  - Quick choice / picker: `[.medium]`.
  - Focused form: `[.large]`.
  - Detail view (read-only): `[.medium, .large]`.

**Allowed exceptions** (must be deliberate):
- Camera/scanner overlays use a custom floating close inside the camera UI.
- Paywall/marketing full-screen surfaces may use a circular `xmark` floating top-right because the layout is brochure-like.

### Tab bar

- Liquid Glass on iOS 26+. Standard `TabView` otherwise.
- Active tint: `primary`. Inactive tint: `text-secondary`.
- Background: `background-elevated`.

### Progress indicators

- **Linear progress bar:** height 6pt, `Capsule` shape, track `surface-2`, fill macro/accent color.
- **Goal reached/exceeded:** fill switches to `success`. Subtle 250ms ease-out transition.
- **Progress ring:** stroke 6pt, track `surface-2`, fill `primary`. `success` on goal reached.

### Macro card

The Swift implementation lives in `iosApp/iosApp/Theme/MacroCard.swift`.

- Layout: icon-tile (28×28, tinted background) + label + big value + progress bar.
- Used in **both** Today and Calendar Day to keep nutrition language consistent.
- Always shows the bar. When no goal exists, the bar fills to 0 and the unit appears without `/ goal`.
- Goal reached → bar fill is `success`.
- Macro accent palette inside the card:
  - Protein → `primary`.
  - Calories → `primary-light`.
  - Fat → `accent-yellow`.
  - Carbs → `accent-indigo`.

### Lists

- Item height: minimum 56pt (touch target).
- Divider: `divider-weak`, inset.
- Swipe actions: destructive uses `error` background.

### Empty / loading / error states

- Empty state component centered, with a calm illustration or SF Symbol, `heading-3` title, `caption` description, optional CTA.
- Loading is either a centered `ProgressView` tinted `primary`, or a skeleton no longer than 2 seconds.
- Errors surface user-safe messages only. **No raw HTTP statuses, stack traces, or developer details** in UI text.

---

## 6. Interaction & Motion

### Principles

- **Purposeful, not decorative.**
- **Quick.** 100–300ms typical. Never above 400ms.
- **Subtle.** The user feels polish; they shouldn't notice individual animations.

### Specific animations

| Interaction | Animation | Duration |
|-------------|-----------|----------|
| Button press | Scale 0.97 + slight darken | 100ms |
| Card tap | Brief `surface-2` flash | 150ms |
| Number change | `.contentTransition(.numericText())` | system default |
| Progress fill | Ease-out smooth fill | 250ms |
| Goal reached | Fill color shift to `success` + light haptic | 300ms |
| Sheet present/dismiss | Native iOS modal | system default |
| Pull to refresh | `primary`-tinted indicator | system default |

### Haptics

- Button press: `.light` impact.
- Goal reached: `.success` notification.
- Destructive confirm: `.medium` impact.
- Errors: `.error` notification.

### Anti-patterns

- No confetti, fireworks, particle effects.
- No bouncing/spring on data values.
- No skeleton shimmer over 2s.
- No transitions over 400ms.

---

## 7. Localization & Copy

- **Source language: English.** All Swift `Text(...)`, `navigationTitle(...)`, `accessibilityLabel(...)`, alert titles, and button labels are written in English.
- French translations live in `iosApp/iosApp/Localizable.xcstrings` under the `fr` locale.
- Programmatic strings use `String(localized: "key")`.
- Android equivalent: `androidApp/src/androidMain/res/values/strings.xml` with `values-fr/strings.xml` mirroring keys.
- Never embed French (or any non-English) string literals directly in Swift or Kotlin source.
- User-facing error copy is friendly, never technical. The repository layer is responsible for mapping infra errors to safe domain messages before they reach the UI.

---

## 8. Platform Adaptation

### iOS (SwiftUI)

- SF Symbols for every icon. No bundled icon assets when an SF Symbol exists.
- Honor Dynamic Type.
- Use `NavigationStack`, never `NavigationView`.
- Use `.sheet(item:)` over `.sheet(isPresented:)` whenever data drives the presentation.
- Use `.task { ... }` for async work, not `.onAppear { Task { ... } }`.
- iOS 26+: use Liquid Glass (`.glassEffect`) on the tab bar and floating overlays only. Gate behind `if #available(iOS 26, *)`.
- Native haptics via `UIImpactFeedbackGenerator` / `UINotificationFeedbackGenerator`.

### Android (Jetpack Compose)

- Material Icons (outlined variant).
- Material 3 `ColorScheme` mapped from this palette.
- Edge-to-edge with `enableEdgeToEdge()`.
- `HapticFeedbackType` for haptics.

### Shared rules

- Same information architecture and copy across platforms.
- Different visual language inside the same design system.
- Platform-native navigation patterns. Don't force iOS patterns on Android or vice versa.

---

## 9. Do's & Don'ts

### Do

- Use surface stepping for depth, not shadows.
- Use the accent sparingly: CTAs, active state, key progress.
- Leave generous whitespace.
- Make data the hero — big numbers, clear labels.
- Localize every user-facing string.
- Reuse the shared button, close, and macro-card primitives.
- Map infra errors to user-safe copy in the repository/use case layer.

### Don't

- Don't write hex literals in views. Use `Color.strakk*` tokens.
- Don't use more than 2 accent colors on one screen outside the macro grid.
- Don't put text on `primary` background unless it's a button.
- Don't use thin font weights (below 400) — gym lighting friendly.
- Don't center-align body text or lists outside empty/loading/error states.
- Don't add decorative chrome that doesn't communicate information.
- Don't expose backend/HTTP details in UI strings.
- Don't write French (or any non-English) literals in Swift / Kotlin source.
- Don't reinvent close/cancel/CTA chrome per screen — use the shared primitives.
