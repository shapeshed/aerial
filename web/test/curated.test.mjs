import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { curatedGroups } from "../js/curated.js";
import { filterHttpsAndNormalize } from "../js/registry.js";

// --- Unit tests against a small synthetic station list, isolating the
// resolution rules ported from RegistryRepository.kt's resolveMoodStation().

test("curatedGroups resolves a ref by (provider, providerId) when providerId is set", () => {
  const stations = [{ name: "Café del Mar", provider: "radio-browser", providerId: "3fd18c3f-8157-11e9-aa30-52543be04c81" }];
  const groups = curatedGroups(stations);
  const relax = groups.find((g) => g.key === "relax");
  assert.ok(relax, "relax group should resolve at least this one station");
  assert.equal(relax.stations[0].name, "Café del Mar");
});

test("curatedGroups falls back to (name, provider) then name-only when providerId is blank", () => {
  // "ABC Lounge" is a curated-bucket ref (no providerId) in the real data.
  const stations = [{ name: "ABC Lounge", provider: "curated", providerId: "" }];
  const groups = curatedGroups(stations);
  const relax = groups.find((g) => g.key === "relax");
  assert.ok(relax.stations.some((s) => s.name === "ABC Lounge"));
});

test("curatedGroups applies a ref's displayName override without touching the source station", () => {
  const stations = [
    {
      name: "nordic lodge copenhagen",
      provider: "radio-browser",
      providerId: "2ee81587-dba9-4d68-82b3-a7b32aafc525",
    },
  ];
  const groups = curatedGroups(stations);
  const relax = groups.find((g) => g.key === "relax");
  const resolved = relax.stations.find((s) => s.providerId === "2ee81587-dba9-4d68-82b3-a7b32aafc525");
  assert.equal(resolved.name, "Nordic Lodge Copenhagen");
});

test("curatedGroups skips a ref that resolves to nothing, and drops an empty group", () => {
  const groups = curatedGroups([]); // no stations at all resolve
  assert.deepEqual(groups, []);
});

// --- Regression test against the real, checked-in registry, filtered the
// same way the app does. These counts also document, in one place, how many
// of each mood group survive the https-only filter (see web/SCOPE.md) —
// re-run this after any registry.json refresh to see what moved.

test("curatedGroups against the real registry.json matches the known-good https-filtered counts", () => {
  const registryPath = fileURLToPath(new URL("../registry.json", import.meta.url));
  const raw = JSON.parse(readFileSync(registryPath, "utf-8"));
  const stations = filterHttpsAndNormalize(raw);

  const groups = curatedGroups(stations);
  const counts = Object.fromEntries(groups.map((g) => [g.key, g.stations.length]));

  assert.deepEqual(counts, {
    relax: 10,
    focus: 10,
    morning: 8,
    driving: 9,
    late_night: 7,
    workout: 7,
  });
});
