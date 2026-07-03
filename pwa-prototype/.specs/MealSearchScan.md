# Spec: Add-Food Flow (3 surfaces): (1) AddPickerSheet — entry-point picker · (2) SearchFoodView — food search with My Foods / Catalog tabs · (3) BarcodeScannerView — live camera scanner overlay

## Purpose
The three-surface "add food" funnel: a short bottom-sheet picker chooses an input method, the search sheet finds saved foods or catalog items and adds a chosen gram portion inline, and the full-screen scanner reads a product barcode with a live camera overlay.

## Layout

=================================================================
SURFACE 1 — AddPickerSheet  (bottom sheet, detent .fraction(0.45) ≈ 45% of viewport height)
=================================================================
Root: sheet container, bg strakkBackground #050918 full-bleed, top grabber/drag-indicator visible (centered pill, ~36x5, white@30%).
└─ NavigationStack
   ├─ Nav bar (inline, transparent over bg #050918, height 44)
   │   ├─ Leading: Close button — SF `xmark` 14pt semibold, color strakkTextSecondary #9CA1B8, 44x44 tap target, nudged leading -8px
   │   └─ Center title: "Quick add" (default) OR "Add to meal" (draft mode) — inline nav title, system semibold ~17pt, strakkTextPrimary #F4F6FF
   └─ Body: VStack spacing=xs(8), padding horizontal=lg(20), padding top=sm(12)
       ├─ optionRow #1  "Search"     icon `magnifyingglass`
       ├─ optionRow #2  "Manual"     icon `pencil`
       ├─ optionRow #3  "Free text"  icon `text.quote`   + PRO badge (if not Pro)
       ├─ optionRow #4  "Photo"      icon `camera.fill`  + PRO badge (if not Pro)   [HIDDEN in draft mode]
       ├─ (conditional) Processing row: HStack spacing=10 → [ProgressView tint strakkPrimary] + Text "Analyzing…" (strakkBody, strakkTextSecondary), padding top=sm(12)
       └─ Spacer()  (pushes rows to top; remaining sheet area empty)

  Each optionRow (button, plain style, full width):
    HStack spacing=md(16), padding horizontal=md(16), fixed height=60,
    background strakkSurface1 #10162F, clipShape RoundedRectangle radius=sm(12)
      ├─ Leading icon chip: ZStack → Circle fill strakkSurface2 #151B38, 40x40 ; SF icon 16pt medium, color strakkPrimary #FF7A3D
      ├─ Text label — strakkBody (subheadline ~15pt regular), strakkTextPrimary
      ├─ Spacer()
      ├─ (Pro rows, non-Pro user) ProBadge — text "PRO" overline(caption2 ~11pt bold), color strakkPrimary, padding h6/v2, bg strakkPrimary@15%, radius 4
      └─ Trailing chevron `chevron.right` 13pt semibold, color strakkTextTertiary #6F748C

=================================================================
SURFACE 2 — SearchFoodView  (sheet, detent .large = full height)
=================================================================
Root: sheet, bg strakkBackground #050918 full-bleed, drag indicator visible.
└─ NavigationStack
   ├─ Nav bar (inline)
   │   ├─ Leading: Close button (same `xmark` as Surface 1)
   │   └─ Center title: "Search a food"
   ├─ Search bar (`.searchable`, sits directly under nav title — native iOS rounded search field, magnifier glyph + placeholder)
   │       placeholder = "Search my foods" (My foods tab)  /  "Apple, chicken…" (Catalog tab)
   └─ Content (switches on view state):
       • state=loading → centered ProgressView, tint strakkPrimary
       • state=error → VStack spacing=16: `exclamationmark.triangle` 40pt strakkError #E05252 ; message strakkBody strakkTextSecondary centered ph40 ; "Retry" button strakkPrimary
       • state=ready → tabbedList:
         VStack spacing=0
           ├─ Segmented Picker (native iOS UISegmentedControl): ["My foods" | "Catalog"], padding h=20, top=8, bottom=8
           └─ ScrollView → LazyVStack(alignment:.leading, spacing:0, padding top=4)
                ├── [MY FOODS TAB]
                │     if NOTHING saved & empty query → emptyMyFoodsView:
                │        VStack spacing=8, frame maxWidth ∞, vertical pad=48 →
                │          `heart` 32pt strakkTextTertiary ; "No saved foods yet" strakkBody strakkTextSecondary ;
                │          "Foods you add and favorites you save will show up here." strakkCaption strakkTextTertiary, centered, ph40
                │     if NOTHING & query present → noResultsView (see below)
                │     else, ordered sections:
                │       ├─ SectionHeader "FAVORITES"   (shown if any favorite meal/food)
                │       │     FavoriteMealCard … (one per favorite meal)
                │       │     FrequentFoodRow (isFavorited=true) … (one per favorite food)
                │       ├─ SectionHeader "RECENT MEALS"  (if any) → RecentMealCard …
                │       └─ SectionHeader "RECENT FOODS"  (if any) → FrequentFoodRow (clock icon) …
                │     SectionHeader = overline text, strakkTextTertiary, kerning 1.0, padding h=20 / top=16 / bottom=4
                │
                ├── [CATALOG TAB]
                │     if empty query → emptyCatalogView:
                │        VStack spacing=8, vpad=48 → `magnifyingglass` 32pt strakkTextTertiary ;
                │          "Search the food catalog" strakkBody strakkTextSecondary ;
                │          "Type to look up an item in CIQUAL / Open Food Facts." strakkCaption strakkTextTertiary ph40
                │     if results empty & searching → centered ProgressView, vpad=40
                │     if results empty & not searching → noResultsView
                │     else → SectionHeader "CATALOG" ; CatalogFoodRow … ; trailing ProgressView vpad=12 while still searching (paged load)
                │
                └── Spacer().frame(height:32)  (bottom breathing room)

  noResultsView: VStack spacing=12, vpad=48 → Text 'No results for "<query>"' strakkBody strakkTextSecondary centered ph40 ;
                 "Manual entry" button strakkCaptionBold strakkPrimary

  ── ROW ANATOMY ──────────────────────────────────────────────
  FrequentFoodRow (recent/favorite single food): VStack spacing=0
    ├─ HStack spacing=12, padding h=20 v=12 (whole row tappable → expand/collapse)
    │   ├─ Leading icon, 13pt, frame width 18: `heart.fill` strakkPrimary (favorited) OR `clock` strakkTextTertiary (recent)
    │   ├─ VStack spacing=2: name strakkBody strakkTextPrimary ; "<kcal> kcal" strakkCaption strakkTextSecondary
    │   ├─ Spacer()
    │   ├─ (if quantity) Text qty strakkCaption strakkTextTertiary
    │   └─ Favorite toggle button: `heart.fill`/`heart` 16pt medium, strakkPrimary/strakkTextTertiary, 32x32 tap
    ├─ (if selected) PortionRowView  — slides/fades in (transition opacity + move from top)
    └─ Divider, color strakkDivider (white@12%), inset padding leading=50

  CatalogFoodRow: VStack spacing=0
    ├─ HStack(align:.top) spacing=12, padding h=20 v=12 (tappable → expand)
    │   ├─ `book.closed` 13pt strakkTextTertiary, width 18, top pad 2
    │   └─ Details VStack spacing=6:
    │        ├─ HStack(top) spacing=8: [VStack spacing=2: name strakkBody strakkTextPrimary lineLimit2 ; brand strakkCaption strakkTextSecondary] + Spacer + NutriscoreBadge (if grade)
    │        ├─ CatalogMacroLine: "<kcal> kcal"(strakkCalories #FF9A55) · "<p> g prot"(strakkProtein #FF7A3D) · "<f> g lip"(strakkAccentYellow #FFC84D) · "<c> g gluc"(strakkAccentIndigo #637CFF) — caption, monospacedDigit, lineLimit1, minScale 0.85, dot separators strakkTextTertiary
    │        └─ HStack spacing=4: `scalemass` 10pt + "values per 100 g" — both strakkCaption strakkTextTertiary
    ├─ (if selected) PortionRowView
    └─ Divider strakkDivider, inset leading=50

  FavoriteMealCard / RecentMealCard: VStack spacing=0
    ├─ HStack spacing=12, padding h=20 v=12 (tappable → expand)
    │   ├─ icon 13pt width18: `heart.fill` strakkPrimary (favorite) / `fork.knife` strakkTextSecondary (recent)
    │   ├─ VStack spacing=2: meal name strakkBody strakkTextPrimary lineLimit1 ; subtitle "<n> item(s) · <kcal> kcal" strakkCaption strakkTextSecondary
    │   ├─ Spacer()
    │   └─ (FavoriteMealCard only) unfavorite button `heart.fill` 16pt strakkPrimary 32x32
    ├─ (if selected) AddMealCtaBar
    └─ Divider strakkDivider, inset leading=50

  AddMealCtaBar (expanded under a meal card): HStack, padding h=20 v=12, bg strakkSurface1 →
     Text "Add as a new meal for today" strakkCaption strakkTextSecondary + Spacer +
     "Add meal" button strakkCaptionBold white, padding h16 v10, bg strakkPrimary, Capsule

  PortionRowView (expanded under a food/catalog row): HStack spacing=12, padding h=20 v=12, bg strakkSurface1
    ├─ Stepper VStack spacing=4:
    │    HStack spacing=8: `minus.circle.fill` 22pt strakkSurface2 (−10g, floor 10) ; "<grams>g" strakkBodyBold strakkTextPrimary monospaced minWidth48 ; `plus.circle.fill` 22pt strakkPrimary (+10g)
    │    Text "<kcal> kcal · <prot>g prot" (live for current grams) strakkCaption strakkTextSecondary monospaced
    ├─ Spacer()
    └─ Add button: "Add" → "Adding..." (with white spinner scaled 0.7 when processing), strakkCaptionBold white, padding h16 v10, Capsule, bg strakkPrimary (→ strakkSurface2 when processing/disabled)

=================================================================
SURFACE 3 — BarcodeScannerView  (full-screen, edge-to-edge, ignores safe area)
=================================================================
Root: ZStack
 ├─ Camera layer: live VisionKit DataScanner feed filling the whole screen; native barcode highlight box drawn around any detected barcode (yellow rounded rect); guidance overlay enabled.
 └─ UI overlay: VStack
     ├─ Top row: HStack → Close button [ `xmark` 15pt semibold white, 44x44, background .ultraThinMaterial blurred Circle ] + Spacer()
     │     padding horizontal=lg(20), padding top = xxxl+lg (40+20 = 60)
     ├─ Spacer()  (pushes hint to bottom)
     └─ Bottom block: VStack spacing=sm(12), padding bottom = xxxl+lg (60)
          ├─ Text "Point the camera at a barcode" strakkCaption, color white@80%, centered
          └─ "Enter manually" button — strakkBodyBold white, padding h=xl(24) v=md(16), background .ultraThinMaterial Capsule

 Unavailable fallback (scanner unsupported/unavailable device): ZStack bg strakkBackground #050918 →
   StrakkEmptyState(icon `barcode.viewfinder` in 64x64 strakkSurface1 circle 24pt strakkTextSecondary; title "Scanner unavailable"; message "The barcode scanner is not available on this device."; primary button "Enter manually")
   + floating Close button top-leading (padding top 60, leading sm(12)).


## Components
- **SheetGrabber**: Native iOS drag indicator: centered ~36x5 pill, white@30%, top of every sheet. Web: render as a non-interactive handle; the sheet is swipe-down dismissible.
- **NavCloseButton (StrakkCloseButton)**: Leading nav-bar dismiss. SF xmark 14pt semibold, strakkTextSecondary, 44x44 hit area, leading offset -8. Press → strakkTextPrimary. aria-label 'Close'. Fires light haptic before dismiss.
- **AddOptionRow**: Picker list item. 60px tall strakkSurface1 card, radius 12, 16px inner padding. Left: 40px strakkSurface2 circle holding a 16pt orange glyph. Center: label. Right: optional PRO badge + 13pt tertiary chevron. Whole row is a button; light haptic on tap.
- **ProBadge**: 'PRO' text in caption2 bold orange, padding h6/v2, bg strakkPrimary@15%, radius 4. Shown only to non-Pro users on gated rows (Free text, Photo).
- **ProcessingInline**: Row shown while AI input is analyzing: orange spinner + 'Analyzing…' secondary text. Appears under the option list.
- **SearchBar**: Native iOS .searchable field under the title: rounded grey field, leading magnifier, dynamic placeholder per tab, clear button when text present. Web: rounded input, debounced onChange.
- **SegmentedTabPicker**: Two-segment iOS control 'My foods' | 'Catalog'. Selected segment is a raised light pill on a dark track. Switching tabs fires a SwitchTab event and changes the search placeholder.
- **SectionHeader**: Uppercase overline label (caption2 bold, strakkTextTertiary, +1.0 letter-spacing). Values: FAVORITES, RECENT MEALS, RECENT FOODS, CATALOG. padding h20/top16/bottom4.
- **FrequentFoodRow**: Single saved/recent food row. Leading heart.fill(orange)=favorite or clock(tertiary)=recent. Two-line title+kcal, trailing quantity, trailing 32px heart toggle. Tapping the row expands an inline PortionRowView. Bottom divider inset 50px (aligns under text, not icon).
- **CatalogFoodRow**: Catalog result. Leading book.closed icon. Name (2 lines) + optional brand, optional Nutriscore badge top-right, a colored macro line (kcal·prot·lip·gluc), and a 'values per 100 g' caption with a scale glyph. Tap expands PortionRowView.
- **NutriscoreBadge**: 22x22 rounded(6) square, white bold 11pt letter A–E, background by grade: A #1F8F3D, B #85BA2E, C #F2C233, D #E67D21, E #BF382B (else tertiary).
- **CatalogMacroLine**: Single monospaced caption line: kcal (strakkCalories #FF9A55) · prot (strakkProtein #FF7A3D) · lip (strakkAccentYellow #FFC84D) · gluc (strakkAccentIndigo #637CFF), separated by tertiary middots; shrinks to 0.85 scale to fit one line.
- **FavoriteMealCard / RecentMealCard**: Saved/recent multi-item meal. Leading heart.fill(orange)/fork.knife(secondary), name + 'N items · X kcal' subtitle. Favorite variant has a trailing unfavorite heart. Tap expands AddMealCtaBar.
- **AddMealCtaBar**: Expanded strip under a meal card on strakkSurface1: caption 'Add as a new meal for today' + a pill 'Add meal' button (white text on orange capsule).
- **PortionRowView**: Expanded inline portion editor on strakkSurface1: −/+ stepper in 10g steps (minus is grey strakkSurface2, plus is orange) with monospaced grams readout (min 48px), a live 'kcal · g prot' recalculation, and an orange 'Add' capsule that becomes 'Adding...' with spinner while saving.
- **EmptyState (myFoods/catalog/noResults)**: Centered icon (32pt tertiary) + secondary headline + tertiary helper caption, 48px vertical padding. noResults adds a 'Manual entry' orange text button.
- **ErrorState**: Centered exclamationmark.triangle 40pt strakkError, message, and orange 'Retry' button.
- **CameraScannerOverlay**: Full-screen live camera with detection highlight. Floating top-left blurred-circle close button, bottom hint text + blurred-capsule 'Enter manually' button. Web equivalent: getUserMedia video + BarcodeDetector/zxing, ultraThin blur = backdrop-filter blur(~20px) over translucent white.
- **ScannerUnavailableState**: Fallback when no camera: StrakkEmptyState with barcode.viewfinder in a 64px surface1 circle, title/message, and 'Enter manually' primary button + floating close.

## Interactions
- Open AddPicker: presents as a 45%-height bottom sheet with visible grabber; swipe-down or tap Close dismisses (light haptic on Close).
- Tap any option row: light haptic, then routes — 'Search' opens SearchFoodView as a .large (full-height) sheet; 'Manual' opens ManualEntryView sheet; 'Free text' opens TextEntryView sheet; 'Photo' opens PhotoMealView as a fullScreenCover.
- Pro gating: for non-Pro users, tapping 'Free text' or 'Photo' does NOT open the tool — it calls onFeatureGated(feature) and dismisses the picker (parent then shows the paywall). Rows show a PRO badge.
- AddPicker AI completion: when quick-add finishes (text/photo), fire light impact haptic and auto-dismiss the sheet. Errors surface via an alert bound to errorMessage.
- SearchFood typing: each keystroke fires SearchFoodEventQueryChanged (debounced in the ViewModel); the search placeholder text depends on the active tab.
- Switch tab (My foods / Catalog): fires SearchFoodEventSwitchTab; content and placeholder swap. iOS segmented control animates the selection pill.
- Tap a food/catalog/meal row: toggles an inline expansion with withAnimation(.easeInOut, 0.15) using transition opacity + move-from-top. Selecting one row collapses any other (single-selection across all sections). Selecting a catalog item seeds the stepper with its defaultPortionGrams; a saved food seeds 100g.
- Portion stepper: minus floors at 10g (−10), plus adds +10g; readout and the live 'kcal · g prot' line recompute as proteinPer100*grams/100 and caloriesPer100*grams/100.
- Tap 'Add' on a portion: in Quick-add mode calls addKnown(...) with quantity formatted '<grams>g'; in draft mode adds the item to the meal draft and dismisses. Button shows spinner + 'Adding...' and disables (bg → strakkSurface2) while processing; on completion the sheet dismisses.
- Tap 'Add meal' on an expanded meal card: adds the favorite/recent meal as a template; on success fires a success notification haptic and dismisses the search sheet.
- Favorite toggle (heart): light haptic; FrequentFoodRow toggles favorite via ToggleFavoriteFood; FavoriteMealCard unfavorites via UnfavoriteMeal. Icon swaps heart ↔ heart.fill and tertiary ↔ orange.
- Error/Retry: error state 'Retry' button re-fires the last query/load. 'Manual entry' links in empty/no-result states dismiss the sheet (handing off to manual entry).
- Barcode scan capture: on first valid barcode the coordinator guards against repeats (didScan), fires a medium impact haptic, and calls onScan(value) once on the main thread; VisionKit shows live guidance + a highlight box around the detected code.
- Scanner controls: top-left Close button (light haptic) cancels; 'Enter manually' button hands off to manual barcode entry. If the device has no DataScanner support, render the unavailable empty-state instead of the camera.
- All sheets present over a dimmed/blurred backdrop; closing returns to the parent (TodayView / meal draft). Web: use Motion spring sheet transitions, focus-trap, Escape-to-close, and backdrop-filter blur for the ultraThinMaterial scanner chrome.

## Copy strings
- Quick add
- Add to meal
- Search
- Manual
- Free text
- Photo
- PRO
- Analyzing…
- Close
- Search a food
- Search my foods
- Apple, chicken…
- My foods
- Catalog
- Retry
- FAVORITES
- RECENT MEALS
- RECENT FOODS
- CATALOG
- No saved foods yet
- Foods you add and favorites you save will show up here.
- Search the food catalog
- Type to look up an item in CIQUAL / Open Food Facts.
- No results for "<query>"
- Manual entry
- values per 100 g
- Add as a new meal for today
- Add meal
- Add
- Adding...
- Favorite
- Remove favorite
- Point the camera at a barcode
- Enter manually
- Scanner unavailable
- The barcode scanner is not available on this device.
- <n> item · <kcal> kcal
- <n> items · <kcal> kcal
- <kcal> kcal
- <grams>g
- <kcal> kcal · <prot>g prot
- <kcal> kcal · <p> g prot · <f> g lip · <c> g gluc

## Mock data

ADD PICKER (non-draft, free user): rows Search / Manual / Free text [PRO] / Photo [PRO]. Title "Quick add".

SEARCH — MY FOODS TAB (populated):
  FAVORITES
    • [meal] "Chicken & Rice Bowl" — 3 items · 620 kcal   (unfavorite heart)
    • [food] "Skyr Vanilla" — 96 kcal — qty 170g   (orange heart.fill; per-100g: 63 kcal, 10g prot)
  RECENT MEALS
    • "Post-Workout Shake" — 2 items · 410 kcal
    • "Breakfast Oats + Berries" — 4 items · 480 kcal
  RECENT FOODS
    • [clock] "Banana" — 107 kcal — qty 120g   (per-100g: 89 kcal, 1.1g prot)
    • [clock] "Greek Yogurt 0%" — 89 kcal — qty 150g   (per-100g: 59 kcal, 10g prot)
    • [clock] "Whey Protein (1 scoop)" — 120 kcal — qty 30g

SEARCH — CATALOG TAB (query "chicken"):
  CATALOG
    • "Chicken breast, raw"           Nutriscore A   119 kcal · 23 g prot · 2 g lip · 0 g gluc   · values per 100 g  (default portion 100g)
    • "Grilled chicken thigh"         Nutriscore B   209 kcal · 26 g prot · 11 g lip · 0 g gluc  · values per 100 g
    • "Chicken nuggets" — brand "Findus"   Nutriscore D   267 kcal · 14 g prot · 16 g lip · 16 g gluc · values per 100 g
    • "Coca-Cola Original" — brand "Coca-Cola"   Nutriscore E   42 kcal · 0 g prot · 0 g lip · 11 g gluc · values per 100 g

PORTION EDITOR example (Chicken breast, selected, default 100g → user taps + to 150g):
  stepper shows "150g" ; live line "179 kcal · 35g prot" ; Add button enabled (orange). Quantity stored as "150g".

EMPTY STATES: My foods empty → "No saved foods yet". Catalog empty query → "Search the food catalog". No match → 'No results for "kiwi"'.

BARCODE SCANNER: live camera, hint "Point the camera at a barcode". Example successful scan payload "3017620422003" (Nutella 400g) → resolves to a catalog product and dismisses with the code; medium haptic on capture.


## Design tokens

COLORS (hex): bg strakkBackground #050918 · strakkBackgroundElevated #080D1F · cards strakkSurface1 #10162F · icon-chip/inactive strakkSurface2 #151B38 · strakkSurface3 #1A2142 · accent/primary/protein strakkPrimary=strakkAccentOrange=strakkProtein #FF7A3D · strakkPrimaryLight/strakkCalories #FF9A55 · lipids strakkAccentYellow #FFC84D · carbs strakkAccentIndigo #637CFF · water strakkWater #4B8DFF · text-primary #F4F6FF · text-secondary #9CA1B8 · text-tertiary #6F748C · text-disabled #50566F · divider = white@12% · error #E05252 · success #4DAE6A · ProBadge bg = #FF7A3D@15% · Nutriscore A #1F8F3D / B #85BA2E / C #F2C233 / D #E67D21 / E #BF382B.
SPACING (px): xxs4 · xs8 · sm12 · md16 · lg20 · xl24 · xxl32 · xxxl40. Common: rows pad h20/v12; option rows pad h16; section header h20/top16/bottom4; scanner top & bottom inset = 60 (xxxl+lg).
RADIUS (px): sm12 (option cards) · md18 · lg24 · xl32 · xxl36 · xxxl56. ProBadge 4 · Nutriscore 6 · pills/CTAs = Capsule (fully rounded). Icon chip & avatars = full circle.
TYPOGRAPHY (semantic → resolved at default Dynamic Type): strakkBody = subheadline ~15pt regular · strakkBodyBold = subheadline ~15pt semibold · strakkBodyLarge = body ~17pt medium · strakkCaption = caption ~12pt regular · strakkCaptionBold = caption ~12pt semibold · strakkOverline/SectionHeader = caption2 ~11pt bold +1.0 tracking · strakkHeading3 = headline ~17pt semibold · inline nav title ~17pt semibold. Numeric readouts (grams, kcal, macro line) use monospacedDigit.
EFFECTS: blurred chrome on scanner = .ultraThinMaterial (web: backdrop-filter blur ~20px over rgba(255,255,255,0.12)). Expand/collapse of portion/CTA rows = easeInOut 0.15s, transition opacity + slide from top edge. Haptics: light on most taps & toggles, success notification when a meal template is added, medium on barcode capture, light on quick-add completion.
ICONS (SF Symbols → map to web icon set): magnifyingglass, pencil, text.quote, camera.fill, chevron.right, heart / heart.fill, clock, fork.knife, book.closed, scalemass, minus.circle.fill, plus.circle.fill, exclamationmark.triangle, xmark, barcode.viewfinder.
DETENTS/PRESENTATION: AddPicker = bottom sheet at 45% height with grabber; SearchFood = full-height (.large) sheet with grabber; BarcodeScanner = full-screen cover, edge-to-edge camera. Sub-flows from picker: Search→sheet, Manual→sheet, Free text→sheet (Pro-gated), Photo→fullScreenCover (Pro-gated).
