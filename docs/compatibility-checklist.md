# Add‑On Compatibility Checklist

Use this list when validating NewSoftKeyboard remains compatible with existing AnySoftKeyboard add‑ons:

- Public compatibility surface (do not rename)
  - Add-on discovery actions + meta-data keys: keep centralized in `api/src/main/java/wtf/uhoh/newsoftkeyboard/api/PluginActions.java`.
  - Add-on API classes that third-party APKs may compile against (package/class names are part of the contract):
    - `api/src/main/java/com/anysoftkeyboard/api/KeyCodes.java`
    - `api/src/main/java/com/anysoftkeyboard/api/MediaInsertion.java`
  - Add-on XML schema and resource names (looked up by string at runtime):
    - Integer resource name `anysoftkeyboard_api_version_code` (queried via `Resources.getIdentifier`) is required for API versioning (`ime/addons/src/main/java/com/anysoftkeyboard/addons/AddOnsXmlParser.java`).
    - XML attribute names are parsed by name (e.g., `id`, `nameResId`, `description`, `index`, `devOnly`, `hidden`, `uiCard`, `name`) and must remain supported (`ime/addons/src/main/java/com/anysoftkeyboard/addons/AddOnsXmlParser.java`).
  - Theme/attributes compatibility (remote theme packs):
    - Attribute mapping uses the _attribute resource entry name_ (`Resources.getResourceEntryName`) to resolve IDs in the remote package (`ime/addons/src/main/java/com/anysoftkeyboard/addons/Support.java`).
    - Do not rename `R.attr.*` names used in any styleables that can be loaded from remote theme packs (these are part of the external theme contract; see `api/src/main/res/values/attrs.xml`).
  - Host exported entrypoints (explicit Intents from other APKs may target these by class name):
    - IME service: `wtf.uhoh.newsoftkeyboard.NewSoftKeyboardService` (NSK flavor) / `com.menny.android.anysoftkeyboard.SoftKeyboard` (askCompat flavor).
    - Launcher/settings entry: `wtf.uhoh.newsoftkeyboard.NskLauncherSettingsActivity` (NSK flavor) / `com.menny.android.anysoftkeyboard.LauncherSettingsActivity` (askCompat flavor).
    - Broadcast receiver: `com.anysoftkeyboard.addons.AddOnUICardReceiver` (`com.anysoftkeyboard.UI_CARD_UPDATE`).
  - Authorities:
    - FileProvider keeps both `${applicationId}.fileprovider` and legacy `com.menny.android.anysoftkeyboard.fileprovider` (`ime/fileprovider/src/main/AndroidManifest.xml`).

- Discovery actions and meta‑data
  - Keyboards: actions `wtf.uhoh.newsoftkeyboard.KEYBOARD`, `com.anysoftkeyboard.plugin.KEYBOARD`, and `com.menny.android.anysoftkeyboard.KEYBOARD`
  - Dictionaries: actions `wtf.uhoh.newsoftkeyboard.DICTIONARY`, `com.anysoftkeyboard.plugin.DICTIONARY`, and `com.menny.android.anysoftkeyboard.DICTIONARY`
  - Themes: actions `wtf.uhoh.newsoftkeyboard.KEYBOARD_THEME` and `com.anysoftkeyboard.plugin.KEYBOARD_THEME`
  - Quick‑text: actions `wtf.uhoh.newsoftkeyboard.QUICK_TEXT_KEY` and `com.anysoftkeyboard.plugin.QUICK_TEXT_KEY`
  - Extension keyboards: actions `wtf.uhoh.newsoftkeyboard.EXTENSION_KEYBOARD` and `com.anysoftkeyboard.plugin.EXTENSION_KEYBOARD`
  - Meta‑data keys accepted:
    - Keyboards/Dictionaries: `wtf.uhoh.newsoftkeyboard.*`, `com.anysoftkeyboard.plugindata.*`, and `com.menny.android.anysoftkeyboard.(keyboards|dictionaries)`
    - Themes/Quick‑text/Extensions: `wtf.uhoh.newsoftkeyboard.plugindata.*` and `com.anysoftkeyboard.plugindata.*`
- XML schema
  - Root/add‑on node names unchanged (Keyboards/Dictionaries, Keyboard/Dictionary)
  - Attributes supported: ids, names, descriptions, sort index, defaultEnabled, icons, etc.
- Runtime behavior
  - Add‑on enablement and ordering are preserved across installs/updates.
  - Broadcast receiver `AddOnUICardReceiver` continues to respond to `com.anysoftkeyboard.UI_CARD_UPDATE`.
  - Packaging/branding changes in NewSoftKeyboard must not require add‑on APK changes; existing ASK packs should work unmodified.
- Manifests and queries
  - App manifest queries include both NewSoftKeyboard and legacy ASK actions (verified under `ime/app/src/main/AndroidManifest.xml`).
- Tests
  - Unit: addon factories resolve receivers for both namespaces.
  - Instrumentation: install a sample ASK keyboard pack APK and verify discovery + switch.
  - Instrumentation (real APKs): `ExternalAddOnSmokeInstrumentedTest` with an installed F-Droid language pack + theme pack.

Renaming rules (safe vs risky)

- Safe renames (won’t affect third-party add-ons)
  - Internal Java/Kotlin classes **not referenced** from:
    - any `AndroidManifest.xml` component `android:name=...`
    - `api/` public classes/constants
    - reflection-by-string (`Class.forName`, `Intent` component strings, etc.)
    - resource-name lookups (`Resources.getIdentifier`, XML attribute-name parsing)
