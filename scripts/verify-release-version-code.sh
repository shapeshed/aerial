#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_FILE="$ROOT_DIR/app/build.gradle"
APK_FILE="${1:-}"

if [[ -z "$APK_FILE" ]]; then
  if [[ -f "$ROOT_DIR/app/build/outputs/apk/release/app-release.apk" ]]; then
    APK_FILE="$ROOT_DIR/app/build/outputs/apk/release/app-release.apk"
  else
    APK_FILE="$ROOT_DIR/app/build/outputs/apk/release/app-release-unsigned.apk"
  fi
fi

if [[ ! -f "$APK_FILE" ]]; then
  echo "Release APK not found: $APK_FILE" >&2
  exit 1
fi

expected_version_code="$(awk '$1 == "versionCode" { print $2; exit }' "$BUILD_FILE")"
if [[ ! "$expected_version_code" =~ ^[0-9]+$ ]]; then
  echo "Could not read versionCode from $BUILD_FILE" >&2
  exit 1
fi

sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ ! -d "$sdk_root/build-tools" && -f "$ROOT_DIR/local.properties" ]]; then
  sdk_root="$(awk -F= '$1 == "sdk.dir" { print $2; exit }' "$ROOT_DIR/local.properties")"
fi

aapt_path="${AAPT:-}"
if [[ -z "$aapt_path" ]] && command -v aapt >/dev/null 2>&1; then
  aapt_path="$(command -v aapt)"
fi
if [[ -z "$aapt_path" && -n "$sdk_root" && -d "$sdk_root/build-tools" ]]; then
  latest_build_tools="$(find "$sdk_root/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1)"
  aapt_path="$latest_build_tools/aapt"
fi
if [[ -z "$aapt_path" || ! -x "$aapt_path" ]]; then
  echo "Could not find aapt; set AAPT or configure ANDROID_SDK_ROOT." >&2
  exit 1
fi

actual_version_code="$("$aapt_path" dump badging "$APK_FILE" | sed -n "s/^package: .*versionCode='\([0-9][0-9]*\)'.*/\1/p")"
if [[ ! "$actual_version_code" =~ ^[0-9]+$ ]]; then
  echo "Could not read versionCode from $APK_FILE" >&2
  exit 1
fi

if [[ "$actual_version_code" != "$expected_version_code" ]]; then
  echo "Release versionCode mismatch: source=$expected_version_code APK=$actual_version_code" >&2
  exit 1
fi

echo "Release versionCode verified: $actual_version_code"
