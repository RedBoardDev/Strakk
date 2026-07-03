# Spec: Today (Home / Hero Dashboard)

## Purpose
The hero home screen: shows today's nutrition at a glance — greeting/date header, 2x2 macro progress grid, compact water tracker, chronological meal/food timeline, and persistent bottom action bar (or an orange floating draft bar when a meal draft is in progress).

## Layout
ROOT: NavigationStack over a full-bleed ZStack. Background layer = solid strakkBackground #050918 (ignores safe area, fills entire viewport).

LOADING STATE: centered spinner only — SwiftUI ProgressView tinted strakkPrimary #FF7A3D, vertically + horizontally centered, nothing else on screen.

READY STATE: a vertically scrolling ScrollView whose content is a single VStack(alignment: .leading, spacing: 0). The bottom action bar is NOT inside the scroll — it is pinned to the bottom safe-area inset (position: sticky bottom on web). Top-to-bottom inside the scroll VStack:

1) HEADER ROW (HStack), padding: top 16px, horizontal 20px.
   - LEFT: Title "Today" — font strakkHeading1 (28px bold), color strakkTextPrimary #F4F6FF.
   - Spacer (pushes rest right).
   - Date label, e.g. "Mon, Jun 30" — font strakkBody (15px regular), color strakkTextSecondary #9CA1B8.
   - RIGHT: icon button, SF Symbol "dumbbell.fill" 18px, color strakkTextSecondary, inside a 44x44 tap target (Hevy/training export). On web use an equivalent dumbbell glyph 18px in a 44x44 hit area.

2) TRIAL BANNER (conditional — only when a Pro trial is active/expiring). Below header: padding top 12px, horizontal 20px. A full-width tappable card: HStack spacing 12px (StrakkSpacing.sm), inner padding horizontal 16px (md) / vertical 12px (sm), background strakkSurface1 #10162F, corner radius 12px (StrakkRadius.sm). Contents left→right: SF "clock.fill" 15px colored strakkWarning #E0A84D; text "Your Pro trial expires in N days" font strakkBody color strakkTextPrimary; Spacer; chevron.right 12px medium color strakkTextSecondary.

3) MACRO GRID (ProgressSection -> MacroProgressGrid). padding top 20px, horizontal 20px. A 2-column LazyVGrid, both columns flexible/equal width, inter-item spacing 12px (sm) both axes. Four MacroCard cells in fixed order: [Protein] [Calories] / [Fat] [Carbs]. See MacroCard component spec.

4) WATER ROW. padding top 20px, horizontal 20px. Single compact card: HStack spacing 12px, inner padding horizontal 16px / vertical 12px, background strakkSurface1, radius 12px. Left→right: (a) 36x36 rounded-rect (radius 8) filled strakkSurface3 #1A2142 with a centered "drop.fill" 15px semibold colored strakkWater #4B8DFF; (b) header text "1.5 L / 2.5 L" font 18px bold, monospaced digits, numericText content transition, color strakkTextPrimary; (c) Spacer; (d) three 40x40 icon buttons radius 10, gap 12px: MINUS button (bg strakkSurface3, fg strakkTextPrimary when enabled else strakkTextTertiary #6F748C, disabled when total water = 0), PLUS button (bg strakkWater @18% opacity, fg strakkWater), SLIDER button "slider.horizontal.3" (bg strakkSurface3, fg strakkTextPrimary) opening the custom-amount sheet. All icons 16px bold.

5) TIMELINE. padding top 32px, horizontal 20px. If empty -> EMPTY STATE (see component). If non-empty -> LazyVStack spacing 6px of timeline rows, each a full-width card (strakkSurface1, radius 12). Two row variants: mealContainerRow and orphanEntryRow (see components). Rows are ordered chronologically as provided by data (typically newest first by createdAt).

6) BOTTOM SPACER: fixed-height 120px transparent spacer so the last timeline row clears the pinned action bar.

PINNED BOTTOM BAR (safeAreaInset .bottom, NOT scrolling): two mutually-exclusive variants —
   A) DEFAULT — stickyActionButtons: HStack spacing 12px, padding horizontal 20px, top 4px, bottom 8px, background = strakkBackground @95% opacity (slight scrim over scrolling content). Two equal-width buttons, each height 56px, radius 14px: LEFT "Meal" button (SF "fork.knife" 16px semibold + label "Meal" strakkBodyBold, fg strakkTextPrimary, bg strakkSurface2 #151B38); RIGHT "Quick" button (SF "bolt.fill" 16px semibold + label "Quick" strakkBodyBold, fg white, bg strakkPrimary #FF7A3D).
   B) DRAFT ACTIVE — floatingDraftBar: a single orange (strakkPrimary) pill card, radius 16px, inner padding horizontal 16 / vertical 12, outer padding horizontal 16 / bottom 8, drop shadow rgba(0,0,0,0.15) blur 8 y -2. Contents: LEFT tappable VStack (meal draft name in strakkBodyBold white; optional subtitle in strakkCaption white@75% e.g. "2 items · 1 pending · 540 kcal"); Spacer; "+ Add" capsule button (strakkCaptionBold white, padding h12/v8, bg white@20%); a final capsule that is "Finish" (white bg, text strakkPrimary) when the draft has items, or "Cancel" (white@20% bg, white text) when empty.

