# Material 3 Typography and Color Audit

Target: `/home/go/src/github.com/shapeshed/aerial`
Date: 2026-08-22
Scope: production Compose typography, theme color schemes, semantic color-role pairing, and Material 3 Expressive usage
Confidence: High

## Scorecard

| Area | Score | Assessment |
|---|---:|---|
| Typography | 9/10 | Material baseline and Expressive emphasized tokens are used without ad-hoc sizing or weight overrides |
| Color roles | 9/10 | Production UI uses semantic roles and valid container/on-container pairs |
| Theme coherence | 8/10 | Dynamic color is correct and static fallbacks now use complete library-managed Material schemes |
| Expressive application | 9/10 | Expressive theme, components, motion, and emphasized typography are applied consistently |
| Overall | 88/100 | Standard Material schemes and disciplined semantic typography/color roles; runtime contrast validation retains headroom |

## Executive Finding

Aerial is close to standard Material 3 at component level. It uses `MaterialExpressiveTheme`, enables Android 12+ dynamic color, uses the default Material type scale, and normally selects colors through `MaterialTheme.colorScheme` rather than literals.

The main theme-architecture issue found by this audit has been resolved. With dynamic color disabled or unavailable, light mode uses the library's complete `expressiveLightColorScheme()` and dark mode uses the complete default `darkColorScheme()`. Android 12+ continues to use the user's dynamic light/dark scheme. Aerial no longer partially overrides a scheme or carries unused custom palette constants.

References: <https://m3.material.io/styles/color/system/overview>, <https://developer.android.com/develop/ui/compose/designsystems/material3>, <https://developer.android.com/develop/ui/compose/designsystems/material3#dynamic_color_schemes>

## Findings

### Resolved: partial custom dark scheme

- `Theme.kt` now uses `darkColorScheme()` without partial overrides.
- The unused custom `Color.kt` palette was removed.
- All current dark roles—including the surface-container ladder, inverse roles, outline variant, fixed accents, and scrim—therefore come from one Material-managed scheme.

The remaining light/dark palette difference is deliberate use of Material's supplied defaults rather than an incomplete brand override. A future branded fallback should only be introduced as a complete generated light/dark pair.

References: <https://m3.material.io/styles/color/system/how-the-system-works>, <https://developer.android.com/develop/ui/compose/designsystems/material3#dynamic_color_schemes>

### 1. Medium: manual font weights bypass Expressive emphasized tokens

- The theme passes `Typography()`, which correctly supplies the standard Material 3 baseline and emphasized scales.
- Most text either uses a semantic `MaterialTheme.typography` style or inherits the correct component token.
- Four call sites manually set `FontWeight`: `MainScreen.kt:1577` and `SleepTimerSheet.kt:118,162,170`.
- `labelLarge` plus `FontWeight.Medium` is redundant. The three `SemiBold` overrides create ad-hoc variants instead of using the corresponding `*Emphasized` style supplied by the current Material 3 API.

Standard direction: retain `Typography()` and replace intentional emphasis with `titleMediumEmphasized`, `titleLargeEmphasized`, or `displaySmallEmphasized` as appropriate. Remove redundant weight overrides. This preserves the full token—weight, tracking, size, and line height—rather than changing one axis independently.

References: <https://m3.material.io/styles/typography/type-scale-tokens>, <https://developer.android.com/develop/ui/compose/designsystems/material3>

### 2. Medium: one image scrim bypasses the color scheme

- `MainScreen.kt:2226` uses `Color.Black.copy(alpha = 0.35f)` over active station artwork.
- A scrim over arbitrary imagery is a legitimate special case, but Material exposes the semantic `scrim` role specifically for this purpose.

Standard direction: use `MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)`. Preserve the tested opacity unless visual/contrast testing supports changing it.

References: <https://m3.material.io/styles/color/roles>, <https://developer.android.com/reference/kotlin/androidx/compose/material3/ColorScheme>

### 3. Low: an unused color parameter obscures intent

- `StationTile` accepts `contentColor`, and callers compute `onPrimaryContainer`, but the parameter is not read. The actual tile glyph and label colors are selected internally.
- Dead theme inputs make pairing audits harder and can conceal future mistakes.

Standard direction: remove the unused `StationTile.contentColor` parameter.

References: <https://m3.material.io/styles/color/roles>, <https://developer.android.com/develop/ui/compose/components>

## What Already Meets the Standard

