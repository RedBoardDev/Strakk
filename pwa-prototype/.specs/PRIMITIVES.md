# Strakk PWA — Primitives & build contract (READ FIRST)

You are implementing ONE screen file for a premium-iOS-feel PWA mockup of Strakk
(dark navy `#050918` + warm orange `#FF7A3D`). Match the iOS app's look and feel.
Use ONLY the primitives, tokens, mock data, and icons listed here. Do NOT add npm
deps. Mock data only — no backend.

## Hard TypeScript / build rules (the build WILL fail otherwise)
- Strict mode + `noUnusedLocals` + `noUnusedParameters`: never leave unused imports/vars.
- `verbatimModuleSyntax`: type-only imports MUST use `import type { X } from '...'`.
- `allowImportingTsExtensions`: EVERY local import MUST include the file extension,
  e.g. `import { Card } from '../components/Card.tsx'`, `import { goals } from '../data/mock.ts'`.
- React 19 automatic JSX runtime — do NOT `import React`. Import hooks by name: `import { useState } from 'react'`.
- Keep it self-contained: one exported component per file, no edits to other files.

## Design tokens (Tailwind utility classes — already configured)
Colors → `bg-*`, `text-*`, `border-*`:
- Surfaces: `bg-bg` `#050918`, `bg-bg-elevated`, `bg-surface-1` `#10162F`, `bg-surface-2` `#151B38`, `bg-surface-3` `#1A2142`
- Text: `text-ink` (primary `#F4F6FF`), `text-ink-2` (`#9CA1B8`), `text-ink-3` (`#6F748C`), `text-ink-4` (`#50566F`)
- Accents: `text-primary`/`bg-primary` `#FF7A3D`, `text-primary-light`, `text-water` `#4B8DFF`, `text-fat` `#FFC84D`, `text-carbs` `#637CFF`, `text-success` `#4DAE6A`, `text-error` `#E05252`, `text-warning`
- Tinted fills: `bg-primary-faint`, `bg-water-faint`, `bg-fat-faint`, `bg-carbs-faint`
- Borders/dividers: `border-hair`, `border-line`, `divide-divider`
Radius: `rounded-card` (12px), `rounded-modal` (18), `rounded-sheet` (24), `rounded-hero` (32), `rounded-full`.
Spacing: Tailwind default 4px scale. Screen horizontal margin = `px-5` (20px). Card padding = `p-4` (16). Section gap = `pt-6`/`pt-8`.
Type sizes (px): title 28 (`text-[28px] font-bold`), heading2 22, heading3 17 (`text-[17px] font-semibold`), body 15 (`text-[15px]`), caption 12 (`text-[12px]`), overline 11 uppercase. Numbers: add `tnum` class for tabular figures.
Utilities: `scroll-y no-scrollbar` (momentum scroll region), `pt-safe`/`pb-safe` (safe area), `glow-primary` (soft orange CTA glow).

## Icons — `import { Icon } from '../components/Icon.tsx'`
`<Icon name="flame.fill" size={16} className="text-primary" />`. Available `name` values:
`house.fill` `calendar` `chart.bar` `gearshape.fill` `flame.fill` `dumbbell.fill` `drop.fill` `drops` `leaf.fill` `wheat` `plus` `barcode` `camera` `search` `xmark` `chevron.right` `chevron.left` `chevron.down` `chevron.up` `pencil` `trash` `check` `sparkles` `crown` `clock` `minus` `egg` `bell` `apple` `fork` `note` `photo` `trend.up` `trend.down` `ruler` `scale` `heart` `back` `info` `logout` `globe` `star`.
(Color an icon with `className="text-primary"` or `color="#FF7A3D"`.)

## Components (import paths relative to `src/`)
- `components/ScreenScroll.tsx` → `<ScreenScroll title="Calendar" trailing={<...>}>children</ScreenScroll>`
  iOS large-title screen with collapsing nav + safe areas. USE THIS as the root of every TAB screen.
  Content is already `px-5` padded with bottom clearance. Don't add your own outer scroll.
- `components/Sheet.tsx` → `<Sheet open={boolean} onClose={fn} title="..." detents={['medium']|['large']|['medium','large']} footer={<...>}>children</Sheet>`
  Draggable bottom sheet (drag-to-dismiss, detents). Content area is `px-5` and scrolls. USE THIS as the root of every SHEET.
- `components/Card.tsx` → `<Card onClick? padding="p-4">…</Card>` surface-1 card, radius 12, tap flash.
- `components/Button.tsx` → `<Button variant="primary|secondary|destructive|text" full glow onClick>Label</Button>` (52px tall).
- `components/MacroCard.tsx` → `<MacroGrid protein={{consumed,goal}} calories={…} fat={…} carbs={…} />` and `<MacroCard macro="protein|calories|fat|carbs" consumed={n} goal={n} />`.
- `components/Ring.tsx` → `<Ring progress={0..1} size={168} stroke={12} reached?>centerChildren</Ring>` animated SVG progress ring.
- `components/ProgressBar.tsx` → `<ProgressBar value={0..1} color="#FF7A3D" reached? />`.
- `components/AnimatedNumber.tsx` → `<AnimatedNumber value={n} format={(x)=>...} className="tnum" />` count-up on change.
- `components/Segmented.tsx` → `<Segmented options={[{value,label}]} value onChange />` iOS segmented control.
- `components/Stepper.tsx` → `<Stepper value onChange step min suffix />` portion stepper.
- `components/Row.tsx` → `<RowGroup>` wraps `<Row icon? iconColor? title subtitle? trailing? chevron? onClick? />` for iOS grouped lists.
- `lib/ios.ts` → `haptic('light'|'medium'|'success'|'error')`, `spring.{sheet,snappy,page,gentle}`, `pressable`.
  Use `motion` from `'motion/react'` for animations (`import { motion } from 'motion/react'`).

## Navigation — `import { useNav } from '../nav.ts'`
`const nav = useNav()` → `nav.open(flow)`, `nav.close()`, `nav.setTab(tab)`.
`Flow` kinds: `{kind:'add'}` `{kind:'search'}` `{kind:'scan'}` `{kind:'foodDetail',food}` `{kind:'mealDetail',meal}` `{kind:'paywall'}`.
Sheet components receive `{ open, onClose }` (+ `food`/`meal` for detail sheets) as props from App — do not call useNav for your own open/close, use the props.

## Mock data — `import { ... } from '../data/mock.ts'` (or `'../../data/mock.ts'` from sheets/)
`goals` (Macros+water), `consumed` (Macros), `meals` (MealEntry[]), `water`, `recentFoods`/`searchResults`/`scannedProduct` (Food[]), `calendarDays` (DayLog[]), `checkIns` (CheckIn[]), `weightTrend` (number[]).
Types: `Macros`, `MealEntry`, `Food`, `DayLog`, `CheckIn`, `MealType`. Import types with `import type`.

## Reference implementation
`src/screens/TodayScreen.tsx` is the canonical example of quality, motion, haptics, and token usage. Mirror its level of polish.
