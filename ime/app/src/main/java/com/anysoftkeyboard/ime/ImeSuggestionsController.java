package com.anysoftkeyboard.ime;

import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.dictionaries.DictionaryBackgroundLoader;
import com.anysoftkeyboard.dictionaries.Suggest;
import com.anysoftkeyboard.dictionaries.SuggestImpl;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.keyboards.views.CandidateView;
import com.anysoftkeyboard.keyboards.views.CandidateViewHost;
import com.anysoftkeyboard.keyboards.views.InputViewBinder;
import com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView;
import com.menny.android.anysoftkeyboard.BuildConfig;
import com.menny.android.anysoftkeyboard.R;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("this-escape")
public abstract class ImeSuggestionsController extends ImeKeyboardSwitchedListener
    implements CandidateViewHost {

  @VisibleForTesting public static final long MAX_TIME_TO_EXPECT_SELECTION_UPDATE = 1500;
  private static final long CLOSE_DICTIONARIES_DELAY = 10 * ONE_FRAME_DELAY;
  private static final long NEVER_TIME_STAMP = -1L * 365L * 24L * 60L * 60L * 1000L; // a year ago.
  @VisibleForTesting public static final long GET_SUGGESTIONS_DELAY = 5 * ONE_FRAME_DELAY;

  @VisibleForTesting
  final KeyboardUIStateHandler mKeyboardHandler = new KeyboardUIStateHandler(this);

  private final SuggestionsSessionState suggestionsSessionState =
      new SuggestionsSessionState(NEVER_TIME_STAMP);
  Suggest mSuggest;
  CandidateView mCandidateView;
  private boolean mFrenchSpacePunctuationBehavior;
  private final KeyboardDictionariesLoader keyboardDictionariesLoader =
      new KeyboardDictionariesLoader();

  @VisibleForTesting
  final CancelSuggestionsAction mCancelSuggestionsAction =
      new CancelSuggestionsAction(() -> abortCorrectionAndResetPredictionState(true));

  private final InputFieldConfigurator inputFieldConfigurator = new InputFieldConfigurator();
  private final SelectionUpdateProcessor selectionUpdateProcessor = new SelectionUpdateProcessor();
  private SuggestionStripController suggestionStripController;
  final CompletionHandler completionHandler = new CompletionHandler();
  private final WordRestartCoordinator wordRestartCoordinator = new WordRestartCoordinator();
  private final SeparatorOutputHandler separatorOutputHandler = new SeparatorOutputHandler();
  private final CursorTouchChecker cursorTouchChecker = new CursorTouchChecker();
  private final WordRestartGate wordRestartGate = new WordRestartGate();
  private final SuggestionCommitter suggestionCommitter =
      new SuggestionCommitter(new SuggestionCommitterHost(this));
  private final SuggestionPicker suggestionPicker =
      new SuggestionPicker(new SuggestionPickerHost(this));
  private final SuggestionRefresher suggestionRefresher = new SuggestionRefresher();
  private final SpaceSwapDecider spaceSwapDecider = new SpaceSwapDecider();
  private final SuggestionsUpdater suggestionsUpdater =
      new SuggestionsUpdater(
          mKeyboardHandler,
          this::performUpdateSuggestions,
          GET_SUGGESTIONS_DELAY,
          KeyboardUIStateHandler.MSG_UPDATE_SUGGESTIONS);
  private final SuggestionSettingsController suggestionSettingsController =
      new SuggestionSettingsController();
  private final WordRevertHandler wordRevertHandler = new WordRevertHandler();
  private final UserDictionaryWorker userDictionaryWorker =
      new UserDictionaryWorker(new UserDictionaryHost(() -> mSuggest, () -> mCandidateView));
  private final AddToDictionaryHintController addToDictionaryHintController =
      new AddToDictionaryHintController(
          new AddToDictionaryHintHost(
              () -> mCandidateView,
              () -> mSuggest,
              this::getCurrentAlphabetKeyboard,
              (suggestions, highlightedIndex) ->
                  ImeSuggestionsController.this.setSuggestions(suggestions, highlightedIndex)));
  private final TypingSimulator typingSimulator = new TypingSimulator();
  private final PredictionGate predictionGate = new PredictionGate();
  private final SeparatorHandler separatorHandler = new SeparatorHandler();
  private final SeparatorHandlerHost separatorHandlerHost =
      new SeparatorHandlerHost(
          this,
          suggestionsSessionState.spaceTimeTracker,
          separatorOutputHandler,
          suggestionsSessionState.sentenceSeparators,
          suggestionsSessionState.predictionState);
  private final PredictionStateUpdater predictionStateUpdater = new PredictionStateUpdater();
  private final CharacterInputHandler characterInputHandler = new CharacterInputHandler();
  private final TextInputDispatcher textInputDispatcher = new TextInputDispatcher(typingSimulator);
  private final CharacterInputHandler.Host characterInputHost =
      new CharacterInputHost(
          this,
          suggestionsSessionState.autoCorrectState,
          suggestionsSessionState.predictionState,
          suggestionsSessionState.shiftStateTracker);
  private final TextInputDispatcher.Host textInputHost =
      new TextInputHost(
          this, suggestionsSessionState.autoCorrectState, suggestionsSessionState.predictionState);
  private final WordRestartCoordinator.Host wordRestartHost = new WordRestartHost(this);
  private final SuggestionRefresher.Host suggestionRefresherHost =
      new SuggestionRefresherHost(this);
  private final WordRevertHandler.Host wordRevertHost = new WordRevertHost(this);

  @Nullable
  protected Keyboard.Key getLastUsedKey() {
    return suggestionsSessionState.lastKeyTracker.lastKey();
  }

  void setAllowSuggestionsRestart(boolean allow) {
    suggestionsSessionState.predictionState.allowSuggestionsRestart = allow;
  }

  void applySuggestionSettings(
      boolean showSuggestions,
      boolean autoComplete,
      int commonalityMaxLengthDiff,
      int commonalityMaxDistance,
      boolean trySplitting) {
    predictionStateUpdater.applySuggestionSettings(
        suggestionsSessionState.predictionState,
        mSuggest,
        showSuggestions,
        autoComplete,
        commonalityMaxLengthDiff,
        commonalityMaxDistance,
        trySplitting,
        keyboardDictionariesLoader::reset,
        this::setDictionariesForCurrentKeyboard,
        this::closeDictionaries);
  }

  @Override
  public void onCreate() {
    super.onCreate();

    mSuggest = createSuggest();

    if (BuildConfig.TESTING_BUILD) {
      try {
        // expose a tiny test-only API to help instrumentation seed context
        com.anysoftkeyboard.ime.ImeTestApi.setService(this);
      } catch (Throwable ignore) {
        // class not present in release builds
      }
    }

    suggestionSettingsController.attach(this, mSuggest);
    // apply current prefs synchronously so tests have suggestions before async streams emit
    suggestionSettingsController.applySnapshot(this, mSuggest);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mKeyboardHandler.removeAllMessages();
    mSuggest.destroy();
  }

  @Override
  public void onStartInput(EditorInfo attribute, boolean restarting) {
    super.onStartInput(attribute, restarting);
    // removing close request (if it was asked for a previous onFinishInput).
    mKeyboardHandler.removeMessages(KeyboardUIStateHandler.MSG_CLOSE_DICTIONARIES);

    abortCorrectionAndResetPredictionState(false);
    keyboardDictionariesLoader.reset();
  }

  @Override
  public void onStartInputView(final EditorInfo attribute, final boolean restarting) {
    super.onStartInputView(attribute, restarting);

    suggestionsSessionState.predictionState.predictionOn = false;
    keyboardDictionariesLoader.reset();
    completionHandler.reset();
    suggestionsSessionState.predictionState.inputFieldSupportsAutoPick = false;

    InputFieldConfigurator.Result inputConfig =
        inputFieldConfigurator.configure(
            attribute, restarting, getKeyboardSwitcher(), mPrefsAutoSpace, TAG);

    predictionStateUpdater.applyInputFieldConfig(
        suggestionsSessionState.predictionState, inputConfig, predictionGate);

    mCancelSuggestionsAction.setCancelIconVisible(false);
    suggestionStripController.attachToStrip(getInputViewContainer());
    getInputViewContainer().setActionsStripVisibility(isPredictionOn());
    clearSuggestions();
    setDictionariesForCurrentKeyboard();
  }

  @Override
  public void onFinishInput() {
    super.onFinishInput();
    mCancelSuggestionsAction.setCancelIconVisible(false);
    suggestionsSessionState.predictionState.predictionOn = false;
    mKeyboardHandler.sendEmptyMessageDelayed(
        KeyboardUIStateHandler.MSG_CLOSE_DICTIONARIES, CLOSE_DICTIONARIES_DELAY);
    suggestionsSessionState.selectionExpectationTracker.clear();
    keyboardDictionariesLoader.reset();
  }

  @Override
  public void onFinishInputView(boolean finishingInput) {
    super.onFinishInputView(finishingInput);
    abortCorrectionAndResetPredictionState(true);
  }

  /*
   * this function is called EVERY TIME them selection is changed. This also
   * includes the underlined suggestions.
   */
  @Override
  public void onUpdateSelection(
      int oldSelStart,
      int oldSelEnd,
      int newSelStart,
      int newSelEnd,
      int candidatesStart,
      int candidatesEnd) {
    final int oldCandidateStart = getCandidateStartPositionDangerous();
    final int oldCandidateEnd = getCandidateEndPositionDangerous();
    super.onUpdateSelection(
        oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);

    selectionUpdateProcessor.onUpdateSelection(
        oldSelStart,
        oldSelEnd,
        newSelStart,
        newSelEnd,
        candidatesStart,
        candidatesEnd,
        new SelectionUpdateHost(this, oldCandidateStart, oldCandidateEnd));
  }

  @Override
  public View onCreateInputView() {
    final View view = super.onCreateInputView();
    mCandidateView = getInputViewContainer().getCandidateView();
    mCancelSuggestionsAction.setOwningCandidateView(mCandidateView);
    suggestionStripController =
        new SuggestionStripController(mCancelSuggestionsAction, mCandidateView);
    suggestionStripController.setHost(this);
    return view;
  }

  protected WordComposer getCurrentComposedWord() {
    return suggestionsSessionState.wordComposerTracker.currentWord();
  }

  @Override
  @CallSuper
  public void onKey(
      int primaryCode, Keyboard.Key key, int multiTapIndex, int[] nearByKeyCodes, boolean fromUI) {
    suggestionsSessionState.lastKeyTracker.record(key, primaryCode);
    super.onKey(primaryCode, key, multiTapIndex, nearByKeyCodes, fromUI);
    if (primaryCode != KeyCodes.DELETE) {
      suggestionsSessionState.autoCorrectState.wordRevertLength = 0;
    }
    mCandidateView.dismissAddToDictionaryHint();
  }

  protected void resetLastPressedKey() {
    suggestionsSessionState.lastKeyTracker.reset();
  }

  @Override
  public void onRelease(int primaryCode) {
    // not allowing undo on-text in clipboard paste operations.
    if (primaryCode == KeyCodes.CLIPBOARD_PASTE)
      suggestionsSessionState.autoCorrectState.wordRevertLength = 0;
    if (suggestionsSessionState.lastKeyTracker.shouldMarkSpaceTime(primaryCode)) {
      setSpaceTimeStamp(primaryCode == KeyCodes.SPACE);
    }
    if (!isCurrentlyPredicting()
        && (primaryCode == KeyCodes.DELETE
            || primaryCode == KeyCodes.DELETE_WORD
            || primaryCode == KeyCodes.FORWARD_DELETE)) {
      postRestartWordSuggestion();
    }
  }

  protected void postRestartWordSuggestion() {
    mKeyboardHandler.removeMessages(KeyboardUIStateHandler.MSG_UPDATE_SUGGESTIONS);
    mKeyboardHandler.removeMessages(KeyboardUIStateHandler.MSG_RESTART_NEW_WORD_SUGGESTIONS);
    mKeyboardHandler.sendEmptyMessageDelayed(
        KeyboardUIStateHandler.MSG_RESTART_NEW_WORD_SUGGESTIONS, 10 * ONE_FRAME_DELAY);
  }

  @Override
  @CallSuper
  public void onMultiTapStarted() {
    final InputViewBinder inputView = getInputView();
    if (inputView != null) {
      inputView.setShifted(suggestionsSessionState.shiftStateTracker.lastCharacterWasShifted());
    }
  }

  @Override
  protected boolean isSelectionUpdateDelayed() {
    return suggestionsSessionState.selectionExpectationTracker.isExpecting();
  }

  protected boolean shouldRevertOnDelete() {
    return suggestionsSessionState.autoCorrectState.shouldRevertOnDelete();
  }

  void clearSpaceTimeTracker() {
    suggestionsSessionState.spaceTimeTracker.clear();
  }

  long getExpectingSelectionUpdateBy() {
    return suggestionsSessionState.selectionExpectationTracker.getExpectingSelectionUpdateBy();
  }

  void clearExpectingSelectionUpdate() {
    suggestionsSessionState.selectionExpectationTracker.clear();
  }

  void setExpectingSelectionUpdateBy(long value) {
    suggestionsSessionState.selectionExpectationTracker.setExpectingSelectionUpdateBy(value);
  }

  WordComposer getCurrentWord() {
    return suggestionsSessionState.wordComposerTracker.currentWord();
  }

  void setPreviousWord(@NonNull WordComposer word) {
    suggestionsSessionState.wordComposerTracker.setPreviousWord(word);
  }

  public void setWordRevertLength(int length) {
    suggestionsSessionState.autoCorrectState.wordRevertLength = length;
  }

  int getWordRevertLength() {
    return suggestionsSessionState.autoCorrectState.wordRevertLength;
  }

  protected void handleCharacter(
      final int primaryCode,
      final Keyboard.Key key,
      final int multiTapIndex,
      int[] nearByKeyCodes) {
    characterInputHandler.handleCharacter(
        primaryCode, key, multiTapIndex, nearByKeyCodes, TAG, characterInputHost);
  }

  // Make sure to call this BEFORE actually making changes, and not after.
  // the event might arrive immediately as changes occur.
  public void markExpectingSelectionUpdate() {
    suggestionsSessionState.selectionExpectationTracker.markExpectingUntil(
        SystemClock.uptimeMillis() + MAX_TIME_TO_EXPECT_SELECTION_UPDATE);
  }

  public void handleSeparator(int primaryCode) {
    if (BuildConfig.DEBUG) {
      Logger.d(
          TAG,
          "handleSeparator code=%d isSpace=%s lastSpace=%s swapCandidate=%s",
          primaryCode,
          primaryCode == KeyCodes.SPACE,
          suggestionsSessionState.spaceTimeTracker.hadSpace(),
          isSpaceSwapCharacter(primaryCode));
    }

    separatorHandler.handleSeparator(primaryCode, separatorHandlerHost);
  }

  @NonNull
  public WordComposer prepareWordComposerForNextWord() {
    return suggestionsSessionState.wordComposerTracker.prepareWordComposerForNextWord();
  }

  public boolean isSpaceSwapCharacter(int primaryCode) {
    return spaceSwapDecider.isSpaceSwapCharacter(
        primaryCode, mFrenchSpacePunctuationBehavior, suggestionsSessionState.sentenceSeparators);
  }

  public void performRestartWordSuggestion() {
    mKeyboardHandler.removeMessages(KeyboardUIStateHandler.MSG_RESTART_NEW_WORD_SUGGESTIONS);
    mKeyboardHandler.removeMessages(KeyboardUIStateHandler.MSG_UPDATE_SUGGESTIONS);

    wordRestartCoordinator.performRestartWordSuggestion(
        getImeSessionState().getInputConnectionRouter(), wordRestartHost);
  }

  @Override
  public void onText(Keyboard.Key key, CharSequence text) {
    textInputDispatcher.onText(text, textInputHost, TAG);
  }

  @Override
  public void onTyping(Keyboard.Key key, CharSequence text) {
    textInputDispatcher.onTyping(key, text, textInputHost, TAG);
  }

  protected void setDictionariesForCurrentKeyboard() {
    if (keyboardDictionariesLoader.isLoaded()) return;

    mSuggest.resetNextWordSentence();

    keyboardDictionariesLoader.ensureLoaded(
        this,
        suggestionsSessionState.predictionState,
        shouldLoadDictionariesForGestureTyping(),
        getCurrentAlphabetKeyboard(),
        isInAlphabetKeyboardMode(),
        suggestionsSessionState.sentenceSeparators,
        mSuggest,
        this::getDictionaryLoadedListener);
  }

  @NonNull
  protected DictionaryBackgroundLoader.Listener getDictionaryLoadedListener(
      @NonNull KeyboardDefinition currentAlphabetKeyboard) {
    return DictionaryBackgroundLoader.SILENT_LISTENER;
  }

  /**
   * Allows subclasses (e.g., gesture typing) to force dictionary loading even when predictions are
   * off.
   */
  protected boolean shouldLoadDictionariesForGestureTyping() {
    return false;
  }

  @Override
  protected void onOrientationChanged(int oldOrientation, int newOrientation) {
    super.onOrientationChanged(oldOrientation, newOrientation);
    abortCorrectionAndResetPredictionState(false);

    String sentenceSeparatorsForCurrentKeyboard =
        getKeyboardSwitcher().getCurrentKeyboardSentenceSeparators();
    if (sentenceSeparatorsForCurrentKeyboard == null) {
      suggestionsSessionState.sentenceSeparators.clear();
    } else {
      suggestionsSessionState.sentenceSeparators.updateFrom(
          sentenceSeparatorsForCurrentKeyboard.toCharArray());
    }
  }

  @CallSuper
  public void abortCorrectionAndResetPredictionState(boolean disabledUntilNextInputStart) {
    mSuggest.resetNextWordSentence();

    suggestionsSessionState.spaceTimeTracker.clear();
    suggestionsSessionState.autoCorrectState.justAutoAddedWord = false;
    mKeyboardHandler.removeAllSuggestionMessages();

    markExpectingSelectionUpdate();
    getImeSessionState().getInputConnectionRouter().finishComposingText();

    clearSuggestions();

    suggestionsSessionState.wordComposerTracker.resetCurrentWord();
    suggestionsSessionState.autoCorrectState.reset();
    if (disabledUntilNextInputStart) {
      Logger.d(TAG, "abortCorrection will abort correct forever");
      final KeyboardViewContainerView inputViewContainer = getInputViewContainer();
      if (inputViewContainer != null) {
        inputViewContainer.removeStripAction(mCancelSuggestionsAction);
      }
      suggestionsSessionState.predictionState.predictionOn = false;
    }
  }

  /** Allows subclasses to force a reload of keyboard dictionaries. */
  protected void invalidateDictionariesForCurrentKeyboard() {
    keyboardDictionariesLoader.reset();
  }

  public void clearSuggestions() {
    mKeyboardHandler.removeAllSuggestionMessages();
    setSuggestions(Collections.emptyList(), -1);
  }

  public void setSuggestions(
      @NonNull List<? extends CharSequence> suggestions, int highlightedSuggestionIndex) {
    mCancelSuggestionsAction.setCancelIconVisible(!suggestions.isEmpty());
    if (mCandidateView != null) {
      mCandidateView.setSuggestions(suggestions, highlightedSuggestionIndex);
    }
  }

  Suggest getSuggestForTests() {
    return mSuggest;
  }

  CandidateView getCandidateViewForTests() {
    return mCandidateView;
  }

  @NonNull
  protected Suggest getSuggest() {
    return mSuggest;
  }

  public Suggest suggest() {
    return mSuggest;
  }

  @Override
  @NonNull
  protected List<Drawable> generateWatermark() {
    final List<Drawable> watermark = super.generateWatermark();
    if (mSuggest.isIncognitoMode()) {
      watermark.add(ContextCompat.getDrawable(this, R.drawable.ic_watermark_incognito));
    }
    return watermark;
  }

  @NonNull
  protected Suggest createSuggest() {
    return new SuggestImpl(this);
  }

  protected abstract boolean isAlphabet(int code);

  public void addWordToDictionary(String word) {
    userDictionaryWorker.addWordToDictionary(word, mInputSessionDisposables::add);
  }

  /** posts an update suggestions request to the messages queue. Removes any previous request. */
  protected void postUpdateSuggestions() {
    suggestionsUpdater.postUpdateSuggestions();
  }

  protected boolean isPredictionOn() {
    return suggestionsSessionState.predictionState.isPredictionOn();
  }

  public boolean isAutoCorrect() {
    return suggestionsSessionState.predictionState.isAutoCorrect();
  }

  public boolean isCurrentlyPredicting() {
    return isPredictionOn() && !suggestionsSessionState.wordComposerTracker.currentWord().isEmpty();
  }

  boolean isAutoCompleteEnabled() {
    return suggestionsSessionState.predictionState.autoComplete;
  }

  public void performUpdateSuggestions() {
    mKeyboardHandler.removeMessages(KeyboardUIStateHandler.MSG_UPDATE_SUGGESTIONS);

    suggestionRefresher.performUpdateSuggestions(
        suggestionsSessionState.predictionState,
        suggestionsSessionState.wordComposerTracker.currentWord(),
        mSuggest,
        suggestionRefresherHost);
  }

  public void pickSuggestionManually(int index, CharSequence suggestion) {
    pickSuggestionManually(index, suggestion, suggestionsSessionState.predictionState.autoSpace);
  }

  @CallSuper
  public void pickSuggestionManually(
      int index, CharSequence suggestion, boolean withAutoSpaceEnabled) {
    suggestionsSessionState.autoCorrectState.wordRevertLength = 0; // no reverts
    final WordComposer typedWord = prepareWordComposerForNextWord();

    suggestionPicker.pickSuggestionManually(
        typedWord,
        withAutoSpaceEnabled,
        index,
        suggestion,
        suggestionsSessionState.predictionState.showSuggestions,
        suggestionsSessionState.autoCorrectState.justAutoAddedWord,
        typedWord.isAtTagsSearchState());
  }

  /**
   * Commits the chosen word to the text field and saves it for later retrieval.
   *
   * @param wordToCommit the suggestion picked by the user to be committed to the text field
   * @param typedWord the word the user typed.
   */
  @CallSuper
  public void commitWordToInput(
      @NonNull CharSequence wordToCommit, @NonNull CharSequence typedWord) {
    suggestionCommitter.commitWordToInput(wordToCommit, typedWord);
  }

  protected boolean canRestartWordSuggestion() {
    final InputViewBinder inputView = getInputView();
    if (!wordRestartGate.canRestartWordSuggestion(
        isPredictionOn(),
        suggestionsSessionState.predictionState.allowSuggestionsRestart,
        inputView)) {
      return false;
    } else if (!isCursorTouchingWord()) {
      Logger.d(TAG, "User moved cursor to no-man land. Bye bye.");
      return false;
    }

    return true;
  }

  private boolean isCursorTouchingWord() {
    return cursorTouchChecker.isCursorTouchingWord(
        getImeSessionState().getInputConnectionRouter(), this::isWordSeparator);
  }

  protected void setSpaceTimeStamp(boolean isSpace) {
    if (isSpace) {
      suggestionsSessionState.spaceTimeTracker.markSpace();
    } else {
      suggestionsSessionState.spaceTimeTracker.clear();
    }
  }

  @Override
  public void onAlphabetKeyboardSet(@NonNull KeyboardDefinition keyboard) {
    super.onAlphabetKeyboardSet(keyboard);
    keyboardDictionariesLoader.reset();

    mFrenchSpacePunctuationBehavior =
        FrenchSpacePunctuationDecider.shouldEnable(mSwapPunctuationAndSpace, keyboard.getLocale());
  }

  public void revertLastWord() {
    WordRevertHandler.Result result =
        wordRevertHandler.revertLastWord(
            suggestionsSessionState.autoCorrectState,
            suggestionsSessionState.predictionState,
            suggestionsSessionState.wordComposerTracker.currentWord(),
            suggestionsSessionState.wordComposerTracker.previousWord(),
            wordRevertHost);
    suggestionsSessionState.wordComposerTracker.setWords(
        result.currentWord(), result.previousWord());
  }

  protected boolean isWordSeparator(int code) {
    return !isAlphabet(code);
  }

  public boolean preferCapitalization() {
    return suggestionsSessionState.wordComposerTracker.currentWord().isFirstCharCapitalized();
  }

  public void closeDictionaries() {
    mSuggest.closeDictionaries();
  }

  @Override
  public void onDisplayCompletions(CompletionInfo[] completions) {
    completionHandler.onDisplayCompletions(
        completions,
        new CompletionHostAdapter(
            this::isFullscreenMode,
            this::clearSuggestions,
            suggestions -> setSuggestions(suggestions.suggestions, suggestions.highlightedIndex),
            () ->
                suggestionsSessionState.wordComposerTracker.currentWord().setPreferredWord(null)));
  }

  public void checkAddToDictionaryWithAutoDictionary(
      CharSequence newWord, Suggest.AdditionType type) {
    suggestionsSessionState.autoCorrectState.justAutoAddedWord = false;

    // unfortunately, has to do it on the main-thread (because we checking mJustAutoAddedWord)
    if (mSuggest.tryToLearnNewWord(newWord, type)) {
      addWordToDictionary(newWord.toString());
      suggestionsSessionState.autoCorrectState.justAutoAddedWord = true;
    }
  }

  @CallSuper
  protected boolean isSuggestionAffectingCharacter(int code) {
    return Character.isLetter(code);
  }

  public void removeFromUserDictionary(String wordToRemove) {
    userDictionaryWorker.removeFromUserDictionary(wordToRemove, mInputSessionDisposables::add);
    suggestionsSessionState.autoCorrectState.justAutoAddedWord = false;
    abortCorrectionAndResetPredictionState(false);
  }

  AddToDictionaryHintController addToDictionaryHintController() {
    return addToDictionaryHintController;
  }
}
