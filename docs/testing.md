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
# Local business-logic tests, lint, and compilation
./gradlew quality

# Compose screenshot validation without a device
./gradlew validateDebugScreenshotTest

# Isolated instrumented tests on an attached device or emulator
./gradlew connectedDeviceTestAndroidTest

# Release verification
./gradlew test lint assembleRelease bundleRelease
```

Do not change `testBuildType` to `debug` and do not use package-clearing
commands against `com.shapeshed.aerial` while running tests.

CI runs the quality gate, screenshot validation, local coverage, and isolated
emulator tests. Local JUnit reports, coverage, and instrumented reports are
uploaded as build artifacts when available, including after failures.

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
