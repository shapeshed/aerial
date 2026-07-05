# AGENTS.md

Guidance for future agents working on Aerial.

## Project

Aerial is a lightweight Android radio player built with Kotlin, Gradle, and
Jetpack Compose Material 3. The package/application id is:

```text
com.shapeshed.aerial
```

Use JDK 17. The app currently compiles with Android SDK 37 and targets SDK 37.

## Common Commands

Build and install a debug build:

```sh
./gradlew installDebug
```

Run the normal release gate:

```sh
./gradlew test lint assembleRelease bundleRelease
```

Build only debug Kotlin when checking a small UI/code change:

```sh
./gradlew compileDebugKotlin
```

## Release Versioning

Release versions use:

- Gradle `versionName`: plain semver, for example `0.1.1`
- Gradle `versionCode`: monotonically increasing integer
- Git tags: prefixed with `v`, for example `v0.1.1`

For a release, update:

- `app/build.gradle`
- `CHANGELOG.md`
- `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`

Use the bump helper when changing versions:

```sh
scripts/bump-version.sh 0.1.2
```

Before committing a release, run:

```sh
scripts/prepare-release.sh
```

This refreshes the bundled Aerial registry cache, runs the Gradle release
gate, validates the Fastlane changelog, and runs F-Droid metadata validation
against `/home/go/src/gitlab.com/fdroid/fdroiddata` when available.

Then commit when the local release candidate is ready. Do not tag, push, or
upload to Google Play unless explicitly asked.

```sh
git commit -S -m "chore(release): v0.1.1"
```

## Signing

Android release signing uses environment variables:

```text
AERIAL_KEYSTORE_FILE
AERIAL_KEYSTORE_PASSWORD
AERIAL_KEY_ALIAS
AERIAL_KEY_PASSWORD
```

The helper script is:

```sh
scripts/create-release-keystore.sh
scripts/create-release-keystore.sh --set-github-secrets
```

Never commit keystores, passwords, signed APKs, or generated local signing
files. The `local/` directory is intentionally ignored.

## Screenshots

Screenshots for README and F-Droid/Fastlane are generated from a connected
Android device:

```sh
scripts/capture-screenshots.sh
```

The script captures a Mango-oriented product flow in light and dark mode:

1. Station discovery search for `Mango`
2. Home favorites view with Mango playing
3. Expanded now playing view with Mango playing
4. Settings with playback detail and backup controls

It updates:

```text
docs/screenshots/
fastlane/metadata/android/en-US/images/phoneScreenshots/
```

## Offline Station Cache

The app bundles `app/src/main/assets/registry.db.compressed` as the offline Aerial
station registry. Generate it from a local registry JSON or JSON.GZ file before
release candidates:

```sh
scripts/generate-registry-db.py --input /path/to/registry.json.gz
```

Do not move this fetch into Gradle. The checked-in compressed SQLite database
keeps GitHub and F-Droid builds offline and reproducible. Local source JSON/GZ
files are ignored and should not be committed.

## F-Droid

The app repo contains `.fdroid.yml` as the source/draft metadata. Official
F-Droid submission uses the separate `fdroiddata` repo:

```text
/home/go/src/gitlab.com/fdroid/fdroiddata
```

The metadata file there is:

```text
metadata/com.shapeshed.aerial.yml
```

For new submissions, the fdroiddata branch name used was:

```text
com.shapeshed.aerial
```

The fdroiddata remotes should be:

```text
origin    git@gitlab.com:shapeshed/fdroiddata.git
upstream  https://gitlab.com/fdroid/fdroiddata.git
```

Copy metadata from this repo into fdroiddata:

```sh
cp /home/go/src/github.com/shapeshed/aerial/.fdroid.yml \
  /home/go/src/gitlab.com/fdroid/fdroiddata/metadata/com.shapeshed.aerial.yml
```

Run validation from the fdroiddata checkout:

```sh
fdroid rewritemeta com.shapeshed.aerial
fdroid lint com.shapeshed.aerial
```

Important fdroiddata details learned:

- Categories should mirror comparable radio apps such as Transistor:
  `Multimedia` and `Radio`.
- Use `Changelog: https://github.com/shapeshed/aerial/blob/HEAD/CHANGELOG.md`
  rather than `/main/`.
- Do not keep `Summary:` in fdroiddata metadata. `tools/make-summary-translatable.py`
  moves it to `metadata/com.shapeshed.aerial/en-US/summary.txt`.
- Upstream Fastlane metadata should stay in this app repo under
  `fastlane/metadata/android/en-US/`.
- Future F-Droid releases should normally be picked up by auto-update when a
  new version tag is pushed. Manual fdroiddata MRs should only be needed when
  build metadata or store metadata changes.

The F-Droid MR for the initial submission was:

```text
https://gitlab.com/fdroid/fdroiddata/-/merge_requests/40759
```

## UI Notes

- Favor a minimal UI. Keep controls direct, avoid decorative complexity, and
  preserve the app's lightweight radio-player feel.
- Prefer Material 3 and Material 3 Expressive components/patterns already used
  in the app rather than introducing custom interaction styles.
- Use Material's default typography unless there is a strong reason to add a
  custom font; avoid bundling fonts for styling alone.
- The home screen favors direct tiles for favorites, featured stations, and
  quick genre playback.
- Add-station entry points should use clear language like `Add a station`.
- Home content should clear the mini-player when active.

## Git Hygiene

The worktree may contain user changes. Do not revert unrelated changes.
Generated F-Droid output directories such as `repo/`, `tmp/`, or `logs/` should
not be committed from the app repo.

Use Conventional Commits for commit messages:

```text
type(scope): description
```

For releases, use:

```text
chore(release): v0.1.1
```

Reference: https://www.conventionalcommits.org/
