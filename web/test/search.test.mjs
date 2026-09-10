import { test } from "node:test";
import assert from "node:assert/strict";
import { searchStations } from "../js/search.js";

const stations = [
  { name: "FIP", tags: "jazz eclectic", country: "France", countryCode: "FR" },
  { name: "KEXP", tags: "indie rock", country: "The United States Of America", countryCode: "US" },
  { name: "Radio Swiss Jazz", tags: "jazz", country: "Switzerland", countryCode: "CH" },
];

test("searchStations returns nothing for a blank query", () => {
  assert.deepEqual(searchStations(stations, ""), []);
  assert.deepEqual(searchStations(stations, "   "), []);
});

test("searchStations matches by name, case-insensitively", () => {
  const results = searchStations(stations, "fip");
  assert.equal(results.length, 1);
  assert.equal(results[0].name, "FIP");
});

test("searchStations matches by tag substring", () => {
  const results = searchStations(stations, "jazz");
  assert.deepEqual(
    results.map((s) => s.name).sort(),
    ["FIP", "Radio Swiss Jazz"],
  );
});

test("searchStations matches by country name or country code", () => {
  assert.equal(searchStations(stations, "switzerland").length, 1);
  assert.equal(searchStations(stations, "ch").length, 1);
});

test("searchStations returns an empty array, not null, when nothing matches", () => {
  assert.deepEqual(searchStations(stations, "nonexistent-xyz"), []);
});
