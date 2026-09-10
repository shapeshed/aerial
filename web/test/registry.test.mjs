import { test } from "node:test";
import assert from "node:assert/strict";
import { filterHttpsAndNormalize, stationKey } from "../js/registry.js";

test("filterHttpsAndNormalize drops http:// entries", () => {
  const raw = [
    { name: "Secure", stream_url: "https://a.example/stream.mp3", provider: "p", provider_id: "1" },
    { name: "Insecure", stream_url: "http://b.example/stream.mp3", provider: "p", provider_id: "2" },
    { name: "Malformed", provider: "p", provider_id: "3" }, // no stream_url at all
  ];

  const result = filterHttpsAndNormalize(raw);

  assert.equal(result.length, 1);
  assert.equal(result[0].name, "Secure");
});

test("filterHttpsAndNormalize maps snake_case registry fields to camelCase", () => {
  const raw = [
    {
      name: "Station",
      stream_url: "https://a.example/s.mp3",
      logo_url: "https://a.example/logo.png",
      country: "Belgium",
      country_code: "BE",
      tags: "jazz chill",
      provider: "radio-browser",
      provider_id: "abc-1",
      description: "desc",
    },
  ];

  const [station] = filterHttpsAndNormalize(raw);

  assert.deepEqual(station, {
    name: "Station",
    streamUrl: "https://a.example/s.mp3",
    logoUrl: "https://a.example/logo.png",
    country: "Belgium",
    countryCode: "BE",
    tags: "jazz chill",
    provider: "radio-browser",
    providerId: "abc-1",
    description: "desc",
  });
});

test("filterHttpsAndNormalize defaults missing optional fields to empty strings", () => {
  const raw = [{ name: "Bare", stream_url: "https://a.example/s.mp3", provider: "p", provider_id: "" }];

  const [station] = filterHttpsAndNormalize(raw);

  assert.equal(station.logoUrl, "");
  assert.equal(station.country, "");
  assert.equal(station.countryCode, "");
  assert.equal(station.tags, "");
  assert.equal(station.description, "");
});

test("stationKey uses provider+providerId when providerId is set", () => {
  const station = { provider: "radio-browser", providerId: "abc-1", name: "Ignored" };
  assert.equal(stationKey(station), "radio-browser::abc-1");
});

test("stationKey falls back to provider+name when providerId is blank (curated bucket)", () => {
  const station = { provider: "curated", providerId: "", name: "ABC Lounge" };
  assert.equal(stationKey(station), "curated::name::ABC Lounge");
});

test("stationKey distinguishes two curated stations with the same name but different providers", () => {
  const a = { provider: "curated", providerId: "", name: "Same Name" };
  const b = { provider: "other", providerId: "", name: "Same Name" };
  assert.notEqual(stationKey(a), stationKey(b));
});
