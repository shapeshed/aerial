# Aerial Compose, Navigation 3, and Material 3 Quality Audit

Target: `/home/go/src/github.com/shapeshed/aerial`

Date: 2026-08-22

Confidence: High

## Executive Summary

The Compose implementation is generally modern: lifecycle-aware Flow collection is used consistently, lazy collections are keyed and typed, animated values are usually read in draw/layout phases, edge-to-edge is enabled, Material 3 Expressive components and semantic color roles are used throughout, and release shrinking is enabled. The Compose compiler confirms Strong Skipping and 100% named restartable composable skippability.

The earlier structural risks are resolved: app navigation is type-safe Navigation 3, the route collects one cohesive UI state, playback metadata is atomic, image analysis and filter derivation run off the UI thread, and navigation chrome responds to window width. Settings now has a stateless content seam, expanded settings/edit layouts constrain readable width, and long-click station actions have accessibility labels. The largest remaining quality risk is narrower: `MainScreen.kt` still combines too many UI responsibilities.

## Resolution Status

Resolved on `refactor/navigation3-compose-quality`:

- App navigation now uses serializable `NavKey` routes, `rememberNavBackStack`, `NavDisplay`, entry-scoped saved state/ViewModels, stable Navigation 3 `1.1.6`, and saved-state restoration tests.
- `MainScreen` collects one nested `MainUiState` rather than roughly thirty independent flows.
- Station identity, playback state, metadata, bitrate, and playback error now share one atomic `PlaybackUiState`; the composition-time metadata reset workaround was removed.
- Logo appearance analysis runs on `Dispatchers.Default` and is cached by logo key with bounded LRU behavior.
- Medium and expanded portrait windows now select navigation rail by width rather than an orientation heuristic.
- Public route/reusable composables gained standard outermost `modifier` seams.

The scores below are the post-refactor result. The original Compose baseline was 66/100, Navigation 3 compliance was 5/10, and Material 3 compliance was 82/100.

## Compose Scores

| Category | Weight | Score | Status |
|---|---:|---:|---|
| Performance | 35% | 9/10 | Excellent |
| State management | 25% | 9/10 | Excellent |
| Side effects | 20% | 9/10 | Excellent |
| Composable API quality | 20% | 8/10 | Solid |
| **Overall** | **100%** | **88/100** | **Solid** |

Calculation: `(9 × .35 + 9 × .25 + 9 × .20 + 8 × .20) × 10 = 88`. Delta: **+22 points**.

### Compiler diagnostics

- Compiler diagnostics used: yes.
- Strong Skipping: enabled (`app/build/compose_audit/release/app-module.json`).
- Module-wide skippability: 246/318 restartable composables, 77.4%. This includes generated/anonymous composable lambdas that cannot all be skipped.
- Named-only skippability: 37/37 restartable named composables, 100% (`app/build/compose_audit/app-composables.csv`).
- Inferred unstable classes: 17, principally Android components, databases, ViewModels, and Navigation 3 infrastructure. No broken or expensive equality behavior was found on shared UI model parameters.
- SSM-on ceiling applied: no cap. Named-only skippability is at least 95%, and source review did not find widespread per-recomposition unstable-parameter recreation.

## Critical Findings

No systemic 0–3 severity Compose issue was found. The following are the highest-impact targeted findings.

### 1. The root route remains overly broad

Evidence: `app/src/main/java/com/shapeshed/aerial/ui/MainScreen.kt:301-733` now collects one `MainUiState`, but the same route still owns navigation chrome, search, sheets, overlays, and most event wiring; the file remains about 2,700 lines.

Recommendation: retain the cohesive state model and split the route into stateless app-shell, search, home, favorites, and overlay content seams receiving state plus event callbacks.

Expected impact: smaller recomposition scopes, simpler previews and targeted semantics tests, and easier ownership of saveable UI state.

References: <https://developer.android.com/topic/architecture/ui-layer/stateholders>, <https://developer.android.com/develop/ui/compose/state-hoisting>

### 2. Activity-level navigation tests await device execution

Evidence: `AerialActivityNavigationTest.kt` and `SettingsContentTest.kt` compile, while the existing navigator and restoration tests cover the underlying contracts. The attached device disconnected before the new activity tests could execute.

Recommendation: execute the isolated connected suite when a device is attached; keep the `.deviceTest` application-id boundary so development data remains untouched.

Expected impact: closes runtime verification for real Material controls, system Back, and the new stateless settings surface.

