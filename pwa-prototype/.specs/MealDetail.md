# Spec: Meal Detail Sheet (modal) — with nested Entry Detail Sheet + Manual Entry form

## Purpose
Bottom-sheet that shows one logged meal: its total macros, an itemized list of food entries (each tappable into an Entry Detail sub-sheet for edit/delete/favorite), plus a destructive "Delete meal" action; the Manual Entry form is the modal used to hand-type a new food item's macros.

## Layout
THREE STACKED MODALS. All are bottom sheets on a deep-navy (#050918) full-bleed background, rounded top corners (iOS default ~10px), with a centered grey drag grabber pill at the very top and an inline nav bar (no large title) holding a leading close (xmark) control. Body is a vertical ScrollView. Horizontal screen margin everywhere = 20px (lg).

=== SHEET A: MealDetailSheet (root) ===
Detents: [medium, large] (user can drag between ~half-height and full). Drag indicator visible.
NavBar: leading = xmark close button (44x44 tap target, icon 14pt semibold, text-secondary #9CA1B8). Title area empty (inline mode, no text).
ScrollView > VStack(align=leading, spacing=0):
  1. HEADER block — padding: left/right 20, top 20, bottom 24.
     HStack(align=top, spacing=12):
       • VStack(align=leading, spacing=4):
           - Meal title — strakkHeading2 (title2/22pt bold), text-primary #F4F6FF.
           - Meta row — HStack(spacing=4): time "08:42"  +  "·"  +  "3 items". All strakkCaption (12pt regular), text-secondary #9CA1B8.
       • Spacer (pushes heart to trailing edge).
       • Favorite heart button — 44x44 tap target, SF Symbol "heart" (outline) or "heart.fill" when favorited, 20pt medium weight. Color = strakkPrimary #FF7A3D when favorited, else text-secondary #9CA1B8.
  2. TOTAL MACROS block — padding: left/right 20, bottom 24.
     MacroBreakdown card: rounded rect radius 12, fill surface1 #10162F, VStack(spacing=0) of MacroRow's, a 1px white-12% divider between each row. Each MacroRow: padding h16 v12, HStack[ label (strakkBody 15pt, text-primary) | Spacer | value (strakkBodyBold 15pt semibold, monospaced digits, colored) ]. Rows in order, fat/carbs only shown when >0:
        - "Protein"  → "61g"   color strakkProtein #FF7A3D
        - "Calories" → "585 kcal" color strakkCalories #FF9A55
        - "Fat"      → "14g"   color strakkAccentYellow #FFC84D
        - "Carbs"    → "61g"   color strakkAccentIndigo #637CFF
  3. "ITEMS" section label (only if entries exist) — strakkOverline (caption2/11pt bold), text-tertiary #6F748C, letter-spacing 1.0px, UPPERCASE literal. padding: left/right 20, bottom 8.
  4. ENTRIES LIST — padding: left/right 20, bottom 24. Container: rounded rect radius 12, fill surface1 #10162F, clipped. VStack(spacing=0) of EntryRow's; between rows a 1px white-12% divider inset 16px from leading. Each EntryRow is a plain Button, layout HStack(spacing=0), padding h16 v12:
        • VStack(align=leading, spacing=2):
            - Entry name — strakkBodyBold (15pt semibold), text-primary, 1 line truncated.
            - Macro caption — strakkCaption 12pt, monospaced digits, HStack(spacing=0): "35g P" (protein #FF7A3D) + " · " (tertiary) + "165 kcal" (calories #FF9A55) + optional " · " (tertiary) + quantity "120g" (tertiary #6F748C).
        • Spacer.
        • chevron.right — 11pt semibold, text-tertiary #6F748C.
  5. DELETE MEAL button — padding: left/right 20, bottom 32. Full-width, height 52, fill surface2 #151B38, radius 12. Centered HStack(spacing=8): trash icon (14pt semibold) + "Delete meal" (strakkBodyBold). Both colored strakkError #E05252.

=== SHEET B: EntryDetailSheet (opens on tapping an EntryRow; presented as a sheet ON TOP of Sheet A) ===
Detents: [medium] ONLY (fixed half-height, not resizable). Drag indicator visible. Same container/close button.
ScrollView > VStack(align=leading, spacing=0):
  1. HEADER — padding left/right 20, top 20, bottom 24. HStack(align=top, spacing=12):
       • VStack(align=leading, spacing=4):
           - Entry name — strakkHeading2 (22pt bold), text-primary, up to 2 lines.
           - Meta row — HStack(spacing=6): time "08:42" + "·" + source icon (SF Symbol, 11pt, text-tertiary) + source label. strakkCaption, text-secondary. Source icon/label pairs: camera.fill→"Photo AI", barcode.viewfinder→"Barcode", pencil→"Manual", text.quote→"Text AI", magnifyingglass→"Search" or "Frequent".
       • Spacer + Favorite heart (identical to Sheet A: 44x44, heart/heart.fill 20pt, primary when favorited).
  2. MACRO BREAKDOWN — padding left/right 20, bottom 24. Same MacroBreakdown card as Sheet A but per-entry, and it appends a "Quantity" row when present: rows in order Protein / Calories / Fat (if set) / Carbs (if set) / Quantity (if set). Quantity row value is the raw string ("1 bowl"), colored text-secondary #9CA1B8.
  3. ACTIONS — padding left/right 20, bottom 32. HStack(spacing=12), two equal-width buttons:
       • "Edit" — StrakkSecondaryButton: height 52, fill surface2 #151B38 (→ surface3 #1A2142 pressed), radius 12, content HStack(spacing=8) pencil icon (16pt semibold) + "Edit" (strakkBodyBold), text-primary.
       • "Delete" — height 52, fill surface2 #151B38, radius 12, HStack(spacing=8) trash (14pt semibold) + "Delete" (strakkBodyBold), colored strakkError #E05252.

=== SHEET C: ManualEntryView (opens via the meal's add picker / or as the Edit target) ===
Detents: [large] ONLY (full height). Drag indicator visible. NavBar inline title = "Manual entry", leading xmark close.
ScrollView > VStack(align=leading, spacing=20), padding: left/right 20, top 16, bottom 32:
  1. FieldGroup "Name *" → full-width InputField, placeholder "e.g. Grilled chicken".
  2. HStack(spacing=12): FieldGroup "Protein (g) *" → NumericField placeholder "35"  |  FieldGroup "Calories *" → NumericField placeholder "400".
  3. HStack(spacing=12): FieldGroup "Fat (g)" → NumericField placeholder "15"  |  FieldGroup "Carbs (g)" → NumericField placeholder "40".
  4. FieldGroup "Quantity" → full-width InputField, placeholder "e.g. 150g, 1 bowl". (NOTE: quantity is a FREE-TEXT field, NOT a numeric stepper.)
  5. Optional error text — strakkCaption, strakkError #E05252, shown only when validation/submit produces a message.
  6. StrakkPrimaryButton — full-width, height 52, radius 12, label "Add" (or "Adding…" while submitting). Fill strakkPrimary #FF7A3D + white text when enabled; fill surface2 #151B38 + text-tertiary when disabled.
  Each FieldGroup = VStack(align=leading, spacing=6): label row HStack(spacing=2)[ label text (strakkCaptionBold 12pt semibold, text-secondary) + optional "*" (strakkCaption, strakkPrimary #FF7A3D) ] then the input.
  Each Input = TextField, font strakkBody (15pt), text-primary; padding h12 v12; fill surface1 #10162F; radius 12; 1px stroke border = strakkDivider (white 12%) normally, switches to strakkError #E05252 when the field is non-empty AND invalid. Numeric fields use decimal keypad.

## Components
- **SheetContainer**: NavigationStack + full-bleed ZStack(strakkBackground #050918) + vertical ScrollView. Top drag grabber, inline nav bar. Shared by all three sheets.
- **SheetCloseButton (StrakkCloseToolbarItem)**: Leading toolbar xmark: SF Symbol xmark 14pt semibold, text-secondary→text-primary on press, 44x44 tap target, leading offset -8px, aria-label 'Close'. Fires light haptic + dismiss.
- **DragGrabber**: Centered grey capsule at sheet top (iOS presentationDragIndicator). On web: ~36x5px rounded pill, white ~30% opacity, ~8px from top.
- **MealDetailHeader**: Title (heading2) + meta row (time · N items) on left, FavoriteHeartButton on right; top-aligned HStack spacing 12.
- **FavoriteHeartButton**: 44x44 button, heart / heart.fill 20pt medium; orange #FF7A3D when active, text-secondary otherwise. Light haptic on tap, toggles favorite. aria 'Favorite meal'/'Remove favorite meal' (meal) or 'Add favorite'/'Remove favorite' (entry).
- **MacroBreakdown card**: Rounded(12) surface1 #10162F container; vertical list of MacroRow with 1px white-12% dividers between.
- **MacroRow**: Row padding h16 v12: label (body 15pt, text-primary) left, value (bodyBold 15pt, monospaced digits, accent color) right via Spacer.
- **SectionOverline**: 'ITEMS' label: caption2 11pt bold, text-tertiary #6F748C, 1px letter-spacing, uppercase.
- **EntriesList**: Rounded(12) surface1 container of EntryRow's, 1px dividers inset 16px leading.
- **EntryRow**: Tappable row: name (bodyBold, 1 line) + colored macro caption (monospaced) on left, chevron.right 11pt tertiary on right. Supports trailing full-swipe Delete and long-press context menu (Edit/Delete). Light haptic on tap opens EntryDetailSheet.
- **DeleteMealButton**: Full-width 52pt, surface2 #151B38, radius 12; trash + 'Delete meal' in error red. Medium haptic, triggers meal deletion.
- **EntryDetailHeader**: Entry name (heading2, 2 lines) + meta row (time · source-icon source-label) + FavoriteHeartButton.
- **SourceBadge**: Inline SF-Symbol + label pair describing entry origin: Photo AI / Barcode / Manual / Text AI / Search / Frequent. Icon 11pt text-tertiary, label caption text-secondary.
- **EntryActionsRow**: Two equal-width 52pt buttons: 'Edit' (StrakkSecondaryButton, surface2→surface3 pressed) and 'Delete' (surface2, error-red text).
- **StrakkSecondaryButton**: 52pt, radius 12, surface2 fill (surface3 when pressed), text-primary, optional leading SF Symbol 16pt; press scale 0.97 ease-out 0.1s.
- **ManualEntryForm**: Vertical form (spacing 20) of FieldGroups, optional error text, and a primary Add button. Large detent.
- **FieldGroup**: Label row (captionBold text-secondary + optional orange '*') above an input, VStack spacing 6.
- **InputField / NumericField**: TextField, body 15pt text-primary, padding h12 v12, surface1 fill, radius 12, 1px divider border that turns error-red when invalid+non-empty. NumericField = decimal keypad.
- **StrakkPrimaryButton**: Full-width 52pt, radius 12, orange #FF7A3D + white text when enabled, surface2 + text-tertiary when disabled; label 'Add'/'Adding…'; light haptic; press scale 0.97 + opacity 0.92 ease-out 0.1s.
- **ErrorText**: caption, error-red, shown only when a validation/submit error message is present.

## Interactions
- Sheet A (MealDetail) presents at medium detent; drag the grabber up to large or down to dismiss. Drag indicator always visible.
- Tap an EntryRow → light impact haptic → EntryDetailSheet (Sheet B) presents stacked on top at FIXED medium detent (not resizable).
- EntryRow trailing edge swipe (full-swipe allowed) reveals a destructive red 'Delete' (trash icon) action that deletes the entry immediately on full swipe.
- EntryRow long-press → context menu with 'Edit' (pencil) and destructive 'Delete' (trash).
- Favorite heart tap (meal or entry) → light impact haptic → toggles heart outline↔heart.fill with orange tint; no sheet change.
- 'Delete meal' button → medium impact haptic → triggers meal deletion (caller is expected to confirm/undo upstream).
- In Sheet B: 'Edit' tap dismisses Sheet B and routes to the edit form (Manual Entry / Edit Entry); 'Delete' tap → medium impact haptic → deletes the entry and dismisses Sheet B.
- Close (xmark) on any sheet → light impact haptic → dismiss that sheet only.
- All Strakk buttons animate on press: scale to 0.97 (primary/secondary/destructive) with opacity 0.92 on filled primary/destructive, easeOut 0.1s.
- Manual Entry presents at large (full-height) detent only.
- Manual Entry numeric fields (Protein/Calories/Fat/Carbs) open the decimal keypad; Name/Quantity open the standard keyboard. submitLabel is 'next' to advance focus.
- Manual Entry live validation: each field's border switches from divider-grey to error-red the moment its content is non-empty AND out of range (Name ≤100 chars; Protein 0–500; Calories 0–5000; Fat 0–500; Carbs 0–500; Quantity ≤50 chars).
- Add button is disabled (grey) until the form is submittable (valid required fields) and re-disables while submitting, with label flipping to 'Adding…'.
- Add tap → light impact haptic (primary button) → submit; on success the form's shouldDismiss flips and the sheet dismisses.
- Comma decimals are normalized to dots on submit (e.g. '12,5' → 12.5).
- On web, replicate haptics where possible (navigator.vibrate / no-op on desktop); preserve the same press/scale animations and the stacked-sheet z-order.

## Copy strings
- ITEMS
- Protein
- Calories
- Fat
- Carbs
- Quantity
- Item
- Delete meal
- Delete
- Edit
- Add
- Adding…
- Manual entry
- Name *
- Protein (g) *
- Calories *
- Fat (g)
- Carbs (g)
- Quantity
- *
- e.g. Grilled chicken
- 35
- 400
- 15
- 40
- e.g. 150g, 1 bowl
- Photo AI
- Barcode
- Manual
- Text AI
- Search
- Frequent
- Close
- ·
- Favorite meal
- Remove favorite meal
- Add favorite
- Remove favorite
- Edit entry
- Delete entry
- Add food item
- 08:42
- 3 items
- 1 item
- 35g P
- 165 kcal
- 120g

## Mock data
MEAL (Sheet A): name "Post-workout breakfast", createdAt "2026-06-30T08:42:00" → time label "08:42", 3 items → meta "08:42 · 3 items". Totals: Protein 61g, Calories 585 kcal, Fat 14g, Carbs 61g (all four macro rows shown since fat & carbs > 0).
ENTRIES:
  1) "Grilled chicken breast" — 35g P · 165 kcal · 120g — fat 4, carbs 0, source Search (magnifyingglass).
  2) "Oatmeal with berries" — 8g P · 290 kcal · 1 bowl — fat 6, carbs 52, source Manual (pencil).
  3) "Greek yogurt" — 18g P · 130 kcal · 200g — fat 4, carbs 9, source Barcode (barcode.viewfinder).
ENTRY DETAIL (Sheet B) opened on entry 2: title "Oatmeal with berries", meta "08:42 · [pencil] Manual", rows Protein 8g / Calories 290 kcal / Fat 6g / Carbs 52g / Quantity 1 bowl. Heart not favorited.
Pluralization examples: 1 entry → "1 item", 0 or 2+ → "items" (code: count > 1 ? 'items' : 'item', so 0 shows '0 item').
MANUAL ENTRY (Sheet C) filled example: Name "Grilled chicken", Protein "35", Calories "400", Fat "15", Carbs "40", Quantity "150g". Empty state shows the grey placeholders listed in copy. Invalid example: Protein "650" → red border (exceeds 500). Submitting state: Add button label "Adding…", disabled.
Singular meal with one favorited item example: meal "Late dinner", "21:15 · 1 item", heart.fill orange.

## Design tokens
COLORS (hex from StrakkColors.swift): background/sheet bg strakkBackground #050918; cards & input fill strakkSurface1 #10162F; secondary/delete button & disabled-primary fill strakkSurface2 #151B38; secondary-button pressed strakkSurface3 #1A2142; primary accent / protein / favorited heart / required '*' / enabled Add button strakkPrimary == strakkProtein #FF7A3D; calories strakkCalories #FF9A55; fat strakkAccentYellow #FFC84D; carbs strakkAccentIndigo #637CFF; text-primary #F4F6FF; text-secondary #9CA1B8; text-tertiary #6F748C; text-disabled #50566F; error / destructive / invalid border strakkError #E05252; divider strakkDivider = white @ 12% opacity. Add button white text on orange when enabled.
SPACING (StrakkSpacing, px): xxs 4, xs 8, sm 12, md 16, lg 20 (screen margin), xl 24, xxl 32. Used: screen h-margin 20; header top 20 / bottom 24; section gaps 24; bottom 32; macro/entry row padding h16 v12; input padding h12 v12; FieldGroup inner spacing 6; form field gap 20; column gap 12; meta-row gap 4–6.
RADIUS (StrakkRadius, px): sm 12 — used for EVERY card, button, input, and the breakdown/list containers. (md 18 / lg 24 exist but unused here.)
TYPOGRAPHY (Font tokens → SwiftUI semantic → web px @ default size, weights): strakkHeading2 = title2 bold ≈ 22px/700 (sheet titles); strakkBody = subheadline regular ≈ 15px/400 (macro labels, input text); strakkBodyBold = subheadline semibold ≈ 15px/600 (entry name, macro values, button labels); strakkCaption = caption regular ≈ 12px/400 (meta rows, entry macro caption, error text); strakkCaptionBold = caption semibold ≈ 12px/600 (field labels); strakkOverline = caption2 bold ≈ 11px/700 + 1.0px letter-spacing, uppercase ('ITEMS'). Numeric macro values use monospaced digits (tabular-nums). Fonts scale with Dynamic Type on iOS — on web honor user font-size where feasible.
ICONS (SF Symbols → map to comparable web icon set): xmark (close), heart / heart.fill, chevron.right, trash, pencil, camera.fill, barcode.viewfinder, text.quote, magnifyingglass. Heart 20pt medium; close 14pt semibold; chevron 11pt semibold; trash/pencil in buttons 14–16pt semibold; source icons 11pt.
ELEVATION/SURFACES: flat dark, no shadows on cards — depth comes purely from surface-step lightening (#050918 bg → #10162F card → #151B38/#1A2142 raised). Sheet itself uses the system bottom-sheet presentation (rounded top, dimmed backdrop behind).
MOTION: button press scale 0.97 + (filled) opacity 0.92, transition easeOut 0.1s. Sheet present/dismiss = iOS spring slide-up; nested sheet stacks above parent. Haptics: light (taps/favorite/close/add), medium (destructive delete actions).