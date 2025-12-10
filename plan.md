# NewSoftKeyboard Remaining Work

## Compatibility & Cleanup
- Continue pruning AnySoftKeyboard-only resources/strings/tasks while keeping `compat-ask` shims for legacy add-ons.
- User-visible branding largely updated (crash strings, privacy policy, About screen); base permission rationales now use NewSoftKeyboard wording. Keep spot-fixing any stragglers as they surface.
- Dual authorities: FileProvider is now split (NSK primary + legacy). Verify prefs/other authorities stay minimal.
- Add-on discovery CTS-style test **done** (ASK + NSK namespaces covered).

## Branding
- Product flavors (`nsk`, `askCompat`) already exist; add flavor-specific names/icons/resources if we keep both.

## Monolith Follow-ups
- Continue splitting large classes (e.g., remaining `AnySoftKeyboard`/`AnySoftKeyboardSuggestions` pieces, `Dictionary`/`BTreeDictionary`, settings fragments into view/data helpers) with behavior preserved and light tests.

## Neural/Prediction Quality
- Host-side predictNextWords test exists; add small UI polish/normalization if desired.

## Immediate Next Steps
1) Continue legacy cleanup (remaining resources/tasks; occasional wording fixes); verify no new ASK-only branding sneaks in.
2) Decide whether to keep `askCompat` flavor and, if so, add flavor-specific branding assets; otherwise trim it.
3) Pick the next monolith slice from the audit list and refactor with tests.
