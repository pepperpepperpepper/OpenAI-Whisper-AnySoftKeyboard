# NewSoftKeyboard Rebrand + ASK Compatibility Plan

## Goal

Rebrand the project away from “AnySoftKeyboard/ASK” to “NewSoftKeyboard/NSK” **without breaking** compatibility with existing AnySoftKeyboard add‑ons (keyboards, dictionaries, themes, quick‑text, extension rows).

Compatibility here means:

- Existing add‑on APKs (especially from F‑Droid) should continue to be **discoverable and usable** without rebuilding.
- We can change branding and internal code organization, but must preserve **external contracts** (intent actions, manifest meta‑data keys, authorities, persisted formats).

## What’s already done (current state)

### 1) NSK-branded entry points (nsk flavor)

For the `nsk` flavor we added NewSoftKeyboard-qualified components and made them the manifest entry points:

- `wtf.uhoh.newsoftkeyboard.NewSoftKeyboardApplication`
- `wtf.uhoh.newsoftkeyboard.NewSoftKeyboardService` (delegates to legacy `SoftKeyboard`)
- `wtf.uhoh.newsoftkeyboard.NskLauncherSettingsActivity`

And updated the `nsk` overlay manifest (`ime/app/src/nsk/AndroidManifest.xml`) to remove legacy entrypoints and register the new ones.

This keeps the UI/launcher/service component names on-brand while still using the existing IME implementation underneath.

### 2) UI/style debranding with compatibility wrappers

We migrated app/prefs UI to `Theme.Nsk*` and `Nsk.*` style families while **keeping legacy `Ask.*` / `Theme.Ask*` styles as wrappers** so older resources or modules don’t break.

This allows new code/resources to use NSK names without rewriting the world.

### 3) User-visible debranding sweep

We updated remaining user-facing places that still said “AnySoftKeyboard/ASK”, including:

- Release info crash-report link (`ime/releaseinfo/src/main/res/values/strings_do_not_translate.xml`)
- Changelog links/content (`ime/releaseinfo/src/main/java/com/anysoftkeyboard/releaseinfo/VersionChangeLogs.java`)
- Play metadata contact website + description (`ime/app/src/main/play/**`)
- Developer tools share labels + tracing/memdump filenames (`ime/app/src/main/java/com/anysoftkeyboard/ui/dev/**`)
- App shortcuts target package (now uses per-flavor string `@string/app_shortcut_target_package`)
- About screen and sharing strings:
  - `ime/app/src/main/res/layout/about_newsoftkeyboard.xml`
  - `ime/app/src/main/res/values/strings.xml` / `strings_dont_translate.xml`
- Localized `app_share_*` strings (removed old store links and `com.menny.android.anysoftkeyboard` references)

We also renamed several internal/packaging assets that still carried ASK-named filenames (these do *not* affect add-on compatibility):

- ProGuard config: `ime/app/proguard-anysoftkeyboard.txt` → `ime/app/proguard-newsoftkeyboard.txt` (and updated Gradle references)
- Theme wallpaper resources: `@drawable/ask_wallpaper` → `@drawable/nsk_wallpaper` (same bitmap; new name)
- Store assets/screenshots renamed to use “NewSoftKeyboard” in the filename

### 4) Add-on discovery compatibility expanded (F‑Droid reality check)

We verified with real F‑Droid APKs that ASK add-ons use **two legacy namespaces**:

1) **“menny” legacy namespace** (very common for language packs):
   - actions: `com.menny.android.anysoftkeyboard.KEYBOARD`, `com.menny.android.anysoftkeyboard.DICTIONARY`
   - meta-data: `com.menny.android.anysoftkeyboard.keyboards`, `com.menny.android.anysoftkeyboard.dictionaries`

2) **“plugin” legacy namespace** (common for themes/quick-text/extensions/etc):
   - actions: `com.anysoftkeyboard.plugin.*`
   - meta-data: `com.anysoftkeyboard.plugindata.*`

We already supported the “plugin” legacy namespace. We added the missing “menny” namespace support:

- Constants added in `api/src/main/java/wtf/uhoh/newsoftkeyboard/api/PluginActions.java`
- Wiring added to factories:
  - `ime/app/src/main/java/com/anysoftkeyboard/keyboards/KeyboardFactory.java`
  - `ime/app/src/main/java/com/anysoftkeyboard/dictionaries/ExternalDictionaryFactory.java`
- Android 11+ package visibility queries updated:
  - `ime/app/src/main/AndroidManifest.xml`

