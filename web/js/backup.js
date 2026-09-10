// Import/export compatible with Aerial's Android backup file
// (ZipSettingsBackupManager.kt): a .zip containing one entry, "backup.json",
// with { version, app, settings, stations }.
//
// The web player skips the logos/ image entries on both export and import
// (see web/SCOPE.md) — station artwork comes from each station's own
// logo_url/logoPath instead of a bundled image file.
//
// Uses the vendored fflate library (web/vendor/fflate.js, loaded as a
// classic <script> before this module) for zip read/write. This is the one
// third-party file the web player depends on.

const BACKUP_MANIFEST = "backup.json";
const BACKUP_VERSION = 1;

function fflate() {
  if (!globalThis.fflate) {
    throw new Error("fflate is not loaded (web/vendor/fflate.js missing a <script> tag?)");
  }
  return globalThis.fflate;
}

/**
 * Builds the same .zip Aerial's Android app would import: one entry,
 * backup.json, holding the manifest below. Returns a Blob.
 */
export function exportBackup(favoriteRecords) {
  const { zipSync, strToU8 } = fflate();

  const manifest = {
    version: BACKUP_VERSION,
    app: "Aerial",
    stations: favoriteRecords.map((s) => ({
      name: s.name,
      streamUrl: s.streamUrl,
      isFavorite: true,
      provider: s.provider,
      providerId: s.providerId,
      tags: s.tags,
      description: s.description,
      country: s.country,
      countryCode: s.countryCode,
      playCount: s.playCount || 0,
      lastPlayedAt: s.lastPlayedAt || 0,
      // A remote URL is a valid logoPath on Android too — see
      // SettingsBackupManager.kt's export(), which only writes a zip
      // logos/ entry for a *local file* path, and otherwise stores the
      // path (URL included) directly as JSON.
      logoPath: s.logoPath || "",
    })),
  };

  const zipped = zipSync(
    { [BACKUP_MANIFEST]: strToU8(JSON.stringify(manifest, null, 2)) },
    { level: 6 },
  );
  return new Blob([zipped], { type: "application/zip" });
}

/**
 * Reads a .zip produced by the Android app (or by exportBackup above).
 * Returns the favorite-shaped station records found in it. Any logos/
 * entries in the zip are ignored. Throws if the manifest is missing or
 * its version is not supported.
 */
export async function importBackup(file) {
  const { unzipSync, strFromU8 } = fflate();

  const buffer = new Uint8Array(await file.arrayBuffer());
  const entries = unzipSync(buffer, {
    // Skip inflating logo image bytes entirely — we never read them.
    filter: (entry) => entry.name === BACKUP_MANIFEST,
  });

  const manifestBytes = entries[BACKUP_MANIFEST];
  if (!manifestBytes) {
    throw new Error("Backup file has no backup.json manifest");
  }

  const manifest = JSON.parse(strFromU8(manifestBytes));
  if (manifest.version !== BACKUP_VERSION) {
    throw new Error(`Unsupported backup version: ${manifest.version}`);
  }

  const stations = Array.isArray(manifest.stations) ? manifest.stations : [];
  return stations
    .filter((item) => typeof item.name === "string" && typeof item.streamUrl === "string")
    .map((item) => ({
      name: item.name.trim(),
      streamUrl: item.streamUrl.trim(),
      isFavorite: true,
      provider: (item.provider || "").trim(),
      providerId: (item.providerId || "").trim(),
      tags: (item.tags || "").trim(),
      description: (item.description || "").trim(),
      country: (item.country || "").trim(),
      countryCode: (item.countryCode || "").trim(),
      playCount: item.playCount || 0,
      lastPlayedAt: item.lastPlayedAt || 0,
      // A logoFile entry (a bundled image) has no web equivalent — leave
      // artwork blank for those rather than guessing at a path.
      logoPath: item.logoFile ? "" : (item.logoPath || ""),
    }));
}
