#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FDROIDDATA_DIR="${FDROIDDATA_DIR:-/home/go/src/gitlab.com/fdroid/fdroiddata}"
APP_ID="com.shapeshed.aerial"

usage() {
  cat <<'USAGE'
Prepare a local Aerial release candidate.

Usage:
  scripts/prepare-release.sh [--draft] [--skip-fdroid]

Runs:
  - Gradle test/lint/release builds
  - Fastlane changelog/version checks
  - F-Droid metadata sync and validation

Environment:
  FDROIDDATA_DIR=/home/go/src/gitlab.com/fdroid/fdroiddata
USAGE
}

draft=false
skip_fdroid=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --draft) draft=true ;;
    --skip-fdroid) skip_fdroid=true ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
  shift
done

version_name() {
  awk -F\" '/versionName/ { print $2; exit }' "$ROOT_DIR/app/build.gradle"
}

version_code() {
  awk '/versionCode/ { print $2; exit }' "$ROOT_DIR/app/build.gradle"
}

release_notes() {
  local version="$1"
  awk -v version="$version" '
    $0 == "## [" version "]" || $0 ~ "^## \\[" version "\\] - " { in_section = 1; next }
    in_section && /^## \[/ { exit }
    in_section { print }
  ' "$ROOT_DIR/CHANGELOG.md" | sed '/^[[:space:]]*$/d'
}

VERSION_NAME="$(version_name)"
VERSION_CODE="$(version_code)"
FASTLANE_CHANGELOG="$ROOT_DIR/fastlane/metadata/android/en-US/changelogs/${VERSION_CODE}.txt"
NOTES_FILE="$(mktemp)"
trap 'rm -f "$NOTES_FILE"' EXIT

echo "Preparing Aerial $VERSION_NAME (versionCode $VERSION_CODE)..."

if [[ ! -f "$FASTLANE_CHANGELOG" ]]; then
  echo "Missing Fastlane changelog: $FASTLANE_CHANGELOG" >&2
  exit 1
fi

if ! release_notes "$VERSION_NAME" >"$NOTES_FILE"; then
  echo "Could not extract CHANGELOG.md release notes for $VERSION_NAME." >&2
  exit 1
fi

if [[ ! -s "$NOTES_FILE" ]]; then
  if [[ "$draft" == true ]]; then
    echo "Draft mode: CHANGELOG.md does not yet contain a section for $VERSION_NAME."
    echo "Move relevant entries from [Unreleased] before final release."
  else
    echo "CHANGELOG.md does not contain a section for $VERSION_NAME." >&2
    echo "Move relevant entries from [Unreleased] before final release, or run with --draft." >&2
    exit 1
  fi
fi

echo "Running Gradle release gate..."
"$ROOT_DIR/gradlew" -p "$ROOT_DIR" test lint assembleRelease bundleRelease

if [[ "$skip_fdroid" == false ]]; then
  if [[ ! -d "$FDROIDDATA_DIR" ]]; then
    echo "F-Droid data checkout not found: $FDROIDDATA_DIR" >&2
    exit 1
  fi
  if ! command -v fdroid >/dev/null 2>&1; then
    echo "fdroid is required for release preparation." >&2
    exit 1
  fi

  echo "Syncing and validating F-Droid metadata..."
  cp "$ROOT_DIR/.fdroid.yml" "$FDROIDDATA_DIR/metadata/${APP_ID}.yml"
  (
    cd "$FDROIDDATA_DIR"
    fdroid rewritemeta "$APP_ID"
    fdroid lint "$APP_ID"
  )
else
  echo "Skipping F-Droid validation."
fi

echo
if [[ -s "$NOTES_FILE" ]]; then
  echo "Release notes for GitHub:"
  cat "$NOTES_FILE"
else
  echo "Release notes for GitHub: not available in draft mode."
fi

echo
echo "Next manual steps:"
echo "  git status --short"
echo "  git add app/build.gradle CHANGELOG.md .fdroid.yml fastlane/metadata/android/en-US/changelogs/${VERSION_CODE}.txt app/src/main/registry/registry.json"
echo "  git add docs/screenshots fastlane/metadata/android/en-US/images/phoneScreenshots"
echo "  git commit -S -m \"chore(release): v${VERSION_NAME}\""
echo
echo "This script does not tag, push, or upload to Google Play."