- API 31+ dynamic light/dark schemes follow the user's wallpaper.
- `MaterialExpressiveTheme` activates the Expressive theme path and motion scheme.
- No custom font is bundled; default platform Roboto/Roboto Flex behavior is appropriate for Material 3.
- No production text uses hard-coded `sp`, line height, letter spacing, or font family.
- Component text generally inherits component typography; explicit styles use the Material scale.
- Primary, secondary-container, error, surface, and inverse roles are normally paired with their correct `on-*` roles.
- Surface hierarchy uses `surfaceContainerLow/High/Highest` rather than arbitrary neutral colors.
- The only non-theme UI color found outside theme definitions is the artwork scrim.

## Recommended Implementation Order

1. Replace the three intentional `SemiBold` overrides with Expressive emphasized typography tokens; remove the redundant medium override.
2. Replace the black artwork overlay with the semantic `scrim` role.
3. Remove the unused `StationTile.contentColor` parameter.
4. Expand deterministic dark screenshot coverage beyond Settings when those screens receive further visual changes.

## Acceptance Standard

- Dynamic color remains enabled by default on API 31+.
- Static light and dark schemes are generated from one seed and include all roles used by the current Compose Material 3 API.
- Every foreground uses the documented `on-*` role for its container; surface text uses `onSurface` or `onSurfaceVariant`.
- Typography uses component defaults, baseline type-scale tokens, or emphasized type-scale tokens—no one-off sizes, tracking, line heights, or weights.
- Light, dark, dynamic, and increased system contrast are visually checked; screenshot tests remain deterministic with dynamic color disabled.

## Surface, Text Size, and Foreground Audit Addendum

### Updated scorecard

| Area | Score | Assessment |
|---|---:|---|
| Surface hierarchy | 9/10 | Standard component surfaces and explicit tonal roles are used without redundant elevation |
| Text scale selection | 9/10 | Component defaults plus semantic baseline/emphasized tokens are used consistently |
| Foreground/container pairing | 9/10 | Audited foregrounds now use valid semantic pairs under static and dynamic schemes |

Remediation status: all eight findings below were addressed on 2026-08-22. Post-fix focused scores are **9/10** for surface hierarchy, **9/10** for text-scale selection, and **9/10** for foreground/container pairing. A score of 10 is reserved until representative screens are also exercised under dynamic and increased-contrast schemes, not only deterministic static screenshot themes.

### 1. High: a container color is used as foreground text

- `MainScreen.kt:1891-1905` assigns `FilledTonalToggleButtonDefaults.filledTonalToggleButtonColors().checkedContainerColor` as a `TextButton`'s `contentColor`.
- A container role is a fill, not a foreground role. It is not contrast-guaranteed against the page surface under dynamic color or increased contrast.
- Use the normal `TextButton` content color, `primary` for a high-emphasis action on a surface, or the matching checked **content** color if the control truly needs to mirror the adjacent toggle.

References: <https://m3.material.io/styles/color/roles>, <https://developer.android.com/reference/kotlin/androidx/compose/material3/ColorScheme>

### 2. High: selected favourite icon crosses accent-role pairs

- `NowPlayingScreen.kt:385-396` places `primary` on a `FilledTonalIconButton`, whose container is normally `secondaryContainer` and whose guaranteed foreground is `onSecondaryContainer`.
- A primary foreground is allowed directly on a surface, but it is not the documented pair for a secondary container.
- Model this as a true toggle button and use its selected/unselected component colors, or keep the tonal button's inherited content color. If stronger selection emphasis is needed, change container and content together as a valid pair.

References: <https://m3.material.io/components/icon-buttons/specs>, <https://m3.material.io/styles/color/roles>

### 3. Medium: two surface elevation declarations do not express standard resting levels

- `NowPlayingScreen.kt:498-508` gives station artwork `tonalElevation = 8.dp`. Material level 4 is intended for hover/focus, not a resting artwork container. The surface already has an explicit `primaryContainer`, inverse surface, or high surface-container color.
- `NowPlayingScreen.kt:555-558` combines `surfaceContainerHigh` with `tonalElevation = 1.dp`. This mixes an explicit tonal hierarchy role with an elevation-derived tone and obscures which system communicates depth.
- Prefer explicit container roles at elevation zero for both elements. Add shadow elevation only if artwork needs protection from a genuinely busy overlapping background.

References: <https://m3.material.io/styles/elevation/applying-elevation>, <https://developer.android.com/develop/ui/compose/designsystems/material3>

### 4. Medium: Mood cards override the standard filled-card surface

- `MainScreen.kt:1545-1551` uses `surfaceContainerHigh` for a filled `Card`.
- The standard filled-card role is `surfaceContainerHighest`; `ForYouStationCard` correctly obtains it from `CardDefaults`.
- The current value is contrast-safe with `onSurface`, so this is hierarchy inconsistency rather than an accessibility defect. Use `CardDefaults.cardColors()` unless the lower emphasis is an explicit product decision.

