// Browser-local favorites, stored in localStorage. No accounts, no sync
// with the Android app or across browsers/devices — see web/SCOPE.md.
//
// A favorite is stored as a full station snapshot, not a foreign key into
// the registry. This mirrors Aerial's own Station table (Station.kt) and
// is what makes the shape compatible with the Android backup file's
// "stations" array (see backup.js).

import { stationKey } from "./registry.js";

const STORAGE_KEY = "aerial-web-favorites-v1";

function read() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch (err) {
    console.warn("Could not read favorites from localStorage", err);
    return [];
  }
}

function write(list) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(list));
  } catch (err) {
    console.warn("Could not save favorites to localStorage", err);
  }
}

function toRecord(station) {
  return {
    name: station.name,
    streamUrl: station.streamUrl,
    isFavorite: true,
    provider: station.provider,
    providerId: station.providerId,
    tags: station.tags,
    description: station.description,
    country: station.country,
    countryCode: station.countryCode,
    playCount: station.playCount || 0,
    lastPlayedAt: station.lastPlayedAt || 0,
    // logoPath doubles as a remote URL on Android when it isn't a local
    // file path (see SettingsBackupManager.kt's export()) — reusing the
    // registry's logo_url here keeps artwork on round trip without
    // shipping an image file.
    logoPath: station.logoUrl || "",
  };
}

export function listFavorites() {
  return read();
}

export function isFavorite(station) {
  const key = stationKey(station);
  return read().some((f) => stationKey(f) === key);
}

export function toggleFavorite(station) {
  const key = stationKey(station);
  const list = read();
  const index = list.findIndex((f) => stationKey(f) === key);
  if (index >= 0) {
    list.splice(index, 1);
  } else {
    list.push(toRecord(station));
  }
  write(list);
  return index < 0; // true if it is now favorited
}

/**
 * Merges imported station records into favorites, replacing any existing
 * entry with the same identity (same behaviour as the Android app's
 * repository.upsertImported — see SettingsBackupManager.kt).
 */
export function mergeImported(records) {
  const list = read();
  for (const incoming of records) {
    const key = stationKey(incoming);
    const index = list.findIndex((f) => stationKey(f) === key);
    if (index >= 0) {
      list[index] = incoming;
    } else {
      list.push(incoming);
    }
  }
  write(list);
}
