#!/usr/bin/env bash
# One-stop F-Droid publish script for NewSoftKeyboard.
# Flow:
#  1) source env, guard required vars
#  2) sync from S3 -> staging (repo/archive)
#  3) bump version (unless SKIP_BUMP=1)
#  4) build signed release APK
#  5) stage APK into repo/, regenerate metadata from inventory
#  6) fdroid update --create-metadata
#  7) validate counts/current version
#  8) backup, sync to S3, invalidate CDN
#  9) optional git commit (unless SKIP_COMMIT=1)

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
FDROID_DATA="${FDROID_DATA:-/home/arch/fdroid}"
cd "$ROOT"

ENV_FILE="${ENV_FILE:-$FDROID_DATA/.env}"
KEEPVERSIONS="${KEEPVERSIONS:-7}"
EXPECTED_MIN_APKS="${EXPECTED_MIN_APKS:-10}"
AWS_CF_ID="${FDROID_AWS_CF_DISTRIBUTION_ID:-E2RWHYJEODFGYE}"
BACKUP_DIR="${ROOT}/backups"
TIMESTAMP="$(date -u +%Y%m%d-%H%M%S)"

fail() { echo "ERROR: $*" >&2; exit 1; }

log() { echo "== $*"; }

require_cmd() { command -v "$1" >/dev/null || fail "Missing command: $1"; }

require_cmd aws
require_cmd aapt
require_cmd /home/arch/fdroid-env/bin/fdroid

[ -f "$ENV_FILE" ] || fail "Env file not found: $ENV_FILE"
set -a && source "$ENV_FILE" && set +a

scripts/fdroid/check_keystore_env.sh

log "Syncing from S3 -> local staging"
aws s3 sync "s3://${FDROID_AWS_BUCKET}/repo" "${FDROID_DATA}/repo"
aws s3 sync "s3://${FDROID_AWS_BUCKET}/archive" "${FDROID_DATA}/archive"

log "Enforcing keepversions=${KEEPVERSIONS} in config.yml"
python - "$KEEPVERSIONS" "$FDROID_DATA/config.yml" <<'PY'
import sys, re
keep=sys.argv[1]
path=sys.argv[2]
text=open(path).read()
if re.search(r'archive_older:\s*\d+', text):
    text=re.sub(r'archive_older:\s*\d+', f'archive_older: {keep}', text)
else:
    text = text.rstrip()+"\narchive_older: "+keep+"\n"
open(path,"w").write(text)
PY

if [[ "${SKIP_BUMP:-0}" != "1" ]]; then
  log "Bumping versionCode/versionName in ime/app/build.gradle"
  python - <<'PY'
from pathlib import Path
import re
gradle = Path("ime/app/build.gradle")
text = gradle.read_text()
vc_match = re.search(r"versionCode\s+(\d+)", text)
vn_match = re.search(r'versionName\s+"([^"]+)"', text)
if not vc_match or not vn_match:
    raise SystemExit("version fields not found")
vc = int(vc_match.group(1)) + 1
vn_parts = vn_match.group(1).split(".")
vn_parts[-1] = str(int(vn_parts[-1]) + 1)
vn = ".".join(vn_parts)
text = re.sub(r"versionCode\s+\d+", f"versionCode {vc}", text, count=1)
text = re.sub(r'versionName\s+"[^"]+"', f'versionName "{vn}"', text, count=1)
gradle.write_text(text)
print(f"Bumped to versionCode={vc}, versionName={vn}")
PY
fi

log "Building signed release APK"
GRADLE_USER_HOME=${GRADLE_USER_HOME:-/mnt/finished/.gradle} \
KEY_STORE_FILE_PASSWORD=${KEY_STORE_FILE_PASSWORD:-$FDROID_KEYSTORE_PASS} \
KEY_STORE_FILE_DEFAULT_ALIAS_PASSWORD=${KEY_STORE_FILE_DEFAULT_ALIAS_PASSWORD:-$FDROID_KEY_PASS} \
./gradlew :ime:app:assembleRelease -x lint

apk_path="ime/app/build/outputs/apk/release/app-release.apk"
[ -f "$apk_path" ] || fail "APK not found at $apk_path"

log "Copying APK into repo/"
cp "$apk_path" "${FDROID_DATA}/repo/wtf.uhoh.newsoftkeyboard_$(date +%s).apk"

log "Regenerating metadata from inventory"
CURRENT_VERSION_CODE=
(cd "$FDROID_DATA" && "$ROOT/scripts/fdroid/generate_metadata.py")

log "Running fdroid update"
(cd "$FDROID_DATA" && /home/arch/fdroid-env/bin/fdroid update --create-metadata --verbose)

log "Validating APK count >= $EXPECTED_MIN_APKS"
count=$(find -L "${FDROID_DATA}/repo" "${FDROID_DATA}/archive" -name "wtf.uhoh.newsoftkeyboard_*.apk" | wc -l)
[[ $count -ge $EXPECTED_MIN_APKS ]] || fail "Only $count APKs found"

log "Backup repo+archive+metadata to $BACKUP_DIR"
mkdir -p "$BACKUP_DIR"
tar czf "$BACKUP_DIR/fdroid-${TIMESTAMP}.tar.gz" -C "$FDROID_DATA" repo archive metadata config.yml

log "Syncing back to S3"
aws s3 sync "${FDROID_DATA}/repo" "s3://${FDROID_AWS_BUCKET}/repo"
aws s3 sync "${FDROID_DATA}/archive" "s3://${FDROID_AWS_BUCKET}/archive"
aws s3 sync "${FDROID_DATA}/metadata" "s3://${FDROID_AWS_BUCKET}/metadata"
aws s3 cp "${FDROID_DATA}/config.yml" "s3://${FDROID_AWS_BUCKET}/config.yml"

log "Invalidating CloudFront ($AWS_CF_ID)"
aws cloudfront create-invalidation --distribution-id "$AWS_CF_ID" --paths "/repo/*" "/archive/*" "/metadata/*" "/index.*" "/status/*" "/diff/*" || true

if [[ "${SKIP_COMMIT:-0}" != "1" ]]; then
  log "Creating git commit"
  git add fdroid/scripts fdroid/.env.example || true
  git add ime/app/build.gradle
  git commit -m "fdroid publish"
fi

log "Publish complete."
