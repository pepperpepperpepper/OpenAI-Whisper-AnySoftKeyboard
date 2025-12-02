# Add‑On Compatibility Checklist

Use this list when validating NewSoftKeyboard remains compatible with existing AnySoftKeyboard add‑ons:

- Discovery actions and meta‑data
  - Keyboards: actions `wtf.uhoh.newsoftkeyboard.KEYBOARD` and `com.anysoftkeyboard.plugin.KEYBOARD`
  - Dictionaries: actions `wtf.uhoh.newsoftkeyboard.DICTIONARY` and `com.anysoftkeyboard.plugin.DICTIONARY`
  - Themes: action `com.anysoftkeyboard.plugin.KEYBOARD_THEME`
  - Quick‑text: action `com.anysoftkeyboard.plugin.QUICK_TEXT_KEY`
  - Meta‑data keys accepted: `wtf.uhoh.newsoftkeyboard.*` and `com.anysoftkeyboard.plugindata.*`
- XML schema
  - Root/add‑on node names unchanged (Keyboards/Dictionaries, Keyboard/Dictionary)
  - Attributes supported: ids, names, descriptions, sort index, defaultEnabled, icons, etc.
- Runtime behavior
  - Add‑on enablement and ordering are preserved across installs/updates.
  - Broadcast receiver `AddOnUICardReceiver` continues to respond to `com.anysoftkeyboard.UI_CARD_UPDATE`.
- Manifests and queries
  - App manifest queries include both NewSoftKeyboard and legacy ASK actions (verified under `ime/app/src/main/AndroidManifest.xml`).
- Tests
  - Unit: addon factories resolve receivers for both namespaces.
  - Instrumentation: install a sample ASK keyboard pack APK and verify discovery + switch.

Notes
- Keep constants centralized in `wtf/uhoh/newsoftkeyboard/api/PluginActions.java` to avoid drift.
- When adding new add‑on surfaces, provide both `wtf.uhoh.newsoftkeyboard.*` and legacy `com.anysoftkeyboard.*` variants until deprecation.