## Components
- **MacroCard**: Single macro stat cell. Container: VStack(align leading, spacing 8/xs), inner padding 16px (md) all sides, bg strakkSurface1 #10162F, radius 12px continuous. Row 1 (HStack spacing 8): a 28x28 rounded-rect (radius 7 continuous) filled with the macro color at 12% opacity, centered SF icon 13px semibold in the macro color; then label text font strakkCaption (12px) color strakkTextSecondary, 1 line. Row 2 (HStack lastTextBaseline, spacing 2): big value Int(consumed) font strakkHeading2 (22px bold) color strakkTextPrimary, monospaced digits, numericText transition; then EITHER '/ {goal}{unit}' font strakkCaption color strakkTextTertiary #6F748C when goal set, OR just the unit when no goal. Row 3: progress bar — full-width track Capsule height 4px filled strakkSurface2 #151B38, overlaid left-aligned fill Capsule height 4px width = clamp(consumed/goal,0..1) * trackWidth; fill color = macro color normally, switches to strakkSuccess green #4DAE6A once consumed>=goal; animate width with ease-out 0.25s. The four instances: Protein(icon dumbbell.fill, color strakkPrimary #FF7A3D, unit g), Calories(icon flame.fill, color strakkPrimaryLight #FF9A55, unit kcal), Fat(icon drop.fill, color strakkAccentYellow #FFC84D, unit g), Carbs(icon leaf.fill, color strakkAccentIndigo #637CFF, unit g).
- **WaterRow**: Compact one-line water tracker card (see layout item 4). Header text format: '{total} L / {goal} L' both to 1 decimal, e.g. '1.5 L / 2.5 L'; if no goal, just '{total} L'. Minus button disabled+greyed when total water is 0. Default add/remove step is 250 mL.
- **WaterIconButton**: 40x40 square button, radius 10, centered SF icon 16px bold. Params: systemName, background color, foreground color, enabled flag. Disabled state lowers opacity / blocks tap.
- **WaterCustomAmountSheet**: Bottom sheet, detent height 200px, drag indicator visible, background strakkSurface1. VStack spacing 28: title 'Custom amount' (strakkBodyBold, strakkTextPrimary, top pad 24); stepper HStack spacing 28 — 'minus.circle.fill' 38px (strakkSurface2, greys to strakkSurface3 + disabled at min 50 mL), center '{amount} mL' 28px bold monospaced numericText minWidth 110 centered, 'plus.circle.fill' 38px (strakkWater, greys to strakkSurface3 + disabled at max 2000 mL); actions HStack (h-pad 24): 'Cancel' (strakkBody, strakkTextSecondary, plain), Spacer, optional 'Remove' capsule (shown only when water>0; strakkBodyBold strakkTextSecondary, bg strakkSurface2), 'Add' capsule (strakkBodyBold white, bg strakkWater). Step 50 mL, min 50, max 2000, default 250.
- **MealContainerRow**: Timeline card for a meal that groups multiple food entries. Full-width Button, bg strakkSurface1, radius 12, inner padding h14/v12. HStack spacing 10: time label 'HH:mm' (strakkCaptionBold 12px semibold, color strakkTextTertiary, fixed width 44 left-aligned); SF 'fork.knife' 13px color strakkTextSecondary in 16px-wide slot; VStack(spacing 2): title row (HStack spacing 6: meal name strakkHeading3 17px semibold strakkTextPrimary 1-line + optional 'heart.fill' 11px semibold strakkPrimary if favorited) and subtitle '{n} item' / '{n} items' strakkCaption strakkTextSecondary; Spacer; chevron.right 12px semibold strakkTextTertiary. Tap opens MealDetailSheet (light impact haptic). Swipe-trailing (not full-swipe) + long-press context menu both expose 'Delete' (destructive, trash icon).
- **OrphanEntryRow**: Timeline card for a standalone logged food (no parent meal). Same card chrome as MealContainerRow. HStack spacing 10: time 'HH:mm' (strakkCaptionBold strakkTextTertiary width 44); a source icon 11px strakkTextTertiary in 16px slot (camera.fill=Photo AI, barcode.viewfinder=Barcode, pencil=Manual, text.quote=Text AI, magnifyingglass=Search/Frequent); food name (strakkBody 15px strakkTextPrimary, 1-line, fallback 'Item'); optional 'heart.fill' 11px strakkPrimary if favorited; optional quantity text strakkCaption strakkTextTertiary (e.g. '150 g'); Spacer. Tap opens EntryDetailSheet (light haptic). Swipe-trailing FULL-swipe + context menu: 'Edit' (pencil) and 'Delete' (trash, destructive).
- **EmptyTimelineState**: Shown when no items logged. VStack spacing 20 centered, vertical padding 32. Top: HStack spacing 14 = left gradient hairline (1px, clear->strakkDivider white@12%) + a 64x64 circle (fill strakkSurface2, 1px strakkDivider stroke, centered 'fork.knife' 26px medium strakkTextSecondary) + right gradient hairline (strakkDivider->clear). Below: VStack spacing 5 = 'No items today' (strakkBodyBold strakkTextPrimary) and 'Use the buttons below to get started' (strakkCaption strakkTextTertiary, centered).
- **StickyActionButtons**: Default bottom bar (see layout). Left 'Meal' secondary button (bg strakkSurface2), right 'Quick' primary button (bg strakkPrimary, white). Both 56px tall, radius 14, equal width, gap 12.
- **FloatingDraftBar**: Orange persistent draft bar replacing the action buttons while a meal draft exists (see layout variant B). Subtitle string built from parts joined by ' · ': '{n} item(s)' if resolved>0, '{n} pending' if pending>0, '{kcal} kcal' always. Right capsules: '+ Add' always; trailing capsule 'Finish' (white bg/orange text) when draft non-empty else 'Cancel' (translucent).
- **TrialBanner**: Conditional Pro-trial reminder card (see layout item 2). Tappable -> opens paywall/plans, fires light haptic.
- **HeaderRow**: 'Today' title + date + dumbbell training-export icon button (44x44 hit area).

