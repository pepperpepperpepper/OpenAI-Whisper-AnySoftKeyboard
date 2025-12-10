# Monolith Inventory (LOC by file)

Generated from `wc -l` over *.java and *.kt. Focus on files ≥500 LOC.

| LOC | File |
| ---:| --- |
| 2350 | ime/app/src/main/java/com/anysoftkeyboard/keyboards/views/AnyKeyboardViewBase.java |
| 2077 | ime/app/src/main/java/com/anysoftkeyboard/AnySoftKeyboard.java |
| 1471 | ime/dictionaries/src/main/java/com/anysoftkeyboard/dictionaries/Dictionary.java |
| 1433 | ime/app/src/main/java/com/anysoftkeyboard/ime/AnySoftKeyboardSuggestions.java |
| 1296 | ime/app/src/main/java/com/anysoftkeyboard/keyboards/AnyKeyboard.java |
| 1047 | ime/app/src/main/java/com/anysoftkeyboard/keyboards/KeyboardSwitcher.java |
| 1037 | ime/app/src/main/java/com/anysoftkeyboard/keyboards/Keyboard.java |
|  849 | ime/app/src/main/java/com/anysoftkeyboard/dictionaries/SuggestionsProvider.java |
|  809 | ime/addons/src/main/java/com/anysoftkeyboard/addons/AddOnsFactory.java |
|  764 | ime/app/src/main/java/com/anysoftkeyboard/ui/settings/MainFragment.java |
|  632 | ime/app/src/main/java/com/anysoftkeyboard/keyboards/views/CandidateView.java |
|  604 | ime/app/src/main/java/com/anysoftkeyboard/keyboards/views/PointerTracker.java |
|  590 | ime/app/src/main/java/com/anysoftkeyboard/dictionaries/SuggestImpl.java |
|  562 | ime/app/src/main/java/com/anysoftkeyboard/ime/AnySoftKeyboardWithGestureTyping.java |
|  555 | ime/app/src/main/java/com/anysoftkeyboard/ui/settings/NextWordSettingsFragment.java |
|  539 | ime/app/src/main/java/com/anysoftkeyboard/keyboards/ExternalAnyKeyboard.java |

Tests ≥500 LOC (for awareness): AnySoftKeyboard* tests, SuggestionProviderTest, gesture/clipboard/quicktext suites, etc. Full list available via `python tools/loc_monolith.py` (not yet added).

## Suggested Refactor Order
1) InputConnection/Editor state seam: `AnySoftKeyboard.java`, `AnySoftKeyboardSuggestions.java`.
2) Touch handling: `AnyKeyboardViewBase.java` + `PointerTracker.java` (extract TouchDispatcher).
3) Suggestion orchestration: `SuggestionsProvider.java` (split engine orchestration vs. normalization already started).
4) Keyboard model/core: `AnyKeyboard.java`, `Keyboard.java`, `KeyboardSwitcher.java` (pull parsing/layout concerns apart).
5) Dictionary core: `Dictionary.java` (separate trie/B-tree logic from lifecycle).
6) Settings UI: `MainFragment.java`, `NextWordSettingsFragment.java` (view vs. data helpers).

Keep behavior identical; add light tests per extraction. Preserve ASK compatibility surfaces while refactoring.

Recent extractions:
- InputConnectionRouter (AnySoftKeyboard*).
- TouchDispatcher (AnyKeyboardViewBase/PointerTracker) now owns touch gating flags.
- PointerTrackerRegistry (PointerTracker ownership/iteration).
- KeyPressTimingHandler extracted from AnyKeyboardViewBase.
- PointerConfigLoader binds long-press/multitap prefs to shared pointer config.
