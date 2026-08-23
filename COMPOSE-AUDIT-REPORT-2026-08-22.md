# Jetpack Compose Audit Report

Target: `/home/go/src/github.com/shapeshed/aerial`
Date: 2026-08-22
Scope: Android `app` production Compose, state holders, navigation, resources, and test coverage
Excluded from scoring: generated build output and screenshot reference images
Confidence: High
Overall Score: 90/100

## Scorecard

| Category | Score | Weight | Status | Notes |
|----------|-------|--------|--------|-------|
| Performance | 9/10 | 35% | excellent | Strong Skipping; keyed adaptive lazy grids; phase-correct custom animation |
| State management | 9/10 | 25% | excellent | One lifecycle-aware route state and atomic playback state |
| Side effects | 9/10 | 20% | excellent | Effects and event coroutines have appropriate ownership |
| Composable API quality | 9/10 | 20% | excellent | Root route delegates its major surfaces to stateless, independently testable seams |

Calculation: `(9 × .35 + 9 × .25 + 9 × .20 + 9 × .20) × 10 = 90`.

## Critical Findings

There is no systemic Compose correctness or performance defect. The remaining high-value work is targeted:

1. **Performance measurement: startup has no benchmark or baseline profile**
   - Why it matters: source and compiler evidence are strong, but startup and scroll performance have not been measured. Optimization should be driven by Macrobenchmark evidence rather than speculative rules.
   - Evidence: no baseline-profile module or `ReportDrawnWhen` use exists; release R8/resource shrinking is enabled in `app/build.gradle`.
   - Fix direction: add a benchmark/baseline-profile module and report first meaningful draw when initialization content is ready.
   - References: <https://developer.android.com/develop/ui/compose/performance/baseline-profiles>, <https://developer.android.com/develop/ui/compose/performance/tooling>

2. **Adjacent coverage: expanded layouts lack keyboard/focus tests**
   - Why it matters: adaptive rail and multi-column layouts now support larger devices, where hardware keyboard and directional navigation are more common.
   - Evidence: adaptive screenshots and eight isolated device tests exist, but no `FocusRequester`, key-event, or focused-state test was found under `app/src`.
   - Fix direction: add a compact keyboard-search test and an expanded rail/grid focus-order test.
   - References: <https://developer.android.com/develop/ui/compose/testing>, <https://developer.android.com/develop/ui/compose/accessibility>

## Adjacent Findings

### Android Launch UX

- Android 12+ splash icon status: clean.
- Evidence: `app/src/main/res/values/themes.xml:10` references `splash_foreground`; API 31+ resolves to the animated-vector wrapper in `app/src/main/res/drawable-v31/splash_foreground.xml`.
- References: <https://developer.android.com/develop/ui/views/launch/splash-screen>, <https://developer.android.com/reference/androidx/core/splashscreen/SplashScreen>

## Category Details

### Performance — 9/10

**Ceiling check**

- Strong Skipping: on.
- Ceiling table applied: SSM-on.
- Module-wide `skippable%`: 246/318 = 77.4%; zero-argument composable lambdas anchor this metric.
- Named-only `skippable%`: 37/37 = 100%.
- Unstable compiler-inferred classes: 17, consisting primarily of Android components, databases, ViewModels, and infrastructure; no expensive or broken equality was found on shared UI parameters.
- SSM-on binding evidence: no repeated unstable-parameter recreation, expensive equality, or unjustified skipping opt-out observed.
- Qualitative score: 9/10.
- Ceiling: none.
- Applied score: 9/10.

**What is working**

- Home, favourites, and registry results use `LazyVerticalGrid` with adaptive sizing, stable keys, and content types.
- Logo analysis and filter derivation run outside composition; logo results use a bounded cache.
- Animated drag is read in `graphicsLayer`, and equalizer animation is drawn through `Canvas` rather than rebuilding layout.
- Release minification/resource shrinking and the Gradle build cache are enabled.

**What is hurting the score**

- There is no measured baseline profile/startup benchmark yet.

**Animation performance signals**

- Status: clean. No composition-phase per-frame offset/alpha defect or unremembered `Animatable` was found.

**Evidence**

- `app/src/main/java/com/shapeshed/aerial/ui/MainScreen.kt:945` — search results use an adaptive keyed grid. · References: <https://developer.android.com/develop/ui/compose/lists>
- `app/src/main/java/com/shapeshed/aerial/ui/MainScreen.kt:1431` — Home mood content uses adaptive columns. · References: <https://developer.android.com/develop/ui/compose/lists>
- `app/src/main/java/com/shapeshed/aerial/ui/NowPlayingScreen.kt:154` — drag translation is deferred to the draw phase. · References: <https://developer.android.com/develop/ui/compose/performance/phases>

### State Management — 9/10

**What is working**

- `MainScreen` collects one `MainUiState` through `collectAsStateWithLifecycle()`.
- Playback identity, metadata, bitrate, errors, and queue are atomic within `PlaybackUiState`.
- Stateless `MainAppContent`, `SettingsContent`, filter surfaces, and `MiniPlayer` receive values and event callbacks rather than ViewModels.
- Primitive measured/gesture state uses typed state factories.

**What is hurting the score**

- Search/sheet controller state remains route-owned, which is appropriate for its lifecycle but leaves room for a dedicated saveable state holder if process-death restoration becomes a product requirement.

