import { test } from "node:test";
import assert from "node:assert/strict";
import zlib from "node:zlib";
import { Buffer } from "node:buffer";
import { installFflate } from "../test-support/install-fflate.mjs";

installFflate();
const { exportBackup, importBackup } = await import("../js/backup.js");

const favorite = {
  name: "Test Station",
  streamUrl: "https://example.com/stream.mp3",
  provider: "radio-browser",
  providerId: "abc-123",
  tags: "jazz chill",
  description: "a test",
  country: "Testland",
  countryCode: "TL",
  playCount: 3,
  lastPlayedAt: 123456,
  logoPath: "https://example.com/logo.png",
};

function fakeFile(buffer) {
  return { arrayBuffer: async () => buffer };
}

test("exportBackup produces a zip containing backup.json with version 1", async () => {
  const blob = exportBackup([favorite]);
  const buf = Buffer.from(await blob.arrayBuffer());

  // Sanity-check it is a real PKZIP file (Android's ZipInputStream, or any
  // other zip tool, must be able to open it) without depending on fflate's
  // own reader for this assertion.
  assert.equal(buf.readUInt32LE(0), 0x04034b50, "local file header signature");
});

test("exportBackup -> importBackup round-trips a favorite exactly", async () => {
  const blob = exportBackup([favorite]);
  const buf = Buffer.from(await blob.arrayBuffer());

  const [imported] = await importBackup(fakeFile(buf));

  assert.equal(imported.name, favorite.name);
  assert.equal(imported.streamUrl, favorite.streamUrl);
  assert.equal(imported.provider, favorite.provider);
  assert.equal(imported.providerId, favorite.providerId);
  assert.equal(imported.tags, favorite.tags);
  assert.equal(imported.country, favorite.country);
  assert.equal(imported.countryCode, favorite.countryCode);
  assert.equal(imported.playCount, favorite.playCount);
  assert.equal(imported.lastPlayedAt, favorite.lastPlayedAt);
  assert.equal(imported.logoPath, favorite.logoPath);
  assert.equal(imported.isFavorite, true);
});

test("importBackup reads an Android-shaped zip and skips its logos/ entry", async () => {
  const manifest = {
    version: 1,
    app: "Aerial",
    settings: { showStreamBitrate: true, showHome: true },
    stations: [
      {
        name: "Android Fave",
        streamUrl: "https://stream.example/a.mp3",
        isFavorite: true,
        provider: "radio-browser",
        providerId: "xyz",
        tags: "rock",
        description: "",
        country: "US",
        countryCode: "US",
        playCount: 5,
        lastPlayedAt: 999,
        logoFile: "logos/0_logo.png", // a bundled image entry, not a URL
      },
      {
        name: "Android Fave 2",
        streamUrl: "https://stream.example/b.mp3",
        isFavorite: true,
        provider: "curated",
        providerId: "",
        tags: "chill",
        description: "",
        country: "",
        countryCode: "",
        playCount: 0,
        lastPlayedAt: 0,
        logoPath: "https://stream.example/b-logo.png",
      },
    ],
  };
  const zip = buildAndroidStyleZip(manifest, { "logos/0_logo.png": Buffer.from("fake-png-bytes") });

  const imported = await importBackup(fakeFile(zip));

  assert.equal(imported.length, 2);
  assert.equal(imported[0].name, "Android Fave");
  assert.equal(imported[0].logoPath, "", "a bundled logoFile has no web equivalent, so it is dropped");
  assert.equal(imported[1].logoPath, "https://stream.example/b-logo.png", "a URL logoPath passes through");
});

test("importBackup rejects an unsupported backup version", async () => {
  const zip = buildAndroidStyleZip({ version: 2, app: "Aerial", stations: [] }, {});
  await assert.rejects(() => importBackup(fakeFile(zip)), /version/i);
});

test("importBackup rejects a zip with no backup.json", async () => {
  const zip = buildAndroidStyleZip(null, { "logos/only.png": Buffer.from("x") });
  await assert.rejects(() => importBackup(fakeFile(zip)), /manifest/i);
});

// Builds a plain PKZIP (STORE method) file by hand, independent of fflate,
// so these tests do not just check fflate against itself.
function buildAndroidStyleZip(manifest, extraFiles) {
  const files = { ...extraFiles };
  if (manifest !== null) {
    files["backup.json"] = Buffer.from(JSON.stringify(manifest, null, 2), "utf-8");
  }

  const localParts = [];
  const centralParts = [];
  let offset = 0;

  for (const [name, data] of Object.entries(files)) {
    const nameBuf = Buffer.from(name, "utf-8");
    const crc = crc32(data);

    const local = Buffer.alloc(30);
    local.writeUInt32LE(0x04034b50, 0);
    local.writeUInt16LE(20, 4); // version needed
    local.writeUInt16LE(0, 6); // flags
    local.writeUInt16LE(0, 8); // method: store
    local.writeUInt16LE(0, 10); // mod time
    local.writeUInt16LE(0, 12); // mod date
    local.writeUInt32LE(crc, 14);
    local.writeUInt32LE(data.length, 18); // compressed size
    local.writeUInt32LE(data.length, 22); // uncompressed size
    local.writeUInt16LE(nameBuf.length, 26);
    local.writeUInt16LE(0, 28); // extra length
    localParts.push(local, nameBuf, data);

    const central = Buffer.alloc(46);
    central.writeUInt32LE(0x02014b50, 0);
    central.writeUInt16LE(20, 4); // version made by
    central.writeUInt16LE(20, 6); // version needed
    central.writeUInt16LE(0, 8); // flags
    central.writeUInt16LE(0, 10); // method
    central.writeUInt16LE(0, 12); // mod time
    central.writeUInt16LE(0, 14); // mod date
    central.writeUInt32LE(crc, 16);
    central.writeUInt32LE(data.length, 20);
    central.writeUInt32LE(data.length, 24);
    central.writeUInt16LE(nameBuf.length, 28);
    central.writeUInt16LE(0, 30); // extra length
    central.writeUInt16LE(0, 32); // comment length
    central.writeUInt16LE(0, 34); // disk number
    central.writeUInt16LE(0, 36); // internal attrs
    central.writeUInt32LE(0, 38); // external attrs
    central.writeUInt32LE(offset, 42);
    centralParts.push(central, nameBuf);

    offset += local.length + nameBuf.length + data.length;
  }

  const centralStart = offset;
  const centralBuf = Buffer.concat(centralParts);

  const end = Buffer.alloc(22);
  end.writeUInt32LE(0x06054b50, 0);
  end.writeUInt16LE(0, 4);
  end.writeUInt16LE(0, 6);
  end.writeUInt16LE(Object.keys(files).length, 8);
  end.writeUInt16LE(Object.keys(files).length, 10);
  end.writeUInt32LE(centralBuf.length, 12);
  end.writeUInt32LE(centralStart, 16);
  end.writeUInt16LE(0, 20);

  return new Uint8Array(Buffer.concat([...localParts, centralBuf, end]));
}

function crc32(buf) {
  // zlib doesn't expose crc32 directly pre-Node 22; deflate/inflate round
  // trip through gzip's own crc via zlib.crc32 (Node 22+) with a manual
  // fallback for older runtimes.
  if (typeof zlib.crc32 === "function") return zlib.crc32(buf) >>> 0;
  let crc = ~0;
  for (const byte of buf) {
    crc ^= byte;
    for (let i = 0; i < 8; i++) {
      crc = (crc >>> 1) ^ (0xedb88320 & -(crc & 1));
    }
  }
  return (~crc) >>> 0;
}
