# TODO / Status Report (monolith refactor + Genymotion smoke)

Last updated: 2025-12-18

This file is the single source of truth for:
- what’s changed locally (dirty working tree),
- what’s passing/failing (unit + device),
- what we ran to verify,
- and what must be fixed *before* any commit/push.

---

## Repo state (local working copy)

- Repo: `/mnt/finished/AnySoftKeyboard`
- Branch: `main`
- HEAD: `949343eb1`
- Working tree: **clean**
- Latest push: `origin/main` at `949343eb1`

Guardrail for future commits (per user):
- **Do not commit/push** until ALL are true:
  1) `:ime:app:testNskDebugUnitTest` is green ✅
  2) A meaningful Genymotion smoke run is green ✅
  3) Smoke scripts detect failures by parsing output (don’t trust `adb` exit code)

---

## What changed (local, uncommitted) — high-level

### A) Genymotion androidTest robustness (IME component resolution)

Fix:
- androidTests now discover the active IME component via `ime list -a -s` and select it, instead of hardcoding `.SoftKeyboard` (which is not present in `nsk`).

Primary files:
- `ime/app/src/androidTest/java/com/anysoftkeyboard/dictionaries/presage/NextWordSuggestionsUiAutomatorTest.java`
- `ime/app/src/androidTest/java/com/anysoftkeyboard/dictionaries/presage/NextWordSuggestionsViaHookInstrumentedTest.java`
- `ime/app/src/androidTest/java/com/anysoftkeyboard/ime/EmojiSearchOverlayInstrumentedTest.java`
- `ime/app/src/androidTest/java/com/anysoftkeyboard/ime/StickyModifiersInstrumentedTest.java`
- `ime/app/src/androidTest/java/com/anysoftkeyboard/ime/MikeRozoffKeyboardSwitchInstrumentedTest.java`

### B) StickyModifiers test + modifier-key correctness

Root causes (pre-fix):
- The test ran on the default English layout (not Mike Rozoff), so long-press assertions were wrong.
- Generic-row keys (Ctrl/Alt/Fn from the dev extension row) retained `row.mParent` from the `GenericRowKeyboard`, so
  `AnyKeyboardKey#getCurrentDrawableState` used the wrong keyboard instance. This broke “checked” state for Ctrl/Alt/Fn
  and made label color assertions fail.
- Shift was re-enabled by auto-caps after a Ctrl+letter combo that doesn’t insert text (cursor still at start).

Fixes:
- `GenericRowApplier` now repoints each inserted generic-row key’s `row.mParent` to the target keyboard, so drawable state
  and theming resolve against the real active keyboard instance.
- `StickyModifiersInstrumentedTest` now:
  - forces Mike Rozoff layout as the active keyboard,
  - requires the Mike Rozoff add-on (fails loudly if missing),
  - disables `auto_caps` for determinism,
  - uses real UI taps for Ctrl/Alt/Fn state and adds a delay to avoid Function “double tap to lock” behavior.

Primary files:
- `ime/app/src/main/java/com/anysoftkeyboard/keyboards/GenericRowApplier.java`
- `ime/app/src/androidTest/java/com/anysoftkeyboard/ime/StickyModifiersInstrumentedTest.java`

### C) MikeRozoff keyboard-switch CTS-style smoke

Root cause (pre-fix):
- The app path for `CUSTOM_KEYBOARD_SWITCH` stopped logging the “CustomKeyboardSwitch” tag, but the test depends on it.

Fix:
- `KeyboardSwitchHandler` logs `Log.d("CustomKeyboardSwitch", <targetId>)` when handling a custom switch key.
- `MikeRozoffKeyboardSwitchInstrumentedTest` now **fails** (not skips) if the add-on is missing.

Primary files:
- `ime/app/src/main/java/com/anysoftkeyboard/ime/KeyboardSwitchHandler.java`
- `ime/app/src/androidTest/java/com/anysoftkeyboard/ime/MikeRozoffKeyboardSwitchInstrumentedTest.java`

---

## Current test status (the actual truth as of this update)

### 1) Unit tests (host/JVM) ✅

Ran:
- `GRADLE_USER_HOME=/mnt/finished/.gradle ./gradlew :ime:app:testNskDebugUnitTest -x lint`

Result:
- ✅ green

### 2) Genymotion smoke (localhost:42865) ✅

Pre-req: Mike Rozoff add-on installed on the device
- APK location in this environment:
  - `$HOME/mike-rozoff-anysoftkeyboard-addon/build/outputs/apk/debug/Mike Rozoff Keyboard Addon-debug.apk`
- Install:
  - `timeout 120s adb -s localhost:42865 install -r -t "$HOME/mike-rozoff-anysoftkeyboard-addon/build/outputs/apk/debug/Mike Rozoff Keyboard Addon-debug.apk"`
- Package sanity:
  - `adb -s localhost:42865 shell pm list packages | rg -i rozoff`
  - expected: `package:wtf.uhoh.mikerozoff.keyboard`

Build used:
- `GRADLE_USER_HOME=/mnt/finished/.gradle ./gradlew :ime:app:assembleNskDebug :ime:app:assembleNskDebugAndroidTest -x lint`

Install used:
- `timeout 90s adb -s localhost:42865 install -r -t ime/app/build/outputs/apk/nsk/debug/app-nsk-debug.apk`
- `timeout 120s adb -s localhost:42865 install -r -t ime/app/build/outputs/apk/androidTest/nsk/debug/app-nsk-debug-androidTest.apk`

Passing smoke matrix (verified with output parsing, not adb exit code):
- ✅ `com.anysoftkeyboard.dictionaries.presage.NextWordSuggestionsUiAutomatorTest`
- ✅ `com.anysoftkeyboard.dictionaries.presage.NextWordSuggestionsViaHookInstrumentedTest`
- ✅ `com.anysoftkeyboard.dictionaries.presage.NeuralNonsenseSentenceInstrumentedTest`
- ✅ `com.anysoftkeyboard.ime.EmojiSearchOverlayInstrumentedTest`
- ✅ `com.anysoftkeyboard.ime.StickyModifiersInstrumentedTest`
- ✅ `com.anysoftkeyboard.ime.MikeRozoffKeyboardSwitchInstrumentedTest`

---

## Script gotcha (still true)

- `adb shell am instrument ...` can return exit code `0` even when tests fail (it still prints `FAILURES!!!`).
- Any smoke script must parse stdout for failure signals:
  - `FAILURES!!!`
  - `INSTRUMENTATION_STATUS_CODE: -2`

---

## Next actions (before any commit/push)

Done (2025-12-18):
- `GRADLE_USER_HOME=/mnt/finished/.gradle ./gradlew spotlessApply` ✅
- `GRADLE_USER_HOME=/mnt/finished/.gradle ./gradlew :ime:app:testNskDebugUnitTest -x lint` ✅
- `GRADLE_USER_HOME=/mnt/finished/.gradle ./gradlew :ime:app:assembleNskDebug :ime:app:assembleNskDebugAndroidTest -x lint` ✅
- Genymotion smoke matrix ✅ (see “Genymotion smoke” section above)
- Commit + push ✅ (`217bc0821`, `949343eb1`)

Next actions:
- Keep peeling the next monolith slice (per `docs/monolith-inventory.md`), then repeat the same unit+device gate before committing.
