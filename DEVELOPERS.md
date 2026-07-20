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

## Data and Backup

Aerial stores stations in Room, preferences in DataStore, and downloaded station logos in internal app storage under `logos/`.

The in-app export/import feature is the supported way to move user data between installs. Android system backup excludes the internal logo cache to avoid large or stale auto-backups.

## Offline Station Cache

Aerial bundles a compressed SQLite Aerial station registry for station
discovery. The source registry data is checked in at:

```text
app/src/main/registry/registry.json
```

Gradle generates the APK asset during Android builds by running
`scripts/generate-registry-db.py`. The generated file is:

```text
app/build/generated/aerialRegistry/assets/registry.db.compressed
```

You can run the generator manually when validating registry changes:

```sh
scripts/generate-registry-db.py \
  --input app/src/main/registry/registry.json \
  --output app/build/generated/aerialRegistry/assets/registry.db.compressed \
  --schema app/schemas/com.shapeshed.aerial.data.RegistryDatabase/1.json
```

Do not fetch registry data from Gradle. Refresh the source JSON outside the
build only when intentionally updating the offline registry, then commit the
uncompressed `registry.json`. Generated compressed database assets and local
JSON.GZ files are ignored and should not be committed.

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

## Google Play publishing

CI publishes to Google Play automatically using the [Gradle Play
Publisher](https://github.com/Triple-T/gradle-play-publisher) plugin — there
is no manual upload step.

The release workflow (`.github/workflows/release.yml`) builds an AAB when a
`v*` tag is pushed and publishes it to the **production** track at 100%
rollout. There is no beta or internal testing track in use — nightly builds
(`.github/workflows/nightly.yml`) only produce a GitHub release APK, they do
not publish to Play. Use the GitHub `nightly` release, or an F-Droid build
from source, for pre-release testing instead.

Version codes are resolved automatically against Play
(`resolutionStrategy = AUTO` in `app/build.gradle`) whenever Play credentials
are present, so the checked-in `versionCode` does not need to be bumped for
each release. When no credentials are configured — local builds without
`AERIAL_PLAY_SERVICE_ACCOUNT_JSON_FILE` set, and F-Droid's reproducible
build — the plugin falls back to the static checked-in `versionCode`, with no
network call and no credentials required.

Required GitHub repository secret:

- `AERIAL_PLAY_SERVICE_ACCOUNT_JSON_BASE64`: base64 encoded Google Cloud
  service account JSON key.

  1. Create a service account in Google Cloud Console and enable the Google
     Play Android Developer API for that project.
  2. Create a JSON key for it and download it.
  3. In Play Console, go to account-level **Setup > API access** and link the
     Google Cloud project (it should be auto-detected once the API is
     enabled).
  4. In Play Console, go to **Users and permissions**, invite the service
     account's email (the `client_email` field in the JSON key), and grant it
     app permissions for Aerial: release to production, and view app
     information. Permissions can take a few minutes to propagate.
  5. Encode the key and store it as the secret:

     ```sh
     # macOS (BSD base64, no -w flag)
     base64 -i play-service-account.json | gh secret set AERIAL_PLAY_SERVICE_ACCOUNT_JSON_BASE64

     # Linux (GNU base64)
     base64 -w 0 play-service-account.json | gh secret set AERIAL_PLAY_SERVICE_ACCOUNT_JSON_BASE64
     ```

  Delete the local JSON key file once it's stored as a secret.

For a local publish, set `AERIAL_PLAY_SERVICE_ACCOUNT_JSON_FILE` to the path
of the JSON key alongside the release-signing env vars:

```sh
source local/release-signing.env
export AERIAL_PLAY_SERVICE_ACCOUNT_JSON_FILE=/path/to/play-service-account.json
./gradlew publishBundle --track production
```

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

## Android Auto

`PlayerService` is a `media3` `MediaLibraryService` exposing a browse tree:
root -> Favorites | Moods | Recently Played, plus voice/typed search. The
tree-building logic lives in `data/MediaBrowseTree.kt` (plain suspend
functions over `StationRepository`/`RegistryRepository`, no session types) so
it's unit-testable without Robolectric — see
`app/src/test/java/com/shapeshed/aerial/data/MediaBrowseTreeTest.kt`. It's
named/placed generically (not car-specific) since Google TV support is
planned next and will reuse it.

Android Auto renders the browse tree itself — there's no custom UI to build
or theme; the only app-controlled surfaces are the browse tree's content and
the accent colour/icon declared in the manifest (`res/xml/automotive_app_desc.xml`,
`Theme.Aerial.Car` in `themes.xml`).

This can't be rendered or screenshotted in a normal dev environment. To test
with the [Desktop Head Unit](https://developer.android.com/training/cars/testing):

```sh
./gradlew installDebug
adb forward tcp:5277 tcp:5277
```

On the phone, open the Android Auto app's settings and enable
**Developer settings > Start head unit server** — without this the DHU
connects but sits at "waiting for phone". Then launch the DHU binary
(`extras/google/auto/desktop-head-unit` in the Android SDK). If the phone-side
toggle was enabled after the DHU connected, restart the DHU.

Check: the app appears in Android Auto's app list, the root shows
Favorites/Moods/Recently Played, mood folders show their stations with logos,
tapping a station plays it and queues its list (skip next/previous wraps
around), playing updates Recently Played, and voice search ("Ok Google, play
`<station>` on Aerial") resolves and plays.

Live streams always report `speed=0` / unknown position through media3's
legacy session bridge (`MediaSessionLegacyStub.createPlaybackStateCompat`
checks `isCurrentMediaItemLive()`), so Android Auto's queue equalizer
indicator shows static bars rather than animating. This is expected for all
media3 live-radio apps, not a bug.

The Recently Played folder reorders on re-entry but not while it stays on
screen: `notifyChildrenChanged("recent")` fires after each recorded play
(visible in logcat), but the Auto host decides whether to re-query a folder
it is currently displaying, and the DHU does not. Not fixable app-side.

## Hardware / Bluetooth media buttons

`media3`'s default `MediaSession.Callback` handles `onSkipToNext`/`onSkipToPrevious`
(from headphones, a car stereo's AVRCP controls, or the media notification) by
seeking within the player's timeline — it needs a timeline with neighbouring items to
do anything. `MainViewModel.play()`/`playFromRegistry()`/`loadStationPaused()` take an
optional `queue` parameter: whichever list the phone UI is already showing (favourites
in the user's sort order, an active mood, For You) is queued as real `MediaItem`s via
`setMediaItems`, the same list `NowPlayingScreen`'s swipe pager steps through. A
station played outside any visible list (an ephemeral search result) still falls back
to a single-item queue, same as before.

This can be verified without real Bluetooth hardware:

```sh
adb shell input keyevent 87  # KEYCODE_MEDIA_NEXT
adb shell input keyevent 88  # KEYCODE_MEDIA_PREVIOUS
```

Play a favourite, confirm skip moves through the same list/order the Now Playing
pager swipes through, and check `logcat -s AerialPlayerService` for errors.
