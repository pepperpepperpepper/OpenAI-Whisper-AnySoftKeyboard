# Building NewSoftKeyboard

This is a lean fork of AnySoftKeyboard. Keep models out of the APK; build fast; ship through the downloader/catalog.

## Prerequisites
- JDK 17
- Android SDK 34 (platforms + build-tools 34.x)
- NDK r27
- Gradle cache: set `GRADLE_USER_HOME=/mnt/finished/.gradle` to avoid disk pressure
- Presage sources: run `scripts/setup_presage.sh` once (or wire into CI prebuild)

## Common builds
- Debug app: `./gradlew :ime:app:assembleDebug`
- AndroidTest APK: `./gradlew :ime:app:assembleAndroidTest -x lint`
- Unit tests (tokenizer/neural): `./gradlew :engine-neural:test`
- Release (unsigned if keystore envs missing): `./gradlew :ime:app:assembleRelease`

Signing (when available):
- Keystore path defaults to `/tmp/anysoftkeyboard.keystore` (symlink to your real store).
- Env vars (either prefix works):
  - `KEY_STORE_FILE_PASSWORD` / `FDROID_KEYSTORE_PASS`
  - `KEY_STORE_FILE_DEFAULT_ALIAS_PASSWORD` / `FDROID_KEY_ALIAS_PASS`
  - `override_release_key_alias` / `FDROID_KEY_ALIAS` (default `fdroidrepo`)

## Devices & tests
- Emulator target: Genymotion at `localhost:42865`; wrap `adb` in `timeout` for reliability.
- Build + install debug app & tests:
  ```bash
  ./gradlew :ime:app:assembleNskDebug :ime:app:assembleAndroidTest -x lint
  adb -s localhost:42865 install -r -t ime/app/build/outputs/apk/nsk/debug/app-nsk-debug.apk
  adb -s localhost:42865 install -r -t ime/app/build/outputs/apk/androidTest/nsk/debug/app-nsk-debug-androidTest.apk
  ```
- Run neural sentence sanity (neural manager):
  ```bash
  adb -s localhost:42865 shell am instrument -w -r \
    -e class com.anysoftkeyboard.dictionaries.presage.NeuralNonsenseSentenceInstrumentedTest#buildNonsenseSentenceFromNeuralPredictions \
    wtf.uhoh.newsoftkeyboard.test/androidx.test.runner.AndroidJUnitRunner
  ```
- Run UI tap sentence test (end-to-end suggestions):
  ```bash
  adb -s localhost:42865 shell am instrument -w -r \
    -e class com.anysoftkeyboard.dictionaries.presage.NextWordSuggestionsUiAutomatorTest#composeNonsenseSentenceUsingOnlySuggestions \
    wtf.uhoh.newsoftkeyboard.test/androidx.test.runner.AndroidJUnitRunner
  ```
- Inspect sentence:
  ```bash
  adb -s localhost:42865 logcat -d | grep NON_SENSE_SENTENCE=
  # expected example: first time I had a chance to meet him in the
  ```
- Optional neural debug logs: set `adb -s localhost:42865 shell setprop NSK_TEST_LOGS true` before running tests to dump top‑k logits/decoded tokens.
- Host tokenizer/neural sanity: `./gradlew :engine-neural:test`

## Models
- Catalog URL baked in: `https://fdroid.uh-oh.wtf/models/catalog.json?v=3`
- Models download to `no_backup/presage/models/<model-id>` with SHA‑256 validation. Keep APK free of large assets.

## Publishing to F-Droid
- Build release APK: `./gradlew :ime:app:assembleRelease`
- Metadata/YAML lives under `outputs/fdroid/`. When ready, run your deployment script (e.g., `scripts/update_and_deploy.sh`) with the F-Droid env vars exported (`source /home/arch/fdroid/.env`).

## Don’ts
- Don’t run `./gradlew build` (Android aggregates are flaky); use module tasks above.
- Don’t add new runtime third‑party dependencies without approval.
