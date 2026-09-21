# Aerial Quality & Maintenance Improvement Plan

Status: **living plan**. Phase 0 is complete and verified but **uncommitted**.
Phases 1–4 are not started. Update this file as work lands; do not rewrite history
silently — mark items done with the commit that did them.

Audience: the next engineer/model continuing this work. Read
[AGENTS.md](../AGENTS.md) and [docs/testing.md](testing.md) first. The repository's
policy is strict: every feature adds behavior tests, every bug fix starts with a
failing regression test, and generated screenshot references are not updated just
to make comparisons pass.

- Target: `com.shapeshed.aerial`, reviewed at `main` / v0.7.2 (`008c769`, local
  branch `fix/ci-search-test-timeout` at `c9e844f`).
- Snapshot date: 2026-09-21.
- All changes below are on the current worktree and have not been committed.

---

## 1. Current working-tree state (uncommitted)

```
 M .gitignore
 M AGENTS.md
 M DEVELOPERS.md
 M app/build.gradle
 M build.gradle
 M docs/testing.md
 R COMPOSE-AUDIT-REPORT-2026-08-22.md -> docs/audits/COMPOSE-AUDIT-REPORT-2026-08-22.md
 R COMPOSE-AUDIT-REPORT.md          -> docs/audits/COMPOSE-AUDIT-REPORT.md
 R MATERIAL3-TYPOGRAPHY-COLOR-AUDIT.md -> docs/audits/MATERIAL3-TYPOGRAPHY-COLOR-AUDIT.md
?? .editorconfig
?? app/config/ktlint/baseline.xml
?? docs/audits/README.md
?? gradle/libs.versions.toml
```

New files:
- `gradle/libs.versions.toml` — version catalog.
- `.editorconfig` — formatting baseline.
- `app/config/ktlint/baseline.xml` — generated ktlint baseline (~1,131 entries).
- `app/src/screenshotTest/.../PreviewAdaptiveFormFactors.kt` — extracted multi-preview
  annotation (see §2.4).
- `docs/audits/README.md` — index for archived audits.
- `opencode.jsonc` — project OpenCode configuration (see §2.8).

Suggested commit split (Conventional Commits):
1. `build(deps): add Gradle version catalog`
2. `build(test): exclude generated classes and gate coverage at 22%`
3. `chore(quality): add ktlint with baseline and editorconfig`
4. `docs: archive audits and align JDK/tooling docs`

---

## 2. Phase 0 — guardrails (DONE, verified, uncommitted)

### 2.1 Version catalog — DONE

- `gradle/libs.versions.toml` is the single source of truth for plugin and
  dependency versions.
- `build.gradle` and `app/build.gradle` use `libs.*` aliases; no inline version
  strings remain in dependency/plugin declarations.
- Alias naming rule observed: **an alias must not be a strict dash-prefix of
  another alias** (Gradle generates one nested accessor per segment). Hence
  `compose-ui-core`, `media3-hls`, `material3-core`, etc.
- Renovate updates the catalog's `[versions]` table.

### 2.2 Coverage made trustworthy — DONE

In `app/build.gradle`:
- Report + verification share one `executionData` and one `classDirectories`
  definition so they cannot drift.
- Excluded generated/toolchain classes: `**/*_Impl*` (Room), `**/*Hilt*`,
  `**/Dagger*`, `**/*_Factory*`, `**/*_MembersInjector*`, `R`, `BuildConfig`,
  `Manifest`, `**/*$WhenMappings.class`.
- Measured owned-code line coverage on 2026-09-21: **1506/6566 = 22.9%**
  (previously ~18.9% with generated classes polluting the denominator).
- Gate raised from `0.18` to `0.22`; baseline recorded in a code comment.
- `./gradlew :app:jacocoDeviceTestUnitTestReport` no longer logs the
  "Classes in bundle 'app' do not match with execution data" mismatch.
- Root `quality` task now depends on
  `:app:jacocoDeviceTestUnitTestCoverageVerification`.

