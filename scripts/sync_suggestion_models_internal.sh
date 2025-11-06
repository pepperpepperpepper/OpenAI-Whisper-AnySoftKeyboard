#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SUGGESTIONS_ROOT="${SUGGESTIONS_ROOT:-$HOME/suggestions}"

SRC_KENLM="${SUGGESTIONS_ROOT}/models/kenlm"
DST_KENLM="${REPO_ROOT}/ime/app/src/main/assets/models/kenlm"

copy_dir() {
  local src="$1"
  local dst="$2"
  local name="$3"
  if [[ ! -d "${src}" ]]; then
    echo "[warn] ${name} source ${src} missing; skip"
    return
  fi
  mkdir -p "${dst}"
  shopt -s nullglob
  for file in "${src}"/*; do
    base="$(basename "${file}")"
    if [[ "${base}" == ".gitignore" ]]; then
      continue
    fi
    if [[ "${name}" == "KenLM" && "${base}" == *.arpa ]]; then
      echo "[skip] ${name} ${base} (not packaged)"
      continue
    fi
    echo "[copy] ${name} ${base}"
    rsync -a --delete --prune-empty-dirs "${file}" "${dst}/"
  done
}

copy_dir "${SRC_KENLM}" "${DST_KENLM}" "KenLM"

if [[ -d "${SUGGESTIONS_ROOT}/models/distilgpt2" ]]; then
  echo "[skip] DistilGPT-2 assets present but not bundled; neural path not wired yet."
else
  echo "[skip] DistilGPT-2 assets not found (expected while neural path disabled)."
fi

echo "[done] suggestion models synced into assets (untracked)"
