# Changelog

All notable changes to Aerial will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and version numbers should follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html) once public releases begin.

## [Unreleased]

### Added

- Release preparation script that refreshes the bundled Radio Browser cache,
  runs Gradle checks, validates Fastlane changelog coverage, and runs F-Droid
  metadata validation.
- Version bump script for updating Gradle version fields and creating the
  matching Fastlane changelog stub.
- Script for refreshing the bundled Radio Browser offline station cache.
- Home now scrolls to and briefly highlights a station after it is added from discovery.
- Monochrome station logos option in Settings (Android 10+): applies the theme primary colour while preserving luminance via `BlendMode.Color`.
- Custom notification small icon using the app broadcast symbol instead of the generic play icon.
- Developer documentation split out from the product README.
- Privacy policy for app store listings.
- GitHub community health files and Renovate dependency update configuration.
- Android media controls now include a favorite button that reflects the current station state.
- README install badge for adding Aerial to Obtainium.
- Agent guidance covering release, F-Droid, screenshot, UI, and commit conventions.
- Git ignore rules for local fdroidserver output directories.
- README link to the Radio Browser project.
- Splash screen shown on launch using the app icon; content is held until the station database has loaded, preventing a flash of empty state.
- Station discovery now fires automatically after three characters are typed, with a 300 ms debounce, so explicit search submission is optional.
- Station discovery results transition between loading, error, results, and empty states with spring-based `AnimatedContent` animations.
- Typed error states in station discovery: connectivity failures, service outages, and timeouts each show a distinct icon and consumer-friendly message with a retry button.

### Changed

- Station list and grid now use `AsyncImage` instead of `SubcomposeAsyncImage`, reducing composition overhead per row.
- Scroll-hide logic for the FAB is now shared between list and grid views instead of duplicated.
- Logo filenames now use UUIDs instead of millisecond timestamps, preventing collisions during bulk import.
- The SVG-to-PNG image loader is now shared across calls rather than recreated per conversion.
- Radio Browser requests now use a shuffled DNS-discovered server list and retry the next server when one fails.
- Room database schema export enabled; schema file is now tracked for migration validation.
- Database version bumped to 6 with an index on `radioBrowserUuid` for faster import lookups.
- Removed the bundled custom font and restored Material default typography.
- F-Droid metadata now relies on upstream Fastlane metadata for summary and description.
- F-Droid metadata now uses a full release commit hash and reproducible build fields.
- Audio playback now enables Media3 audio offload when the device and stream support it.
- Playback animations now avoid infinite transitions while idle or buffering.
- The expanded player now shows numeric bitrate when available, with no playback-status text in that badge.

### Fixed

- Media controller no longer crashes when switching between dark and light mode; the service binding now uses `applicationContext` and is guarded against double-connect on Activity recreation.
- Station logos with unknown file extensions are now decoded via `BitmapFactory` and re-encoded as JPEG for media artwork, rather than being silently dropped.
- Station discovery search bar stays expanded and keyboard remains visible while editing or clearing the search term.
- Station discovery discovery list items use rounded ripple and spacing in place of dividers, aligned with Material 3 Expressive conventions.
- Removed unused `RadioDiscoveryScreen` which was superseded by `AddStationScreen`.
- Local logo files are now deleted when a station is removed, preventing unbounded storage growth.
- The HTTP connection used to download station logos is now always disconnected after use.
- Station list no longer subscribes to the database twice when computing filtered and current-station state.
- Local logo files are no longer passed to Android media controls as private artwork URIs.
- SVG station logos keep their original file for in-app display and get a PNG media-control copy when imported.
- Duplicate ICY metadata no longer replaces the active media item repeatedly.
- Bitrate state now resets when changing stations to avoid stale bitrate labels.
- Playback failures now clear buffering and show a concise error in the active station UI.
- Adding a discovered station now reuses and highlights an existing station instead of creating a duplicate.

## [0.1.1] - 2026-06-18

### Added

- Full-row station discovery actions so tapping a result adds it directly.
- Material 3 expressive loading states in the mini-player and home buffering row.
- Swipe-down dismissal and sharing in the expanded radio view.
- F-Droid/Fastlane metadata, screenshots, and a repeatable screenshot capture script.
- Locally bundled Google Sans Flex font files under the SIL Open Font License.

### Changed

- Mini-player, home list, and station grid spacing were tightened so row and grid actions stay reachable.
- The home buffering state now shows explicit loading feedback instead of a paused state.
- The add-station FAB now uses the Material 3 expressive toggle style and moves off screen while scrolling.
- README screenshots were refreshed with matching light and dark Mango playback flows.
- Release APKs no longer include Android dependency metadata signing blocks, allowing F-Droid binary scanning.

## [0.1.0] - 2026-06-17

### Added

- Station and settings backup export/import, including locally saved station logos.
- Material 3 aligned home, station list, station grid, search, empty state, and settings updates.
- Adaptive launcher icon assets, including themed icon support.
- Android backup rules that exclude the internal logo cache from system backup and transfer.
- GitHub Actions CI for unit tests, Android lint, and debug APK builds.
- GitHub release workflow for tag-based draft releases with release APK artifacts.
- Unit tests for Radio Browser station de-duplication.
- Initial production-readiness baseline for Aerial.

### Changed

- Search is now launched from the top app bar to preserve home-screen space.
- The empty home state directs first-time users to station discovery.
- Settings no longer includes the placeholder About section.
