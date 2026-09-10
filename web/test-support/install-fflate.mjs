// backup.js expects fflate as a global (loaded via a classic <script> tag
// in index.html — see web/js/backup.js). Load the same vendored UMD build
// as a CommonJS module here so tests see the same global.
import { createRequire } from "node:module";

export function installFflate() {
  const require = createRequire(import.meta.url);
  globalThis.fflate = require("../vendor/fflate.js");
  return globalThis.fflate;
}
