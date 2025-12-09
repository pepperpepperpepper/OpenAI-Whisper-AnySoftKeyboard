# NewSoftKeyboard Remaining Work

## Compatibility & Cleanup
- Prune unused AnySoftKeyboard-only resources/strings/tasks while keeping `compat-ask` shims for legacy add-ons.
- Verify dual authorities (FileProvider/prefs) are only kept where required for ASK compatibility.
- Add an add-on discovery CTS-style test that installs a sample ASK plug-in and asserts discovery under both action namespaces.

## Branding
- Finalize optional product flavors (`nsk` vs `askCompat`) with flavor-specific names/icons/resources; runtime APIs remain unchanged.

## Monolith Follow-ups
- Continue splitting large classes (e.g., remaining `AnySoftKeyboard`/`AnySoftKeyboardSuggestions` pieces, `Dictionary`/`BTreeDictionary`, settings fragments into view/data helpers) with behavior preserved and light tests.

## Neural/Prediction Quality
- Host-side predictNextWords test exists; add small UI polish/normalization if desired.

## Immediate Next Steps
1) Finish legacy cleanup pass and land the add-on discovery CTS test.
2) Decide and implement the flavor branding split.
3) Pick the next monolith slice from the audit list and refactor with tests.
