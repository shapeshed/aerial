// Loads the shared station registry (app/src/main/registry/registry.json,
// symlinked into web/registry.json) and filters it to https:// streams only.
//
// A page served over HTTPS blocks playback of http:// audio as mixed
// content, so those entries are dropped here rather than surfaced as
// stations that silently fail to play. See web/SCOPE.md.

const REGISTRY_URL = "./registry.json";

let cachedStations = null;

/**
 * A stable identity for a registry row, used for favorites and de-duping.
 * Curated entries always have an empty provider_id (see
 * RegistryRepository.kt), so those fall back to name+provider.
 */
export function stationKey(station) {
  if (station.providerId) {
    return `${station.provider}::${station.providerId}`;
  }
  return `${station.provider}::name::${station.name}`;
}

function normalize(raw) {
  return {
    name: raw.name,
    streamUrl: raw.stream_url,
    logoUrl: raw.logo_url || "",
    country: raw.country || "",
    countryCode: raw.country_code || "",
    tags: raw.tags || "",
    provider: raw.provider || "",
    providerId: raw.provider_id || "",
    description: raw.description || "",
  };
}

/**
 * Pure transform from raw registry.json rows to the https-only, normalized
 * station shape the rest of the app uses. Split out from loadStations() so
 * it can be unit-tested without a fetch/DOM environment — see
 * web/test/registry.test.mjs.
 */
export function filterHttpsAndNormalize(rawEntries) {
  return rawEntries
    .filter((entry) => typeof entry.stream_url === "string" && entry.stream_url.startsWith("https://"))
    .map(normalize);
}

/**
 * Fetches and caches the https-only station list. Call once at startup;
 * later calls return the cached array.
 */
export async function loadStations() {
  if (cachedStations) return cachedStations;

  const response = await fetch(REGISTRY_URL);
  if (!response.ok) {
    throw new Error(`Could not load registry.json: HTTP ${response.status}`);
  }
  const raw = await response.json();

  cachedStations = filterHttpsAndNormalize(raw);

  return cachedStations;
}
