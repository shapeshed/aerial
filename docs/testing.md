# Testing Aerial

Aerial follows the Android testing pyramid used by current Google Compose
reference projects such as [Now in Android](https://github.com/android/nowinandroid#testing)
and [Jetcaster](https://github.com/android/compose-samples/tree/main/Jetcaster).

## Test placement

- `app/src/test/` contains fast JVM tests for repositories, ViewModels, parsers,
  navigation contracts, and other business logic.
- `app/src/androidTest/` contains tests that require a real Android runtime,
  including Activity lifecycle, rotation, Media3, Room's SQLite engine, and
  system integration. Compose tests use the v2 test rule and semantic matchers.
- `app/src/screenshotTest/` contains deterministic Compose Preview Screenshot
  Tests. References live under `app/src/screenshotTestDebug/reference/` and
  cover compact, medium, and expanded layouts, dark mode, and larger fonts.
- End-to-end tests should remain a small minority of the suite and cover major
  user journeys only.

## Feature and bug policy

Every feature must include tests for its observable behavior at the lowest
appropriate level. Add local tests for state and business logic, Compose
behavior tests for UI interaction, and instrumented tests only when Android
runtime behavior is part of the requirement.

Every bug fix must begin with a regression test that reproduces the bug and
fails against the old code. The fix is complete only when that test passes and
the relevant existing test suite remains green. Do not weaken or delete a
regression test to make a change pass.

Pull requests use the repository checklist to record this evidence. Reviewers
should request the missing test before approving a feature or bug fix.

## Safe device testing

Instrumented tests use the `deviceTest` build type and target:

```text
com.shapeshed.aerial.deviceTest
```

The normal development app remains:

```text
com.shapeshed.aerial
```

`AerialTestRunner` and `AerialTestEnvironment` fail if the instrumentation target
is not the isolated test application. Tests that use real application state
should install `AerialTestEnvironmentRule`, which resets test preferences before
and after each test. Test-owned databases must likewise be in-memory or cleaned
up by the owning test; they must never be cleared through commands targeting the
normal application.

Run the suites with:

```sh
# Local business-logic tests, lint, compilation, and coverage gate
./gradlew quality

# Compose screenshot validation without a device
./gradlew validateDebugScreenshotTest

# Isolated instrumented tests on an attached device or emulator
./scripts/check-device-ready.sh
./gradlew connectedDeviceTestAndroidTest

# Release verification
./gradlew test lint assembleRelease bundleRelease

# Release shrinker/configuration verification
./gradlew assembleRelease analyzeReleaseR8Config
```

Do not change `testBuildType` to `debug` and do not use package-clearing
commands against `com.shapeshed.aerial` while running tests.

CI runs the quality gate, screenshot validation, local coverage, and isolated
emulator tests. Local JUnit reports, coverage, and instrumented reports are
uploaded as build artifacts when available, including after failures.

### External coverage tracking

CI can upload the JaCoCo XML report to Codecov for project and pull-request
trends. Add a `CODECOV_TOKEN` repository secret in GitHub; the upload step is
skipped when the secret is absent. The local JaCoCo verification task remains
the required, provider-independent coverage gate.

## Dependency injection

The application and main activity are Hilt-enabled, and screen ViewModels are
migrated incrementally behind explicit modules and constructor injection.
The station editor uses Hilt assisted injection for its route-provided station
ID, so navigation arguments remain explicit rather than being read from global
state.
Local unit tests should continue to instantiate classes directly with fakes;
they do not need Hilt. When an instrumented test first requires injected
dependencies, add `@HiltAndroidTest`, `HiltAndroidRule`, and switch the test
runner application to `HiltTestApplication` for that test setup. Until then,
the isolated runner remains intentionally simple and protects the developer's
normal app installation.

## Macrobenchmarks

The `:benchmark` module holds Macrobenchmark tests. They measure the app's
release-like `benchmark` build type, so they require a connected, unlocked device
or emulator and are not part of CI:

```sh
# Cold-start timing (StartupBenchmark)
./gradlew :benchmark:connectedBenchmarkAndroidTest
```

The Macrobenchmark library refuses to run on emulators because their numbers are
not representative. To do an indicative smoke run on an emulator anyway:

```sh
./gradlew :benchmark:connectedBenchmarkAndroidTest -PallowEmulatorBenchmarks
```

`BaselineProfileGenerator` records a baseline profile the same way. To wire it up,
apply the `androidx.baselineprofile` plugin to `:app` as well, then run the
generator task; the generated profile is committed under the app's baseline
profile source set so release builds ship it.

Benchmarks are a measurement tool, not a pass/fail gate. Record a baseline on a
representative device before claiming a startup, scroll, or search improvement.

## Minified-release smoke

The `benchmark` build type is R8-minified and debug-signed, so it can be installed
without release keystore environment variables to smoke-test shrinking and the
exported Media3 service:

```sh
./gradlew :app:assembleBenchmark
adb install -r app/build/outputs/apk/benchmark/app-benchmark.apk
adb shell am start -n com.shapeshed.aerial/.MainActivity
# Expect no FATAL/AndroidRuntime crash in logcat and a registered media session:
adb shell dumpsys media_session | grep -i aerial
```
