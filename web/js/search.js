// Simple substring search across name, tags, and country — see web/SCOPE.md.
// This is not a port of the Android app's FTS4 search; it is a plain
// case-insensitive substring match, sufficient for the v1 web player.

export function searchStations(stations, query) {
  const q = query.trim().toLowerCase();
  if (!q) return [];

  return stations.filter((s) => {
    return (
      s.name.toLowerCase().includes(q) ||
      s.tags.toLowerCase().includes(q) ||
      s.country.toLowerCase().includes(q) ||
      s.countryCode.toLowerCase().includes(q)
    );
  });
}