## Interactions
- Tap dumbbell.fill (header): if Pro -> present Hevy export flow as fullScreenCover; else present feature-gate/paywall sheet. 44x44 target.
- Tap trial banner: light haptic, navigate to plans/paywall.
- Tap a MacroCard: no action (display only). Progress fill animates ease-out 0.25s when values change; turns green when goal reached.
- Water MINUS tap: if water>0, light impact haptic + remove 250 mL; disabled (no haptic) at 0.
- Water PLUS tap: light impact haptic + add 250 mL. Header number animates via numericText content transition.
- Water SLIDER tap: open custom-amount bottom sheet (detent 200px, drag indicator). +/- steps of 50 mL each with light haptic; Add/Remove fire medium haptic then dismiss; Cancel dismisses.
- Tap meal container row: light impact haptic -> present MealDetailSheet (item sheet).
- Tap orphan entry row: light impact haptic -> present EntryDetailSheet (item sheet).
- Swipe meal row trailing (partial, no full-swipe): reveal red Delete; deletes meal.
- Swipe orphan entry trailing (full-swipe enabled): Delete entry.
- Long-press any timeline row: context menu — meal=Delete; orphan=Edit + Delete.
- Tap 'Meal' button: light impact haptic -> start a new meal draft, then navigate (push) to MealDraftView once the draft has started.
- Tap 'Quick' button: medium impact haptic -> present AddPickerSheet in quick-add mode (item sheet).
- Draft bar — tap name area: push MealDraftView. Tap '+ Add': present AddPickerSheet in draft mode. Tap 'Finish': process/commit the draft (on commit success: reset nav stack + success notification haptic). Tap 'Cancel' (empty draft): discard draft.
- On successful meal commit: navigation path resets to root and a UINotificationFeedback success fires.
- Sheets used: MealDetailSheet, EntryDetailSheet, EditEntrySheet, AddPickerSheet, WaterCustomAmountSheet (detent .height(200)); fullScreenCover: HevyExportFlow, PaywallView. Feature-gate + error-alert overlays bound to view state.
- Whole screen scrolls vertically; bottom action/draft bar stays pinned (sticky) over a 95%-opacity background scrim of the page color.
- Numeric values (macro big numbers, water header, custom-amount counter) use animated digit roll transitions (web: animate digit changes).

## Copy strings
- Today
- Mon, Jun 30
- Your Pro trial expires in 5 days
- Protein
- Calories
- Fat
- Carbs
- g
- kcal
- 1.5 L / 2.5 L
- Custom amount
- 250 mL
- Cancel
- Remove
- Add
- No items today
- Use the buttons below to get started
- Meal
- Quick
- + Add
- Finish
- item
- items
- pending
- kcal
- Delete
- Edit
- Item

