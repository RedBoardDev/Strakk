# Spec: Weekly Check-ins — List + Trends/Stats (two-screen module: CheckInListView pushes CheckInStatsView)

## Purpose
Hub for the user's weekly body-progress check-ins: a scrollable list with quick body-metric stat cards (Screen A), drilling into a full Trends dashboard with weight/volume charts, macro compliance, and consistency (Screen B).

## Layout
Two screens. Both are dark (Color.strakkBackground #050918 filling full safe area, ignoresSafeArea) inside a NavigationStack with a LARGE iOS nav title. Body is a vertical ScrollView. While `state == .loading` the body is a single centered ProgressView tinted strakkPrimary (#FF7A3D). When `.ready`, render the trees below.

=== SCREEN A — CheckInListView (nav title "Check-ins", large display mode) ===
Nav bar: large title "Check-ins" on left; trailing toolbar primaryAction = a borderless icon button `plus.circle.fill` at SF font `.title3` (~20pt), color strakkPrimary. a11y "New check-in".
ScrollView > LazyVStack(alignment: .leading, spacing: xl=24), padded `.horizontal lg=20`, `.vertical xl=24`. Top-to-bottom:

1. QUICK STATS section (only if quickStats != nil), VStack(spacing: sm=12):
   1a. Overline label "QUICK STATS" — font strakkOverline (caption2 bold ~11), color strakkTextTertiary (#6F748C). NOTE: literal string is already uppercase; do NOT add letter-spacing beyond what bold caption2 gives.
   1b. HStack(spacing: xs=8) of 3 equal-width stat cards (each `.frame(maxWidth:.infinity)`):
       Card = VStack(alignment:.leading, spacing: xs=8), padding sm=12, background strakkSurface1 (#10162F), clipShape RoundedRectangle cornerRadius 12. Card contents:
         - title (strakkOverline, strakkTextTertiary, lineLimit 1, minimumScaleFactor 0.8)
         - value row: HStack(.firstTextBaseline, spacing 2) — big number strakkHeading3 (headline semibold ~17) strakkTextPrimary (#F4F6FF) + unit strakkCaption (~12) strakkTextSecondary (#9CA1B8). If value nil → single "—" strakkHeading3 strakkTextTertiary.
         - delta label (see deltaLabel): if nil → "—" caption tertiary; if 0 → "=" ; if >0 → "↑ +1.2" ; if <0 → "↓ -0.8". ALL delta text is strakkCaption color strakkTextSecondary (neutral grey — NOT red/green here).
       The 3 cards in order: ["Weight" / kg], ["Avg. arms" / cm], ["Waist" / cm].
   1c. "View detailed stats" button (left-aligned, full width, padded top xxs=4): HStack(spacing xs=8) of Text strakkCaptionBold (caption semibold) strakkPrimary + `chevron.right` SF system size 11 weight semibold strakkPrimary.

2. CHECK-INS section, VStack(alignment:.leading, spacing: sm=12). Two states:
   2a. If checkIns empty AND hiddenCount==0 → EMPTY STATE (VStack spacing md=16, centered, paddingTop xxxl=40, horizontal md=16):
       - `chart.line.uptrend.xyaxis` SF size 48, strakkTextTertiary
       - "No check-ins yet" strakkHeading3 strakkTextPrimary
       - "Start tracking your progress by creating your first weekly check-in." strakkBody strakkTextSecondary, centered multiline
       - "Get started" primary button: Text strakkBodyBold white, frame maxWidth infinity, height 52, background strakkPrimary, corner radius 12, paddingTop xs=8.
   2b. Else (has items or hidden history):
       - Overline "RECENT CHECK-INS" strakkOverline strakkTextTertiary
       - ForEach check-in card (full-width tappable button): HStack(spacing sm=12), padding md=16, background strakkSurface1, radius 12. Left = VStack(.leading, spacing xxs=4): week title "Week {n}" strakkBodyBold strakkTextPrimary, then a metadata HStack(spacing sm=12) of SF `Label`s at strakkCaption: weight "{x} kg" with `scalemass` icon (strakkTextSecondary, only if weight present); photo count "{n}" with `camera` icon (strakkTextSecondary, always); "AI" with `sparkles` icon (strakkPrimary, only if hasAiSummary). Right = Spacer + `chevron.right` SF size 13 semibold strakkTextTertiary.
       - If hiddenCount>0 → HISTORY-LIMIT BANNER (tappable button): HStack(spacing sm=12), padding md=16, background strakkSurface1, radius 12, PLUS an overlay RoundedRectangle(radius 12).strokeBorder strakkPrimary @ opacity 0.25, lineWidth 1. Contents: `lock.fill` SF size 15 medium strakkTextTertiary inside a 30×30 strakkSurface2 (#151B38) rounded-8 square; VStack(.leading, spacing 2): "{n} older check-ins" strakkBodyBold strakkTextSecondary + "Unlock full history with Pro" strakkCaption strakkTextTertiary; Spacer; ProBadge.
   ProBadge = Text "PRO" strakkOverline strakkPrimary, padding h6/v2, background strakkPrimary@0.15 rounded-4.

=== SCREEN B — CheckInStatsView (nav title "Trends", large display mode) ===
ScrollView > VStack(alignment:.leading, spacing: xl=24), padded `.horizontal lg=20`, `.vertical xl=24`. Loading state same centered ProgressView. Top-to-bottom:

1. PERIOD PICKER: native SwiftUI `.pickerStyle(.segmented)` (UISegmentedControl look) with 3 segments "4 wk" | "12 wk" | "All". a11y "Display period". Full width.

2. OVERVIEW 2×2 GRID: LazyVGrid, 2 flexible columns, column spacing sm=12, row spacing sm=12. Each cell = OverviewCard: VStack(.leading, spacing sm=12), frame maxWidth infinity leading, padding md=16, background strakkSurface1, radius 12. Card top = a 36×36 RoundedRectangle(radius 8) filled iconColor@0.12 with centered SF icon size 16 semibold in iconColor. Then the value content. Then bottom label strakkCaption strakkTextTertiary. The four cards:
   - "Weight": icon `scalemass.fill` color strakkPrimary. Value = VStack(.leading, spacing xxs=4): "{x.x} kg" strakkHeading2 (title2 bold ~22) strakkTextPrimary monospacedDigit; below a weightTrendLabel HStack(spacing 2): arrow + "{sign}{delta} kg" strakkCaption. TREND COLOR LOGIC (cut context): delta>0 (gained) → strakkError #E05252 + "↑"; delta<0 (lost) → strakkSuccess #4DAE6A + "↓"; delta==0 → strakkTextTertiary, no arrow. Positive prefixed "+". If weightKg nil → "—" heading2 tertiary.
   - "Avg volume / wk": icon `dumbbell.fill` color strakkAccentIndigo #637CFF. Value = formatVolume() heading2 primary monospaced. (formatVolume: kg>=1000 → "%.1ft"; else "%.0f kg".)
   - "Sessions / wk": icon `figure.strengthtraining.traditional` color strakkSuccess. Value = "%.1f" sessions heading2 primary monospaced.
   - "Nutrition": icon `fork.knife` color strakkWarning #E0A84D. Value = HStack(.center, spacing xs=8): "{p}%" heading2 primary monospaced + ComplianceGauge ring. ComplianceGauge = 28×28 ZStack: full Circle stroke strakkSurface2 lineWidth 4 + trimmed Circle (0→fraction) stroke colored lineWidth 4 round-cap, rotated -90°. Gauge color: ≥80 strakkSuccess, ≥50 strakkWarning, else strakkError. Animates easeOut 0.25 on fraction change.

3. WEIGHT chart section (only if weightSeries.count>=2): VStack(.leading, spacing sm=12). Overline "WEIGHT". Card = VStack padding md=16 background strakkSurface1 radius 12, containing a Swift Charts `Chart` height 160: per point an AreaMark (LinearGradient strakkAccentOrange@0.25 → clear, top→bottom) + LineMark strakkAccentOrange, both `.interpolationMethod(.catmullRom)` (smooth curve). X axis = abbreviated week labels ("W26") strakkCaption strakkTextTertiary. Y axis leading, ~4 auto ticks, AxisGridLine strakkDivider + labels strakkCaption tertiary. Transparent chart background.

4. TRAINING section (only if training != nil): VStack(.leading, spacing sm=12). Overline "TRAINING". Card (padding md=16, strakkSurface1, radius 12) = VStack(spacing md=16):
   - Volume chart (only if volumeSeries.count>=2): same Chart style as weight but height 120, color strakkAccentIndigo (Area gradient indigo@0.25→clear + indigo line, catmullRom); Y axis ~3 ticks.
   - Summary row: HStack(spacing 0) of 3 StatInlineItems separated by two vertical Dividers height 32 strakkDivider. Each StatInlineItem = VStack(spacing xxs=4) value strakkBodyBold strakkTextPrimary monospaced + label strakkCaption strakkTextTertiary, frame maxWidth infinity. Items: "Sessions" (count), "Duration" (formatDuration: h>0 → "{h}h{mm}", else "{m}m"), "Volume" (formatVolume).

5. NUTRITION section (only if nutrition != nil): VStack(.leading, spacing sm=12). Overline "NUTRITION". Card (padding md=16, strakkSurface1, radius 12) = VStack(spacing md=16):
   - ForEach macro → MacroComplianceRow: VStack(.leading, spacing xs=8): HStack[ name strakkBody strakkTextSecondary + Spacer + "{p}%" strakkCaption strakkTextTertiary monospaced ]; then a 6pt-tall progress bar = GeometryReader ZStack(.leading): track Capsule strakkSurface2 + fill Capsule width=geo*fraction. Fill color: if percentage>=100 → strakkSuccess; else by name: Calories→strakkPrimaryLight #FF9A55, Protein→strakkPrimary, Carbs→strakkAccentIndigo, Fat→strakkAccentYellow #FFC84D. fraction = min(p/100,1).
   - Water row (only if avgWater & waterGoal>0): VStack(.leading, spacing xs=8): HStack[ `drop.fill` size 12 strakkWater #4B8DFF + "Water" strakkBody strakkTextSecondary | Spacer | "{avg} / {goal} ml" strakkCaption strakkTextTertiary monospaced ]; then 6pt progress bar track strakkSurface2 / fill strakkWater, fraction = min(avg/goal,1).

6. CONSISTENCY card: VStack(.leading, spacing xs=8). Overline "CONSISTENCY". Card (padding md=16, strakkSurface1, radius 12) = VStack(.leading, spacing sm=12): HStack[ "{n}/{total} weeks" strakkBodyBold strakkTextPrimary | Spacer | "{p}%" strakkHeading3 strakkPrimary monospaced ]; then an 8pt-tall progress bar GeometryReader ZStack(.leading): track Capsule strakkSurface2 + fill Capsule strakkPrimary width=geo*p/100. a11y "Consistency: {p}%".

## Components
- **CheckInListView (Screen A)**: NavigationStack root, large title 'Check-ins', trailing plus.circle.fill add button, ScrollView+LazyVStack(spacing 24, pad h20/v24). Holds loading ProgressView + ready content.
- **QuickStatsSection**: Overline 'QUICK STATS' + HStack of 3 equal QuickStatCards + 'View detailed stats' chevron link button.
- **QuickStatCard**: strakkSurface1 card radius12 pad12: overline title (tertiary), value(heading3 primary)+unit(caption secondary) baseline-aligned, delta line (caption secondary, '↑ +x' / '↓ -x' / '=' / '—'). Width flexible.
- **CheckInCard**: Tappable strakkSurface1 row radius12 pad16: left VStack week title (bodyBold)+meta SF Labels (scalemass weight, camera count, sparkles 'AI'); right chevron.right tertiary.
- **HistoryLimitBanner**: Tappable strakkSurface1 row radius12 pad16 with strakkPrimary@0.25 1pt border: lock.fill in 30x30 strakkSurface2 square + '{n} older check-ins' + 'Unlock full history with Pro' + ProBadge. Triggers paywall gate.
- **ProBadge**: 'PRO' overline strakkPrimary, pad h6/v2, strakkPrimary@0.15 fill, radius4.
- **EmptyState**: Centered: chart.line.uptrend.xyaxis 48pt tertiary, 'No check-ins yet' heading3, body subtitle, 'Get started' full-width 52pt orange button.
- **CheckInStatsView (Screen B)**: Large title 'Trends', ScrollView+VStack(spacing 24, pad h20/v24). Loading ProgressView + ready content.
- **PeriodPicker**: Native segmented control: '4 wk' / '12 wk' / 'All'. Emits OnPeriodSelected(fourweeks/twelveweeks/all).
- **OverviewCard**: strakkSurface1 card radius12 pad16: 36x36 rounded-8 tinted-12% icon chip + value content + caption label. Generic content slot.
- **WeightTrendLabel**: Arrow+'{sign}{delta} kg' caption; red(error)+↑ on gain, green(success)+↓ on loss, tertiary on 0.
- **ComplianceGauge**: 28x28 ring, 4pt track strakkSurface2 + trimmed colored arc (success/warning/error by %), -90° start, easeOut 0.25 animation.
- **WeightChart**: Swift Charts AreaMark(orange@0.25→clear gradient)+LineMark(strakkAccentOrange), catmullRom, height160, leading Y axis 4 ticks, abbreviated W## X labels.
- **TrainingVolumeChart**: Same chart style, strakkAccentIndigo, height120, 3 Y ticks.
- **TrainingSummaryRow**: 3 StatInlineItems (Sessions/Duration/Volume) split by 2 vertical 32pt dividers.
- **StatInlineItem**: VStack value(bodyBold primary monospaced)+label(caption tertiary), centered, flexible width.
- **MacroComplianceRow**: Name+'{p}%' header then 6pt capsule progress; fill color per-macro (calories light-orange, protein orange, carbs indigo, fat yellow) or green when >=100%.
- **WaterRow**: drop.fill + 'Water' + '{avg} / {goal} ml' then 6pt strakkWater capsule progress.
- **ConsistencyCard**: '{n}/{total} weeks' bodyBold + '{p}%' heading3 orange, then 8pt strakkPrimary capsule progress bar.
- **FeatureGateSheet**: Bottom sheet detent height 420 with drag indicator: gradient glow icon, ProBadge, title 'Unlimited history', desc, 'Unlock with Pro' 52pt orange CTA (light haptic→paywall), 'Later' dismiss.

## Interactions
- Tap '+' (plus.circle.fill, top-right): emits OnCreateNew → presents CheckInWizardView as a fullScreenCover (modal sliding up from bottom, covers whole screen).
- Tap 'View detailed stats': emits OnOpenStats → NavigationStack push to CheckInStatsView (standard iOS horizontal slide-in; large 'Check-ins' title collapses to back button).
- Tap any CheckInCard: emits OnOpenDetail(id) → push CheckInDetailView (horizontal slide).
- Tap HistoryLimitBanner (lock row): emits OnUnlockHistory → sets gatedFeature(unlimitedHistory) → presents FeatureGateSheet as a sheet at presentationDetents([.height(420)]) with presentationDragIndicator(.visible).
- FeatureGateSheet 'Unlock with Pro' CTA: fires HapticEngine.light() (light impact haptic) → dismisses sheet → presents PaywallView as fullScreenCover. 'Later' just dismisses the sheet.
- Empty-state 'Get started' button: same as '+', opens wizard fullScreenCover.
- Loading: centered ProgressView tinted strakkPrimary while state==.loading on either screen.
- Period segmented control change (Screen B): emits OnPeriodSelected(fourweeks|twelveweeks|all); overview, charts and summaries recompute/reload. Native segmented selection slide animation.
- ComplianceGauge ring animates with .easeOut(duration 0.25) whenever its fraction (compliance %) changes.
- Charts are static (no scrub/tap/long-press interaction implemented) — line+area with catmullRom smoothing only.
- No swipe-to-delete / pull-to-refresh on the list. Back-swipe gesture / back button returns from Stats and Detail to the list (standard NavigationStack).
- No haptics on the list or stats screens themselves; the only haptic in this flow is the paywall CTA light impact.

## Copy strings
- Check-ins
- New check-in
- QUICK STATS
- Weight
- Avg. arms
- Waist
- kg
- cm
- View detailed stats
- RECENT CHECK-INS
- Week 26
- AI
- 8 older check-ins
- Unlock full history with Pro
- PRO
- No check-ins yet
- Start tracking your progress by creating your first weekly check-in.
- Get started
- Create my first check-in
- =
- ↑ +0.2
- ↓ -0.6
- —
- Trends
- Display period
- 4 wk
- 12 wk
- All
- Avg volume / wk
- Sessions / wk
- Nutrition
- WEIGHT
- TRAINING
- Sessions
- Duration
- Volume
- NUTRITION
- Calories
- Protein
- Carbs
- Fat
- Water
- 2600 / 3000 ml
- CONSISTENCY
- 11/12 weeks
- 92%
- Consistency: 92%
- Unlock with Pro
- Later
- Unlimited history
- Access your full history without limit.

## Mock data
SCREEN A (CheckInListView): QuickStats — Weight 78.4 kg, delta '↓ -0.6'; Avg. arms 38.5 cm, delta '↑ +0.2'; Waist 82.0 cm, delta '↓ -1.0'. RECENT CHECK-INS (most-recent first): [Week 26 · 78.4 kg · 4 photos · AI✓], [Week 25 · 79.0 kg · 4 photos · AI✓], [Week 24 · 79.2 kg · 2 photos · no AI], [Week 23 · 80.1 kg · 3 photos · AI✓], [Week 22 · 80.6 kg · 4 photos · AI✓]. hiddenCount = 8 → banner 'Unlock full history with Pro'. (Empty-state variant: 0 check-ins, 0 hidden.)
SCREEN B (CheckInStatsView): selected period = '12 wk'. Overview grid: Weight 78.4 kg, trend delta -1.7 → '↓ -1.7 kg' in green; Avg volume / wk = 11.9t (avgWeeklyVolumeKg 11850); Sessions / wk = 3.8; Nutrition 86% (gauge green, ≥80). Weight chart points (weekLabel→kg, 12 weeks): 2026-W15 80.6, W16 80.3, W17 80.1, W18 79.9, W19 79.7, W20 79.5, W21 79.2, W22 79.0, W23 78.9, W24 78.7, W25 78.6, W26 78.4 (X labels render 'W15'…'W26'). Training: volumeSeries (kg) ≈ W15 9800, W16 10250, W17 10800, W18 11200, W19 10950, W20 11500, W21 11850, W22 12100, W23 11700, W24 12400, W25 12050, W26 12600; totalSessions 45; totalDurationMin 3120 → '52h00'; totalVolumeKg 142200 → '142.2t'. Nutrition macros: Calories 92%, Protein 88%, Carbs 79%, Fat 84%; avgWater 2600, waterGoal 3000 → '2600 / 3000 ml'. Consistency: checkInCount 11, totalWeeks 12 → '11/12 weeks', 92%.

## Design tokens
COLORS (hex): strakkBackground #050918 (screen bg); strakkSurface1 #10162F (every card/banner); strakkSurface2 #151B38 (progress-bar tracks, gauge track, 30x30 lock chip); strakkPrimary/strakkAccentOrange #FF7A3D (accent: add button, links, CTAs, weight chart line+area, protein bar, consistency bar, PRO badge, weight-card icon); strakkPrimaryLight #FF9A55 (calories macro bar); strakkAccentIndigo #637CFF (volume chart line+area, volume icon, carbs bar); strakkAccentYellow #FFC84D (fat macro bar); strakkWater #4B8DFF (water icon+bar); strakkSuccess #4DAE6A (weight-loss trend, sessions icon, gauge ≥80, macro ≥100%); strakkError #E05252 (weight-gain trend, gauge <50); strakkWarning #E0A84D (nutrition icon, gauge 50–79); strakkTextPrimary #F4F6FF; strakkTextSecondary #9CA1B8; strakkTextTertiary #6F748C; strakkDivider rgba(255,255,255,0.12) (chart gridlines + summary dividers). Border: strakkPrimary@0.25 1pt on history banner; icon chips use iconColor@0.12 fill; ProBadge bg strakkPrimary@0.15; chart area gradients accentColor@0.25→clear.
SPACING (pt): xxs 4, xs 8, sm 12, md 16, lg 20, xl 24, xxl 32, xxxl 40. Section blocks spaced xl=24; card padding md=16 (quick-stat cards sm=12); inner label spacing sm=12; screen pad horizontal lg=20 + vertical xl=24.
RADIUS (pt): cards/banners/charts 12 (StrakkRadius.sm / literal 12); icon chips 8; ProBadge 4; paywall CTA 18 (StrakkRadius.md). Progress bars & gauge use Capsule (fully rounded).
TYPOGRAPHY (iOS Dynamic Type → ~px at default): strakkDisplay large nav title ~34 bold; strakkHeading2 = title2 bold ~22 (overview card values); strakkHeading3 = headline semibold ~17 (quick-stat values, consistency %, empty title); strakkBodyBold = subheadline semibold ~15 (card titles, StatInlineItem values); strakkBody = subheadline regular ~15 (macro/water labels, empty subtitle); strakkCaption = caption regular ~12 (deltas, meta labels, axis labels, % values); strakkCaptionBold = caption semibold ~12 ('View detailed stats'); strakkOverline = caption2 bold ~11 (section headers, quick-stat titles, PRO). Numeric values use monospacedDigit. Chart sizing: weight chart height 160, training volume chart 120; gauge 28x28 stroke 4; macro/water bars height 6, consistency bar height 8; summary dividers height 32.