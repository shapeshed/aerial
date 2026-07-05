# Privacy Policy

Effective date: 2026-07-05

Aerial is a lightweight Android radio player. This policy explains what data the
app uses and how it is handled.

## Summary

Aerial does not include advertising, analytics, tracking SDKs, Firebase,
Crashlytics, Google Play Services, or user accounts. The app does not sell
personal data.

## Data Stored On Your Device

Aerial stores the following data locally on your device:

- Saved radio stations
- Favorite station state
- Listening statistics — how many times you have played each saved station,
  and when you last played it — used only to sort your favorites
- Recent search terms, which you can remove in the app
- App preferences, such as list or grid view and sort order
- Station logos saved for faster display

This data is stored in the app's private storage. Other apps cannot normally
access it. You can remove this data by deleting stations in the app, clearing
the app's storage, or uninstalling the app.

Listening statistics never leave your device. Aerial does not send them to the
developer or to anyone else. They appear in backup files only when you create
a backup yourself.

## Search

Station search runs entirely on your device, against the station registry
bundled with the app and your saved stations. Aerial never sends search
queries over the network.

## Backups

Aerial can export your stations — including their listening statistics —
settings, and local station logos to a zip file when you choose to use the
export feature. The file is created only when you
request it and is saved to the location you select. You are responsible for how
you store or share exported backup files.

Aerial can also import a backup file that you select.

## Network Access

Aerial uses network access for radio playback:

- When you play a station, Aerial connects to that station's stream URL.
- When a station has a logo URL, Aerial may download the logo and store it
  locally.

The station registry ships inside the app, so browsing and searching for
stations requires no network connection and contacts no server. The registry
is built by the app developer from:

https://aerial.shapeshed.com/registry.json.gz

Individual radio streams and logo URLs are operated by third parties. They may
receive technical information such as your IP address, request time, device
network details, and the station, stream, or logo URL requested. Their handling
of that information is governed by their own policies.

## Now Playing Enrichment

Aerial has an optional setting that enriches the Now Playing screen with
programme and track details. When you enable it, your device contacts the
station provider's own now-playing API directly for supported stations (for
example the BBC, Radio France, Global, and Bauer), and may look up track and
cover art details on MusicBrainz and the Cover Art Archive. No Aerial or
developer-operated service sits in between, and the developer never sees these
requests. The requests identify the station, and for track lookups the song
title and artist reported by the stream. As with streams and logos, these
providers may receive technical information such as your IP address, and their
handling of it is governed by their own policies. When the setting is off,
Aerial makes none of these requests.

## Notifications And Media Controls

Aerial uses Android media playback controls so you can control playback from
system surfaces such as notifications, lock screen controls, Bluetooth devices,
and quick settings. On Android versions that require notification permission,
Aerial may request permission to show playback notifications.

## Data Shared By The App

Aerial does not send your saved station list, favorites, listening statistics,
search history, settings, or backup files to the developer.

The app shares data only when needed for features you use, such as connecting
to a stream URL, downloading station logos, fetching now-playing details for
the enrichment setting, or using Android's share sheet when you choose to
share a station.

## Children

Aerial is not directed at children and does not knowingly collect personal data
from children.

## Changes

This policy may be updated when Aerial's features or data handling change. The
latest version is maintained in the project repository.

## Contact

For privacy questions, contact:

george@shapeshed.com
