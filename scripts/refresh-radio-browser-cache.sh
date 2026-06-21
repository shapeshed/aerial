#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CACHE_FILE="$ROOT_DIR/app/src/main/res/raw/fallback_stations.json"
RADIO_BROWSER_SERVER="${RADIO_BROWSER_SERVER:-de1.api.radio-browser.info}"

version_name() {
  awk -F\" '/versionName/ { print $2; exit }' "$ROOT_DIR/app/build.gradle"
}

cache_limit() {
  if [[ -n "${RADIO_BROWSER_CACHE_LIMIT:-}" ]]; then
    printf '%s\n' "$RADIO_BROWSER_CACHE_LIMIT"
    return
  fi

  if [[ -f "$CACHE_FILE" ]]; then
    jq 'length' "$CACHE_FILE"
    return
  fi

  printf '10000\n'
}

usage() {
  cat <<'USAGE'
Refresh the bundled Radio Browser offline station cache.

Usage:
  scripts/refresh-radio-browser-cache.sh

Environment:
  RADIO_BROWSER_CACHE_LIMIT=10000       Override the existing cache size
  RADIO_BROWSER_SERVER=de1.api.radio-browser.info
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

for tool in curl jq; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "$tool is required." >&2
    exit 1
  fi
done

LIMIT="$(cache_limit)"
USER_AGENT="Aerial/$(version_name) (release-cache-refresh; +https://github.com/shapeshed/aerial)"
TMP_FILE="$(mktemp)"
trap 'rm -f "$TMP_FILE"' EXIT

url="https://${RADIO_BROWSER_SERVER}/json/stations/search?limit=${LIMIT}&order=votes&reverse=true&hidebroken=true"
echo "Fetching $LIMIT stations from $RADIO_BROWSER_SERVER..."
curl -fsSL --compressed \
  -A "$USER_AGENT" \
  "$url" \
  -o "$TMP_FILE"

count="$(jq 'length' "$TMP_FILE")"
if [[ ! "$count" =~ ^[0-9]+$ || "$count" -le 0 ]]; then
  echo "Radio Browser response was empty or invalid." >&2
  exit 1
fi

jq -c 'map({
  stationuuid,
  name,
  url,
  url_resolved,
  favicon,
  tags,
  country,
  countrycode,
  votes,
  codec,
  bitrate,
  clickcount
})' "$TMP_FILE" > "$CACHE_FILE"
echo "Updated $CACHE_FILE with $count stations."
