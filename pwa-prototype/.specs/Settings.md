# Spec: Settings (account / Pro plan / daily goals / integrations) + Pro Paywall (full-screen cover)

## Purpose
Settings is a scrollable preferences list (account, current subscription plan card, editable daily macro goals, Hevy API key, data attribution, sign out); the Pro Paywall is a premium brochure-style full-screen upsell with a feature list and two pinned price-plan cards plus a gradient subscribe CTA.

## Layout
=== SCREEN A: SETTINGS ===
Root: ZStack { full-bleed background Color #050918 (ignoresSafeArea) ; content }. Two states: .loading and .ready.

LOADING STATE: VStack(align:leading, spacing:0) -> "Settings" title (strakkHeading1, #F4F6FF, padding leading/trailing 20, padding top 16) -> Spacer -> ProgressView (tint #FF7A3D, centered, maxWidth infinity) -> Spacer.

READY STATE: ScrollView -> VStack(align:leading, spacing:0), strictly top-to-bottom, gaps created by fixed-height Spacers:
1. Header "Settings" (strakkHeading1 ~28/700, #F4F6FF) padding horizontal 20, padding top 16.
2. Spacer height 24.
3. ACCOUNT section: overline label "ACCOUNT" (strakkOverline ~11/700, #6F748C, kerning 1.0, padding horizontal 20) ; 8pt gap ; card = single row inside RoundedRectangle r=12 fill #10162F, outer horizontal margin 20. Row HStack padding horizontal 16 / vertical 14: SF "envelope" (size 13, #6F748C) + 4pt spacer + "Email" (strakkCaptionBold ~12/600, #9CA1B8) + Spacer + email value (strakkBody ~15/400, #F4F6FF, lineLimit 1, right-aligned).
4. Spacer height 24.
5. CURRENT PLAN section: overline "CURRENT PLAN" (#6F748C, kerning 1.0, padding horizontal 20) ; 8pt gap ; ONE of four plan-card variants (see components: Free / Trial / Active / PaymentFailed). All plan cards: RoundedRectangle r=12 fill #10162F, padding 16, horizontal margin 20, internal VStack spacing 12 (StrakkSpacing.sm).
6. [DEV BUILDS ONLY — bundle display name == "Strakk Dev"; omit on web/prod] Spacer 24 + DEV SUBSCRIPTION section: overline + a single row card (r=12 #10162F, padding h16/v14) "Plan" (strakkBodyBold) + Spacer + native menu Picker tinted #FF7A3D.
7. Spacer height 24.
8. DAILY GOALS section: overline "DAILY GOALS" (kerning 1.0) ; 8pt gap ; ONE card r=12 fill #10162F (horizontal margin 20) containing 5 goal rows separated by full-bleed-minus-leading Dividers. Card has an overlay RoundedRectangle r=12 strokeBorder that is Color.clear normally and #FF7A3D @ 0.5 lineWidth 1 when ANY goal field is focused (animated). Each goalRow HStack padding h16/v14: colored Circle 8x8 + 8pt spacer + label (strakkBodyBold #F4F6FF) + Spacer + trailing TextField (strakkHeading3 ~17/600, monospaced digits, right-aligned, numberPad, tint #FF7A3D, maxWidth 80) + unit (strakkCaption #6F748C). Divider color white@0.12 with leading inset 16. Row order: Protein / Calories / Fat / Carbs / Water.
9. Spacer height 24.
10. HEVY INTEGRATION section: overline ; card r=12 #10162F (margin 20), single row padding h16/v14: SF "dumbbell.fill" (size 13, #6F748C) + 8pt spacer + "API Key" (strakkBodyBold #F4F6FF) + Spacer + SecureField placeholder "Paste Hevy API key" (strakkBody #F4F6FF, right-aligned, maxWidth 200, tint #FF7A3D).
11. Spacer height 24.
12. DATA SOURCES section: overline "DATA SOURCES" (#6F748C, NO kerning) ; 12pt gap ; VStack(align:leading, spacing 8, padding horizontal 20): "Food data provided by:" (strakkCaption #9CA1B8) / "• Open Food Facts (ODbL)" / "• CIQUAL 2020 — ANSES" (both strakkCaption #9CA1B8).
13. Spacer height 32.
14. SIGN OUT button: full-width Button, label "Sign out" (strakkBodyBold, #E05252) centered, height 52, background #10162F r=12, horizontal margin 20.
15. Spacer height 81 (32 + 49 tab-bar inset).

=== SCREEN B: PRO PAYWALL (presented as iOS fullScreenCover — full screen, slides up from bottom, NO sheet detents) ===
Root: ZStack { background #050918 (ignoresSafeArea) ; scroll content ; bottom inset ; close button overlay }.

SCROLL BODY: ScrollView(showsIndicators:false), scrollContentBackground hidden -> VStack(align:leading, spacing 0) with padding horizontal 20 (StrakkSpacing.lg) and padding bottom 12:
A. Compact header VStack(align:leading, spacing 12): Spacer height 8 ; "Unlock the full potential" (strakkHeading1 ~28/700, #F4F6FF, multiline) ; "Smarter insights, cleaner tracking, and premium tools to help you stay consistent." (strakkBody ~15/400, #9CA1B8, multiline).
B. Features section (padding top 20): overline "EVERYTHING IN PRO" (#6F748C, kerning 0.8, padding bottom 16) ; VStack spacing 6 of 7 featureRows.

featureRow HStack(spacing 12) padding horizontal 12 / vertical 10, background #10162F r=10 ONLY when highlighted else clear: [icon tile 30x30 r=8, SF size 15 medium; bg #FF7A3D@0.10 + glyph #FF7A3D when highlighted, else bg #151B38 + glyph #9CA1B8] + VStack(spacing 2)[ title (strakkBodyBold if highlighted else strakkBody, #F4F6FF) ; optional quota (strakkCaption #6F748C) ] + Spacer + SF "checkmark" (size 11 bold, #6F748C).

BOTTOM PINNED STACK (safeAreaInset edge:.bottom, NOT scrollable, background #050918): VStack(spacing 0): top hairline Rectangle fill white@0.12 height 0.5 ; VStack(align:leading, spacing 12) padding horizontal 20 / top 16 / bottom 20: overline "CHOOSE YOUR PLAN" (#6F748C, kerning 0.8) ; VStack(spacing 8)[ monthlyPlanCard ; annualPlanCard ] ; subscribe CTA button.
- monthlyPlanCard: Button(plain) VStack(align:leading, spacing 6) padding 12, bg #10162F r=12, overlay r=12 strokeBorder (#FF7A3D lineWidth 2 if selected else #858FBE@0.18 lineWidth 1): "Monthly access" (strakkBodyBold #F4F6FF) ; HStack(firstTextBaseline, spacing 4)[ price (strakkHeading3 #F4F6FF) + "/month" (strakkCaption #6F748C) ].
- annualPlanCard (DEFAULT SELECTED): Button(plain) VStack(align:leading, spacing 6) padding 12, bg #10162F r=12, overlay border same rule: HStack[ "Annual" (strakkBodyBold #F4F6FF) + Spacer + badge "2 MONTHS FREE" (strakkOverline #FF7A3D, padding h6/v2, bg #FF7A3D@0.12 r=4) ] ; HStack(firstTextBaseline, spacing 4)[ annual price (strakkHeading3 #F4F6FF) + "/year" (strakkCaption #6F748C) ] ; "That's only {perMonth} per month" (strakkCaption #6F748C).
- CTA: Button height 50, RoundedRectangle r=12 filled with LinearGradient leading->trailing [#FF7A3D -> #FF9A55]; label "Unlock the full potential" (strakkBodyBold, white). When processing: gradient becomes [#FF7A3D@0.5] and shows white ProgressView spinner instead of text. Disabled (non-interactive) when processing OR selected plan is the user's current plan (then label = "Current plan").

CLOSE BUTTON (overlay align topTrailing, padding trailing 12 / top 32): ZStack Circle 32x32 fill #151B38 + SF "xmark" (size 11 semibold, #6F748C), wrapped in 44x44 hit target.

## Components
- **SectionHeader (overline)**: Uppercase label: strakkOverline (~11px, weight 700, color #6F748C). Kerning 1.0 in Settings sections, 0.8 in Paywall. Padding horizontal 20. Used for ACCOUNT / CURRENT PLAN / DAILY GOALS / HEVY INTEGRATION / DATA SOURCES / EVERYTHING IN PRO / CHOOSE YOUR PLAN.
- **ListRowCard**: RoundedRectangle radius 12, fill #10162F, outer horizontal margin 20. Single-row variant padding horizontal 16 / vertical 14. Hosts Email row and Hevy API Key row.
- **EmailRow**: HStack: SF envelope (13px, #6F748C) + 4pt spacer + 'Email' (strakkCaptionBold #9CA1B8) + Spacer + email value (strakkBody #F4F6FF, single line right-aligned).
- **GoalsCard**: One #10162F r=12 card containing 5 GoalRows divided by white@0.12 dividers (leading inset 16). Animated focus border: strokeBorder transitions clear -> #FF7A3D@0.5 (lineWidth 1) via easeInOut 0.15s when any field focused.
- **GoalRow**: HStack padding h16/v14: 8x8 Circle (macro color) + 8pt spacer + label (strakkBodyBold #F4F6FF) + Spacer + numeric TextField (strakkHeading3, monospacedDigit, right-aligned, numberPad, tint #FF7A3D, maxWidth 80) + unit (strakkCaption #6F748C). Colors: Protein #FF7A3D, Calories #FF9A55, Fat #FFC84D, Carbs #637CFF, Water #4B8DFF.
- **HevyApiKeyRow**: HStack padding h16/v14: SF dumbbell.fill (13px #6F748C) + 8pt spacer + 'API Key' (strakkBodyBold) + Spacer + SecureField (masked dots) placeholder 'Paste Hevy API key', right-aligned, maxWidth 200, tint #FF7A3D.
- **PlanCard wrapper (planCard)**: VStack spacing 12 inside #10162F r=12, padding 16, horizontal margin 20. Base container for Free/Trial/Active plan variants.
- **StatusPill**: Tiny pill: strakkOverline text, padding h8/v4, background = pill color @ 0.12 opacity, radius 6. Variants: 'FREE' (#9CA1B8), 'TRIAL'/'ACTIVE' (#4DAE6A green).
- **ProFreeCard**: planCard: StatusPill 'FREE' + 'Track faster with Pro' (strakkBodyBold) + body copy (strakkCaption #9CA1B8) + StrakkPrimaryButton 'Upgrade to Pro' + centered text button 'Restore a purchase' (strakkCaption #FF7A3D).
- **ProTrialCard**: planCard: HStack[StatusPill 'TRIAL' (green) + Spacer + '{n} days left' (strakkCaptionBold #F4F6FF)] + 'Your Pro trial is active' (strakkBodyBold) + 'Trial ends: {date}' (strakkCaption #E0A84D warning) + reassurance caption (#9CA1B8) + StrakkPrimaryButton 'Pick my plan'.
- **ProActiveCard**: planCard: HStack[StatusPill 'ACTIVE' (green) + Spacer + 'Strakk Pro' (strakkCaptionBold #FF7A3D)] + '{Monthly|Annual} plan' (strakkBodyBold) + 'Renews: {date}' (strakkCaption #9CA1B8) + optional AnnualUpsellCard + StrakkSecondaryButton 'Manage subscription'.
- **AnnualUpsellCard**: Nested card #151B38 r=10, padding 12: 'Save with annual' (strakkCaptionBold) + 'Two months free vs the monthly plan.' (strakkCaption #9CA1B8) + text button 'See the annual offer' (strakkCaptionBold #FF7A3D).
- **ProPaymentFailedCard**: #10162F r=12 card padding 16 with a 3pt #E05252 left accent bar (rounded): 'Payment issue' (strakkBodyBold #E05252) + 'Update your payment method to keep your Pro access.' (#9CA1B8) + full-width 'Fix payment' button height 52, #E05252 fill r=12, white bodyBold.
- **StrakkPrimaryButton**: Full-width CTA height 52, radius 12, fill #FF7A3D (or #151B38 when disabled), label strakkBodyBold white (disabled text #6F748C). Press: scale 0.97 + opacity 0.92, easeOut 0.1s. Fires light haptic on tap.
- **StrakkSecondaryButton**: Full-width height 52, radius 12, fill #151B38 (#1A2142 when pressed), label strakkBodyBold #F4F6FF. Press scale 0.97.
- **SignOutButton**: Full-width height 52, fill #10162F r=12, label 'Sign out' strakkBodyBold #E05252. Triggers light haptic then confirmation alert.
- **DataSourcesBlock**: Attribution VStack (spacing 8): intro caption + two bullet lines, all strakkCaption #9CA1B8.
- **PaywallHeader**: Title 'Unlock the full potential' (strakkHeading1 #F4F6FF) + subtitle (strakkBody #9CA1B8), left-aligned, multiline.
- **FeatureRow**: Row with 30x30 icon tile (r=8), title (+ optional quota caption), and trailing checkmark. Highlighted state: row bg #10162F r=10, icon tile bg #FF7A3D@0.10 with #FF7A3D glyph, title bolded; default: clear bg, icon tile #151B38 with #9CA1B8 glyph.
- **PlanSelectCard (monthly/annual)**: Tappable card #10162F r=12 padding 12; selected -> 2pt #FF7A3D border, unselected -> 1pt #858FBE@0.18 border. Annual carries a '2 MONTHS FREE' badge (overline #FF7A3D on #FF7A3D@0.12 r=4) and a per-month subline.
- **SubscribeCTA**: Height 50 gradient button (#FF7A3D->#FF9A55 horizontal), radius 12, white strakkBodyBold label. Shows spinner + dimmed gradient when processing; disabled when current plan selected. Medium haptic on tap.
- **CloseButton**: Top-right 32x32 circle #151B38 with SF xmark (11px semibold #6F748C), 44x44 hit area.
- **KeyboardToolbar**: Numeric keyboard accessory: chevron.up (prev field, disabled on Protein) + chevron.down (next field, disabled on Water) + Spacer. Moves focus across the 5 goal fields.
- **DevSubscriptionPicker (dev only)**: Menu picker tinted #FF7A3D to force subscription display states; omit from web/prod build.

## Interactions
- Paywall presentation: opened as a full-screen cover (fullScreenCover) from Settings, NOT a bottom sheet — no detents; default iOS transition is a slide-up from the bottom edge, dismiss slides back down. Triggered by 'Upgrade to Pro', 'Pick my plan', or 'See the annual offer'.
- Paywall close: tapping the top-right X (or programmatic dismiss effect) closes the full-screen cover.
- Plan selection on Paywall: tapping Monthly or Annual card selects it (border animates to 2pt #FF7A3D, other card returns to 1pt faint border). Light haptic on each selection. Annual is selected by default.
- Subscribe CTA: medium haptic on tap; while processing shows white spinner over a dimmed (50% opacity) orange gradient and is non-interactive; if the selected plan equals the user's current plan the button reads 'Current plan' and is disabled.
- Goal field editing: tapping a goal value focuses a numberPad TextField; the whole GoalsCard border animates to #FF7A3D@0.5 (easeInOut 0.15s) while any field is focused. Each keystroke debounces a save event to the ViewModel (Hevy key uses a 500ms debounce).
- Keyboard navigation: an accessory toolbar shows up/down chevrons to step focus between Protein->Calories->Fat->Carbs->Water (up disabled on Protein, down disabled on Water).
- Dismiss keyboard: scrollDismissesKeyboard(.interactively) lets a downward scroll drag the keyboard away; a tap anywhere on the scroll content also clears focus.
- Sign out: tapping 'Sign out' fires a light haptic and presents a confirmation alert titled 'Sign out' with body 'Are you sure you want to sign out?' and buttons Cancel (cancel role) / Sign out (destructive red role).
- Manage subscription: 'Manage subscription' / 'Fix payment' opens the external URL https://apps.apple.com/account/subscriptions via the system browser/App Store account page.
- Restore a purchase: text button triggers a restore flow; result surfaced via a 'Strakk Pro' alert with a single OK button (also reused as a generic toast/info alert).
- Error handling: failures present an alert titled 'Error' with the message body and an OK (cancel) button.
- Button press feedback: all StrakkPrimary/Secondary/Destructive buttons scale to 0.97 (primary/destructive also drop to 0.92 opacity) with easeOut 0.1s while pressed; StrakkPrimaryButton emits a light haptic, AnnualUpsell 'See the annual offer' emits light haptic, subscribe emits medium haptic.
- Loading: Settings initially shows the 'Settings' title with a centered orange ProgressView until data resolves.

## Copy strings
- Settings
- ACCOUNT
- Email
- CURRENT PLAN
- FREE
- Track faster with Pro
- Photo and text AI analysis plus a weekly summary so you log less by hand.
- Upgrade to Pro
- Restore a purchase
- TRIAL
- 5 days left
- Your Pro trial is active
- Trial ends: Jul 7, 2026
- You keep photo analysis, free text and AI summaries until then.
- Pick my plan
- ACTIVE
- Strakk Pro
- Monthly plan
- Annual plan
- Renews: Jul 14, 2026
- Save with annual
- Two months free vs the monthly plan.
- See the annual offer
- Manage subscription
- Payment issue
- Update your payment method to keep your Pro access.
- Fix payment
- DAILY GOALS
- Protein
- Calories
- Fat
- Carbs
- Water
- 150
- 2200
- 70
- 250
- 2000
- g
- kcal
- mL
- HEVY INTEGRATION
- API Key
- Paste Hevy API key
- DATA SOURCES
- Food data provided by:
- • Open Food Facts (ODbL)
- • CIQUAL 2020 — ANSES
- Sign out
- Error
- OK
- Are you sure you want to sign out?
- Cancel
- Strakk Pro
- DEV SUBSCRIPTION
- Plan
- Backend
- Free
- Trial - 7 days
- Trial - 1 day
- Expired
- Pro monthly
- Pro annual
- Payment failed
- Unlock the full potential
- Smarter insights, cleaner tracking, and premium tools to help you stay consistent.
- EVERYTHING IN PRO
- AI photo analysis
- AI text analysis
- AI weekly summary
- Health sync
- Unlimited history
- Photo comparison
- Hevy export
- 100/month
- 5/month
- 2/month
- CHOOSE YOUR PLAN
- Monthly access
- /month
- Annual
- 2 MONTHS FREE
- /year
- That's only $5.00 per month
- Current plan
- —

## Mock data
SETTINGS account: email = "ott.thomas68@gmail.com". DAILY GOALS values (override the gray placeholders): Protein 165 g, Calories 2350 kcal, Fat 72 g, Carbs 248 g, Water 2500 mL (placeholders shown when empty are 150 / 2200 / 70 / 250 / 2000). HEVY API Key: present but masked as SecureField dots (underlying e.g. "hevy_3f9a2c81d4e7b6..."). CURRENT PLAN — pick one variant to render: (FREE) default upsell card; (TRIAL) "5 days left", trial ends Jul 7, 2026; (ACTIVE) "Monthly plan", renews Jul 14, 2026, show the 'Save with annual' upsell sub-card since canUpgradeToAnnual=true; (ACTIVE annual) "Annual plan", renews Mar 14, 2027, no upsell; (PAYMENT FAILED) error card. Dates are formatted .medium e.g. "Jul 7, 2026". PAYWALL prices (store-localized strings; fall back to em dash "—" while loading): monthly "$9.99", annual "$59.99", per-month subline "$5.00" -> "That's only $5.00 per month". Default selected plan = Annual. Feature list order with icon + quota: 1 AI photo analysis (camera.fill, 100/month), 2 AI text analysis (text.bubble.fill, 100/month), 3 AI weekly summary (chart.bar.fill, 5/month), 4 Health sync (heart.fill, no quota), 5 Unlimited history (clock.fill, no quota), 6 Photo comparison (photo.on.rectangle.angled, no quota), 7 Hevy export (dumbbell.fill, 2/month). Highlighted feature (when paywall opened from a gated feature) e.g. AI photo analysis: its row gets the #10162F bg, orange icon tile, bold title.

## Design tokens
COLORS (hex / rgba): background #050918 (screen), backgroundElevated #080D1F, backgroundEdge #0B1028; surface1 #10162F (cards/rows/sign-out), surface1GradientTop #121833, surface1GradientBottom #0C1127, surface2 #151B38 (nested upsell card, close-button circle, feature icon tile default, disabled primary btn), surface3 #1A2142 (secondary btn pressed); borderSubtle rgba(125,137,190,0.25), borderFaint rgba(133,143,190,0.18) = unselected plan border, dividerStrong rgba(150,157,200,0.22), divider/dividerWeak rgba(255,255,255,0.12) = row dividers & paywall hairline (0.5pt); text: primary #F4F6FF, secondary #9CA1B8, tertiary #6F748C, disabled #50566F; accent primary/orange #FF7A3D (CTAs, focus, selected border, highlight), primaryLight #FF9A55 (gradient end / Calories dot); macro dots: Protein #FF7A3D, Calories #FF9A55, Fat (yellow) #FFC84D, Carbs (indigo) #637CFF, Water (blue) #4B8DFF; semantic: success #4DAE6A (TRIAL/ACTIVE pill), error #E05252 (sign out text, payment-failed bar/btn), warning #E0A84D (trial-ends date). Translucent uses: pill bg = pillColor@0.12, badge bg #FF7A3D@0.12, highlighted feature icon bg #FF7A3D@0.10, focus border #FF7A3D@0.5, processing CTA gradient #FF7A3D@0.5.
SPACING scale (StrakkSpacing): xxs 4, xs 8, sm 12, md 16, lg 20, xl 24, xxl 32, xxxl 40. Screen horizontal margin = lg(20). Card inner padding = 16 (rows: h16/v14). Plan/feature card inner padding = sm(12). Inter-section gaps in Settings = fixed 24pt Spacers (32 before sign out, 81 at bottom). Paywall: header->features top 20, overline bottom 16, feature rows gap 6, plan cards gap 8, bottom stack padding top 16 / bottom 20.
RADIUS (StrakkRadius): sm 12 (cards, buttons, plan cards), md 18, lg 24, xl 32, xxl 36, xxxl 56. Feature row r=10, status pill r=6, annual badge r=4, icon tile r=8, payment bar r=2.
TYPOGRAPHY (SwiftUI Dynamic Type semantic styles; web px @ default size / weight): strakkHeading1 = title 28/700 (screen titles); strakkHeading3 = headline 17/600 (goal values, plan prices); strakkBodyLarge = body 17/500; strakkBody = subheadline 15/400 (body copy, email value); strakkBodyBold = subheadline 15/600 (row labels, plan names, CTA text); strakkCaption = caption 12/400 (units, helper text, quotas); strakkCaptionBold = caption 12/600 ('Email' label, days-left); strakkOverline = caption2 11/700 (all section labels, pills, badge). Goal value uses monospaced digits. Section overlines use letter-spacing 1.0 (Settings) / 0.8 (Paywall).
BUTTON METRICS: standard CTA height 52 r=12 (paywall subscribe is 50 r=12); pressed scale 0.97, opacity 0.92 (primary/destructive), animation easeOut 0.1s. Focus border animation easeInOut 0.15s. HAPTICS: light = UIImpactFeedback .light (primary buttons, plan select, sign-out tap, annual upsell), medium = .medium (subscribe). ELEVATION: flat dark surfaces, no drop shadows — depth conveyed purely by surface1/surface2/surface3 layering and 1-2pt accent borders.