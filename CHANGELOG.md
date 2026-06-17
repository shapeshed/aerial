# Changelog

All notable changes to Aerial will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and version numbers should follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html) once public releases begin.

## [Unreleased]

### Added

- Station and settings backup export/import, including locally saved station logos.
- Material 3 aligned home, station list, station grid, search, empty state, and settings updates.
- Adaptive launcher icon assets, including themed icon support.
- Android backup rules that exclude the internal logo cache from system backup and transfer.
- GitHub Actions CI for unit tests, Android lint, and debug APK builds.
- GitHub release workflow for tag-based draft releases with release APK artifacts.
- Unit tests for Radio Browser station de-duplication.

### Changed

- Search is now launched from the top app bar to preserve home-screen space.
- The empty home state directs first-time users to station discovery.
- Settings no longer includes the placeholder About section.

## [0.1.0] - 2026-06-17

### Added

- Initial production-readiness baseline for Aerial.