### 2.3 ktlint static analysis — DONE (format attempt evaluated & rejected)

- Plugin `org.jlleitschuh.gradle.ktlint` v14.2.0, ktlint CLI pinned to **1.8.0**
  in the catalog (`ktlintGradle`, `ktlint`), because the plugin default 1.5.0 is
  older. ktlint 1.8.0 parses the Kotlin 2.4 sources successfully (no parse errors).
- Configured in `app/build.gradle`: pinned `version`, `android = true`, baseline
  at the plugin default `app/config/ktlint/baseline.xml`.
- **Important:** the plugin's `android = true` did **not** by itself apply the
  Android code style — `ktlint_code_style = android_studio` in `.editorconfig`
  is what took effect. Adding it, plus
  `ktlint_function_naming_ignore_when_annotated_with = Composable, Preview, PreviewTest`,
  halved the baseline from **2,604 → 1,115 entries**.
- Baseline generated with `./gradlew :app:ktlintGenerateBaseline`.
- Root `quality` task now depends on `:app:ktlintCheck`.
- Verified: `:app:ktlintCheck --rerun-tasks` passes with the 1,115-entry baseline;
  a deliberately malformed file fails `:app:ktlintMainSourceSetCheck`.
- **Mass-format evaluation (2026-09-21): do not run `ktlintFormat` as a one-shot.**
  Two attempts on the whole project did not converge: ktlint reported "not able to
  resolve all violations ... in 3 consecutive runs" and left ~2,231 remaining
  violations in `main` (dominated by `standard:indent` and
  `standard:function-signature`), i.e. **worse** than the 1,115-entry baseline. It
  also rewrites `class MainViewModel @Inject constructor(...)` into a split
  annotation form and collapses multi-line function signatures onto long single
  lines. The reformat was reverted; the baseline remains the mechanism for
  existing violations. The 1,115 baseline entries are spread across 76 files
  (largest: `StationEditScreen.kt` 127, `SettingsScreen.kt` 107). To reduce it,
  either: adopt the [ktlint Compose Rules](https://mrmans0n.github.io/compose-rules/ktlint/)
  ruleset, or format incrementally per package with review and regenerate the
  baseline after each reviewed step.

### 2.4 Jetpack Compose Rules — DONE

- Added the ktlint Compose ruleset `io.nlopez.compose.rules:ktlint:0.6.6`
  (`composeRules` in the catalog) via the `ktlintRuleset` configuration. 0.6.6 is
  built against ktlint 1.8.0, matching the pinned CLI exactly.
- Compose rules are active and fail the build for new violations.
- Fixed all preview violations in the screenshot tests rather than baselining
  them: the 23 public `@Preview` composables in `SettingsAdaptiveScreenshotTest.kt`
  are now `private`, and the multi-preview annotation was renamed from
  `AdaptiveFormFactorPreviews` to `PreviewAdaptiveFormFactors` and moved to its own
  correctly-named file (the `standard:filename` rule only fires once the file's
  other declarations are private).
- `validateDebugScreenshotTest` still discovers all **43** previews after the
  visibility change; the same 3 pre-existing environmental failures remain.
- **16 pre-existing findings were baselined** (rules active, debt tracked). Fixing
  them is API churn and deferred to an incremental follow-up:

  | Rule | Count | Locations |
  |---|---:|---|
  | `compose:parameter-naming` | 8 | `AdaptiveNavigationShell.kt:33`, `FavoritesContent.kt:65,265`, `MainDestinationContent.kt:24,35`, `MainRouteContent.kt:55`, `MiniPlayer.kt:68` |
  | `compose:lambda-param-in-effect` | 4 | `FavoritesContent.kt:60`, `MainDestinationContent.kt:25`, `MiniPlayer.kt:67`, `NowPlayingScreen.kt:127` |
  | `compose:content-slot-reused` | 3 | `MainNavigationHost.kt:21,23`, `SearchResults.kt:133` |
  | `compose:modifier-without-default-check` | 1 | `ArtworkContent.kt:184` |
  | `compose:vm-forwarding-check` | 1 | `MainScreen.kt:445` |

  Note: the `parameter-naming` rule requires `on...` lambda parameters to be
  present tense (`onSortSelected` -> `onSortSelect`); this is the largest and most
  churn-heavy group. `MainScreen`/`MainNavigationHost` findings are arguably
  acceptable patterns and may warrant `.editorconfig` allowlists instead of edits.

### 2.5 Hygiene — DONE

- Archived tracked audits into `docs/audits/` (with `docs/audits/README.md`);
  `.gitignore` now ignores only root-level `COMPOSE-AUDIT-REPORT-*.md` snapshots.
- Removed the stale untracked `benchmark/build/` directory (leftover; there is
  still no `:benchmark` module — see Phase 2).
- `.editorconfig` added (Kotlin 4-space/120 col/trailing commas; 2-space for
  YAML/JSON; 4-space XML/Gradle).
- JDK docs aligned in `AGENTS.md` and `DEVELOPERS.md`: builds target Java 17
  bytecode, CI uses JDK 25. `DEVELOPERS.md` points at the version catalog.

### 2.6 Phase 0 verification

| Check | Result |
|---|---|
| `./gradlew quality` (compile + lint + **ktlintCheck** + unit tests + **coverage**) | PASS |
| `./gradlew :app:jacocoDeviceTestUnitTestCoverageVerification` at 22% | PASS (22.9%) |
| `./gradlew :app:ktlintCheck --rerun-tasks` | PASS with 1,131-entry baseline |
| New ktlint violation rejected | PASS (fails as expected) |
| Compose ruleset (`io.nlopez.compose.rules:ktlint:0.6.6`) loaded | PASS; caught 24 real findings, all fixed or tracked |
| One-shot `:app:ktlintFormat` | REJECTED — does not converge; reverted |
| `./gradlew validateDebugScreenshotTest` | PASS — 43 previews, now stable with a dirty tree (see §2.7) |

### 2.7 Screenshot golden determinism — DONE

**Root cause of the "local screenshot failures":** `SettingsScreen` rendered
`BuildConfig.BUILD_LABEL`, which is `dirty-<sha>` whenever the git working tree is
dirty. The Settings goldens were captured on a clean tree (`Aerial 0.7.2`), so any
uncommitted change changed the footer text and failed
`SettingsAdaptiveScreenshot_400x1000/_610x1000/_900x1000`. (Earlier this was
misdiagnosed as environmental — re-running with the original build files still
failed because the tree was still dirty.)

**Fix:** hoist the label out of `SettingsContent` into a required `versionLabel`
parameter. `SettingsScreen` computes the real dirty/nightly/version label, while
the screenshot preview passes a fixed
`stringResource(R.string.version_format, BuildConfig.VERSION_NAME)`. The build
label stays a real developer aid in the app, and the goldens no longer depend on
git state. Verified: `validateDebugScreenshotTest` passes with a dirty tree.
Changing the app version (`VERSION_NAME`) still legitimately changes the golden,
as with any visible UI change.

This is a behavioural bug fix, so the repository policy applies: the fix was
validated by running the suite in the previously-failing dirty state.

### 2.8 Project OpenCode configuration — DONE

- Added `opencode.jsonc` (V2 config shape): ignores build output for file
  watching, asks before `git push`/`tag`/`reset --hard`/release/publish/`rm -rf`,
  denies editing `local/**` and keystores, and defines `quality`, `screenshots`,
  `release-gate`, `ktlint-baseline`, and `plan` commands plus an `android-reviewer`
  subagent.
- Validated with `opencode debug config` (the document is discovered and parsed).

---

## 3. Phase 1 — decompose the orchestration hubs (IN PROGRESS)

Highest maintenance payoff. `MainViewModel.kt` is ~1,074 lines and
`PlayerService.kt` ~746 lines. They combine independently changing
responsibilities; changes to search, persistence, queue ordering, artwork
recovery, and player/session integration converge in one place.

Current state (already partly extracted — keep these seams):
`SearchStateHolder`, `FavoritesQueueCoordinator`, `MediaControllerGateway`,
`PlaybackSnapshotStore`, `SleepTimerStore`, `MediaBrowseTree`.

Known coupling to remove:
- `MainViewModel.kt:87` — `(application as AerialApp).networkMonitor`.
- `MainViewModel.kt:364` — `getApplication<Application>().getString(...)`.
- `MainViewModel.kt:84-88` — test/preview-only default constructor params.
- `PlayerService.kt:122-207` — `onCreate()` constructs ExoPlayer, session, browse
  tree and starts collections inline.
- `PlayerService.kt:209` (`icyListener`), `:329` (`librarySessionCallback`) —
  large inline anonymous objects.

Proposed extractions (behavior, not arbitrary file fragments):
1. **DONE** — `StationArtworkResolver` (`data/StationArtworkResolver.kt`, commit
   `2706680`). Moved `PlayerService.recoverArtwork` into a constructor-injected
   class with four JVM tests (`StationArtworkResolverTest`). Also removed an
   unused local in the original. `PlayerService` now builds the resolver in
   `onCreate` and calls `artworkResolver.recover(...)`.
2. **DONE (helpers)** — `PlaybackStateSynchronizer`. Pure `PlaybackStateSync.kt`
   helpers extracted from `MainViewModel` and covered by JVM tests
   (`PlaybackStateSyncTest`): `PlaybackStationIdentity.of`,
   `playbackStationChanged`, `clearedPerStationState`,
   `stationNamesForMetadataFilter`, `metadataArrival`,
   `PendingPlaybackMetadata.matching`, and `reducePlaybackSync`. The
   pending-metadata state machine and the sync reducer now delegate to them.
   Existing `MainViewModelStateTest` characterization tests
   (`metadataBeforeStationTransitionRemainsVisibleForNowPlaying`,
   `previousStationLabelDeliveredAfterTransitionIsNotTrackMetadata`) pin the
   behavior across station transitions. Also extracted
   `data/StreamMetadataFrames.kt` (`streamMetadataFrames`) from
   `PlayerService.onMetadata`, covered by `StreamMetadataTest`.
   `MainViewModel` still owns the orchestration and effects (identity slots,
   persistence, bitrate), which can move next.
3. `PlaybackSessionCoordinator` — service-side session/player wiring.
4. `FavoritesOrdering` — remaining ordering/membership logic.
5. Inject `NetworkMonitor` and a string/resource provider instead of casting
   `Application`.

Per-step workflow that worked for (1): add the new class + tests first, wire it
in, run `:app:ktlintCheck` (new files must be ktlint-clean), regenerate
`app/config/ktlint/baseline.xml` because edited files shift line numbers, then
run `quality`.

### Coverage inventory for `PlaybackSessionCoordinator` (2026-09-21)

Characterization tests must exist before moving service logic:

- **Covered:** `expandControllerQueue` (`PlaybackQueuePlanTest`), resumption and
  queue policy (`PlaybackResumptionPolicyTest`, `StationQueueTest`), browse tree
  (`MediaBrowseTreeTest`), station resolution (`PlaybackStationResolverTest`),
  and instrumented `MediaSessionQueueExpansionTest`,
  `PlayerServiceQueueRecoveryTest`, `Media3QueueNavigationTest`.
- **Newly characterized:** favorite toggle
  (`data/FavoriteToggle.kt` + `FavoriteToggleTest`), extracted test-first and now
  used by `PlayerService.onCustomCommand`.
- **Newly characterized:** the sleep timer — `data/SleepTimerPolicy.kt`
  (`pollDelayMs`, `fadeVolumes`) and `data/SleepTimerController.kt` (injectable
  clock/volume/pause/state seams), covered by `SleepTimerPolicyTest` and
  `SleepTimerControllerTest`, and now used by `PlayerService`.
- **Still uncovered — add tests before moving:** the `onConnectAsync` advertised
  command set. Do not move it into the coordinator until it has a test.

Approach (required by repo policy):
1. Write characterization tests that pin current observable behavior first.
2. Extract one type at a time behind an interface with constructor injection.
3. Keep public UI state and MediaSession behavior unchanged.
4. Extracted types get JVM tests with fakes (no mocked `Application`, no
   lifecycle reflection).

Acceptance criteria:
- `MainViewModel` no longer constructs the `MediaController` or casts `Application`.
- `PlayerService.onCreate()` is composition/wiring plus lifecycle cleanup.
- Existing playback, navigation, and screenshot suites stay green.
- `MainViewModel` and `PlayerService` line counts and public API surface trend down.

References: <https://developer.android.com/topic/architecture/recommendations>,
<https://developer.android.com/training/dependency-injection>.

---

## 4. Phase 2 — make performance measurable (NOT STARTED)

- There is **no** `:benchmark` module or baseline profile; startup/scroll/search
  performance is unmeasured. (A stale `benchmark/build/` dir was deleted.)
- Add a Macrobenchmark module for: cold start, first Home content, search
  typing/results, favorites scroll, Now Playing expansion.
- Add a baseline profile once budgets exist. Add `ReportDrawnWhen` only when a
  meaningful-content condition is defined.
- Profile artwork-heavy flows (Coil) on a representative low/mid-tier device
  before changing bitmap/artwork code.

References: <https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview>,
<https://developer.android.com/topic/performance/baselineprofiles/overview>.

---

## 5. Phase 3 — risk-based testing & UI hardening (NOT STARTED)

- Extend the screenshot matrix to Search, Station Edit, modal/bottom-sheet, and
  loading/empty/error states across compact/medium/expanded × short/tall × dark ×
  1.5× font. Settings already has the full 3×3; Home/Favorites/Now Playing are
  partial.
- Add `ArtworkProvider` boundary tests (traversal, extra path segments, invalid
  mode, missing files, writes) — it is exported read-only.
- Add a minified-release smoke test for exported Media3 service behavior.
- Add ~5% end-to-end journeys (UI Automator/Compose) for major flows.
- Edge-to-edge audit: `enableEdgeToEdge()` and `adjustResize` are present, but
  `NavigationSuiteScaffold` does not propagate `PaddingValues`. Verify each
  destination applies insets (list `contentPadding`, IME on text fields) and add a
  device screenshot with system bars + keyboard.
- Consider adding `testTag("station-edit-screen")` and deterministic navigation
  waiters (also relevant to the PR #249 flake below).

References: <https://developer.android.com/develop/ui/compose/testing>.

---

## 6. Phase 4 — security & polish (NOT STARTED)

- Re-evaluate `ArtworkProvider` exposure (exported, no permission, read-only,
  canonical-parent guarded) and document the decision; keep
  `grantUriPermissions="false"`.
- Keep `PlayerService` authorization behavior covered by regression tests
  (exported by design for Media3/Auto). `PendingIntent` is already explicit +
  `FLAG_IMMUTABLE`.
- No deep-link `VIEW` intent filters exist today; if added, use `IntentSanitizer`.
- Raise the coverage threshold incrementally as behavior tests land.
- Consider strengthening Android lint (currently only `fatal 'UnusedResources'`,
  `MissingTranslation` disabled).
- Reduce the 1,115-entry ktlint baseline incrementally (inventory cleanup) rather
  than with a one-shot `ktlintFormat`; see §2.3 for why the formatter is unsafe
  here. The largest contributors are `StationEditScreen.kt` and
  `SettingsScreen.kt`.

---

## 7. Separate workstream — CI failure on PR #249

PR: <https://github.com/shapeshed/aerial/pull/249>
Branch `fix/ci-search-test-timeout`. The PR only bumps two `waitUntil` timeouts
from 10s → 30s in
`app/src/androidTest/java/com/shapeshed/aerial/ui/MainActivityNavigationTest.kt`.

Findings (see the PR review conversation for full evidence):
- Failing job: **Instrumented tests and coverage**; failing step
  **Start Android emulator and run isolated device tests**; failing test
  `MainActivityNavigationTest.noSearchResultsOpenAddStationAndSystemBackReturnsToSearch`
  at `MainActivityNavigationTest.kt:59` — the **second** `waitUntil`, waiting for
  the **Add Station** screen after clicking "Add your own station"
  (`ComposeTimeoutException: Condition still not satisfied after 30000 ms`).
- It is **not** steady slowness: the same test passed in 9.8s on run
  `35594381897` (main `c59074a`) but timed out at 10s and again at 30s on other
  runs. `main` CI has been red on most runs since ~2026-09-16.
- Likely causes: a race between `MainSearchOverlay`'s `onCollapse()` animation and
  `navigator.navigate(AerialRoute.AddStation)`, and/or a regression from the
  recent `material3 1.5.0-alpha28` bump (test duration grew ~40% with no app-code
  change).
- The timeout bump alone does not fix it.

Recommended next steps:
1. Add failure diagnostics to CI (logcat + screenshot upload on instrumented
   failure) so the next flake is self-explaining.
2. Give `StationEditScreen` a stable `testTag`; wait on a deterministic
   navigation signal instead of a slow-rendered string.
3. Fix the app-side race: navigate without a competing suspend collapse (or
   collapse via state so the overlay disposes cleanly).
4. Confirm/rule out the Material3 alpha by temporarily pinning the previous
   version.
5. Don't merge the timeout-only change as the resolution.

---

## 8. Constraints & gotchas for the next engineer

- **JDK**: builds target Java 17 bytecode; CI uses JDK 25; the local
  `gradle/gradle-daemon-jvm.properties` (Java 21) is gitignored. Do not commit it
  without checking CI.
- **detekt**: latest published release is 1.23.8 (Feb 2025, pre-Kotlin-2.4) and
  its Gradle plugin is not compatible with this Kotlin version. Use ktlint
  (already wired) instead; revisit detekt only when a Kotlin-2.4-compatible
  release lands.
- **Screenshot goldens are deterministic now.** The old local 1000dp Settings
  failures were caused by `BuildConfig.BUILD_LABEL` changing with the git dirty
  state, not the environment; see §2.7. Never update goldens to make a failure
  pass — inspect the reference/rendered/diff images under
  `app/build/outputs/screenshotTest-results/preview/debug/` and only update after
  an intentional UI change.
- **The ktlint baseline is line-number sensitive.** Editing a file that has
  baseline entries shifts line numbers and makes those entries stop matching, so
  `ktlintCheck` fails until you re-run `:app:ktlintGenerateBaseline`. The baseline
  count is unchanged by pure line shifts.
- **Configuration cache is not enabled globally**: it races KSP sources with the
  combined release gate. It is fine for a narrow `compileDebugKotlin`.
- **Do not run instrumentation against `com.shapeshed.aerial`**: tests use the
  isolated `deviceTest` build type / `com.shapeshed.aerial.deviceTest`.
- **No version literals** in Gradle scripts; add versions to
  `gradle/libs.versions.toml`.
- **ktlint formatter does not converge on this codebase.** `ktlintFormat` leaves
  more violations than the baseline and rewrites annotated constructors and
  multi-line signatures. Use `ktlintCheck` + the baseline for enforcement, and
  only reformat reviewed, small batches. The effective code style comes from
  `ktlint_code_style` in `.editorconfig`, not the plugin's `android` flag.

## 9. Verification commands

```sh
./gradlew quality                    # compile + lint + ktlint + unit tests + coverage
./gradlew :app:ktlintCheck           # formatting gate
./gradlew :app:ktlintGenerateBaseline # only for reviewed reformats
./gradlew validateDebugScreenshotTest # may show 3 environmental failures locally
./gradlew :app:jacocoDeviceTestUnitTestReport
./gradlew test lint assembleRelease bundleRelease   # release gate
./gradlew connectedDeviceTestAndroidTest            # on an unlocked device/emulator
```
