#!/usr/bin/env bash
set -euo pipefail

# Generate the bundled offline station registry in the same F-Droid buildserver
# image the release workflow uses.
#
# F-Droid builds the tagged source and compares it byte for byte against the
# reference APK published on the GitHub release. The compressed registry
# database is baked into that APK, so generating it with a different Python or
# SQLite than F-Droid's buildserver can change the bytes and fail the
# reproducible-build check — which is why the release pipeline does not let the
# local `generateRegistryAsset` Gradle task produce it. This script is the one
# place that knows how to generate it correctly; CI and the local release
# scripts both call it rather than duplicating the docker invocation.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Pinned to the digest in .github/workflows/release.yml. Do not change it
# without validating reproducibility; a floating tag can move under us.
FDROID_BUILDSERVER_IMAGE="${FDROID_BUILDSERVER_IMAGE:-registry.gitlab.com/fdroid/fdroidserver:buildserver-trixie@sha256:9cb68105642ca4e7b295f0ceab10f069f5b3247dc18fa7c36046e9d81aa469a8}"

INPUT="app/src/main/registry/registry.json"
OUTPUT="app/build/generated/aerialRegistry/assets/registry.db.compressed"
SCHEMA="app/schemas/com.shapeshed.aerial.data.RegistryDatabase/1.json"

if [[ "${AERIAL_REGISTRY_LOCAL:-0}" == "1" ]]; then
  echo "WARNING: generating the registry with the local python3 (AERIAL_REGISTRY_LOCAL=1)." >&2
  echo "WARNING: this does not match F-Droid's buildserver and may break the reproducible build." >&2
  python3 "${ROOT_DIR}/scripts/generate-registry-db.py" \
    --input "${ROOT_DIR}/${INPUT}" \
    --output "${ROOT_DIR}/${OUTPUT}" \
    --schema "${ROOT_DIR}/${SCHEMA}"
  exit 0
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required to generate the registry database the way F-Droid does." >&2
  echo "Install docker, or set AERIAL_REGISTRY_LOCAL=1 to generate it locally (not reproducible)." >&2
  exit 1
fi

docker run --rm \
  --user "$(id -u):$(id -g)" \
  -v "${ROOT_DIR}:/workspace" \
  -w /workspace \
  "${FDROID_BUILDSERVER_IMAGE}" \
  python3 scripts/generate-registry-db.py \
  --input "${INPUT}" \
  --output "${OUTPUT}" \
  --schema "${SCHEMA}"
