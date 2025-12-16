# Monolith Inventory (LOC by file)

Generated from `wc -l` over *.java and *.kt. Focus on files ≥500 LOC.

| LOC | File |
| ---:| --- |
| 1538 | ime/app/src/main/java/com/anysoftkeyboard/AnySoftKeyboard.java |
| 1296 | ime/app/src/main/java/com/anysoftkeyboard/keyboards/AnyKeyboard.java |
| 1292 | ime/dictionaries/src/main/java/com/anysoftkeyboard/dictionaries/BaseCharactersTable.java* |
|  982 | ime/app/src/main/java/com/anysoftkeyboard/keyboards/views/AnyKeyboardViewBase.java |
|  978 | ime/app/src/main/java/com/anysoftkeyboard/ime/AnySoftKeyboardSuggestions.java |
| 1047 | ime/app/src/main/java/com/anysoftkeyboard/keyboards/KeyboardSwitcher.java |
| 1037 | ime/app/src/main/java/com/anysoftkeyboard/keyboards/Keyboard.java |
|  849 | ime/app/src/main/java/com/anysoftkeyboard/dictionaries/SuggestionsProvider.java |
|  809 | ime/addons/src/main/java/com/anysoftkeyboard/addons/AddOnsFactory.java |
|  632 | ime/app/src/main/java/com/anysoftkeyboard/keyboards/views/CandidateView.java |
|  604 | ime/app/src/main/java/com/anysoftkeyboard/keyboards/views/PointerTracker.java |
|  590 | ime/app/src/main/java/com/anysoftkeyboard/dictionaries/SuggestImpl.java |
|  562 | ime/app/src/main/java/com/anysoftkeyboard/ime/AnySoftKeyboardWithGestureTyping.java |
|  539 | ime/app/src/main/java/com/anysoftkeyboard/keyboards/ExternalAnyKeyboard.java |

Tests ≥500 LOC (for awareness): AnySoftKeyboard* tests, SuggestionProviderTest, gesture/clipboard/quicktext suites, etc. Full list available via `python tools/loc_monolith.py` (not yet added).

Shrunk below 500 LOC (recent):
- MainFragment.java: 445 LOC (add-on cards + backup/restore helpers moved out)
- NextWordSettingsFragment.java: ~355 LOC (usage stats, summaries, clear-data helpers)

## Suggested Refactor Order
1) InputConnection/Editor state seam: `AnySoftKeyboard.java`, `AnySoftKeyboardSuggestions.java`.
2) Touch handling: `AnyKeyboardViewBase.java` + `PointerTracker.java` (extract TouchDispatcher).
3) Suggestion orchestration: `SuggestionsProvider.java` (split engine orchestration vs. normalization already started).
4) Keyboard model/core: `AnyKeyboard.java`, `Keyboard.java`, `KeyboardSwitcher.java` (pull parsing/layout concerns apart).
5) Dictionary core: `Dictionary.java` (separate trie/B-tree logic from lifecycle).
6) Settings UI: `MainFragment.java`, `NextWordSettingsFragment.java` (view vs. data helpers).

Keep behavior identical; add light tests per extraction. Preserve ASK compatibility surfaces while refactoring.

*BaseCharactersTable is data-only (BASE_CHARS) lifted out of Dictionary to shrink the logic file.*

