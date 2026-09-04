---
name: strakk-design-system
description: Applies Strakk's warm dark mobile design system from DESIGN.md. Use when proposing or implementing SwiftUI or Compose UI.
paths:
  - "DESIGN.md"
  - "iosApp/**/*.swift"
  - "androidApp/**/*.kt"
---
# Strakk Design System

Use this skill whenever a task touches UI, interaction, empty states, charts, forms, sheets, navigation, or visual polish.

## Source Of Truth

Read `DESIGN.md` before making UI decisions. Do not invent a new palette, typography scale, or visual personality.

## Product Feel

- Warm dark, friendly, premium, approachable.
- Supportive coach, not drill sergeant.
- Revolut-level information density, Duolingo-level approachability.
- Gym context: dim lighting, one-handed use, sweaty fingers, quick glances.

## Implementation Rules

- Use design tokens by name in reasoning: `background`, `surface-1`, `surface-2`, `primary`, `text-secondary`, `radius-md`.
- Use `primary` sparingly for CTAs, active state, and key progress.
- Prefer surface contrast over borders and shadows.
- Data is the hero: large numbers, concise labels, clear progress.
- Minimum touch targets: 48pt/dp.
- Avoid generic AI patterns: centered icon cards, neon effects, gradients, fake dashboard grids, decorative placeholders.

## Platform Adaptation

- iOS: use SF Symbols, system font, native navigation, sheets, haptics, and Liquid Glass only for navigation-layer controls when available.
- Android: use Material 3, semantic `MaterialTheme.colorScheme`, edge-to-edge, NavigationBar, and Material motion patterns.
- Same information architecture across platforms; platform-native execution.

## Review Checklist

- Does the screen have one clear primary job?
- Can the user understand the main data in 1-2 seconds?
- Are actions reachable with one thumb?
- Does the empty state teach the next action without filler art?
- Does the design feel specific to Strakk rather than generic health-app UI?
