# Aerial

Aerial is a lightweight Android radio player.

## Install

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.shapeshed.aerial%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fshapeshed%2Faerial%22%2C%22author%22%3A%22shapeshed%22%2C%22name%22%3A%22Aerial%22%2C%22supportFixedAPKURL%22%3Afalse%7D">
  <img src="docs/badges/get-it-on-obtainium.png" alt="Get it on Obtainium" height="60">
</a>

## Screenshots

Light mode:

<p>
  <img src="docs/screenshots/find-station-light.png" alt="Find a station in Aerial in light mode" width="220">
  <img src="docs/screenshots/home-playing-light.png" alt="Aerial station list while playing in light mode" width="220">
  <img src="docs/screenshots/now-playing-light.png" alt="Aerial now playing view in light mode" width="220">
</p>

Dark mode:

<p>
  <img src="docs/screenshots/find-station.png" alt="Find a station in Aerial" width="220">
  <img src="docs/screenshots/home-playing.png" alt="Aerial station list while playing" width="220">
  <img src="docs/screenshots/now-playing.png" alt="Aerial now playing view" width="220">
</p>

Regenerate README and F-Droid/Fastlane screenshots from a connected Android
device:

```sh
scripts/capture-screenshots.sh
```

## Features

- Search for stations using the Radio Browser API.
- Add stations manually when a stream URL is known.
- Play live streams with Android media controls.
- Save favorite stations.
- Switch between list and grid station views.
- Store station logos locally for faster loading.
- Export and import stations, settings, and saved logos as a zip backup.
- Adaptive launcher icon with Android themed icon support.

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
./gradlew test lint assembleRelease
```

Without signing environment variables, the release APK produced locally is unsigned:

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

## Release Process

GitHub Actions runs CI on pushes to `main` and on pull requests. CI runs unit tests, Android lint, and a debug APK build.

The release workflow runs when a version tag is pushed and can also be started manually from GitHub Actions. It builds the release APK, uploads it as a workflow artifact, and creates a draft GitHub release for tag pushes.

For release changes:

1. Update `versionCode` and `versionName` in `app/build.gradle` when the app version changes.
2. Update `CHANGELOG.md`.
3. Merge the release commit to `main`.
4. Tag the release commit with a version tag, for example:

```sh
git tag v0.1.0
git push origin v0.1.0
```

## Signing

GitHub releases are signed with a private Android release keystore stored in repository secrets. The same key must be used for every public release so Android can install updates over previous versions.

Required GitHub repository secrets:

- `AERIAL_KEYSTORE_BASE64`: base64 encoded release keystore file
- `AERIAL_KEYSTORE_PASSWORD`: keystore password
- `AERIAL_KEY_ALIAS`: key alias
- `AERIAL_KEY_PASSWORD`: key password

The helper script creates `local/aerial-release.jks` and `local/release-signing.env`. The `local/` directory is ignored by git.

Generate the keystore:

```sh
scripts/create-release-keystore.sh
```

Generate the keystore and upload the GitHub secrets with the authenticated `gh` CLI:

```sh
scripts/create-release-keystore.sh --set-github-secrets
```

For local signed builds:

```sh
source local/release-signing.env
./gradlew assembleRelease
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

Do not commit keystores, passwords, generated signed APKs, or Play Console credentials.

## F-Droid

Aerial is licensed under Apache-2.0 and does not include advertising, analytics, Google Play Services, Firebase, Crashlytics, or other proprietary tracking SDKs.

Upstream store metadata lives in `fastlane/metadata/android/en-US/`. A draft F-Droid build metadata file is available at `.fdroid.yml`, which can be used with `fdroid build` from the source repository or adapted for F-Droid Data.

F-Droid builds should use the unsigned release path:

```sh
./gradlew assembleRelease
```

The release build must not require local signing environment variables.

## Data And Backup

Aerial stores stations in Room, preferences in DataStore, and downloaded station logos in internal app storage under `logos/`.

The in-app export/import feature is the supported way to move user data between installs. Android system backup excludes the internal logo cache to avoid large or stale auto-backups.

## License

Aerial is licensed under the Apache License, Version 2.0. See `LICENSE`.

Bundled Google Sans Flex font files are licensed under the SIL Open Font
License, Version 1.1. See `third_party/licenses/google_sans_flex/OFL.txt`.