**Evidence**

- `app/src/main/java/com/shapeshed/aerial/ui/MainScreen.kt:303` — one lifecycle-aware route state collection. · References: <https://developer.android.com/develop/ui/compose/state>, <https://developer.android.com/develop/ui/compose/state-hoisting>
- `app/src/main/java/com/shapeshed/aerial/ui/MainAppContent.kt:17` — stateless shell exposes data/events and a content slot. · References: <https://developer.android.com/develop/ui/compose/api-guidelines>
- `app/src/main/java/com/shapeshed/aerial/ui/MiniPlayer.kt:63` — mini-player is callback-driven and ViewModel-free. · References: <https://developer.android.com/develop/ui/compose/state-hoisting>

### Side Effects — 9/10

**What is working**

- Composition-driven work is keyed through `LaunchedEffect`; user-event animations use the remembered coroutine scope.
- Media-controller connection is idempotent and released with ViewModel lifetime.
- Background transformations live in ViewModel/coroutine code rather than composition.

**What is hurting the score**

- No repeated defect was found. The score reserves headroom for runtime lifecycle profiling.

**Animation side-effect signals**

- Status: clean. Target-driven animations are declarative/effect-owned; event-driven transitions are launched from callbacks.

**Evidence**

- `app/src/main/java/com/shapeshed/aerial/ui/MainScreen.kt:377` — connection and search updates are effect-owned. · References: <https://developer.android.com/develop/ui/compose/side-effects>
- `app/src/main/java/com/shapeshed/aerial/ui/MainScreen.kt:810` — swipe dismissal observes state from `LaunchedEffect` and delegates the event. · References: <https://developer.android.com/develop/ui/compose/side-effects>

### Composable API Quality — 9/10

**What is working**

- Reusable screens/components consistently expose `modifier: Modifier = Modifier` and apply it to the outer node.
- `MainAppContent` uses a slot API and owns only adaptive scaffold mechanics.
- `MiniPlayer` exposes state and events directly and has focused visual/behavior tests.
- Destination content, expanded search, and modal surfaces are stateless functions with explicit values and event callbacks.
- Home and favourites content are previewable without the application graph.

**What is hurting the score**

- `MainScreen.kt` still contains many private feature components; moving them by feature would improve source ownership, but file splitting alone would not change runtime behavior.

**Evidence**

- `app/src/main/java/com/shapeshed/aerial/ui/MainScreen.kt:295` — the route now focuses on state collection, controller ownership, and event wiring. · References: <https://developer.android.com/develop/ui/compose/api-guidelines>
- `app/src/main/java/com/shapeshed/aerial/ui/MainAppContent.kt:17` — extracted stateless adaptive shell follows modifier/slot conventions. · References: <https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md>
- `app/src/main/java/com/shapeshed/aerial/ui/MiniPlayer.kt:63` — extracted stateless mini-player has explicit callback wiring. · References: <https://developer.android.com/develop/ui/compose/state-hoisting>

## Material 3 and Navigation 3 Addendum

- Material 3 compliance: **91/100** (previously 89). Adaptive navigation and adaptive Home/favourites/search grids improved Layout and Navigation.
- Navigation 3 compliance: **9/10**. Typed `NavKey` routes, saveable back stack, entry decorators, predictive pop, and restoration/device tests are present.

| Material category | Score |
|---|---:|
| Color tokens | 9/10 |
| Typography | 9/10 |
| Shape | 8/10 |
| Elevation | 9/10 |
| Components | 9/10 |
| Layout | 10/10 |
| Navigation | 10/10 |
| Motion | 8/10 |
| Accessibility | 9/10 |
| Theming | 10/10 |

Evidence: `AdaptiveNavigationShell.kt:66`, `MainScreen.kt:1431`, `MainScreen.kt:1872`, `SettingsScreen.kt:138`, `MainActivity.kt:67`, and the 18 validated previews under `app/src/screenshotTest`.

## Prioritized Fixes

1. Add a measured Macrobenchmark/baseline-profile module and `ReportDrawnWhen` signal after defining first meaningful content. <https://developer.android.com/develop/ui/compose/performance/baseline-profiles>
2. Add keyboard/focus behavior coverage for expanded navigation and adaptive grids. <https://developer.android.com/develop/ui/compose/testing>
3. Move remaining private feature components out of `MainScreen.kt` when doing so creates a useful ownership or test boundary. <https://developer.android.com/develop/ui/compose/api-guidelines>

## Notes And Limits

- Default 35/25/20/20 weights were used; no categories were renormalized.
- Compiler diagnostics used: yes, from `app/build/compose_audit`; Strong Skipping is enabled and the SSM-on ceiling was applied.
- UI coverage: 18 local screenshot tests and eight isolated on-device tests passed on a Pixel 9 Pro XL running Android 17.
- Focus/keyboard: no focused key-navigation coverage found.
- KMP/CMP: not applicable; this is an Android-only app.
- Android launch UX resources: clean; API 31+ has an animated-vector splash wrapper.

## Suggested Follow-Up

- Continue the route decomposition only where it creates stateless test seams; file splitting alone does not change recomposition behavior.
- Run a focused keyboard/focus review once expanded-window keyboard behavior is implemented.
- Measure startup before adding profile rules.