## Mock data
Header date label: 'Mon, Jun 30'. No trial banner in default mock (or 'Your Pro trial expires in 5 days' to show the variant).
Macro grid (mid-day): Protein 142 / 160 g; Calories 1840 / 2200 kcal; Fat 54 / 70 g; Carbs 210 / 240 g. (Protein bar ~89%, Calories ~84%, Fat ~77%, Carbs ~88% — none green yet.)
Water: total 1.5 L, goal 2.5 L (1500 / 2500 mL) -> header '1.5 L / 2.5 L', minus enabled.
Timeline (newest first):
  - 13:10  meal 'Lunch'  •  fork.knife  •  '3 items'  •  favorited (orange heart)  [meal container]
  - 10:25  Greek yogurt  •  magnifyingglass(Search)  •  '170 g'  [orphan entry]
  - 08:05  meal 'Breakfast'  •  fork.knife  •  '2 items'  [meal container]
  - 07:40  Black coffee  •  pencil(Manual)  [orphan entry, no quantity]
Draft-bar variant (when active): name 'Dinner', subtitle '2 items · 1 pending · 540 kcal', trailing capsule 'Finish'.
Empty-state variant: timeline replaced by circle+hairlines with 'No items today' / 'Use the buttons below to get started'.
Reached/green variant for QA: Protein 165/160, Calories 2150/2200, Fat 68/70, Carbs 248/240 — Protein and Carbs fills render strakkSuccess green.

## Design tokens
COLORS (hex): strakkBackground #050918 (page); strakkBackgroundElevated #080D1F; strakkBackgroundEdge #0B1028; strakkSurface1 #10162F (cards: macro, water, timeline rows, trial banner); strakkSurface2 #151B38 ('Meal' button bg, macro progress track, stepper minus, water remove capsule); strakkSurface3 #1A2142 (water icon chip + minus/slider buttons); text strakkTextPrimary #F4F6FF, strakkTextSecondary #9CA1B8, strakkTextTertiary #6F748C, strakkTextDisabled #50566F; accent strakkPrimary #FF7A3D (Quick button, draft bar, protein, favorite heart, loading tint), strakkPrimaryLight #FF9A55 (calories); strakkWater #4B8DFF (water icon/+button/sheet add) — +button bg is strakkWater @18% opacity; strakkAccentYellow #FFC84D (fat); strakkAccentIndigo #637CFF (carbs); strakkSuccess #4DAE6A (macro fill once goal reached); strakkWarning #E0A84D (trial clock); strakkError #E05252 (delete); strakkDivider = white @12% opacity (hairlines, circle stroke). Pinned bar background = strakkBackground @95% opacity.
SPACING scale (px): xxs 4, xs 8, sm 12, md 16, lg 20, xl 24, xxl 32, xxxl 40. Screen horizontal padding = 20 (lg). Section top paddings: header 16, trial 12, macro 20, water 20, timeline 32. Macro grid inter-cell gap 12 (sm). Timeline row gap 6. Bottom clearance spacer 120.
RADIUS scale (px): sm 12, md 18, lg 24, xl 32, xxl 36, xxxl 56. Cards/rows = 12; action buttons 14; draft bar 16; water icon buttons 10; macro icon chip 7 (continuous); water drop chip 8; capsules = pill (height-based).
TYPOGRAPHY (iOS semantic -> concrete pt at default Dynamic Type; system font, supports Dynamic Type scaling): strakkDisplayHero=largeTitle heavy ~34; strakkDisplay=largeTitle bold ~34; strakkHeading1=title bold 28 ('Today'); strakkHeading2=title2 bold 22 (macro big number); strakkHeading3=headline semibold 17 (meal name); strakkBodyLarge=body medium 17; strakkBody=subheadline regular 15 (date, food name, banner); strakkBodyBold=subheadline semibold 15 (button labels, sheet titles); strakkCaption=caption regular 12 (labels, subtitles); strakkCaptionBold=caption semibold 12 (time label, draft capsules); strakkOverline=caption2 bold 11. Water header is a one-off 18px bold monospaced-digit. Custom-amount counter 28px bold monospaced. Numeric fields use monospacedDigit + numericText animated transitions.
ICONS: SF Symbols (web: substitute equivalent line/fill glyphs) — dumbbell.fill, clock.fill, chevron.right, fork.knife, flame.fill, drop.fill, leaf.fill, heart.fill, drop.fill, minus, plus, slider.horizontal.3, minus.circle.fill, plus.circle.fill, bolt.fill, camera.fill, barcode.viewfinder, pencil, text.quote, magnifyingglass, trash.
HAPTICS (web: optional vibration/none): light impact = water +/- and step, meal-row/entry-row tap, 'Meal' button, trial banner; medium impact = 'Quick' button, water sheet Add/Remove; success notification = meal committed.
ANIMATIONS: macro progress fill width ease-out 0.25s (also on goal-reached color flip); numericText digit roll on all live numbers; sheets are iOS bottom sheets with detents (water sheet fixed 200px height, drag indicator). Overall aesthetic: premium dark navy, warm-orange accent, flat surfaces (no gloss), generous tap targets (>=40px), monospaced live data.