### 5) Tests updated/added to prevent drift

- AndroidTest receiver now advertises **NewSoftKeyboard + legacy** contracts so we can validate discovery without relying on external APKs:
  - `ime/app/src/androidTest/AndroidManifest.xml`
  - `ime/app/src/androidTest/res/xml/test_*`
- Instrumentation verifies factories discover add-ons via:
  - NewSoftKeyboard namespace (`wtf.uhoh.newsoftkeyboard.*`)
  - plugin legacy namespace (`com.anysoftkeyboard.plugin.*` + `com.anysoftkeyboard.plugindata.*`)
  - menny legacy namespace (`com.menny.android.anysoftkeyboard.*`)
  - `ime/app/src/androidTest/java/com/anysoftkeyboard/addons/cts/AddOnDiscoveryInstrumentedTest.java`
- Instrumentation verifies the `PluginActions` classifier recognizes all supported action namespaces:
  - `ime/app/src/androidTest/java/com/anysoftkeyboard/compat/PluginActionsInstrumentedTest.java`
- Instrumentation smoke verifies discovery with *real* external add-ons (gated; skips if packages are not installed):
  - `ime/app/src/androidTest/java/com/anysoftkeyboard/addons/cts/ExternalAddOnSmokeInstrumentedTest.java`
- Robolectric wiring test verifies the factories include the expected receiver specs:
  - `ime/app/src/test/java/com/anysoftkeyboard/compat/PluginActionsWiringTest.java`
- Updated the Robolectric app to tolerate the NSK service entrypoint when running unit tests:
  - `ime/app/src/test/java/com/menny/android/anysoftkeyboard/AnyRoboApplication.java`

Build verification (local):

- `:ime:app:assembleNskDebug -x lint` ✅
- `:ime:app:assembleAndroidTest -x lint` ✅
- Focused unit test `PluginActionsWiringTest` ✅
- Genymotion instrumentation (nsk debug): `AddOnDiscoveryInstrumentedTest` ✅ and `PluginActionsInstrumentedTest` ✅

## What we should keep as “legacy” (compat surfaces)

These must remain stable (or require a careful migration strategy):

- Add-on discovery contracts:
  - `com.anysoftkeyboard.plugin.*` + `com.anysoftkeyboard.plugindata.*`
  - `com.menny.android.anysoftkeyboard.(KEYBOARD|DICTIONARY)` + `com.menny.android.anysoftkeyboard.(keyboards|dictionaries)`
- Persisted formats/keys used for backups and migrations:
  - `AnySoftKeyboardPrefs.xml` filename and the XML root node name `AnySoftKeyboardPrefs` are still present and should not be casually renamed.
- Authorities exposed to other apps (e.g., legacy FileProvider authorities) should remain as aliases if external code might reference them.

## What we can safely rebrand (without breaking ASK add-ons)

These are safe to rebrand because they are **not** external compatibility contracts:

- App branding/UI:
  - launcher labels, icons, About screen content, website links, and other user-visible strings (keep the same resource keys when referenced cross-module; change values freely).
  - style/theme names, as long as legacy names remain as wrappers/aliases (e.g., keep `Ask.*`/`Theme.Ask*` as compatibility shells).
- Internal code organization:
  - package/class/file names and module structure, as long as the manifest entry points and wiring are updated.
  - refactors that don’t change external behavior (e.g., breaking monoliths into helpers).
- Internal-only build metadata:
  - Gradle root project name (`rootProject.name`) and local helper scripts (not consumed by add-on APKs).

## Compatibility caveats (important)

Some add-on APKs include a **launcher UI** (Activity) that checks whether a supported host keyboard app is installed and/or whether the IME service class exists. Example in our repo:

- `addons/base/apk/src/main/java/com/anysoftkeyboard/addon/apk/MainActivity.kt`
  - checks for a host keyboard package being installed:
    - `wtf.uhoh.newsoftkeyboard` (NSK)
    - `wtf.uhoh.newsoftkeyboard.askcompat` (NSK compat)
    - `com.menny.android.anysoftkeyboard` (legacy ASK)
  - and verifies the IME service class exists for that package

This affects only the add-on’s “helper UI” (“Install/Launch ASK”), not the add-on discovery itself.

Options:

1) **Do nothing** (recommended short-term):
   - The add-on still works when discovered; the helper UI may still tell users to install AnySoftKeyboard.

2) **Rebuild our add-on packs for NSK** (only for packs we control):
   - Update the add-on UI flow to recognize NSK package(s) too. ✅ done for in-repo add-ons via `MainActivityBase`.

