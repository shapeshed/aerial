#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOCS_DIR="$ROOT_DIR/docs/screenshots"
FASTLANE_DIR="$ROOT_DIR/fastlane/metadata/android/en-US/images/phoneScreenshots"
PACKAGE_NAME="${PACKAGE_NAME:-com.shapeshed.aerial}"

ADB="${ADB:-adb}"

usage() {
  cat <<'USAGE'
Capture repeatable Aerial screenshots from a connected Android device.

The script captures:
  1. Searching for "Mango"
  2. Home list view with Mango playing
  3. Expanded radio view with Mango playing

It captures the set in light mode and then dark mode, updating:
  docs/screenshots/
  fastlane/metadata/android/en-US/images/phoneScreenshots/

Usage:
  scripts/capture-screenshots.sh

Environment:
  ADB=/path/to/adb                 Override adb executable
  PACKAGE_NAME=com.shapeshed.aerial Override app package

Before running:
  - Install a debug or release build on the device.
  - Unlock the device and keep it awake.
  - Be ready to manually navigate to each prompted screen.
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

require_adb() {
  if ! command -v "$ADB" >/dev/null 2>&1; then
    echo "adb not found. Set ADB=/path/to/adb or install Android platform tools." >&2
    exit 1
  fi

  if ! "$ADB" get-state >/dev/null 2>&1; then
    echo "No adb device is available. Connect and unlock a device first." >&2
    exit 1
  fi
}

set_theme() {
  local mode="$1"

  case "$mode" in
    light) "$ADB" shell cmd uimode night no >/dev/null ;;
    dark) "$ADB" shell cmd uimode night yes >/dev/null ;;
    *) echo "Unknown theme mode: $mode" >&2; exit 1 ;;
  esac

  "$ADB" shell am force-stop "$PACKAGE_NAME" >/dev/null || true
  "$ADB" shell monkey -p "$PACKAGE_NAME" 1 >/dev/null 2>&1 || true
  sleep 1
}

capture() {
  local destination="$1"
  local prompt="$2"

  echo
  echo "$prompt"
  read -r -p "Press Enter to capture..."

  mkdir -p "$(dirname "$destination")"
  "$ADB" exec-out screencap -p > "$destination"

  if command -v oxipng >/dev/null 2>&1; then
    oxipng -q -o 4 "$destination"
  elif command -v optipng >/dev/null 2>&1; then
    optipng -quiet "$destination"
  fi

  echo "Wrote $destination"
}

copy_fastlane_screenshots() {
  mkdir -p "$FASTLANE_DIR"
  find "$FASTLANE_DIR" -maxdepth 1 -type f -name '*.png' -delete

  cp "$DOCS_DIR/find-station-light.png" "$FASTLANE_DIR/01-search-mango-light.png"
  cp "$DOCS_DIR/home-playing-light.png" "$FASTLANE_DIR/02-home-playing-mango-light.png"
  cp "$DOCS_DIR/now-playing-light.png" "$FASTLANE_DIR/03-radio-view-mango-light.png"
  cp "$DOCS_DIR/find-station.png" "$FASTLANE_DIR/04-search-mango-dark.png"
  cp "$DOCS_DIR/home-playing.png" "$FASTLANE_DIR/05-home-playing-mango-dark.png"
  cp "$DOCS_DIR/now-playing.png" "$FASTLANE_DIR/06-radio-view-mango-dark.png"
}

capture_theme() {
  local mode="$1"
  local suffix="$2"

  echo
  echo "Switching device to $mode mode..."
  set_theme "$mode"

  capture \
    "$DOCS_DIR/find-station${suffix}.png" \
    "Open Add Station / Find a station and search for \"Mango\"."

  capture \
    "$DOCS_DIR/home-playing${suffix}.png" \
    "Return to the home list view and start playing Mango."

  capture \
    "$DOCS_DIR/now-playing${suffix}.png" \
    "Open the expanded radio view by tapping the mini player while Mango is playing."
}

main() {
  require_adb
  mkdir -p "$DOCS_DIR" "$FASTLANE_DIR"

  capture_theme "light" "-light"
  capture_theme "dark" ""
  copy_fastlane_screenshots

  echo
  echo "Screenshots updated:"
  echo "  $DOCS_DIR"
  echo "  $FASTLANE_DIR"
}

main "$@"
