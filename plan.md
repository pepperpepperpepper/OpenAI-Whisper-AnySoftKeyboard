# NewSoftKeyboard Remaining Work

## Compatibility & Cleanup
- Continue pruning AnySoftKeyboard-only resources/strings/tasks while keeping `compat-ask` shims for legacy add-ons.
- User-visible branding largely updated (crash strings, privacy policy, About screen); base permission rationales now use NewSoftKeyboard wording. French and Basque setup/power-save/clipboard strings now debranded. Keep spot-fixing any stragglers as they surface.
- Dual authorities: FileProvider is now split (NSK primary + legacy). Verify prefs/other authorities stay minimal.
- Add-on discovery CTS-style test **done** (ASK + NSK namespaces covered).

## Branding
- Product flavors (`nsk`, `askCompat`) already exist; add flavor-specific names/icons/resources if we keep both.

## Monolith Follow-ups
- Continue splitting large classes (see `docs/monolith-inventory.md` for LOC/top targets): remaining `AnySoftKeyboard`/`AnySoftKeyboardSuggestions` (more `InputConnection` routing moved to `InputConnectionRouter`; gesture typing and UI handler now use it), `AnyKeyboardViewBase`/`PointerTracker` (PointerTrackerRegistry + TouchDispatcher + KeyPressTimingHandler + PointerConfigLoader own touch/timing state; keep peeling rendering/input apart), `Dictionary`/`BTreeDictionary`, settings fragments into view/data helpers. Keep behavior identical; add light tests.

## Neural/Prediction Quality
- Host-side predictNextWords test exists; add small UI polish/normalization if desired.

## Immediate Next Steps
1) Continue legacy cleanup (remaining resources/tasks; occasional wording fixes). Market “leave app store” dialog strings are now rebranded across locales; permission rationales were already debranded.
2) Keep `askCompat` flavor for seamless legacy add-on compatibility; flavor now uses a distinct (green) launcher background for easy identification. Add more overlays/icons only if further differentiation is needed.
3) Pick the next monolith slice from the audit list and refactor with tests.
