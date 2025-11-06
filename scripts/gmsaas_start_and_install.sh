#!/usr/bin/env bash
set -euo pipefail

# Starts a Genymotion SaaS device, connects ADB, installs New Soft Keyboard and the
# Mike Rozoff add-on, and enables the IME.

RECIPE_UUID="${RECIPE_UUID:-9074ccc1-7aba-4c9b-b615-e69ef389738c}" # Android 14.0 - Genymotion Phone
INSTANCE_NAME="${INSTANCE_NAME:-nsk-android14}"
ASK_APK="${ASK_APK:-$(pwd)/ime/app/build/outputs/apk/debug/app-debug.apk}"
ROZOFF_APK="${ROZOFF_APK:-$HOME/mike-rozoff-anysoftkeyboard-addon/build/outputs/apk/debug/app-debug.apk}"

if ! command -v gmsaas >/dev/null 2>&1; then
  echo "gmsaas is not in PATH. Please install/login first." >&2
  exit 2
fi
if ! command -v adb >/dev/null 2>&1; then
  echo "adb is not in PATH." >&2
  exit 2
fi

echo "Starting Genymotion SaaS instance: $RECIPE_UUID as $INSTANCE_NAME ..."
set +e
START_OUT=$(gmsaas instances start "$RECIPE_UUID" "$INSTANCE_NAME" 2>&1)
RET=$?
set -e
echo "$START_OUT"
if [[ $RET -ne 0 || "$START_OUT" == *"LICENSE_EXPIRED"* ]]; then
  echo "Unable to start instance (return=$RET). If this contains LICENSE_EXPIRED, renew your Genymotion SaaS license or use another account/token." >&2
  exit 3
fi

echo "Waiting for instance to appear in the list..."
UUID=""
for _ in {1..60}; do
  LINE=$(gmsaas instances list | awk -v name="$INSTANCE_NAME" 'NR>2 && $2==name {print $0}')
  if [[ -n "$LINE" ]]; then
    UUID=$(echo "$LINE" | awk '{print $1}')
    break
  fi
  sleep 2
done
if [[ -z "$UUID" ]]; then
  echo "Failed to locate the instance UUID by name ($INSTANCE_NAME)." >&2
  exit 4
fi
echo "Instance UUID: $UUID"

echo "Connecting ADB to $UUID ..."
ADB_OUT=$(gmsaas instances adbconnect "$UUID")
echo "$ADB_OUT"
SERIAL=$(echo "$ADB_OUT" | awk '/adb serial/ {print $4}')
if [[ -z "$SERIAL" ]]; then
  echo "Failed to extract ADB serial. Output: $ADB_OUT" >&2
  exit 5
fi

echo "Waiting for device $SERIAL ..."
adb -s "$SERIAL" wait-for-device

if [[ ! -f "$ASK_APK" ]]; then
  echo "Host APK not found: $ASK_APK. Build it first: ./gradlew :ime:app:assembleDebug" >&2
  exit 6
fi
if [[ ! -f "$ROZOFF_APK" ]]; then
  echo "Rozoff add-on APK not found: $ROZOFF_APK. Build it first in ~/mike-rozoff-anysoftkeyboard-addon: ./gradlew assembleDebug" >&2
  exit 7
fi

echo "Installing New Soft Keyboard ($ASK_APK) …"
adb -s "$SERIAL" install -r "$ASK_APK"

echo "Installing Mike Rozoff add-on ($ROZOFF_APK) …"
adb -s "$SERIAL" install -r "$ROZOFF_APK"

echo "Enabling and setting default IME …"
adb -s "$SERIAL" shell ime enable wtf.uhoh.newsoftkeyboard/.SoftKeyboard || true
adb -s "$SERIAL" shell ime set wtf.uhoh.newsoftkeyboard/.SoftKeyboard

echo "Done. Device serial: $SERIAL"

