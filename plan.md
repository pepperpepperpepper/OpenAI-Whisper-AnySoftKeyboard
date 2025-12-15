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
- Progress: Next-word usage stats UI moved from `NextWordSettingsFragment` into `NextWordUsageStatsLoader`; neural failure + summary building moved into `NextWordPreferenceSummaries`; clear-data flow moved into `NextWordDataCleaner` (fragment now ~355 LOC). Add-on UI card rendering loop + navigation + backup/restore chooser/execution are now wired through `AddOnUICardViewFactory`/`AddOnUICardPresenter`/`AddOnLinkNavigator` and `BackupRestoreLauncher`; `MainFragment` shrunk to ~445 LOC. `AnySoftKeyboardSuggestions` continues to slim via `SeparatorActionHelper`, `PredictionState` (flags), `SuggestionRefresher` (update flow), `WordRevertHandler`, `DictionaryLoaderHelper`, `SuggestionCommitterHost`/`SuggestionPickerHost`, and `SelectionExpectationTracker, `UserDictionaryWorker`..
- Progress: Next-word usage stats UI moved from `NextWordSettingsFragment` into `NextWordUsageStatsLoader`; neural failure + summary building moved into `NextWordPreferenceSummaries`; clear-data flow moved into `NextWordDataCleaner` (fragment now ~355 LOC). Add-on UI card rendering loop + navigation + backup/restore chooser/execution are now wired through `AddOnUICardViewFactory`/`AddOnUICardPresenter`/`AddOnLinkNavigator` and `BackupRestoreLauncher`; `MainFragment` shrunk to ~445 LOC. `AnySoftKeyboardSuggestions` continues to slim via helpers (`SeparatorActionHelper`, `PredictionState`, `SuggestionRefresher`, `WordRevertHandler`, `DictionaryLoaderHelper`, `SuggestionCommitterHost`/`SuggestionPickerHost`, `SelectionExpectationTracker`, `UserDictionaryWorker`, `WordRestartCoordinator`, `SeparatorHandler`, `PredictionStateUpdater`, `CharacterInputHandler`, `TextInputDispatcher`, `AddToDictionaryHintHost`, `UserDictionaryHost`, `CompletionHostAdapter`); current size ~975 LOC. Began nibbling `AnySoftKeyboard` with `VoiceUiHelper` and `StatusIconHelper`; file now ~1,538 LOC after extracting `handleEnterKey`. In `AnyKeyboardViewBase`, added `DrawDecisions`, `KeyboardNameVisibilityDecider`, `ModifierColorResolver`, `KeyboardThemeHost`, `ClipDecider`, `DrawInvalidationHelper`, `KeyDrawHelper`, `RenderSetup`, `SpecialKeysApplier`, and `KeyPreviewControllerBinder`, moving theme/draw visibility, per-key draw, clip/dirty bookkeeping, special-key application, and preview wiring out; size ~1,314 LOC.
- Progress: AnyKeyboardViewBase theme plumbing moved to `KeyboardThemeHost`, dropping the file to ~1,295 LOC.

## Neural/Prediction Quality
- Host-side predictNextWords test exists; add small UI polish/normalization if desired.

## Immediate Next Steps
1) Continue legacy cleanup (remaining resources/tasks; occasional wording fixes). Market “leave app store” dialog strings are now rebranded across locales; permission rationales were already debranded.
2) Keep `askCompat` flavor for seamless legacy add-on compatibility; flavor now uses a distinct (green) launcher background for easy identification. Add more overlays/icons only if further differentiation is needed.
3) Pick the next monolith slice from the audit list and refactor with tests (remaining top targets: AnySoftKeyboard*, AnyKeyboardViewBase/PointerTracker, Dictionary/BTreeDictionary).