References: <https://developer.android.com/develop/ui/compose/performance/bestpractices>, <https://developer.android.com/topic/architecture/ui-layer/state-production>

### 3. Navigation behavior coverage is not yet fully end-to-end

Evidence: `AerialNavigatorTest.kt` covers root protection, typed arguments, duplicate suppression, and stack pops; `AerialNavigationRestorationTest.kt` covers saved-state restoration. There is not yet an activity-level semantics test that clicks each real entry point and verifies forward/back behavior and destination content.

Recommendation: add a small activity-level test matrix for settings, add station, edit station, system Back, and rapid repeated entry actions. Keep the existing focused back-stack and restoration tests.

Expected impact: verifies that the correctly structured Navigation 3 graph remains connected to the actual Material controls and lifecycle behavior.

References: <https://developer.android.com/guide/navigation/navigation-3/migration-guide>, <https://developer.android.com/guide/navigation/navigation-3/save-state>

## Compose Category Detail

### Performance — 9/10

Positive evidence:

- Lazy lists generally provide stable `key` and `contentType`, for example `MainScreen.kt:1008-1013` and `1513-1522`.
- `EqualizerBars.kt:28-48` defers animated state reads to the `Canvas` draw phase.
- `NowPlayingScreen.kt:163-167` applies the drag value in `graphicsLayer`, avoiding per-frame recomposition of layout content.
- `FilterPickerItems.kt` produces localization/filtering/sorting on `Dispatchers.Default` and has deterministic unit coverage.
- Release builds enable minification and resource shrinking (`app/build.gradle:50-55`).

Deductions:

- There is no baseline-profile module or `ReportDrawnWhen` signal yet. This is optimization headroom rather than evidence of a current regression.

References: <https://developer.android.com/develop/ui/compose/performance/bestpractices>, <https://developer.android.com/develop/ui/compose/lists>

### State management — 9/10

Positive evidence:

- `MainScreen` collects exactly one lifecycle-aware `MainUiState` (`MainScreen.kt:309`).
- Playback identity, state, metadata, bitrate, and error are represented atomically in `PlaybackUiState`, preventing mixed station/playback snapshots.
- Primitive drag and measured-height state uses `mutableFloatStateOf`/`mutableIntStateOf`.

Deductions:

- A few transient sheet/query values intentionally use `remember` (`MainScreen.kt:350-364`). If product behavior says open filters and their in-sheet query should survive recreation, promote only those values to `rememberSaveable`; dialogs tied to object instances should remain transient.

References: <https://developer.android.com/develop/ui/compose/state>, <https://developer.android.com/develop/ui/compose/state-hoisting>

### Side effects — 9/10

Positive evidence:

- Target-driven state changes and background logo analysis are in keyed `LaunchedEffect`s; event-driven animations use `rememberCoroutineScope` from handlers.
- Search and pager synchronization effects have explicit keys.
- No plain `collectAsState()` or obvious I/O directly in a composable body was found.

Tradeoff:

- `MainScreen.kt:389` starts the screen-owned MediaController connection from `LaunchedEffect`; `MainViewModel.connect` is explicitly idempotent and releases the future from `onCleared`, matching the ViewModel lifetime.

References: <https://developer.android.com/develop/ui/compose/side-effects>, <https://developer.android.com/topic/libraries/architecture/coroutines>

### Composable API quality — 8/10

Positive evidence:

- Route and reusable components now consistently accept and apply `modifier` to the outermost node, including `MainScreen`, `SettingsScreen`, `StationEditScreen`, `NowPlayingScreen`, language, and sleep-timer surfaces.
- `SettingsScreen` is a state-collecting route over a stateless `SettingsContent` surface with explicit event callbacks.
- Callbacks are generally expressed as events rather than exposing mutable Compose state.

Deductions:

- `MainScreen.kt` remains roughly 2,600 lines; extracting the filter sheet and settings content helped, but further decomposition should be driven by independently testable UI responsibilities rather than file size alone.

References: <https://developer.android.com/develop/ui/compose/api-guidelines>, <https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md>

## Navigation 3 Compliance

Score: 9/10 (modern and compliant; test-depth headroom remains). Delta: **+4**.

The app uses stable Navigation 3 `1.1.6` with serializable `NavKey`s, `rememberNavBackStack`, `NavDisplay`, an `entryProvider`, saveable-state and ViewModel entry decorators, predictive-pop transitions, root protection, and duplicate-visible-destination suppression. `EditStation` carries a typed `Long`, and saved-state restoration is exercised on device. The graph correctly remains a single stack because the app has no deep links, nested graphs, or independent app-level tab histories.

