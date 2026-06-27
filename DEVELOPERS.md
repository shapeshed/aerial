# Developer Guide

This guide covers local development, release tasks, signing, and store metadata
for Aerial.

## Requirements

- JDK 17
- Android SDK with platform 37 installed
- Android Studio or the Gradle wrapper

The app currently targets SDK 37 and has a minimum SDK of 26.

## Build

```sh
./gradlew assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Test And Lint

```sh
./gradlew test lint
```

For the release build path, run:

```sh
./gradlew test lint assembleRelease bundleRelease
```

Without signing environment variables, the release APK produced locally is
unsigned:

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

With signing environment variables configured, Gradle produces:

```text
app/build/outputs/apk/release/app-release.apk
```

## Install On A Device

Connect a device with USB debugging enabled, then run:

```sh
adb devices
./gradlew installDebug
```

## Screenshots

Regenerate README and F-Droid/Fastlane screenshots from a connected Android
device:

```sh
scripts/capture-screenshots.sh
```

The script updates:

```text
docs/screenshots/
fastlane/metadata/android/en-US/images/phoneScreenshots/
```

## Offline Station Cache

Aerial bundles a gzipped Aerial station registry as the offline seed for station
discovery. Refresh it before release candidate commits:

```sh
scripts/refresh-registry-cache.sh
```

The script downloads the registry from the Aerial service, validates the
decompressed JSON with `jq`, and updates:

```text
app/src/main/assets/registry.json.gz
```

Do not fetch this cache from Gradle. The checked-in asset keeps F-Droid and
release builds offline and reproducible.

## Release Process

GitHub Actions runs CI on pushes to `main` and on pull requests. CI runs unit
tests, Android lint, and a debug APK build.

The release workflow runs when a version tag is pushed and can also be started
manually from GitHub Actions. Local release preparation does not tag, push, or
upload to Google Play.

For release changes:

1. Update `versionCode` and `versionName` in `app/build.gradle` when the app version changes.
2. Create the Fastlane changelog for the new `versionCode`.
   This can be done with:

```sh
scripts/bump-version.sh 0.1.2
```

3. Update `CHANGELOG.md`.
4. Update `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
5. Refresh screenshots if the UI changed.
6. Run release preparation:

```sh
scripts/prepare-release.sh
```

The release preparation script refreshes the Aerial registry cache, runs
`test lint assembleRelease bundleRelease`, validates the Fastlane changelog, copies
`.fdroid.yml` to the local fdroiddata checkout, and runs:

```sh
fdroid rewritemeta com.shapeshed.aerial
fdroid lint com.shapeshed.aerial
```

7. Commit the local release candidate when ready.
8. Only when explicitly ready to publish, tag the release commit with a signed version tag, for example:

```sh
git tag -s v0.1.0 -m "v0.1.0"
git push origin v0.1.0
```

## Signing

GitHub releases are signed with a private Android release keystore stored in
repository secrets. The same key must be used for every public release so
Android can install updates over previous versions.

Required GitHub repository secrets:

- `AERIAL_KEYSTORE_BASE64`: base64 encoded release keystore file
- `AERIAL_KEYSTORE_PASSWORD`: keystore password
- `AERIAL_KEY_ALIAS`: key alias
- `AERIAL_KEY_PASSWORD`: key password

The helper script creates `local/aerial-release.jks` and
`local/release-signing.env`. The `local/` directory is ignored by git.

Generate the keystore:

```sh
scripts/create-release-keystore.sh
```

Generate the keystore and upload the GitHub secrets with the authenticated
`gh` CLI:

```sh
scripts/create-release-keystore.sh --set-github-secrets
```

For local signed builds:

```sh
source local/release-signing.env
./gradlew assembleRelease
```

For a local Play artifact:

```sh
source local/release-signing.env
./gradlew bundleRelease
```

Manual keystore generation is also possible:

```sh
keytool -genkeypair -v \
  -keystore aerial-release.jks \
  -alias aerial \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Encode a manually generated keystore for GitHub Actions:

```sh
base64 -w 0 aerial-release.jks
```

Do not commit keystores, passwords, generated signed APKs, or Play Console
credentials.

## F-Droid

Aerial is licensed under Apache-2.0 and does not include advertising,
analytics, Google Play Services, Firebase, Crashlytics, or other proprietary
tracking SDKs.

Upstream store metadata lives in `fastlane/metadata/android/en-US/`. A draft
F-Droid build metadata file is available at `.fdroid.yml`, which can be used
with `fdroid build` from the source repository or adapted for F-Droid Data.

F-Droid builds should use the unsigned release path:

```sh
./gradlew assembleRelease
```

The release build must not require local signing environment variables.
