import { test } from "node:test";
import assert from "node:assert/strict";
import { installFakeLocalStorage } from "../test-support/fake-local-storage.mjs";

installFakeLocalStorage();
// favorites.js reads localStorage at call time (not at import time), so
// installing the fake before this import is enough — the fake must exist
// before any test in this file runs.
const { listFavorites, isFavorite, toggleFavorite, mergeImported } = await import("../js/favorites.js");

const station = {
  name: "Café del Mar",
  streamUrl: "https://streams.radio.co/se1a320b47/listen",
  provider: "radio-browser",
  providerId: "3fd18c3f-8157-11e9-aa30-52543be04c81",
  tags: "ambient lounge",
  description: "",
  country: "Spain",
  countryCode: "ES",
  logoUrl: "https://cafedelmar.com/apple-touch-icon.png",
};

test.beforeEach(() => {
  globalThis.localStorage.clear();
});

test("a station starts out not favorited", () => {
  assert.equal(isFavorite(station), false);
  assert.deepEqual(listFavorites(), []);
});

test("toggleFavorite adds then removes a station", () => {
  const nowFavorited = toggleFavorite(station);
  assert.equal(nowFavorited, true);
  assert.equal(isFavorite(station), true);
  assert.equal(listFavorites().length, 1);

  const nowFavorited2 = toggleFavorite(station);
  assert.equal(nowFavorited2, false);
  assert.equal(isFavorite(station), false);
  assert.equal(listFavorites().length, 0);
});

test("toggleFavorite stores a full station snapshot, not just an id", () => {
  toggleFavorite(station);
  const [saved] = listFavorites();
  assert.equal(saved.name, "Café del Mar");
  assert.equal(saved.streamUrl, station.streamUrl);
  assert.equal(saved.logoPath, station.logoUrl); // logoUrl -> logoPath, see favorites.js
  assert.equal(saved.isFavorite, true);
});

test("mergeImported adds a new station and replaces one with the same identity", () => {
  toggleFavorite(station);

  const updated = {
    name: "Café del Mar (renamed)",
    streamUrl: station.streamUrl,
    provider: station.provider,
    providerId: station.providerId,
    tags: "",
    description: "",
    country: "",
    countryCode: "",
    playCount: 9,
    lastPlayedAt: 0,
    logoPath: "",
  };
  const brandNew = {
    name: "New Station",
    streamUrl: "https://example.com/new.mp3",
    provider: "curated",
    providerId: "",
    tags: "",
    description: "",
    country: "",
    countryCode: "",
    playCount: 0,
    lastPlayedAt: 0,
    logoPath: "",
  };

  mergeImported([updated, brandNew]);

  const favorites = listFavorites();
  assert.equal(favorites.length, 2, "same-identity import replaces rather than duplicates");
  const replaced = favorites.find((f) => f.streamUrl === station.streamUrl);
  assert.equal(replaced.name, "Café del Mar (renamed)");
  assert.equal(replaced.playCount, 9);
  assert.ok(favorites.some((f) => f.name === "New Station"));
});
