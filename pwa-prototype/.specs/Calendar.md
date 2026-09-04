# Spec: Calendar — Month grid with per-day adherence dots + Day Detail bottom sheet (reuses the macro grid)

## Purpose
A scrollable month calendar where each logged day shows an orange adherence dot; tapping a day opens a full-height sheet showing that day's macro progress grid, water row, and meal timeline with full edit/delete affordances.

## Layout
SCREEN A — CalendarView (root tab screen)
Root: NavigationStack → ZStack {
  • Layer 0: Color strakkBackground (#050918), ignoresSafeArea (fills whole screen behind everything).
  • Layer 1: calendarContent.
}
NavBar: large navigationTitle "Calendar" (.navigationBarTitleDisplayMode(.large) — iOS large-title that collapses to inline on scroll). No leading/trailing bar buttons.

calendarContent has two states:
STATE = loading: a single centered ProgressView (spinner) tinted strakkPrimary (#FF7A3D), centered in the ZStack.
STATE = ready: ScrollView (vertical) → VStack(spacing: 0) {
  1. MONTH NAVIGATOR — HStack, padding(.horizontal, 20), padding(.top, 16):
       [chevron.left button, 44×44] — Spacer — [Month label, centered] — Spacer — [chevron.right button, 44×44].
       Month label text = "MMMM yyyy".capitalized, e.g. "June 2026", font strakkHeading2 (title2/bold ~22pt), color strakkTextPrimary (#F4F6FF).
       Chevrons: SF Symbol chevron.left / chevron.right, system size 16 semibold, color strakkTextPrimary, inside a 44×44 tappable frame.
  2. WEEKDAY HEADER — LazyVGrid, 7 flexible columns, column-spacing 4. padding(.horizontal,20), padding(.top,16), padding(.bottom,8).
       7 cells, each Text centered (frame maxWidth infinity): "M","T","W","T","F","S","S" (Monday-first). Font strakkOverline (caption2/bold ~11pt), color strakkTextTertiary (#6F748C).
  3. CALENDAR GRID — LazyVGrid, 7 flexible columns, column-spacing 4, row-spacing 8. padding(.horizontal,20).
       Leading empty cells (to align day-1 to its weekday, Monday=index0) render as Color.clear with height 44.
       Trailing pad cells (to complete the final week) also Color.clear height 44.
       Each real DAY CELL = Button → VStack(spacing: 3) centered {
           • Day number Text "\(day)", font strakkBody (subheadline/regular ~15pt). Color: selected→strakkPrimary, else today→strakkPrimary, else strakkTextPrimary. fontWeight .semibold if today OR selected else .regular.
           • Adherence dot: Circle 5×5 — filled strakkPrimary (#FF7A3D) if that date is in activeDays, otherwise a Circle of Color.clear 5×5 (reserves layout height so rows never jump).
       }
       Cell frame: maxWidth infinity, height 44.
       Cell background: if selected → strakkPrimary @ opacity 0.15 inside RoundedRectangle cornerRadius 8; else clear.
       Cell overlay: if today AND not selected → RoundedRectangle cornerRadius 8 strokeBorder strakkPrimary @ opacity 0.4, lineWidth 1.
  4. Bottom spacer: Spacer().frame(height: 81)  // = 32 + 49, clearance so last row clears the floating tab bar.
}

SCREEN B — DayDetailSheet (presented as a sheet over Screen A; .presentationDetents([.large]) full-height card, .presentationDragIndicator(.visible) grabber pill at top center).
Root: NavigationStack → ZStack {
  • Color strakkBackground ignoresSafeArea.
  • ScrollView → VStack(alignment: .leading, spacing: 0) {
      1. SectionHeader "NUTRITION" — padding(.horizontal, 20), padding(.top, 20).
      2. MacroProgressGrid (the shared 2×2 macro grid, see component) — padding(.horizontal, 20), padding(.top, 8).
      3. SectionHeader "WATER" — padding(.horizontal, 20), padding(.top, 24).
      4. WaterRow (single compact row) — padding(.horizontal, 20), padding(.top, 8).
      5. IF day has meals: SectionHeader "MEALS" — padding(.horizontal, 20), padding(.top, 24).
         VStack(spacing: 6) — padding(.horizontal, 20), padding(.top, 8) {
            • One MealContainerRow per meal container (grouped meals), in order.
            • Then one OrphanEntryRow per quick-add/orphan entry, in order.
         }
      6. StrakkPrimaryButton title "Add for this day", icon "plus" — padding(.horizontal,20), padding(.top,24), padding(.bottom,32).
  }
}
NavBar (inline): navigationTitle = formatted date "EEEE d MMMM".capitalized, e.g. "Tuesday 30 June" (.navigationBarTitleDisplayMode(.inline)). Toolbar leading: StrakkCloseButton (xmark, 44×44 tap target, color strakkTextSecondary→strakkTextPrimary on press).
DayDetailSheet hosts THREE nested sheets (presented on top of it): MealDetailSheet (when a meal row tapped), EntryDetailSheet (when an orphan entry tapped), EditEntrySheet (when Edit chosen). These default to their own detents (treat as full/large cards).

## Components
- **CalendarScreen (root)**: NavigationStack + ZStack(background #050918) + large nav title 'Calendar'. Holds loading vs ready states. Manages selected day, add-date sheet, and feature-gate sheet.
- **MonthNavigator**: HStack: chevron.left (44×44, SF size16 semibold, #F4F6FF) | Spacer | month label (strakkHeading2 title2/bold, e.g. 'June 2026') | Spacer | chevron.right (44×44). a11y labels 'Previous month' / 'Next month'.
- **WeekdayHeader**: LazyVGrid 7 cols, col-spacing 4. Cells M T W T F S S (Monday-first), strakkOverline caption2/bold, color #6F748C, centered.
- **CalendarGrid**: LazyVGrid 7 cols, col-spacing 4, row-spacing 8. Leading offset + trailing pad cells are Color.clear height 44 to align Monday-first and complete final week.
- **DayCell**: Button → VStack(spacing 3): day number (strakkBody) + 5×5 dot (filled #FF7A3D if active else clear). Frame maxW∞ × 44. Selected bg = #FF7A3D@0.15 rounded-8; today (unselected) = rounded-8 border #FF7A3D@0.4 1px; number turns #FF7A3D + semibold when today or selected. a11y 'Day N, meals logged'.
- **DayDetailSheet (root)**: NavigationStack + ZStack(bg #050918) + ScrollView/VStack. Full-height sheet (.large) with drag indicator. Inline nav title = formatted weekday-date. Leading toolbar close (xmark).
- **SectionHeader**: Uppercase label, font strakkOverline (caption2/bold ~11pt), color #6F748C, letter-spacing/kerning 1.0. Used for 'NUTRITION', 'WATER', 'MEALS'.
- **MacroProgressGrid**: LazyVGrid 2 cols, spacing 12. Four MacroCards in fixed order: Protein, Calories, Fat, Carbs. Identical component reused on the Today screen so nutrition language matches.
- **MacroCard**: VStack(leading,spacing 8) in rounded-12 surface #10162F, padding 16. Row1: 28×28 rounded-7 icon chip (icon color @0.12 fill, SF size13 semibold) + label (strakkCaption, #9CA1B8). Row2 (lastTextBaseline,spacing2): big consumed Int (strakkHeading2/title2-bold, monospacedDigit, numericText transition) + '/ {goal}{unit}' (strakkCaption #6F748C) or unit if no goal. Row3: progress bar — track Capsule #151B38 h4 + fill Capsule h4 width=min(consumed/goal,1)×W, fill color = macro color, OR #4DAE6A (success/green) once goal reached. Fill animates easeOut 0.25. Per-macro: Protein dumbbell.fill #FF7A3D 'g'; Calories flame.fill #FF9A55 'kcal'; Fat drop.fill #FFC84D 'g'; Carbs leaf.fill #637CFF 'g'.
- **WaterRow**: Rounded-12 surface #10162F row, padding 16/12. HStack(spacing12): 36×36 rounded-8 #1A2142 chip with drop.fill (#4B8DFF, size15) | header text '1.5 L / 2.5 L' (system 18 bold monospacedDigit, numericText) | Spacer | minus btn | plus btn | custom btn. Buttons = WaterIconButton 40×40 rounded-10, SF size16 bold. Minus: bg #1A2142, fg #F4F6FF (or #6F748C disabled when water=0). Plus: bg #4B8DFF@0.18, fg #4B8DFF. Custom: slider.horizontal.3, bg #1A2142, fg #F4F6FF.
- **WaterCustomAmountSheet**: Presented from the custom (slider) button. Small sheet .presentationDetents([.height(200)]), drag indicator, bg #10162F. Title 'Custom amount' (strakkBodyBold). Stepper row: minus.circle.fill (size38) | '{n} mL' (system 28 bold monospaced, numericText, minWidth110) | plus.circle.fill (size38, #4B8DFF). Actions row: 'Cancel' (text, #9CA1B8) | Spacer | 'Remove' (capsule #151B38, shown only if water>0) | 'Add' (capsule #4B8DFF, white text). Step 50 mL, min 50, max 2000, default 250.
- **MealContainerRow**: Tappable rounded-12 #10162F row, padding 14/12. HStack(spacing10): time '08:24' (strakkCaptionBold caption/semibold #6F748C, fixed width 44) | fork.knife (size13 #9CA1B8, w16) | VStack(leading,sp2): meal name (strakkHeading3 headline/semibold #F4F6FF, 1 line) + 'N items' (strakkCaption #9CA1B8) | Spacer | chevron.right (size12 semibold #6F748C). contextMenu Delete; swipeActions trailing (no full-swipe) Delete.
- **OrphanEntryRow**: Tappable rounded-12 #10162F row, padding 14/12. HStack(spacing10): time (strakkCaptionBold #6F748C w44) | source icon (camera.fill/barcode.viewfinder/pencil/text.quote/magnifyingglass, size11 #6F748C, w16) | name or 'Item' (strakkBody #F4F6FF, 1 line) | quantity if present (strakkCaption #6F748C) | Spacer. swipeActions trailing full-swipe Delete; contextMenu Edit + Delete.
- **StrakkPrimaryButton ('Add for this day')**: Full-width 52pt-high button, bg #FF7A3D, white text strakkBodyBold, leading SF 'plus' size16 semibold, rounded-12 continuous. Pressed: scale 0.97, opacity 0.92, easeOut 0.1. Fires light haptic then action.
- **StrakkCloseButton (toolbar)**: Leading toolbar xmark, SF size14 semibold, color #9CA1B8, 44×44 tap target. a11y 'Close'. Light haptic on tap.
- **Nested sheets**: MealDetailSheet (meal tapped), EntryDetailSheet (orphan tapped), EditEntrySheet (edit chosen). AddPickerSheet presented from CalendarView when 'Add for this day' is tapped, pre-scoped to the day's date. FeatureGateSheet + errorAlert attached at CalendarView root.

## Interactions
- Tap chevron.left / chevron.right → navigate to previous/next month (wraps year at Dec↔Jan). Grid + month label re-render. No haptic on chevrons.
- Tap a day cell → selects that date, fetches day detail, then presents DayDetailSheet as a full-height (.large) sheet with a visible drag-indicator grabber. Selected cell gets the #FF7A3D@0.15 rounded background.
- Swipe-down / tap drag-indicator / tap close (xmark) on DayDetailSheet → dismisses, fires DismissDay, clears selection.
- Inside DayDetailSheet, tap 'Add for this day' → light haptic, dismisses the day sheet, then CalendarView presents AddPickerSheet scoped to that date (logDate). May route into FeatureGateSheet if a gated feature is chosen.
- MacroCard progress bar animates fill width with easeOut 0.25 on value change; fill color cross-fades to green (#4DAE6A) when goal reached (also easeOut 0.25). Consumed numbers use .numericText() content transition (rolling digits).
- WaterRow minus button → light haptic, removes 250 mL (disabled + greyed when day water = 0). Plus button → light haptic, adds 250 mL. Water header animates via .numericText().
- WaterRow custom (slider.horizontal.3) button → presents WaterCustomAmountSheet at detent height 200. Stepper +/- changes by 50 mL (light haptic each step, clamped 50–2000). 'Add'/'Remove' → medium haptic, applies, dismisses. 'Cancel' dismisses.
- Tap a MealContainerRow → light haptic, presents MealDetailSheet for that meal. Long-press → context menu with destructive 'Delete'. Swipe leading-to-trailing (partial, no full-swipe) reveals red 'Delete'.
- Tap an OrphanEntryRow → light haptic, presents EntryDetailSheet. Swipe trailing with FULL swipe → 'Delete' (auto-commits at full extent). Long-press → context menu 'Edit' (pencil) + destructive 'Delete' (trash).
- From MealDetailSheet/EntryDetailSheet choosing 'Edit' closes that sheet and opens EditEntrySheet; saving calls onEditEntry (name/protein/calories/fat/carbs/quantity) and dismisses. Delete actions call onDeleteMeal/onDeleteEntry and dismiss.
- All StrakkPrimaryButton presses: scale to 0.97 + opacity 0.92 over easeOut 0.1; light haptic on release.
- Errors surface via .errorAlert (native alert) bound to viewModel.errorMessage at the CalendarView level.

## Copy strings
- Calendar
- M
- T
- W
- T
- F
- S
- S
- June 2026
- NUTRITION
- WATER
- MEALS
- Protein
- Calories
- Fat
- Carbs
- g
- kcal
- / 160g
- / 2200kcal
- / 70g
- / 240g
- 1.5 L / 2.5 L
- Add for this day
- Breakfast
- Lunch
- Dinner
- 3 items
- 4 items
- 2 items
- 1 item
- Item
- Whey Protein Shake
- Banana
- Custom amount
- Cancel
- Remove
- Add
- Delete
- Edit
- Close
- Previous month
- Next month
- Day 30, meals logged
- Tuesday 30 June
- Add 250 mL of water
- Remove 250 mL of water

## Mock data
Displayed month: June 2026 (June 1 2026 = Monday, so grid offset = 0; 30 days; 5 trailing empty cells). Today = 2026-06-30 (Tuesday → gets today border #FF7A3D@0.4).
activeDays (orange dots) = ["2026-06-01","2026-06-02","2026-06-03","2026-06-05","2026-06-06","2026-06-08","2026-06-09","2026-06-10","2026-06-12","2026-06-13","2026-06-15","2026-06-16","2026-06-17","2026-06-19","2026-06-20","2026-06-22","2026-06-23","2026-06-24","2026-06-26","2026-06-27","2026-06-29","2026-06-30"] — gives a realistic ~73% adherence look with a few gaps.
Opened day (DayDetailSheet) = 2026-06-30, title 'Tuesday 30 June'.
Day summary: protein 142 / goal 160 (bar ~89%, orange); calories 1840 / goal 2200 (~84%, orange-light); fat 54 / goal 70 (~77%, yellow); carbs 210 / goal 240 (~88%, indigo); water 1500 mL of 2500 mL → header '1.5 L / 2.5 L'.
For a 'goal reached' visual variant on another day (e.g. 2026-06-29): protein 165/160, calories 2210/2200 → those bars fill 100% and turn green #4DAE6A.
Meal containers (grouped):
 • 'Breakfast' — time 08:24 (createdAt 2026-06-30T08:24:00) — '3 items'.
 • 'Lunch' — time 12:47 — '4 items'.
 • 'Dinner' — time 19:30 — '2 items'.
Orphan entries (quick-adds, shown after containers):
 • 'Banana' — time 10:05 — source Frequent (magnifyingglass icon) — quantity '1 medium'.
 • 'Whey Protein Shake' — time 16:10 — source Manual (pencil icon) — quantity '1 scoop (30g)'.
Empty-day variant: a day with no logs shows NUTRITION grid all zeros (bars empty, '0 / goal'), WATER '0.0 L / 2.5 L' with minus disabled, and the MEALS section omitted entirely (only the 'Add for this day' button remains).

## Design tokens
COLORS (hex, from StrakkColors.swift): strakkBackground #050918 (screen + sheet bg); strakkSurface1 #10162F (macro cards, water row, meal/entry rows, custom-amount sheet bg); strakkSurface2 #151B38 (macro bar track, 'Remove' capsule); strakkSurface3 #1A2142 (water icon chip, minus/custom button bg); strakkPrimary #FF7A3D (active dots, selected number/cell tint, protein, primary button, today border); strakkPrimaryLight #FF9A55 (calories macro); strakkAccentYellow #FFC84D (fat macro); strakkAccentIndigo #637CFF (carbs macro); strakkWater #4B8DFF (water accents); strakkSuccess #4DAE6A (macro bar fill once goal reached); strakkTextPrimary #F4F6FF; strakkTextSecondary #9CA1B8; strakkTextTertiary #6F748C. OPACITY VARIANTS USED: primary@0.15 (selected cell bg), primary@0.4 (today border 1px), macroColor@0.12 (macro icon chip fill), water@0.18 (plus button bg).
SPACING (StrakkSpacing): xxs 4, xs 8, sm 12, md 16, lg 20, xl 24, xxl 32. Screen horizontal margin = 20 (lg). Section header top = 20 (NUTRITION) / 24 (WATER, MEALS). Macro grid inter-card spacing = 12 (sm). Meals list inter-row spacing = 6. Calendar: column-spacing 4, row-spacing 8, weekday top 16 / bottom 8, month-nav top 16. Tab-bar bottom clearance spacer = 81 (32+49).
RADIUS (StrakkRadius): sm 12 (cards, rows, primary button, water row — all .continuous); 8 (day-cell selected bg + today border, water icon chip); 7 (macro icon chip, .continuous); 10 (water +/-/custom buttons).
TYPOGRAPHY (iOS Dynamic Type semantic → approx pt at default size; all Dynamic-Type scalable): strakkDisplayHero=largeTitle/heavy(~34); strakkHeading1=title/bold; strakkHeading2=title2/bold(~22) → month label + macro consumed number; strakkHeading3=headline/semibold(~17) → meal name; strakkBodyLarge=body/medium; strakkBody=subheadline/regular(~15) → day number, entry name; strakkBodyBold=subheadline/semibold → primary button, custom-amount title; strakkCaption=caption/regular(~12) → macro label, 'N items', quantity, '/ goal'; strakkCaptionBold=caption/semibold → time labels; strakkOverline=caption2/bold(~11) → section headers (kerning 1.0) + weekday symbols. Numeric labels (macro consumed, water header, custom amount) use monospacedDigit + .numericText() rolling transition. Non-typography literals: day cell height 44, dot 5×5, time-label fixed width 44, source/meal glyph slot width 16, water chip 36×36, water buttons 40×40, macro icon chip 28×28, macro bar height 4, primary button height 52.
SF SYMBOLS: chevron.left, chevron.right, plus, xmark, dumbbell.fill, flame.fill, drop.fill, leaf.fill, minus, slider.horizontal.3, minus.circle.fill, plus.circle.fill, fork.knife, chevron.right, camera.fill, barcode.viewfinder, pencil, text.quote, magnifyingglass, trash.
ANIMATIONS: macro bar fill easeOut 0.25; button press easeOut 0.10 (scale 0.97 / opacity 0.92); numericText digit roll on number changes. HAPTICS (UIImpactFeedbackGenerator): light = day/meal/entry tap, water +/-, water stepper step, close button, primary button; medium = water custom Add/Remove commit.