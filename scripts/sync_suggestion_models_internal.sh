#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SUGGESTIONS_ROOT="${SUGGESTIONS_ROOT:-$HOME/suggestions}"

SRC_KENLM="${SUGGESTIONS_ROOT}/models/kenlm"
SRC_DISTIL="${SUGGESTIONS_ROOT}/models/distilgpt2"

DST_KENLM="${REPO_ROOT}/ime/app/src/main/assets/models/kenlm"
DST_DISTIL="${REPO_ROOT}/ime/app/src/main/assets/models/distilgpt2"

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
copy_dir "${SRC_DISTIL}" "${DST_DISTIL}" "DistilGPT-2"

echo "[done] suggestion models synced into assets (untracked)"
