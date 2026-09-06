#!/usr/bin/env bash
set -euo pipefail

device_count="$(adb devices | awk 'NR > 1 && $2 == "device" { count++ } END { print count + 0 }')"
if [[ "$device_count" != "1" ]]; then
  echo "Expected exactly one ready Android device; found $device_count." >&2
  echo "Connect one device and keep it awake before running device tests." >&2
  exit 1
fi

policy="$(adb shell dumpsys window)"
if ! grep -Eq 'mShowingLockscreen=false|isKeyguardShowing=false' <<<"$policy"; then
  echo "The connected device is locked. Unlock the secure keyguard, then retry." >&2
  exit 1
fi

echo "Android device is connected and unlocked."
