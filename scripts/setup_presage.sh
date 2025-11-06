#!/usr/bin/env bash
#
# Fetches the Presage predictive text library sources and stages them under
# third_party/presage for Android builds.

set -euo pipefail

VERSION="${PRESAGE_VERSION:-0.9.1}"
ARCHIVE_NAME="presage-${VERSION}.tar.gz"
DOWNLOAD_URL="https://downloads.sourceforge.net/project/presage/presage/${VERSION}/${ARCHIVE_NAME}"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
THIRD_PARTY_DIR="${REPO_ROOT}/third_party/presage"
STAGING_DIR="${THIRD_PARTY_DIR}/presage-${VERSION}"
TMP_ROOT="${REPO_ROOT}/tmp/presage"

mkdir -p "${THIRD_PARTY_DIR}" "${TMP_ROOT}"

ARCHIVE_PATH="${TMP_ROOT}/${ARCHIVE_NAME}"

if [[ ! -f "${ARCHIVE_PATH}" ]]; then
  echo "[fetch] ${DOWNLOAD_URL}"
  curl -L --fail --retry 3 --retry-delay 2 -o "${ARCHIVE_PATH}.partial" "${DOWNLOAD_URL}"
  mv "${ARCHIVE_PATH}.partial" "${ARCHIVE_PATH}"
else
  echo "[skip] archive already present at ${ARCHIVE_PATH}"
fi

EXTRACT_DIR="${TMP_ROOT}/src-${VERSION}"
if [[ -d "${EXTRACT_DIR}" ]]; then
  echo "[skip] ${EXTRACT_DIR} already extracted"
else
  echo "[extract] ${ARCHIVE_PATH}"
  mkdir -p "${EXTRACT_DIR}"
  tar -xzf "${ARCHIVE_PATH}" -C "${EXTRACT_DIR}" --strip-components=1
fi

if [[ -d "${STAGING_DIR}" ]]; then
  echo "[clean] removing previous staged sources at ${STAGING_DIR}"
  rm -rf "${STAGING_DIR}"
fi

echo "[stage] copying sources into ${STAGING_DIR}"
cp -a "${EXTRACT_DIR}" "${STAGING_DIR}"

echo "[done] Presage ${VERSION} sources staged."