3) **True drop-in appId compatibility build** (only if required):
   - Ship a special build whose `applicationId` is exactly `com.menny.android.anysoftkeyboard`.
   - Pros: satisfies package-name checks in third-party add-ons.
   - Cons: cannot be installed alongside upstream AnySoftKeyboard; higher risk; must be intentional.

## Next steps (recommended plan)

### Phase A — Lock in add-on compatibility

- Keep constants centralized in `PluginActions`.
- Ensure *all* factory paths accept all three namespaces (NSK + plugin legacy + menny legacy):
  - keyboards ✅
  - dictionaries ✅
  - themes (plugin legacy) ✅
  - quick-text (plugin legacy) ✅
  - extension keyboard (plugin legacy) ✅
  - (verify any remaining add-on surfaces as they appear)
- Add a small doc section explaining the “two legacy namespaces” so it doesn’t regress. ✅ (see `docs/compatibility-checklist.md`)

### Phase B — Finish debranding (user-facing) ✅

- Completed: removed remaining user-visible “ASK/AnySoftKeyboard” wording, except explicit compatibility statements (“Compatible with AnySoftKeyboard add-ons”).
- We keep upstream issue links in comments for now (non-user-visible).

### Phase C — Decide the compatibility distribution story ✅

Current intended product line:

- `nsk` (primary): main NewSoftKeyboard experience.
  - package/appId: `wtf.uhoh.newsoftkeyboard`
  - entry points: `wtf.uhoh.newsoftkeyboard.*` (`NewSoftKeyboardApplication`, `NewSoftKeyboardService`, `NskLauncherSettingsActivity`)
  - intended for: end users + default testing.
- `askCompat` (secondary): side-by-side install variant to maximize compatibility testing without touching external contracts.
  - package/appId: `wtf.uhoh.newsoftkeyboard.askcompat`
  - IME service component exposed: `com.menny.android.anysoftkeyboard.SoftKeyboard` (legacy class name)
  - intended for: validating add-on “helper UI” flows that hard-check for the legacy service class name, and for troubleshooting discovery issues.
- Optional “appId-compatible” build (NOT shipping by default):
  - Would set `applicationId` to `com.menny.android.anysoftkeyboard` for drop-in package-name checks.
  - Tradeoff: cannot be installed alongside upstream AnySoftKeyboard; only do this if we find add-ons that are unusable without it.

Build/install commands live in `BUILDING.md` and are the source of truth for developers.

### Phase D — Smoke tests for “install an add-on and it appears”

On Genymotion / device:

- Install a real F‑Droid language pack APK and verify its keyboard/dictionary show up.
- Install a theme pack and verify theme discovery works.
- Status: validated on Genymotion (German language pack + 3D theme) via `ExternalAddOnSmokeInstrumentedTest` ✅
- Optional automated check (skips if packages not installed):
  - Build/install prereq (recommended): `TEST_BUILD_TYPE=debug ./gradlew :ime:app:assembleNskDebug :ime:app:assembleAndroidTest -x lint` then install the `app-nsk-debug.apk` + `app-nsk-debug-androidTest.apk`.
  - Install (example):
    - `com.anysoftkeyboard.languagepack.german` (menny legacy namespace)
    - `com.anysoftkeyboard.theme.three_d` (plugin legacy namespace)
  - Run:
    - `timeout 240s adb -s localhost:42865 shell am instrument -w -r -e expected_language_pack com.anysoftkeyboard.languagepack.german -e expected_theme_pack com.anysoftkeyboard.theme.three_d -e class com.anysoftkeyboard.addons.cts.ExternalAddOnSmokeInstrumentedTest wtf.uhoh.newsoftkeyboard.test/androidx.test.runner.AndroidJUnitRunner`

## Reference: The contracts we now support

- NewSoftKeyboard:
  - actions: `wtf.uhoh.newsoftkeyboard.*`
  - meta-data: `wtf.uhoh.newsoftkeyboard.*`
- Legacy (“plugin”):
  - actions: `com.anysoftkeyboard.plugin.*`
  - meta-data: `com.anysoftkeyboard.plugindata.*`
- Legacy (“menny”):
  - actions: `com.menny.android.anysoftkeyboard.KEYBOARD`, `com.menny.android.anysoftkeyboard.DICTIONARY`
  - meta-data: `com.menny.android.anysoftkeyboard.keyboards`, `com.menny.android.anysoftkeyboard.dictionaries`
