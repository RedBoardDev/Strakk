# Strakk — Design System

> Warm dark, friendly, clean. Revolut-level quality, Duolingo-level approachability.
> This file is the single source of truth for all UI decisions across iOS (SwiftUI) and Android (Jetpack Compose).

---

## 1. Visual Theme

**Personality:** A supportive coach, not a drill sergeant. The app motivates without guilt-tripping.
**Aesthetic:** Warm, grounded, confident. High-end sportswear brand, not a gaming app.
**Mode:** Dark primary (gym context — low light, reduces eye strain).
**Feel:** Premium but approachable. Data-rich without being overwhelming. You open it with pleasure.

**Key references:**
- Revolut: information density done right, clean data presentation, subtle depth
- Duolingo dark mode: warm dark background, friendly feel, makes you want to come back
- NOT: neon gym bro aesthetic, cold tech blue, cluttered dashboards

---

## 2. Colors

### Core Palette

| Token | Hex | Usage |
|-------|-----|-------|
| `background` | `#151720` | App background, base layer |
| `surface-1` | `#1C1D2B` | Cards, input fields, bottom sheets |
| `surface-2` | `#242536` | Elevated cards, dropdown menus, popovers |
| `surface-3` | `#2C2D40` | Modals, overlays, floating elements |
| `primary` | `#E07C4F` | CTAs, active states, progress indicators, selected tabs |
| `primary-light` | `#F0A868` | Badges, highlights, secondary buttons, hover/press states |
| `text-primary` | `#F0F0F5` | Headings, body text, primary labels |
| `text-secondary` | `#9898AC` | Captions, placeholders, inactive labels |
| `text-tertiary` | `#6B6B80` | Disabled text, hints |
| `divider` | `#2C2D40` | Separators, borders (subtle, not prominent) |

### Semantic Colors

| Token | Hex | Usage |
|-------|-----|-------|
| `success` | `#4DAE6A` | Goals reached, session completed, PRs |
| `error` | `#E05252` | Destructive actions, validation errors |
| `warning` | `#E0A84D` | Approaching limits, attention needed |
| `water` | `#5B9BD5` | Water tracking (the only blue — contextually meaningful) |
| `protein` | `#E07C4F` | Protein tracking (uses primary — the hero metric) |
| `calories` | `#F0A868` | Calorie tracking (uses primary-light) |

### Color Rules

- The primary accent (`#E07C4F`) is used **sparingly** — CTAs, active states, key progress indicators. NOT on every element.
- Most of the UI is neutral (surfaces + text). The accent is a warm pop, not a flood.
- Depth is created through **surface layers** (surface-1 → 2 → 3), not heavy shadows.
- Borders are rare and subtle (`divider` color). Prefer spacing and surface contrast to separate elements.
- iOS: adapt to system semantic colors where possible, use these tokens for custom components.
- Android: define a Material 3 `ColorScheme` from these tokens. Support dynamic color as an option but default to this palette.

---

## 3. Typography

### Font

- **iOS:** SF Pro (system default) — do NOT use custom fonts
- **Android:** System default (Roboto / Google Sans) — do NOT use custom fonts
- Consistency comes from **weight and size hierarchy**, not typeface variety.

### Scale

| Token | Size | Weight | Usage |
|-------|------|--------|-------|
| `display` | 32pt | Bold (700) | Hero numbers (daily protein total, timer) |
| `heading-1` | 24pt | Semibold (600) | Screen titles |
| `heading-2` | 20pt | Semibold (600) | Section headers |
| `heading-3` | 17pt | Medium (500) | Card titles, list item titles |
| `body` | 15pt | Regular (400) | Body text, descriptions |
| `body-bold` | 15pt | Semibold (600) | Emphasized body text |
| `caption` | 13pt | Regular (400) | Secondary info, timestamps |
| `caption-bold` | 13pt | Medium (500) | Labels, badges |
| `overline` | 11pt | Semibold (600) | Section labels, categories (uppercase tracking) |

### Typography Rules

