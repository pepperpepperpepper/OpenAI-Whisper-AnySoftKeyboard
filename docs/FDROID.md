FDroid Publishing Guide
=======================

What you need
- JDK 17, Android SDK 34, NDK r27 (`ANDROID_HOME`/`ANDROID_SDK_ROOT` set).
- Keystore at `/tmp/anysoftkeyboard.keystore` (symlink to your real store is fine).
- Env vars (either set the KEY_STORE_* pair or the FDROID_* pair):
  - `KEY_STORE_FILE_PASSWORD` / `KEY_STORE_FILE_DEFAULT_ALIAS_PASSWORD`
  - or `FDROID_KEYSTORE_PASS` / `FDROID_KEY_ALIAS_PASS`
  - optional alias override: `override_release_key_alias` or `FDROID_KEY_ALIAS` (default: `fdroidrepo`).
- F-Droid repo credentials/config (your local fdroiddata checkout or S3 bucket creds).

Build & stage release APK
1) Ensure Presage vendor is staged (needed for JNI builds):
   `GRADLE_USER_HOME=/mnt/finished/.gradle ./gradlew :engine-presage:setupPresageVendor`
2) Build release APK (nsk flavor by default):
   `GRADLE_USER_HOME=/mnt/finished/.gradle ./gradlew :ime:app:assembleRelease -x lint`
   Output: `ime/app/build/outputs/apk/nsk/release/app-nsk-release.apk`
   If signing env vars are set, the APK is signed with your fdroid key; otherwise it is unsigned.

Generate/update F-Droid metadata
- A YAML entry is auto-generated when assembleRelease runs:
  `outputs/fdroid/wtf.uhoh.newsoftkeyboard.yaml`
- If you need to regenerate only the YAML:
  `GRADLE_USER_HOME=/mnt/finished/.gradle ./gradlew :ime:app:assembleRelease -x lint`
  (the `fdroid_yaml_output.gradle` hook finalizes assembleRelease).

Publish to your F-Droid repo
1) Copy the signed release APK to your repo’s `repo/` (or upload to your S3/CloudFront bucket).
2) Place or update the YAML in your fdroiddata `metadata/` directory (or wherever your pipeline reads it).
3) Run the usual F-Droid index step, e.g.:
   - Local fdroiddata: `fdroid update --clean --create-metadata`
   - Custom pipeline: your `scripts/update_and_deploy.sh` that syncs to S3/CloudFront.
4) Invalidate CDN if applicable (e.g., CloudFront distribution `E2RWHYJEODFGYE` used previously).

Smoke checks before publishing
- Install and sanity-test on Genymotion:
  `adb -s localhost:42865 install -r ime/app/build/outputs/apk/nsk/release/app-nsk-release.apk`
  `adb -s localhost:42865 shell ime enable|set wtf.uhoh.newsoftkeyboard/com.menny.android.anysoftkeyboard.SoftKeyboard`
  `adb -s localhost:42865 shell am start -n wtf.uhoh.newsoftkeyboard/com.anysoftkeyboard.debug.TestInputActivity`
- Instrumented tap test (optional, uses debug hooks):
  `adb -s localhost:42865 shell am instrument -w -r -e class com.anysoftkeyboard.dictionaries.presage.NextWordSuggestionsUiAutomatorTest#composeNonsenseSentenceUsingOnlySuggestions wtf.uhoh.newsoftkeyboard.test/androidx.test.runner.AndroidJUnitRunner`

Notes
- Models are downloaded at runtime; keep APK free of large model assets.
- Release will build arm64/armeabi-v7a/x86/x86_64 unless abiFilters are changed.
- If signing vars are absent, assembleRelease emits an unsigned APK—sign it manually before publishing.
