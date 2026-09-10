// Resolves the curated mood groups (web/js/curated-data.js) against the
// loaded station list. Ported from RegistryRepository.kt's
// curatedMoodStations()/resolveMoodStation() so the web browse screen shows
// the same groups, in the same order, as the Android app's home screen.

import { MOOD_GROUP_ORDER, MOOD_LABELS, CURATED_MOOD_STATIONS } from "./curated-data.js";

function resolveMoodStation(target, stations) {
  let match = null;

  if (target.providerId) {
    match = stations.find(
      (s) => s.provider === target.provider && s.providerId === target.providerId,
    );
  } else if (target.provider) {
    match =
      stations.find((s) => s.name === target.name && s.provider === target.provider) ||
      stations.find((s) => s.name === target.name && s.provider === "curated") ||
      stations.find((s) => s.name === target.name);
  } else {
    match = stations.find((s) => s.name === target.name);
  }

  if (!match) return null;
  if (target.displayName) {
    return { ...match, name: target.displayName };
  }
  return match;
}

/**
 * Returns an ordered array of { key, title, desc, stations } groups, each
 * containing the resolved stations that exist in the (https-filtered)
 * registry. A ref that no longer resolves (e.g. dropped from the registry
 * snapshot) is skipped, same as the Android app.
 */
export function curatedGroups(stations) {
  return MOOD_GROUP_ORDER.map((key) => {
    const refs = CURATED_MOOD_STATIONS[key] || [];
    const resolved = refs
      .map((ref) => resolveMoodStation(ref, stations))
      .filter((s) => s !== null);
    return {
      key,
      title: MOOD_LABELS[key].title,
      desc: MOOD_LABELS[key].desc,
      stations: resolved,
    };
  }).filter((group) => group.stations.length > 0);
}