- **Numbers and data** use `display` or `heading-1` with tabular figures (monospaced numbers for alignment).
- Headlines are **left-aligned** (no center alignment except empty states).
- Line height: 1.4× for body text, 1.2× for headings.
- Max line width: ~65 characters for readability.

---

## 4. Spacing & Layout

### Spacing Scale (base unit: 4pt)

| Token | Value | Usage |
|-------|-------|-------|
| `space-xs` | 4pt | Tight gaps (icon to label) |
| `space-sm` | 8pt | Inside compact components |
| `space-md` | 12pt | Default inner padding |
| `space-lg` | 16pt | Card inner padding, between related items |
| `space-xl` | 24pt | Between sections |
| `space-2xl` | 32pt | Screen top/bottom padding, major separations |

### Layout Rules

- **Screen margins:** 20pt horizontal
- **Card padding:** 16pt all sides
- **Section spacing:** 24pt between sections
- **Generous whitespace** — the app should breathe. When in doubt, add more space.
- **Single column** layout — no side-by-side cards except summary stats (protein / calories / water).
- Summary stat cards: 3 across, equal width, same height.

### Corner Radius

| Token | Value | Usage |
|-------|-------|-------|
| `radius-sm` | 8pt | Small elements (badges, chips, tags) |
| `radius-md` | 12pt | Cards, input fields, buttons |
| `radius-lg` | 16pt | Bottom sheets, modals |
| `radius-full` | 9999pt | Circular elements (avatars, FABs, progress rings) |

### Elevation (via surface layers, not shadows)

Depth is communicated through **background color stepping**, not drop shadows:

