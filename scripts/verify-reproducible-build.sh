#!/usr/bin/env bash
set -euo pipefail

# Verify that the release APK is byte-for-byte reproducible.
#
# F-Droid rebuilds the tagged source and compares the result against the APK on
# the GitHub release. This script does the same build twice, from a clean tree,
# with the registry generated in F-Droid's buildserver image and the Gradle
# build cache disabled, then compares the two APK hashes. A mismatch means the
# build depends on build order, a timestamp, or local tooling, and F-Droid will
# reject the release.
#
# It is a local proxy: it cannot prove the artifact matches F-Droid's build
# environment, only that this repository builds deterministically.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK_DIR="${ROOT_DIR}/app/build/outputs/apk/release"

find_apk() {
  if [[ -f "${APK_DIR}/app-release.apk" ]]; then
    echo "${APK_DIR}/app-release.apk"
  elif [[ -f "${APK_DIR}/app-release-unsigned.apk" ]]; then
    echo "${APK_DIR}/app-release-unsigned.apk"
  fi
}

build_release() {
  # Clean first: the registry asset lives under app/build, so generating it
  # before clean would delete it. Then generate it the F-Droid way and build
  # without letting the local generateRegistryAsset task overwrite it.
  "${ROOT_DIR}/gradlew" -p "${ROOT_DIR}" clean
  "${ROOT_DIR}/scripts/generate-registry-db-container.sh"
  "${ROOT_DIR}/gradlew" -p "${ROOT_DIR}" assembleRelease \
    -x generateRegistryAsset --no-build-cache --rerun-tasks
}

hash_build() {
  local apk hash
  apk="$(find_apk)"
  if [[ -z "${apk}" ]]; then
    echo "No release APK produced by the build." >&2
    exit 1
  fi
  hash="$(sha256sum "${apk}" | awk '{print $1}')"
  printf '%s' "${hash}"
}

echo "== Reproducible build check: first build =="
build_release
first="$(hash_build)"

echo "== Reproducible build check: second build =="
build_release
second="$(hash_build)"

if [[ "${first}" == "${second}" ]]; then
  echo "Release APK is reproducible (sha256 ${first})."
  exit 0
fi

echo "Release APK is NOT reproducible." >&2
echo "  first pass:  ${first}" >&2
echo "  second pass: ${second}" >&2
echo "Inspect the two APKs with diffoscope to find the differing entries." >&2
exit 1