- Risky renames (require shims/aliases, or should not be done)
  - Anything in `api/src/main/java/com/anysoftkeyboard/api/*` (other APKs may compile against it).
  - Any intent action/meta-data string used for add-on discovery (`PluginActions`, manifest `<queries>`).
  - Any exported component class name (activities/services/receivers/providers that other APKs may target explicitly).
  - Any attribute/resource name used by remote packs via string lookup (`Support.createBackwardCompatibleStyleable`, `AddOnsXmlParser`).
- When you want new names anyway, prefer compatibility shims:
  - Java: keep an old-named class as a thin delegating wrapper to the new owner.
  - Android components: prefer `activity-alias` / `receiver` wrapper components over removing legacy names.

Concrete internal rename candidates (safe for add-on compatibility, but high-churn)

- These are **not** part of the add-on contract (unless referenced by manifest/reflection), so renaming them won’t break
  third-party keyboard/theme packs. They _will_ be large internal refactors, so treat them as multi-step work:
  - `ime/app/src/main/java/com/anysoftkeyboard/ImeServiceBase.java` (core IME logic owner)
    - Done (2025-12-24): introduced `ImeServiceBase` and removed the temporary `AnySoftKeyboard` shim after migrating internal call-sites.
  - `ime/app/src/main/java/com/anysoftkeyboard/keyboards/KeyboardDefinition.java` (keyboard model/definition)
    - Done (2025-12-24): introduced `KeyboardDefinition` and removed the temporary `AnyKeyboard`/`AnyKeyboard.AnyKey` shim after
      migrating all internal call-sites.
  - `ime/app/src/main/java/com/anysoftkeyboard/keyboards/PopupKeyboard.java` (popup keyboard model)
    - Done (2025-12-24): introduced `PopupKeyboard` and removed the temporary `AnyPopupKeyboard` shim after migrating internal call-sites.
  - `ime/app/src/main/java/com/anysoftkeyboard/keyboards/ExternalKeyboard.java` (add-on keyboard model)
    - Done (2025-12-24): introduced `ExternalKeyboard` and removed the temporary `ExternalAnyKeyboard` shim after migrating internal call-sites.
  - `ime/app/src/main/java/com/anysoftkeyboard/ime/ImeSuggestionsController.java` (suggestions lifecycle)
    - Done (2025-12-24): introduced `ImeSuggestionsController` and removed the temporary `AnySoftKeyboardSuggestions` shim after migrating internal call-sites.
  - Migration follow-up (safe, internal-only)
    - Done (2025-12-24): migrated internal call-sites away from legacy `AnySoftKeyboard*` type names to owned
      `ImeServiceBase`/`ImeSuggestionsController`, then deleted the legacy shims.
    - Done (2025-12-24): migrated production + tests away from the `AnyKeyboard`/`AnyKeyboard.AnyKey` shim to the owned
      `KeyboardDefinition`/`KeyboardKey` types, then deleted the shim.
    - Done (2025-12-24): migrated production + tests away from the `AnyPopupKeyboard` shim to the owned `PopupKeyboard` type, then deleted the shim.
    - Done (2025-12-24): migrated production + tests away from the `ExternalAnyKeyboard` shim to the owned `ExternalKeyboard` type, then deleted the shim.
    - Done (2025-12-24): renamed internal IME plumbing wrappers away from `AnySoftKeyboard*` to `Ime*`
      (`ImeServiceInitializer`, `ImeFunctionKeyHost`, `ImeModifierKeyStateHost`, `ImeDeleteActionHost`, etc.).
    - Done (2025-12-24): renamed internal IME feature-mixin base classes under `com.anysoftkeyboard.ime` from `AnySoftKeyboard*`
      to `Ime*` (`ImeBase`, `ImeTokenService`, `ImeDialogProvider`, `ImeRxPrefs`, `ImeKeyboardSwitchedListener`, `ImeInlineSuggestions`,
      `ImeKeyboardTagsSearcher`, `ImeThemeOverlay`, `ImeWithGestureTyping`, etc.). External add-on compatibility surfaces unchanged.
    - Done (2025-12-24): introduced `NskApplicationBase` and turned `AnyApplication` into a thin shim, so the NSK flavor no longer
      needs to inherit from `AnyApplication`.
    - Done (2025-12-24): removed remaining internal `Ask*` naming by renaming `AskOnGestureListener` → `NskOnGestureListener` and
      `AskInsertionRequestCallback` → `MediaInsertionRequestCallback`.
  - `ime/app/src/main/java/com/anysoftkeyboard/keyboards/views/KeyboardViewBase.java` (view host)
    - Suggested direction: keep as host/wiring; move behavior into owned collaborators (render/touch/state holders).
  - `ime/app/src/main/java/com/anysoftkeyboard/keyboards/KeyboardSwitcher.java` (mode/layout switching)
    - Suggested direction: split into “mode resolver” + “layout selection” + “current session state” owners.

Notes

- Keep constants centralized in `wtf/uhoh/newsoftkeyboard/api/PluginActions.java` to avoid drift.
- When adding new add‑on surfaces, provide both `wtf.uhoh.newsoftkeyboard.*` and legacy `com.anysoftkeyboard.*` variants until deprecation.
