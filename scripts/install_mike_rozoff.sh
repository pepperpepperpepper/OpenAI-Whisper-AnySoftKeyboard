#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK_PATH="$ROOT_DIR/ime/app/build/outputs/apk/debug/app-debug.apk"

echo "🛠  Building AnySoftKeyboard debug APK (includes Mike Rozoff layouts)..."
(
  cd "$ROOT_DIR"
  ./gradlew :ime:app:assembleDebug >/dev/null
)

if [[ ! -f "$APK_PATH" ]]; then
  echo "❌ APK artifact not found at $APK_PATH"
  exit 1
fi

echo "🔌 Waiting for an adb device..."
adb wait-for-device

echo "📦 Installing $APK_PATH"
adb install -r "$APK_PATH"

echo "✅ Mike Rozoff-enabled build installed."
