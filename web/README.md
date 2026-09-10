# Aerial Web Player

A static, no-build browser player for Aerial's station registry. See
[SCOPE.md](SCOPE.md) for what is in and out of scope.

## Run it locally

No install step. Serve this directory with any static file server, then
open it in a browser:

```sh
cd web
python3 -m http.server 8000
```

Open <http://localhost:8000/>.

## Tests

Pure logic (registry filtering, curated groups, search, favorites, backup
zip, and the player state machine) has a test suite in `test/`, using
Node's built-in test runner. No package manager, no test framework — the
same no-build-step rule as the shipped site (see SCOPE.md).

```sh
cd web
node --test
```

`test-support/` holds two small stand-ins the tests need and Node has no
built-in for: an in-memory `localStorage`, and a loader for the vendored
zip library as a global. They exist only for tests and ship nowhere near
the site. `test-support/` (not `test/support/`) is deliberate — Node's
test runner treats every file under any directory literally named `test`
as a test file to run, so a helper nested inside `test/` gets executed as
an (empty, harmless, but noisy) test suite of its own.

DOM-level glue (`js/app.js`, and the DOM-facing parts of `js/player.js`)
has no unit tests — see the browser walkthrough in the pull request
instead for that.

## How it fits together

- `registry.json` is a symlink to
  `../app/src/main/registry/registry.json`, so the web player and the
  Android app read one shared, manually maintained station list. No
  build step copies or regenerates it.
- `js/registry.js` loads that file and filters it to `https://` stream
  URLs (see SCOPE.md).
- `js/curated-data.js` and `js/curated.js` port the curated mood groups
  and their resolution logic from
  `RegistryRepository.kt`/`CURATED_MOOD_STATIONS`. There is no build
  step keeping this in sync — if the Kotlin source changes, update
  `curated-data.js` by hand.
- `js/favorites.js` stores favorites in `localStorage`, browser-local
  only.
- `js/backup.js` reads and writes the same `.zip`/`backup.json` shape as
  the Android app's `SettingsBackupManager.kt`, skipping the `logos/`
  image entries on both sides.
- `vendor/fflate.js` is the one third-party file this project uses, for
  zip read/write (MIT license, see `vendor/LICENSE-fflate.txt`).

## Known gap: deploying this

Where this site gets hosted is explicitly deferred (see SCOPE.md). The
`registry.json` symlink assumes the deploy step serves this directory
with the symlink preserved (or otherwise resolved to a real file at that
path) — that has not been decided or tested yet.