Remaining test matrix:

1. Click the real settings/add/edit controls and assert destination semantics.
2. Exercise system Back from each real destination.
3. Verify rapid repeated edit taps through the activity UI, matching the already-tested navigator contract.

References: <https://developer.android.com/guide/navigation/navigation-3>, <https://developer.android.com/guide/navigation/navigation-3/migration-guide>

## Material 3 Compliance

Overall: 89/100. Delta: **+7 points**.

| Category | Score | Status | Evidence |
|---|---:|---|---|
| Color tokens | 9/10 | Pass | Predominantly semantic `MaterialTheme.colorScheme` roles with paired content colors; the black artwork scrim is a justified media overlay. |
| Typography | 9/10 | Pass | Uses `MaterialTheme.typography`; default Material typography is intentional. |
| Shape | 8/10 | Pass | Theme/component shapes are common; a few screen-specific corner values remain. |
| Elevation | 9/10 | Pass | Tonal containers and Material surfaces dominate; little custom shadow use. |
| Components | 9/10 | Pass | Material 3/Expressive search, navigation, sheets, cards, buttons, and progress components are used consistently. |
| Layout | 9/10 | Pass | Width-based bar/rail adaptation covers portrait and landscape medium/expanded windows; settings and station-edit content now have readable maximum widths. |
| Navigation | 9/10 | Pass | Material navigation bar/rail selection is adaptive and app destinations use typed, saveable Navigation 3. |
| Motion | 8/10 | Pass | Expressive motion specs and phase-aware custom animation are used; custom fixed-duration cases are localized. |
| Accessibility | 9/10 | Pass | Icon actions and long-click station actions have labels; decorative icons correctly use null descriptions; traversal grouping is present. Focused TalkBack/keyboard runtime coverage remains useful. |
| Theming | 10/10 | Pass | Dynamic light/dark schemes on Android 12+, explicit dark fallback, expressive light fallback, and `MaterialExpressiveTheme`. |

Material recommendations:

- Consider a Navigation 3 list-detail/supporting-pane scene only where simultaneous content provides clear product value (for example favorites plus now-playing detail); do not add panes merely to fill space.
- Keep current semantic color/typography/component usage; do not replace the intentional artwork scrim with a theme surface token because it serves image legibility rather than app-surface theming.
- Expand the existing 599/600/900dp policy unit tests with screenshot/semantics coverage at compact portrait, medium portrait, expanded landscape, and a foldable posture.

References: <https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes>, <https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation>, <https://m3.material.io/foundations/layout/applying-layout/window-size-classes>

## Adjacent Coverage

- Android launch UX: pass. `themes.xml:10` references `splash_foreground`; API 31+ resolves to the animated-vector wrapper at `app/src/main/res/drawable-v31/splash_foreground.xml`, avoiding the static-splash upscaling issue.
- UI tests: the isolated device harness covers playback callback/state, Media3 queue behavior, and Navigation 3 saved-state restoration. Activity-level navigation semantics and adaptive screenshots remain useful additions.
- Focus/keyboard: no focused keyboard/D-pad test coverage was found. This becomes more important once expanded-window support is treated as a first-class target.
- KMP: not applicable; this is an Android-only application.

## Prioritized Fixes

1. **Split the oversized route into stateless content seams** — retain the single collection at `MainScreen.kt:309`, but extract the app shell/search/overlays from `MainScreen.kt:301-733` where doing so creates independently testable UI. <https://developer.android.com/develop/ui/compose/state-hoisting>
2. **Run and extend the isolated device behavior suite** — execute `AerialActivityNavigationTest.kt` and `SettingsContentTest.kt` when the device reconnects, then add compact/medium/expanded semantics coverage. <https://developer.android.com/develop/ui/compose/testing>
3. **Add measured startup optimization** — introduce a benchmark/baseline-profile module and `ReportDrawnWhen` only with macrobenchmark evidence, rather than adding unverified profile rules. <https://developer.android.com/develop/ui/compose/performance/baseline-profiles>

## Notes And Limits

- The report was produced from source review plus a release Compose compiler metrics build using the audit init script. Generated metrics under `app/build/compose_audit` are build output and are not intended for source control.
- Material scoring is a source-level compliance review; contrast was not measured from screenshots for every dynamic device palette.
- Runtime jank and startup were not profiled, so baseline-profile work remains a measured follow-up rather than a speculative code change.
