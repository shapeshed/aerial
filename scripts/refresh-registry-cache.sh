#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CACHE_FILE="$ROOT_DIR/app/src/main/assets/registry.json.gz"
REGISTRY_URL="${REGISTRY_URL:-https://aerial.shapeshed.com/registry.json.gz}"

version_name() {
  awk -F\" '/versionName/ { print $2; exit }' "$ROOT_DIR/app/build.gradle"
}

usage() {
  cat <<'USAGE'
Refresh the bundled Aerial station registry cache.

Usage:
  scripts/refresh-registry-cache.sh

Environment:
  REGISTRY_URL=https://aerial.shapeshed.com/registry.json.gz
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

for tool in curl gzip jq; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "$tool is required." >&2
    exit 1
  fi
done

TMP_FILE="$(mktemp)"
TMP_JSON="$(mktemp)"
trap 'rm -f "$TMP_FILE" "$TMP_JSON"' EXIT

USER_AGENT="Aerial/$(version_name) (registry-cache-refresh; +https://github.com/shapeshed/aerial)"

echo "Fetching Aerial registry from $REGISTRY_URL..."
curl -fsSL \
  -A "$USER_AGENT" \
  "$REGISTRY_URL" \
  -o "$TMP_FILE"

gzip -t "$TMP_FILE"
gzip -cd "$TMP_FILE" >"$TMP_JSON"

count="$(jq 'length' "$TMP_JSON")"
if [[ ! "$count" =~ ^[0-9]+$ || "$count" -le 0 ]]; then
  echo "Aerial registry response was empty or invalid." >&2
  exit 1
fi

jq -e '
  type == "array" and
  all(.[]; (.name | type == "string") and (.stream_url | type == "string"))
' "$TMP_JSON" >/dev/null

mkdir -p "$(dirname "$CACHE_FILE")"
cp "$TMP_FILE" "$CACHE_FILE"
echo "Updated $CACHE_FILE with $count stations."
