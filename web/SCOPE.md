# Web Player — Agreed Scope

Ref: [Virgil-Bulens/aerial#1](https://github.com/Virgil-Bulens/aerial/issues/1)

## Summary

Aerial gets a browser-based radio player. The player lives in this
repository, in a new `web/` directory. The player is a static site. It has
no server component.

## In scope

- A static web app in `web/` that plays radio stations in a browser.
- The player reads `app/src/main/registry/registry.json` as its station
  list.
- The player filters the registry to `https://` stream URLs only. It
  excludes the `http://` entries (about 8,793 of 44,142), because a page
  served over HTTPS blocks mixed-content audio.
- Browse screen: curated collections, matching the curated groups defined
  in `RegistryRepository.kt`.
- Search: text search across the https-filtered registry, by name, tags,
  and country.
- Favorites: save and remove stations. Favorites use browser
  `localStorage`. Favorites do not sync live with the Android app or
  across browsers or devices.
- Playback: play, pause, and switch stations, with the browser's native
  `<audio>` element.
- Data import and export, compatible with the Android app's backup file.
  The web player reads and writes the same `.zip` and `manifest.json`
  structure as `SettingsBackupManager.kt` (`BACKUP_MANIFEST`, `version`,
  `app`, `settings`, `stations`). This lets a user move favorites between
  the Android app and the web player by hand, one file at a time.
  - The web player skips the `logos/` image entries in the zip, on
    export and on import. Station artwork on the web comes from each
    station's own `logo_url` in the registry, not from a bundled image
    file.
  - The web player uses one vendored, single-file JavaScript zip
    library, checked into `web/` and loaded with a plain `<script>`
    tag. This is the one exception to "no third-party code" below. It
    adds no package manager and no build step.
- Plain HTML, CSS, and JavaScript only, beyond the one vendored zip
  library above. No framework, no bundler, no package manager.

## Out of scope

- Any server, proxy, or relay. The player must not add backend
  infrastructure.
- Playback of `http://` stream URLs. Aerial's Android app can play these.
  The web player cannot.
- Accounts, sign-in, or any live/automatic cross-device sync of
  favorites. Import/export is a manual, one-file-at-a-time transfer, not
  a sync service.
- Round-tripping station logo images through the backup file.
- Changes to the Android app itself: playback code, UI, or the Room
  favorites database.
- A build step for `registry.json`. The player reads the checked-in JSON
  file directly. It does not run `generate-registry-db.py` and does not
  produce a SQLite asset.

## Explicitly deferred

Not decided, not built in v1:

- A relay or proxy for `http://` stations, to reach full station parity
  with Android.
- A deploy or hosting target for the static site.

## Data note

`registry.json` has no live sync today. Contributors refresh it by hand
and commit the update (see `DEVELOPERS.md`, "Offline Station Cache"). The
web player and the Android app therefore share one manually maintained
station list.