Recent extractions:
- InputConnectionRouter (AnySoftKeyboard*).
- TouchDispatcher (AnyKeyboardViewBase/PointerTracker) now owns touch gating flags.
- PointerTrackerRegistry (PointerTracker ownership/iteration).
- KeyPressTimingHandler extracted from AnyKeyboardViewBase.
- PointerConfigLoader binds long-press/multitap prefs to shared pointer config.
- TextWidthCache caches label measurement.
- InvalidateTracker handles dirty-rect bookkeeping for AnyKeyboardViewBase.
- SwipeConfiguration owns swipe thresholds and recomputation per keyboard.
- KeyLabelAdjuster holds shift/function label logic used by the view.
- ProximityCalculator computes proximity threshold outside the view.
- HintLayoutCalculator handles hint icon/text placement math.
- KeyPreviewManagerFacade wraps preview show/dismiss logic.
- KeyIconResolver holds icon builders/cache and keycode-based lookup.
- DirtyRegionDecider chooses single-key vs. full redraw based on clip/invalidated key.
- ActionIconStateSetter applies IME action states to enter icons.
- LabelPaintConfigurator owns label/key paint sizing and typeface setup.
- SpecialKeyLabelProvider supplies fallback labels for special keys.
- KeyboardNameRenderer substitutes the space-key label when showing keyboard name.
- KeyHintRenderer draws hint text/icons using HintLayoutCalculator placement.
- KeyLabelRenderer draws key labels (text sizing, emoji scaling, StaticLayout handling).
- KeyIconDrawer draws centered icons and guesses labels when no icon is available.
- KeyTextColorResolver computes per-key text color with modifier state handling.
- Render helper list captured in docs/render-helpers.md for future slices.
- VoiceUiHelper centralizes voice key/status UI updates out of AnySoftKeyboard.
- StatusIconHelper wraps show/hide of the status icon away from AnySoftKeyboard.
- KeyDrawHelper renders the per-key draw loop outside AnyKeyboardViewBase; renderKeyboard helper wraps pre-draw setup.
- ClipDecider wraps clip-region bookkeeping for single-key vs full redraw decisions.
- DrawInvalidationHelper wraps dirty-rect/invalidate tracking; RenderSetup bundles per-frame draw inputs.
- DrawInputsBuilder builds DrawInputs snapshot outside AnyKeyboardViewBase on draw.
- ThemeAttributeLoaderRunner centralizes theme attribute application for theme/overlay changes.
- KeyboardMeasureHelper encapsulates onMeasure sizing math outside AnyKeyboardViewBase.
- ImeActionTypeResolver resolves IME action type bitmask outside AnyKeyboardViewBase.
- InvalidateHelper wraps dirty-rect tracking outside AnyKeyboardViewBase.
- NextKeyboardNameResolver supplies default next-keyboard labels outside AnyKeyboardViewBase.
- SpecialKeyLookup encapsulates special-key icon/label lookup outside AnyKeyboardViewBase.
- SpecialKeyManager owns special-key icon/label resolution and apply flow.
- KeyPreviewInteractor wraps preview show/hide/dismiss wiring to shrink the view.
- KeyShadowStyle holds shadow color/radius/offset to reduce field clutter.
- KeyTextStyleState holds key/label/name/hint text sizes, styles, and hint alignments.
- KeyDisplayState holds draw-text toggle and keys height factor.
- AnimationLevelController owns the animation level subject.
- ShiftStateTracker owns last-shift flag used for multi-tap handling.
- KeyboardNameHintState holds show-name/show-hints toggles and hint gravity.
- KeyBackgroundPadding holds padding Rect to keep the view slimmer.
- ClipRegionHolder keeps the draw clip Rect out of the view class.
- ViewStyleState holds dim/vertical-correction floats outside the view fields.
- SpecialKeysApplier encapsulates special key icon/label application.
- KeyPreviewControllerBinder isolates preview controller wiring.
- KeyboardNameVisibilityDecider determines when to draw keyboard name and hints.
- CancelSuggestionsAction extracted from AnySoftKeyboardSuggestions to isolate strip action logic.
- CompletionHandler extracted from AnySoftKeyboardSuggestions to encapsulate editor-provided completions.
- SentenceSeparators helper extracted to manage per-keyboard separator sets.
- SpaceTimeTracker now owns double-space/swap timing bookkeeping.
- WordRestartHelper extracted to rebuild composing word after cursor moves.
- SeparatorOutputHandler extracted to handle double-space period and punctuation/space swapping.
- SeparatorActionHelper now owns separator handling (auto-pick/commit/swap + output dispatch),
  trimming `AnySoftKeyboardSuggestions`.
- PredictionState now holds the suggestion/prediction flags; `SuggestionPickerHost`/service route
  auto-complete checks through it.
