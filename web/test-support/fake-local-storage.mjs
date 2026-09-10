// Minimal in-memory localStorage stand-in for tests. Node has no global
// localStorage, and the shipped app deliberately has no dependency that
// would provide one (see web/SCOPE.md) — this stub exists only for tests.
export function installFakeLocalStorage() {
  const store = new Map();
  globalThis.localStorage = {
    getItem: (key) => (store.has(key) ? store.get(key) : null),
    setItem: (key, value) => store.set(key, String(value)),
    removeItem: (key) => store.delete(key),
    clear: () => store.clear(),
  };
  return globalThis.localStorage;
}
