# Changelog

All notable changes to Aerial will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and version numbers should follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html) once public releases begin.

## [Unreleased]

### Added

- Developer documentation split out from the product README.
- Privacy policy for app store listings.
- GitHub community health files and Renovate dependency update configuration.
- Android media controls now include a favorite button that reflects the current station state.
- README install badge for adding Aerial to Obtainium.
- Agent guidance covering release, F-Droid, screenshot, UI, and commit conventions.
- Git ignore rules for local fdroidserver output directories.
- README link to the Radio Browser project.

### Changed

- Removed the bundled custom font and restored Material default typography.
- F-Droid metadata now relies on upstream Fastlane metadata for summary and description.
- F-Droid metadata now uses a full release commit hash and reproducible build fields.
- Audio playback now enables Media3 audio offload when the device and stream support it.
- Playback animations now avoid infinite transitions while idle or buffering.
- The expanded player now shows numeric bitrate when available, with no playback-status text in that badge.

### Fixed

- Local logo files are no longer passed to Android media controls as private artwork URIs.
- SVG station logos keep their original file for in-app display and get a PNG media-control copy when imported.
- Duplicate ICY metadata no longer replaces the active media item repeatedly.
- Bitrate state now resets when changing stations to avoid stale bitrate labels.
- Playback failures now clear buffering and show a concise error in the active station UI.

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
