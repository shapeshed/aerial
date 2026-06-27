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
  <img src="docs/screenshots/home-playing-light.png" alt="Aerial favorites while playing in light mode" width="220">
  <img src="docs/screenshots/now-playing-light.png" alt="Aerial now playing view in light mode" width="220">
  <img src="docs/screenshots/settings-light.png" alt="Aerial settings in light mode" width="220">
</p>

Dark mode:

<p>
  <img src="docs/screenshots/find-station.png" alt="Find a station in Aerial" width="220">
  <img src="docs/screenshots/home-playing.png" alt="Aerial favorites while playing" width="220">
  <img src="docs/screenshots/now-playing.png" alt="Aerial now playing view" width="220">
  <img src="docs/screenshots/settings.png" alt="Aerial settings" width="220">
</p>

## Features

- Search for stations from the bundled Aerial registry, refreshed from the Aerial service.
- Add stations manually when a stream URL is known.
- Play live streams with Android media controls.
- Save favorite stations.
- Browse favorite stations, featured stations, and quick genre shortcuts.
- Show song, artist, and artwork when stations provide playback details.
- Store station logos locally for faster loading.
- Export and import stations and saved logos as a zip backup.
- Adaptive launcher icon with Android themed icon support.

## Data And Backup

Aerial stores stations in Room, preferences in DataStore, and downloaded station logos in internal app storage under `logos/`.

The in-app export/import feature is the supported way to move user data between installs. Android system backup excludes the internal logo cache to avoid large or stale auto-backups.

## Development

Developer documentation lives in [DEVELOPERS.md](DEVELOPERS.md).

## Privacy

Aerial does not include advertising, analytics, tracking SDKs, Firebase,
Crashlytics, Google Play Services, or user accounts. See [PRIVACY.md](PRIVACY.md)
for the full privacy policy.

## License

Aerial is licensed under the Apache License, Version 2.0. See `LICENSE`.
