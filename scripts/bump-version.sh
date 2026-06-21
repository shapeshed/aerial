#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_FILE="$ROOT_DIR/app/build.gradle"
CHANGELOG_FILE="$ROOT_DIR/CHANGELOG.md"
FASTLANE_CHANGELOG_DIR="$ROOT_DIR/fastlane/metadata/android/en-US/changelogs"

usage() {
  cat <<'USAGE'
Bump the Aerial release version.

Usage:
  scripts/bump-version.sh <versionName> [versionCode]

Examples:
  scripts/bump-version.sh 0.1.2
  scripts/bump-version.sh 0.1.2 3

When versionCode is omitted, the current Gradle versionCode is incremented by 1.

Updates:
  - app/build.gradle
  - fastlane/metadata/android/en-US/changelogs/<versionCode>.txt

The script prints the CHANGELOG.md section to add, but does not move release
notes automatically.
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

VERSION_NAME="${1:-}"
if [[ -z "$VERSION_NAME" ]]; then
  usage >&2
  exit 1
fi

if [[ ! "$VERSION_NAME" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$ ]]; then
  echo "Invalid versionName: $VERSION_NAME" >&2
  echo "Expected semver-like version, for example 0.1.2" >&2
  exit 1
fi

current_code() {
  awk '/versionCode/ { print $2; exit }' "$BUILD_FILE"
}

current_name() {
  awk -F\" '/versionName/ { print $2; exit }' "$BUILD_FILE"
}

CURRENT_CODE="$(current_code)"
CURRENT_NAME="$(current_name)"
VERSION_CODE="${2:-$((CURRENT_CODE + 1))}"

if [[ ! "$VERSION_CODE" =~ ^[0-9]+$ ]]; then
  echo "Invalid versionCode: $VERSION_CODE" >&2
  exit 1
fi

if (( VERSION_CODE <= CURRENT_CODE )); then
  echo "versionCode must increase: current=$CURRENT_CODE new=$VERSION_CODE" >&2
  exit 1
fi

if grep -q "versionName \"$VERSION_NAME\"" "$BUILD_FILE"; then
  echo "versionName is already $VERSION_NAME" >&2
  exit 1
fi

perl -0pi -e "s/versionCode\\s+\\d+/versionCode $VERSION_CODE/; s/versionName\\s+\"[^\"]+\"/versionName \"$VERSION_NAME\"/;" "$BUILD_FILE"

mkdir -p "$FASTLANE_CHANGELOG_DIR"
FASTLANE_CHANGELOG="$FASTLANE_CHANGELOG_DIR/${VERSION_CODE}.txt"
if [[ ! -f "$FASTLANE_CHANGELOG" ]]; then
  cat > "$FASTLANE_CHANGELOG" <<EOF
- Update release notes before publishing.
EOF
fi

today="$(date +%F)"

echo "Bumped Aerial from $CURRENT_NAME ($CURRENT_CODE) to $VERSION_NAME ($VERSION_CODE)."
echo
echo "Created or kept:"
echo "  $FASTLANE_CHANGELOG"
echo
echo "Add this section to CHANGELOG.md when cutting the release:"
echo
echo "## [$VERSION_NAME] - $today"
echo
echo "Then move the relevant entries from [Unreleased] into that section."