- LastKeyTracker owns last-key/code tracking for suggestion flows.
- ShiftStateTracker owns last-shift flag used for multi-tap handling.
- SelectionUpdateHost extracted from AnySoftKeyboardSuggestions to slim onUpdateSelection host logic.
- StatusIconController owns status-icon visibility outside the service.
- FullscreenModeDecider isolates fullscreen decision logic.
- MultiTapEditCoordinator wraps multi-tap batch edit lifecycle.
- PackageBroadcastRegistrar registers/unregisters package and user-unlock receivers.
- VoiceInputController encapsulates VoiceRecognitionTrigger callbacks.
- WindowAnimationSetter applies IME window animations from prefs.
- CondenseModeManager owns split/condensed mode selection and orientation prefs.
- EmojiSearchController now owns emoji search overlay show/handle/dismiss logic.
- KeyboardSwitchHandler handles keyboard-cycle/split/utility switching logic.
- NavigationKeyHandler handles DPAD/home/end/page navigation keys (including Fn combos).
- SuggestionSettingsController centralizes suggestion prefs + correction tuning.
- AddToDictionaryDecider encapsulates hint gating after manual suggestion picks.
- CursorTouchChecker encapsulates cursor-neighbor checks for suggestion logic.
- SuggestionCommitter encapsulates committing picked suggestions to the input connection.
- SuggestionPicker wraps manual pick flow (auto-space, add-to-dictionary, next suggestions).
- SuggestionsUpdater encapsulates delayed scheduling of suggestion refreshes.
- SuggestionRefresher wraps suggestion retrieval/highlighting.
- DictionaryLoaderHelper loads per-keyboard dictionaries outside AnySoftKeyboardSuggestions.
- SelectionExpectationTracker tracks selection-update waits instead of storing raw timestamps.
- UserDictionaryWorker handles async add/remove user-dictionary operations.
- AddToDictionaryHintController encapsulates post-pick hint/next-suggestions flow.
- SpaceSwapDecider owns swap-character decision logic (French punctuation rules + legacy paren).
- TypingSimulator simulates key-by-key typing for injected text.
- WordRestartCoordinator orchestrates restart-after-cursor-move using WordRestartHelper.
- SeparatorHandler owns separator/space/punctuation flow and next-word suggestion follow-up.
- PredictionStateUpdater centralizes prediction/power gating and suggestion settings application.
- CharacterInputHandler owns character key handling, composing updates, and prediction-aware output.
- TextInputDispatcher owns onText/onTyping text injection, keeping suggestion host lean.
- AddToDictionaryHintHost/UserDictionaryHost wrap hint + user-dictionary callbacks outside the host.
- CompletionHostAdapter wraps completion display callbacks.
- ThemeAttributeLoader owns theme/icon attribute parsing for AnyKeyboardViewBase.
- SpecialKeyAppearanceUpdater sets enter/mode icons & labels outside AnyKeyboardViewBase.
- PreviewPopupPresenter owns preview popup show/hide logic.
- KeyboardThemeHost now bridges theme loading callbacks out of AnyKeyboardViewBase.
- KeyDrawHelper renders per-key draw loop outside AnyKeyboardViewBase.
- ThemeHost extracted from AnyKeyboardViewBase to own ThemeAttributeLoader callbacks.
- LongPressHelper handles long-press tag toast + code dispatch outside AnyKeyboardViewBase.
- KeyboardSetter now owns keyboard apply/reset logic; AnyKeyboardViewBase delegates setKeyboard to it.
- KeyboardSetterHostImpl carries the host wiring out of AnyKeyboardViewBase.
- ThemeOverrideApplier interprets theme case/hint overrides outside AnyKeyboardViewBase.
- SwipeThresholdApplier owns swipe threshold updates and recomputation triggers.
- KeyboardNameHintController + KeyboardNameHintState now own show-name/show-hints/gravity flags outside AnyKeyboardViewBase.
- KeyPreviewControllerBinder wires preview controllers without bloating AnyKeyboardViewBase.
- PointerTrackerAccessor centralizes tracker lookup/wiring.
- InputResetter encapsulates preview/touch cleanup for reset/detach paths.
- KeyLookup handles primary-code lookup for special-key appearance.
- KeyboardActionListenerHolder centralizes listener storage (field removed).
- KeyboardMeasureHelper encapsulates onMeasure sizing math outside AnyKeyboardViewBase.
- Removed unused pointer wrappers in AnyKeyboardViewBase to keep surface minimal.
- NextWordUsageStatsLoader pulls next-word usage stats UI out of NextWordSettingsFragment.
- NextWordPreferenceSummaries builds summaries/failure messages for NextWordSettingsFragment.
- NextWordDataCleaner encapsulates next-word clear-data flow.
- PointerActionDispatcher owns pointer down/up/cancel sequencing.
- ModifierKeyEventHelper moves Fn/Alt combination handling out of AnySoftKeyboard.
- SelectionEditHelper holds selection wrap/case toggle logic out of AnySoftKeyboard.
- DeleteActionHelper encapsulates backspace/forward-delete flows from AnySoftKeyboard.
- SpecialWrapHelper centralizes wrap-character pairs (quote/parens) outside AnySoftKeyboard.
- AddOnUICardViewFactory builds add-on cards outside MainFragment.
- AddOnUICardPresenter handles add-on card rendering loop outside MainFragment.
- AddOnLinkNavigator handles add-on link routing/navigation outside MainFragment (wired).
- BackupRestoreLauncher handles backup/restore chooser + execution outside MainFragment (wired).
- WordRevertHandler encapsulates revert-last-word logic from AnySoftKeyboardSuggestions.
- FrenchSpacePunctuationDecider computes FR space/punctuation behavior.
- DictionaryLoadState tracks per-keyboard dictionary load state.