References: <https://m3.material.io/components/cards/specs>, <https://developer.android.com/develop/ui/compose/components/card>

### 5. Medium: station-grid names use a navigation/caption scale

- `MainScreen.kt:2246-2253` renders the primary station name with `labelMedium` (12sp).
- Label Medium is intended for navigation labels and compact captions. This text is the tile's primary content name.
- Prefer `bodyMedium` (14sp) for the current compact one-line layout. If expanded cards gain more vertical room, `titleSmall` or `titleMedium` becomes appropriate. Continue to respect font scaling and ellipsis.

References: <https://m3.material.io/styles/typography/type-scale-tokens>, <https://developer.android.com/develop/ui/compose/text/fonts>

### 6. Medium: default-size toggle buttons use title typography

- `SleepTimerSheet.kt:229-244` applies `titleMedium` (16sp) inside default-size `ToggleButton`s.
- Button labels conventionally use `labelLarge` (14sp); the component default already supplies its size-appropriate token.
- Remove the explicit style. If a larger Expressive button size is selected through the component API, let that size supply its matching label token rather than scaling text independently.

References: <https://m3.material.io/components/buttons/specs>, <https://m3.material.io/styles/typography/type-scale-tokens>

### 7. Low: primary surfaces use the deprecated background alias at the root

- `MainActivity.kt:74` paints the navigation host with `colorScheme.background`.
- In current Material 3, the primary app background role is `surface`; `background` is retained as a deprecated compatibility alias.
- Replace it with `surface` for current terminology and consistency with `AdaptiveNavigationShell`.

References: <https://developer.android.com/reference/kotlin/androidx/compose/material3/ColorScheme>, <https://m3.material.io/styles/color/roles>

### 8. Low: image scrim should use the semantic scrim role

- `MainScreen.kt:2226` uses translucent black over active station artwork.
- Replace the base color with `MaterialTheme.colorScheme.scrim`, retaining the tested alpha.

References: <https://m3.material.io/styles/color/roles>, <https://developer.android.com/reference/kotlin/androidx/compose/material3/ColorScheme>

### Correct applications confirmed

- Full-screen and navigation backgrounds otherwise use `surface` or a deliberate surface-container role.
- Mini-player containment uses a high tonal surface, while its play control correctly pairs `primary` with `onPrimary`.
- Empty states correctly pair `secondaryContainer` with `onSecondaryContainer`, followed by `titleMedium` and `bodyMedium` on the page surface.
- Search, mood, and favourites rows use Material `ListItem`; their headline/supporting typography aligns with `bodyLarge`/`bodyMedium` either explicitly or through component defaults.
- Top app bars, dialogs, buttons, chips, list items, navigation labels, and text fields mostly inherit their component typography instead of overriding sizes.
- Now Playing uses `headlineLarge` for the station identity, `titleMedium`/`bodyMedium` for metadata, and `displaySmall` for the sleep-timer countdown—appropriate semantic scale choices.
- Supporting text consistently uses `onSurfaceVariant`; primary surface text uses `onSurface`.
- Filled cards, sheets, dialogs, and navigation components use Material components rather than hand-built shadow systems.

### Recommended correction order

1. Fix the Favorites sort foreground token and Now Playing favourite toggle pairing.
2. Remove the artwork/pill tonal elevations and return Mood cards to `CardDefaults`.
3. Change station-grid names to `bodyMedium` and let sleep preset buttons inherit their label style.
4. Apply the previously identified Expressive emphasized styles instead of manual font weights.
5. Replace `background` and literal black with `surface` and `scrim`, then remove the unused `StationTile.contentColor` parameter.

### Applied remediation

- Favorites sort now inherits the standard text-button foreground.
- Now Playing favourite content inherits the tonal icon-button foreground pairing.
- Artwork and bitrate surfaces no longer combine explicit tonal roles with redundant elevation.
- Mood cards use the standard filled-card container from `CardDefaults`.
- Station tile names use `bodyMedium`; sleep preset buttons inherit component label typography.
- Manual weights were replaced with `titleMediumEmphasized`, `titleLargeEmphasized`, and `displaySmallEmphasized` where emphasis is intentional.
- The navigation host uses `surface`, the artwork overlay uses `scrim`, and the unused station-tile color parameter was removed.
- Four affected screenshot references were reviewed and updated; all 18 screenshot tests, unit tests, and debug lint pass.
