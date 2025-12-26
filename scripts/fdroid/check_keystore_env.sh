#!/usr/bin/env bash

set -euo pipefail

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

log() {
  echo "== $*"
}

# Keep in sync with FDROID_PUBLISHING.md
FDROID_DATA="${FDROID_DATA:-/home/arch/fdroid}"

require_any_env() {
  local label="$1"
  shift
  local value=""
  for name in "$@"; do
    value="${!name:-}"
    if [[ -n "$value" ]]; then
      return 0
    fi
  done
  fail "Missing $label (set one of: $*)"
}

require_env() {
  local name="$1"
  [[ -n "${!name:-}" ]] || fail "Missing env: $name"
}

resolve_keystore_file() {
  local candidate=""
  candidate="${FDROID_KEYSTORE_FILE:-${KEY_STORE_FILE:-}}"
  if [[ -n "$candidate" && -f "$candidate" ]]; then
    echo "$candidate"
    return 0
  fi

  # Common local defaults used by this repo.
  for candidate in \
    /tmp/newsoftkeyboard.keystore \
    /tmp/anysoftkeyboard.keystore \
    "${FDROID_DATA}/keystore.jks" \
    "${HOME}/fdroid/keystore.jks"; do
    if [[ -f "$candidate" ]]; then
      echo "$candidate"
      return 0
    fi
  done

  return 1
}

log "Checking F-Droid publish environment (no secrets printed)"

require_any_env "keystore store password" FDROID_KEYSTORE_PASS FDROID_KEY_STORE_PASS KEY_STORE_FILE_PASSWORD
require_any_env "keystore key/alias password" FDROID_KEY_ALIAS_PASS FDROID_KEY_PASS KEY_STORE_FILE_DEFAULT_ALIAS_PASSWORD
require_env FDROID_AWS_BUCKET
require_env FDROID_AWS_ACCESS_KEY_ID
require_env FDROID_AWS_SECRET_KEY

if ! keystore_file="$(resolve_keystore_file)"; then
  fail "Keystore file not found. Set FDROID_KEYSTORE_FILE or KEY_STORE_FILE, or provide one of: /tmp/newsoftkeyboard.keystore, ${FDROID_DATA}/keystore.jks."
fi

log "Keystore file found at $(readlink -f "$keystore_file" || echo "$keystore_file")"
log "Environment OK."

