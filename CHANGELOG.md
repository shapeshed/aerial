# Changelog

All notable changes to Aerial will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and version numbers should follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html) once public releases begin.

## [Unreleased]

## [0.4.0] - 2026-07-09

### Added

- Sleep timer with release-ready presets.
- Per-app language support with localized strings for supported Android locales.
- Full-text station search with accent folding, descriptions, and A-Z station browsing before searching.
- Home and Favourites tabs, favourite sorting by A-Z, last played, and most played, plus favourite swiping in the now playing view.
- Curated mood playlists, local recommendations, country-based `For you` sections, and richer mood artwork.
- Station preview playback directly from search results.
- Now playing enrichment for Radio France, Rinse, and RTE stations.
- Nightly release workflow that builds and signs a testable APK artifact.

### Changed

- The station registry now ships as a preloaded SQLite database generated during the build from checked-in registry JSON.
- Home, favourites, mood cards, and now playing artwork were polished for a simpler radio-focused experience.
- Landscape and tablet layouts now use a wide navigation rail, with capped now playing artwork sizes on larger screens.
- README, Fastlane metadata, screenshots, and store artwork were refreshed around the `Just radio for Android` positioning.
- Privacy documentation now covers on-device search, listening stats, and now playing enrichment.

### Removed

- Remote registry fetching from app builds.
- Registry statistics footer from settings.
- Debug-only 30-second sleep timer preset from release builds.

### Fixed

- F-Droid and release builds now generate the registry asset before asset merging and lint vital tasks.
- Playback reconnects a live stream after a stale pause.
- Favourites are deleted correctly when unfavourited.
- Backup export/import now preserves play statistics.
- Now playing artwork no longer flashes during pager transitions, and corrupt logo downloads are rejected.
- Now playing summaries and duplicated artwork are derived more reliably.
- Localized search-result countries and search headers now satisfy the lint quality gate.

## [0.3.0] - 2026-06-27

### Added

- Aerial station registry with featured stations, country and genre filters, and quick genre playback.
- Home screen favorites tiles with a mini-player and expanded now playing flow.
- `Show what's playing` setting for song, artist, and artwork details when stations provide them.
- Repeatable quality task for compile, lint, and unit test checks.

### Changed

- Station discovery, home playback, now playing, and settings UI were simplified around the new tile-based experience.
- Add-station entry points now use clearer `Add a station` language.
- Store screenshots and metadata now showcase discovery, favorites playback, now playing, and settings.
- Backup export/import now focuses on stations and saved logos.

### Removed

- Monochrome logo setting.
- Bitrate display setting and user-facing bitrate/HD badges.
- Legacy Radio Browser search, fallback cache, voting/click tracking, and bundled fallback JSON.
- Debug-only provider station import flow and its provider discovery helpers.
- Obsolete list/grid view state and unused resources left behind by the UI refactor.

### Fixed

- Tapping media notification controls no longer creates a new activity when the app is already running.
- Tapping media notification controls restores the now playing view without animation if it was open before backgrounding.
- Mini player and playback state now restore correctly when returning to the app from a notification.
- Now playing view, favorites filter, and list/grid scroll position all survive Activity recreation (e.g. after the system reclaims memory while the player service keeps running).
- White flash before Compose renders on app launch is eliminated.

## [0.2.0] - 2026-06-21

### Added

- Material 3 Expressive station discovery and playback refinements.
- Home now scrolls to and briefly highlights a station after it is added from discovery.
- Monochrome station logos in Settings on Android 10+.
- Android media controls now include a favorite button that reflects the current station state.
- Splash screen shown on launch while the station database loads.

### Changed

- Radio Browser requests now use a shuffled DNS-discovered server list and retry the next server when one fails.
- Audio playback enables Media3 audio offload when the device and stream support it.
- The expanded player now shows numeric bitrate when available.
- Removed the bundled custom font and restored Material default typography.

### Fixed

- Media controller no longer crashes when switching between dark and light mode.
- Station discovery search and results flows are more stable and easier to recover from errors.
- Station logos keep their original SVG for in-app display and get a PNG media-control copy when imported.
- Adding a discovered station now reuses and highlights an existing station instead of creating a duplicate.
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