| Level | Surface | Usage |
|-------|---------|-------|
| Base | `background` (#151720) | Screen background |
| Level 1 | `surface-1` (#1C1D2B) | Cards, main content containers |
| Level 2 | `surface-2` (#242536) | Menus, dropdowns, elevated cards |
| Level 3 | `surface-3` (#2C2D40) | Modals, dialog overlays |

Shadows: **minimal to none**. If used, very subtle (2pt blur, 10% opacity black). The surface color IS the elevation.

---

## 5. Components

### Buttons

**Primary (CTA):**
- Background: `primary` (#E07C4F)
- Text: `#FFFFFF` bold
- Radius: `radius-md` (12pt)
- Height: 52pt (large touch target — gym context, sweaty hands)
- Full width on forms, fitted on toolbars
- Press state: darken 10%

**Secondary:**
- Background: `surface-2` (#242536)
- Text: `text-primary` (#F0F0F5)
- Border: none (surface contrast is enough)
- Same radius and height as primary

**Text/Link:**
- No background
- Text: `primary` (#E07C4F)
- Used for "Cancel", "Skip", "See all"

**Destructive:**
- Background: `error` (#E05252)
- Text: `#FFFFFF` bold
- Used only for delete confirmations

### Cards

- Background: `surface-1`
- Radius: `radius-md` (12pt)
- Padding: `space-lg` (16pt)
- No border, no shadow — the surface contrast separates from background
- Content: left-aligned, consistent internal spacing

### Input Fields

- Background: `surface-1`
- Text: `text-primary`
- Placeholder: `text-tertiary`
- Border: 1pt `divider` color, changes to `primary` on focus
- Radius: `radius-md`
- Height: 48pt
- Label above (not floating)

### Bottom Sheets

- Background: `surface-2`
- Radius: `radius-lg` (16pt) top corners only
- Drag indicator: `text-tertiary`, 36pt wide, 4pt tall, centered
- Content padding: `space-lg`

### Tab Bar

- Background: `surface-1`
- Active icon + label: `primary`
- Inactive icon + label: `text-secondary`
- 5 tabs: Today, Sessions, Calendar, Check-ins, Settings
- iOS: respect Liquid Glass when running iOS 26+
- Android: Material 3 NavigationBar component

### Progress Indicators

- **Progress bar:** height 8pt, radius full, track = `surface-2`, fill = `primary`
- **Progress ring:** stroke 6pt, track = `surface-2`, fill = `primary`, animated fill
- **Goal achieved:** fill changes to `success`, subtle pulse animation

### Lists

- Item height: min 56pt (touch target)
- Divider: `divider` color, inset (not full-width)
- Swipe actions: background colored (error for delete, surface-2 for archive)
- Reorderable items: drag handle icon in `text-tertiary`

---

## 6. Interaction & Motion

### Principles

- **Purposeful, not decorative.** Every animation communicates something (state change, feedback, progress).
- **Quick and responsive.** Durations: 150-300ms max. No slow fades.
- **Subtle.** The user should feel the polish, not notice the animations.

### Specific Animations

| Interaction | Animation | Duration |
|-------------|-----------|----------|
| Button press | Scale down to 0.97 + darken | 100ms |
| Card tap | Brief highlight (surface-2 flash) | 150ms |
| Progress update | Smooth fill with ease-out | 300ms |
| Goal reached | Progress ring completes + color shift to success + haptic | 400ms |
| Screen transition | iOS native push/pop, Android shared element | Platform default |
| Pull to refresh | Custom indicator in `primary` color | Platform default |
| Swipe to delete | Slide reveal + red background | 200ms |
| Water quick-add | Ripple from button + counter increment | 200ms |

### Haptic Feedback

- Button press: light impact
- Goal reached: success notification
- Swipe action: medium impact
- Timer complete: heavy impact + sound

### Anti-Patterns (Motion)

- NO confetti, fireworks, particle effects
- NO bouncing/spring animations on data elements
- NO loading skeleton shimmer that lasts more than 2 seconds
- NO auto-playing animations that loop (except active timer)
- NO transitions slower than 400ms

---

## 7. Gym Context UX

### Physical Constraints

- **Large touch targets:** minimum 48pt (sweaty fingers, gloves, one-handed use)
- **One-handed operation:** key actions reachable with thumb (bottom half of screen)
- **Glanceable data:** during a set, the user glances at their phone for 1-2 seconds. Key info (next set, rest timer, weight) must be instantly readable.
- **Minimal text input:** prefer taps, steppers, pickers over keyboards
- **Dark mode:** reduces eye strain in dim gym lighting

### Screen Priorities

- **Today screen:** quick-start workout + daily stats. Not a dashboard — a launchpad.
- **Workout mode:** full-screen focus. One exercise at a time. Big numbers. Timer always visible.
- **Nutrition log:** quick add is key. Scan photo = 1 tap. Add water = 1 tap. Manual entry = minimal fields.
- **Check-in:** weekly ritual, not daily. Low friction, photo-first.

---

## 8. Platform Adaptation

### iOS (SwiftUI)

- Use SF Symbols for all icons
- Respect system font scaling (Dynamic Type)
- iOS 26+: use Liquid Glass (`glassEffect`) on tab bar and floating toolbars
- Native navigation transitions (push/pop)
- `.sheet()` for bottom sheets (data-driven)
- System haptics via UIFeedbackGenerator

### Android (Jetpack Compose)

- Use Material Icons (outlined variant)
- Respect system font scaling
- Material 3 `NavigationBar` for tabs
- Edge-to-edge with `enableEdgeToEdge()`
- Material 3 color tokens mapped from this palette
- `HapticFeedbackType` for haptics

### Shared Rules (Both Platforms)

- Same information architecture (same screens, same data, same flow)
- Different visual language (Apple feel vs Material feel)
- Platform-native navigation patterns (don't force iOS patterns on Android or vice versa)
- Same spacing and sizing tokens (adapted to platform density if needed)

---

## 9. Do's and Don'ts

### Do

- Use surface layers for depth (not shadows)
- Keep the accent (`primary`) for key actions and data
- Leave generous whitespace
- Make data the hero (big numbers, clear labels)
- Design for thumb reach (actions at bottom)
- Test in actual gym lighting conditions (dim)
- Use platform-native components when possible

### Don't

- Don't use more than 2 accent colors on one screen
- Don't put text on `primary` background unless it's a button
- Don't use thin fonts (below weight 400) — hard to read in gym lighting
- Don't center-align body text or lists
- Don't add decorative elements that don't communicate information
- Don't use colored backgrounds for cards (always `surface-1/2/3`)
- Don't make the app feel like a generic LLM-generated UI — every detail should feel intentional
