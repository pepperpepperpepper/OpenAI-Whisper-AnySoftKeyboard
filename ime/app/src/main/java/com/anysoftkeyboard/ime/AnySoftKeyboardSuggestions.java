package com.anysoftkeyboard.ime;

import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import com.anysoftkeyboard.android.PowerSaving;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.dictionaries.Dictionary;
import com.anysoftkeyboard.dictionaries.DictionaryAddOnAndBuilder;
import com.anysoftkeyboard.dictionaries.DictionaryBackgroundLoader;
import com.anysoftkeyboard.dictionaries.Suggest;
import com.anysoftkeyboard.dictionaries.SuggestImpl;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardSwitcher;
import com.anysoftkeyboard.keyboards.views.CandidateView;
import com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView;
import com.anysoftkeyboard.rx.RxSchedulers;
import com.anysoftkeyboard.utils.Triple;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.BuildConfig;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.Observable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public abstract class AnySoftKeyboardSuggestions extends AnySoftKeyboardKeyboardSwitchedListener {

  @VisibleForTesting public static final long MAX_TIME_TO_EXPECT_SELECTION_UPDATE = 1500;
  private static final long CLOSE_DICTIONARIES_DELAY = 10 * ONE_FRAME_DELAY;
  private static final long NEVER_TIME_STAMP = -1L * 365L * 24L * 60L * 60L * 1000L; // a year ago.
  private static final DictionaryBackgroundLoader.Listener NO_OP_DICTIONARY_LOADER_LISTENER =
      new DictionaryBackgroundLoader.Listener() {

        @Override
        public void onDictionaryLoadingStarted(Dictionary dictionary) {}

        @Override
        public void onDictionaryLoadingDone(Dictionary dictionary) {}

        @Override
        public void onDictionaryLoadingFailed(Dictionary dictionary, Throwable exception) {}
      };
  @VisibleForTesting public static final long GET_SUGGESTIONS_DELAY = 5 * ONE_FRAME_DELAY;

  @VisibleForTesting
  final KeyboardUIStateHandler mKeyboardHandler = new KeyboardUIStateHandler(this);

  private final SentenceSeparators sentenceSeparators = new SentenceSeparators();

  private final AutoCorrectState autoCorrectState = new AutoCorrectState();
  private WordComposer mWord = new WordComposer();
  private WordComposer mPreviousWord = new WordComposer();
  Suggest mSuggest;
  CandidateView mCandidateView;
  private final SpaceTimeTracker spaceTimeTracker = new SpaceTimeTracker();
  @Nullable private Keyboard.Key mLastKey;
  private int mLastPrimaryKey = Integer.MIN_VALUE;
  private final SelectionExpectationTracker selectionExpectationTracker =
      new SelectionExpectationTracker(NEVER_TIME_STAMP);
  private boolean mLastCharacterWasShifted = false;
  private boolean mFrenchSpacePunctuationBehavior;
  private final PredictionState predictionState = new PredictionState();
  private final DictionaryLoadGate dictionaryLoadGate = new DictionaryLoadGate();
  private final DictionaryLoaderHelper dictionaryLoaderHelper = new DictionaryLoaderHelper();

  private boolean mDictionariesForCurrentKeyboardLoaded = false;

  @VisibleForTesting
  final CancelSuggestionsAction mCancelSuggestionsAction =
      new CancelSuggestionsAction(() -> abortCorrectionAndResetPredictionState(true));

  private final InputFieldConfigurator inputFieldConfigurator = new InputFieldConfigurator();
  private final SelectionUpdateProcessor selectionUpdateProcessor = new SelectionUpdateProcessor();
  private SuggestionStripController suggestionStripController;
  final CompletionHandler completionHandler = new CompletionHandler();
  private final WordRestartHelper wordRestartHelper = new WordRestartHelper();
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
      new UserDictionaryWorker(
          new UserDictionaryWorker.Host() {
            @Override
            public Suggest suggest() {
              return mSuggest;
            }

            @Override
            public CandidateView candidateView() {
              return mCandidateView;
            }
          });
  private final AddToDictionaryHintController addToDictionaryHintController =
      new AddToDictionaryHintController(
          new AddToDictionaryHintController.Host() {
            @Override
            public CandidateView candidateView() {
              return mCandidateView;
            }

            @Override
            public Suggest suggest() {
              return mSuggest;
            }

            @Override
            public AnyKeyboard currentAlphabetKeyboard() {
              return getCurrentAlphabetKeyboard();
            }

            @Override
            public void setSuggestions(
                List<CharSequence> suggestions, int highlightedIndex) {
              AnySoftKeyboardSuggestions.this.setSuggestions(suggestions, highlightedIndex);
            }
          });
  private final TypingSimulator typingSimulator = new TypingSimulator();
  private final PredictionGate predictionGate = new PredictionGate();

  @Nullable
  protected Keyboard.Key getLastUsedKey() {
    return mLastKey;
  }

  void setAllowSuggestionsRestart(boolean allow) {
    predictionState.allowSuggestionsRestart = allow;
  }

  void applySuggestionSettings(
      boolean showSuggestions,
      boolean autoComplete,
      int commonalityMaxLengthDiff,
      int commonalityMaxDistance,
      boolean trySplitting) {
    final boolean showChanged = predictionState.showSuggestions != showSuggestions;
    predictionState.showSuggestions = showSuggestions;
    if (showChanged && predictionState.showSuggestions) {
      mDictionariesForCurrentKeyboardLoaded = false;
    }
    predictionState.autoComplete = autoComplete;
    mSuggest.setCorrectionMode(
        predictionState.showSuggestions,
        commonalityMaxLengthDiff,
        commonalityMaxDistance,
        trySplitting);
    if (showChanged) {
      if (predictionState.showSuggestions) {
        setDictionariesForCurrentKeyboard();
      } else {
        closeDictionaries();
      }
    }
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
    mDictionariesForCurrentKeyboardLoaded = false;
  }

  @Override
  public void onStartInputView(final EditorInfo attribute, final boolean restarting) {
    super.onStartInputView(attribute, restarting);

    predictionState.predictionOn = false;
    mDictionariesForCurrentKeyboardLoaded = false;
    completionHandler.reset();
    predictionState.inputFieldSupportsAutoPick = false;

    InputFieldConfigurator.Result inputConfig =
        inputFieldConfigurator.configure(
            attribute, restarting, getKeyboardSwitcher(), mPrefsAutoSpace, TAG);

    predictionState.predictionOn = inputConfig.predictionOn;
    predictionState.inputFieldSupportsAutoPick = inputConfig.inputFieldSupportsAutoPick;
    predictionState.autoSpace = inputConfig.autoSpace;

    predictionState.predictionOn =
        predictionGate.shouldRunPrediction(
            predictionState.predictionOn, predictionState.showSuggestions);

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
    predictionState.predictionOn = false;
    mKeyboardHandler.sendEmptyMessageDelayed(
        KeyboardUIStateHandler.MSG_CLOSE_DICTIONARIES, CLOSE_DICTIONARIES_DELAY);
    selectionExpectationTracker.clear();
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
    mCandidateView.setService(this);
    mCancelSuggestionsAction.setOwningCandidateView(mCandidateView);
    suggestionStripController =
        new SuggestionStripController(mCancelSuggestionsAction, mCandidateView);
    return view;
  }

  protected WordComposer getCurrentComposedWord() {
    return mWord;
  }

  @Override
  @CallSuper
  public void onKey(
      int primaryCode, Keyboard.Key key, int multiTapIndex, int[] nearByKeyCodes, boolean fromUI) {
    mLastKey = key;
    mLastPrimaryKey = primaryCode;
    super.onKey(primaryCode, key, multiTapIndex, nearByKeyCodes, fromUI);
    if (primaryCode != KeyCodes.DELETE) {
      autoCorrectState.wordRevertLength = 0;
    }
    mCandidateView.dismissAddToDictionaryHint();
  }

  protected void resetLastPressedKey() {
    mLastKey = null;
  }

  @Override
  public void onRelease(int primaryCode) {
    // not allowing undo on-text in clipboard paste operations.
    if (primaryCode == KeyCodes.CLIPBOARD_PASTE) autoCorrectState.wordRevertLength = 0;
    if (mLastPrimaryKey == primaryCode && KeyCodes.isOutputKeyCode(primaryCode)) {
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
      inputView.setShifted(mLastCharacterWasShifted);
    }
  }

  @Override
  protected boolean isSelectionUpdateDelayed() {
    return selectionExpectationTracker.isExpecting();
  }

  protected boolean shouldRevertOnDelete() {
    return autoCorrectState.shouldRevertOnDelete();
  }

  void clearSpaceTimeTracker() {
    spaceTimeTracker.clear();
  }

  long getExpectingSelectionUpdateBy() {
    return selectionExpectationTracker.getExpectingSelectionUpdateBy();
  }

  void clearExpectingSelectionUpdate() {
    selectionExpectationTracker.clear();
  }

  void setExpectingSelectionUpdateBy(long value) {
    selectionExpectationTracker.setExpectingSelectionUpdateBy(value);
  }

  @Override
  protected InputConnection currentInputConnection() {
    return mInputConnectionRouter.current();
  }

  WordComposer getCurrentWord() {
    return mWord;
  }

  void setWordRevertLength(int length) {
    autoCorrectState.wordRevertLength = length;
  }

  int getWordRevertLength() {
    return autoCorrectState.wordRevertLength;
  }

  protected void handleCharacter(
      final int primaryCode,
      final Keyboard.Key key,
      final int multiTapIndex,
      int[] nearByKeyCodes) {
    if (BuildConfig.DEBUG) {
      Logger.d(
          TAG,
          "handleCharacter: %d, isPredictionOn: %s, isCurrentlyPredicting: %s",
          primaryCode,
          isPredictionOn(),
          isCurrentlyPredicting());
    }

    if (mWord.charCount() == 0 && isAlphabet(primaryCode)) {
      autoCorrectState.wordRevertLength = 0;
      mWord.reset();
    predictionState.autoCorrectOn =
        isPredictionOn() && predictionState.autoComplete && predictionState.inputFieldSupportsAutoPick;
      if (mShiftKeyState.isActive()) {
        mWord.setFirstCharCapitalized(true);
      }
    }

    mLastCharacterWasShifted = (getInputView() != null) && getInputView().isShifted();

    final InputConnection ic = mInputConnectionRouter.current();
    mWord.add(primaryCode, nearByKeyCodes);
    if (isPredictionOn()) {
      if (ic != null) {
        int newCursorPosition = computeCursorPositionAfterChar(primaryCode, key, multiTapIndex);
        if (newCursorPosition > 0) {
          ic.beginBatchEdit();
        }

        markExpectingSelectionUpdate();
        ic.setComposingText(mWord.getTypedWord(), 1);
        if (newCursorPosition > 0) {
          ic.setSelection(newCursorPosition, newCursorPosition);
          ic.endBatchEdit();
        }
      }
      // this should be done ONLY if the key is a letter, and not a inner
      // character (like ').
      if (isSuggestionAffectingCharacter(primaryCode)) {
        if (!isPredictionOn()) {
          // Even when predictions are disabled (e.g., power‑saving), keep the strip in sync so
          // tests and UI see an explicit "no suggestions" update.
          clearSuggestions();
        } else {
          postUpdateSuggestions();
        }
      } else {
        // just replace the typed word in the candidates view
        mCandidateView.replaceTypedWord(mWord.getTypedWord());
      }
    } else {
      if (ic != null) {
        ic.beginBatchEdit();
      }
      markExpectingSelectionUpdate();
      for (char c : Character.toChars(primaryCode)) {
        sendKeyChar(c);
      }
      if (ic != null) {
        ic.endBatchEdit();
      }
    }
    autoCorrectState.justAutoAddedWord = false;
  }

  // Make sure to call this BEFORE actually making changes, and not after.
  // the event might arrive immediately as changes occur.
  protected void markExpectingSelectionUpdate() {
    selectionExpectationTracker.markExpectingUntil(
        SystemClock.uptimeMillis() + MAX_TIME_TO_EXPECT_SELECTION_UPDATE);
  }

  protected void handleSeparator(int primaryCode) {
    performUpdateSuggestions();
    if (!getCurrentAlphabetKeyboard().isLeftToRightLanguage()) {
      if (primaryCode == (int) ')') primaryCode = (int) '(';
      else if (primaryCode == (int) '(') primaryCode = (int) ')';
    }

    final boolean wasPredicting = isCurrentlyPredicting();
    final boolean newLine = primaryCode == KeyCodes.ENTER;
    final boolean isSpace = primaryCode == KeyCodes.SPACE;
    boolean isEndOfSentence = newLine || isSentenceSeparator(primaryCode);

    if (BuildConfig.DEBUG) {
      Logger.d(
          TAG,
          "handleSeparator code=%d isSpace=%s lastSpace=%s swapCandidate=%s",
          primaryCode,
          isSpace,
          spaceTimeTracker.hadSpace(),
          isSpaceSwapCharacter(primaryCode));
    }

    InputConnection ic = mInputConnectionRouter.current();
    if (ic != null) ic.beginBatchEdit();

    final WordComposer typedWord = prepareWordComposerForNextWord();
    final boolean separatorInsideWord = typedWord.cursorPosition() < typedWord.charCount();

    SeparatorActionHelper.Result result =
        SeparatorActionHelper.handleSeparator(
            primaryCode,
            isSpace,
            newLine,
            isEndOfSentence,
            wasPredicting,
            separatorInsideWord,
            isAutoCorrect(),
            mIsDoubleSpaceChangesToPeriod,
            mMultiTapTimeout,
            typedWord,
            ic,
            separatorOutputHandler,
            spaceTimeTracker,
            this::isSpaceSwapCharacter,
            this::commitWordToInput,
            (word, type) -> checkAddToDictionaryWithAutoDictionary(word, type),
            () -> abortCorrectionAndResetPredictionState(false),
            len -> autoCorrectState.wordRevertLength = len,
            code -> sendKeyChar((char) code));

    markExpectingSelectionUpdate();
    if (ic != null) ic.endBatchEdit();

    if (result.endOfSentence) {
      mSuggest.resetNextWordSentence();
      clearSuggestions();
    } else {
      setSuggestions(
          mSuggest.getNextSuggestions(result.wordForNextSuggestions, typedWord.isAllUpperCase()),
          -1);
    }
  }

  WordComposer prepareWordComposerForNextWord() {
    if (mWord.isEmpty()) return mWord;

    final WordComposer typedWord = mWord;
    mWord = mPreviousWord;
    mPreviousWord = typedWord;
    mWord.reset(); // re-using
    return typedWord;
  }

  private boolean isSpaceSwapCharacter(int primaryCode) {
    return spaceSwapDecider.isSpaceSwapCharacter(
        primaryCode, mFrenchSpacePunctuationBehavior, sentenceSeparators);
  }

  private int computeCursorPositionAfterChar(
      int primaryCode, Keyboard.Key key, int multiTapIndex) {
    if (mWord.cursorPosition() == mWord.charCount()) {
      return -1;
    }

    /* Cursor is not at the end of the word. I'll need to reposition.
    The code for tracking the current position is split among several files and difficult to debug.
    This has been proven to work in every case: */
    int newCursorPosition;
    if (multiTapIndex > 0) {
      final int previousKeyCode = key.getMultiTapCode(multiTapIndex - 1);
      newCursorPosition =
          Character.charCount(primaryCode) - Character.charCount(previousKeyCode);
    } else {
      newCursorPosition = Character.charCount(primaryCode);
    }
    newCursorPosition += getCursorPosition();
    return newCursorPosition;
  }

  public void performRestartWordSuggestion(final InputConnection ic) {
    mKeyboardHandler.removeMessages(KeyboardUIStateHandler.MSG_RESTART_NEW_WORD_SUGGESTIONS);
    mKeyboardHandler.removeMessages(KeyboardUIStateHandler.MSG_UPDATE_SUGGESTIONS);
    // I assume ASK DOES NOT predict at this moment!

    // 2) predicting and moved outside the word - abort predicting, update
    // shift state
    // 2.1) to a new word - restart predicting on the new word
    // 2.2) to no word land - nothing else

    // this means that the new cursor position is outside the candidates
    // underline
    // this can be either because the cursor is really outside the
    // previously underlined (suggested)
    // or nothing was suggested.
    // in this case, we would like to reset the prediction and restart
    // if the user clicked inside a different word
    // restart required?
    if (canRestartWordSuggestion()) { // 2.1
      ic.beginBatchEdit(); // don't want any events till I finish handling this touch
      abortCorrectionAndResetPredictionState(false);

      wordRestartHelper.restartWordFromCursor(
          ic,
          mWord,
          new WordRestartHelper.Host() {
            @Override
            public boolean isWordSeparator(int codePoint) {
              return AnySoftKeyboardSuggestions.this.isWordSeparator(codePoint);
            }

            @Override
            public int getCursorPosition() {
              return AnySoftKeyboardSuggestions.this.getCursorPosition();
            }

            @Override
            public void markExpectingSelectionUpdate() {
              AnySoftKeyboardSuggestions.this.markExpectingSelectionUpdate();
            }

            @Override
            public void performUpdateSuggestions() {
              AnySoftKeyboardSuggestions.this.performUpdateSuggestions();
            }

            @Override
            public String logTag() {
              return TAG;
            }
          });

      ic.endBatchEdit();
    } else {
      Logger.d(TAG, "performRestartWordSuggestion canRestartWordSuggestion == false");
    }
  }

  @Override
  public void onText(Keyboard.Key key, CharSequence text) {
    Logger.d(TAG, "onText: '%s'", text);
    InputConnection ic = mInputConnectionRouter.current();
    if (ic == null) {
      return;
    }
    ic.beginBatchEdit();

    // simulating multiple keys
    final WordComposer initialWordComposer = new WordComposer();
    mWord.cloneInto(initialWordComposer);
    abortCorrectionAndResetPredictionState(false);
    ic.commitText(text, 1);

    // this will be the revert
    autoCorrectState.wordRevertLength = initialWordComposer.charCount() + text.length();
    mPreviousWord = initialWordComposer;
    markExpectingSelectionUpdate();
    ic.endBatchEdit();
  }

  @Override
  public void onTyping(Keyboard.Key key, CharSequence text) {
    Logger.d(TAG, "onTyping: '%s'", text);
    // simulate key-by-key typing so suggestions & word state stay consistent
    typingSimulator.simulate(
        text,
        new TypingSimulator.Host() {
          @Override
          public InputConnection currentInputConnection() {
            return mInputConnectionRouter.current();
          }

          @Override
          public Keyboard.Key lastKey() {
            return key;
          }

          @Override
          public void onKey(
              int primaryCode,
              Keyboard.Key keyboardKey,
              int multiTapIndex,
              int[] nearByKeyCodes,
              boolean fromUI) {
            AnySoftKeyboardSuggestions.this.onKey(
                primaryCode, keyboardKey, multiTapIndex, nearByKeyCodes, fromUI);
          }

          @Override
          public void clearSpaceTimeTracker() {
            AnySoftKeyboardSuggestions.this.clearSpaceTimeTracker();
          }

          @Override
          public boolean isAutoCorrectOn() {
            return predictionState.autoCorrectOn;
          }

          @Override
          public void setAutoCorrectOn(boolean on) {
            predictionState.autoCorrectOn = on;
          }
        });
  }

  protected void setDictionariesForCurrentKeyboard() {
    if (mDictionariesForCurrentKeyboardLoaded) return;

    mSuggest.resetNextWordSentence();

    mDictionariesForCurrentKeyboardLoaded =
        dictionaryLoaderHelper.setDictionariesForCurrentKeyboard(
            this,
            predictionState,
            dictionaryLoadGate,
            shouldLoadDictionariesForGestureTyping(),
            getCurrentAlphabetKeyboard(),
            isInAlphabetKeyboardMode(),
            sentenceSeparators,
            mSuggest,
            this::getDictionaryLoadedListener);
  }

  @NonNull
  protected DictionaryBackgroundLoader.Listener getDictionaryLoadedListener(
      @NonNull AnyKeyboard currentAlphabetKeyboard) {
    return NO_OP_DICTIONARY_LOADER_LISTENER;
  }

  /** Allows subclasses (e.g., gesture typing) to force dictionary loading even when predictions are off. */
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
      sentenceSeparators.clear();
    } else {
      sentenceSeparators.updateFrom(sentenceSeparatorsForCurrentKeyboard.toCharArray());
    }
  }

  @CallSuper
  protected void abortCorrectionAndResetPredictionState(boolean disabledUntilNextInputStart) {
    mSuggest.resetNextWordSentence();

    spaceTimeTracker.clear();
    autoCorrectState.justAutoAddedWord = false;
    mKeyboardHandler.removeAllSuggestionMessages();

    final InputConnection ic = mInputConnectionRouter.current();
    markExpectingSelectionUpdate();
    if (ic != null) ic.finishComposingText();

    clearSuggestions();

    mWord.reset();
    autoCorrectState.reset();
    if (disabledUntilNextInputStart) {
      Logger.d(TAG, "abortCorrection will abort correct forever");
      final KeyboardViewContainerView inputViewContainer = getInputViewContainer();
      if (inputViewContainer != null) {
        inputViewContainer.removeStripAction(mCancelSuggestionsAction);
      }
      predictionState.predictionOn = false;
    }
  }

  /** Allows subclasses to force a reload of keyboard dictionaries. */
  protected void invalidateDictionariesForCurrentKeyboard() {
    mDictionariesForCurrentKeyboardLoaded = false;
  }

  protected boolean canRestartWordSuggestion() {
    final InputViewBinder inputView = getInputView();
    if (!wordRestartGate.canRestartWordSuggestion(
        isPredictionOn(), predictionState.allowSuggestionsRestart, inputView)) {
      return false;
    } else if (!isCursorTouchingWord()) {
      Logger.d(TAG, "User moved cursor to no-man land. Bye bye.");
      return false;
    }

    return true;
  }

  protected void clearSuggestions() {
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
    return predictionState.isPredictionOn();
  }

  protected boolean isCurrentlyPredicting() {
    return isPredictionOn() && !mWord.isEmpty();
  }

  protected boolean isAutoCorrect() {
    return predictionState.isAutoCorrect();
  }

  boolean isAutoCompleteEnabled() {
    return predictionState.autoComplete;
  }

  public void performUpdateSuggestions() {
    mKeyboardHandler.removeMessages(KeyboardUIStateHandler.MSG_UPDATE_SUGGESTIONS);

    suggestionRefresher.performUpdateSuggestions(
        predictionState,
        mWord,
        mSuggest,
        new SuggestionRefresher.Host() {
          @Override
          public void clearSuggestions() {
            AnySoftKeyboardSuggestions.this.clearSuggestions();
          }

          @Override
          public void setSuggestions(
              @NonNull List<? extends CharSequence> suggestions, int highlightedSuggestionIndex) {
            AnySoftKeyboardSuggestions.this.setSuggestions(
                suggestions, highlightedSuggestionIndex);
          }
        });
  }

  public void pickSuggestionManually(int index, CharSequence suggestion) {
    pickSuggestionManually(index, suggestion, predictionState.autoSpace);
  }

  @CallSuper
  public void pickSuggestionManually(
      int index, CharSequence suggestion, boolean withAutoSpaceEnabled) {
    autoCorrectState.wordRevertLength = 0; // no reverts
    final WordComposer typedWord = prepareWordComposerForNextWord();

    suggestionPicker.pickSuggestionManually(
        typedWord,
        withAutoSpaceEnabled,
        index,
        suggestion,
        predictionState.showSuggestions,
        autoCorrectState.justAutoAddedWord,
        typedWord.isAtTagsSearchState());
  }

  /**
   * Commits the chosen word to the text field and saves it for later retrieval.
   *
   * @param wordToCommit the suggestion picked by the user to be committed to the text field
   * @param typedWord the word the user typed.
   */
  @CallSuper
  protected void commitWordToInput(
      @NonNull CharSequence wordToCommit, @NonNull CharSequence typedWord) {
    suggestionCommitter.commitWordToInput(wordToCommit, typedWord);
  }

  private boolean isCursorTouchingWord() {
    return cursorTouchChecker.isCursorTouchingWord(
        mInputConnectionRouter.current(), this::isWordSeparator);
  }

  protected void setSpaceTimeStamp(boolean isSpace) {
    if (isSpace) {
      spaceTimeTracker.markSpace();
    } else {
      spaceTimeTracker.clear();
    }
  }

  @Override
  public void onAlphabetKeyboardSet(@NonNull AnyKeyboard keyboard) {
    super.onAlphabetKeyboardSet(keyboard);
    mDictionariesForCurrentKeyboardLoaded = false;

    final Locale locale = keyboard.getLocale();
    mFrenchSpacePunctuationBehavior =
        mSwapPunctuationAndSpace && locale.toString().toLowerCase(Locale.US).startsWith("fr");
  }

  public void revertLastWord() {
    WordRevertHandler.Result result =
        wordRevertHandler.revertLastWord(
            autoCorrectState,
            predictionState,
            mWord,
            mPreviousWord,
            new WordRevertHandler.Host() {
              @Override
              public InputConnection currentInputConnection() {
                return mInputConnectionRouter.current();
              }

              @Override
              public int getCursorPosition() {
                return AnySoftKeyboardSuggestions.this.getCursorPosition();
              }

              @Override
              public void sendDownUpKeyEvents(int keyCode) {
                AnySoftKeyboardSuggestions.this.sendDownUpKeyEvents(keyCode);
              }

              @Override
              public void performUpdateSuggestions() {
                AnySoftKeyboardSuggestions.this.performUpdateSuggestions();
              }

              @Override
              public void removeFromUserDictionary(String word) {
                AnySoftKeyboardSuggestions.this.removeFromUserDictionary(word);
              }
            });
    mWord = result.currentWord();
    mPreviousWord = result.previousWord();
  }

  protected boolean isSentenceSeparator(int code) {
    return sentenceSeparators.isSeparator(code);
  }

  protected boolean isWordSeparator(int code) {
    return !isAlphabet(code);
  }

  public boolean preferCapitalization() {
    return mWord.isFirstCharCapitalized();
  }

  public void closeDictionaries() {
    mSuggest.closeDictionaries();
  }

  @Override
  public void onDisplayCompletions(CompletionInfo[] completions) {
    completionHandler.onDisplayCompletions(
        completions,
        new CompletionHandler.Host() {
          @Override
          public boolean isFullscreenMode() {
            return AnySoftKeyboardSuggestions.this.isFullscreenMode();
          }

          @Override
          public void clearSuggestions() {
            AnySoftKeyboardSuggestions.this.clearSuggestions();
          }

          @Override
          public void setSuggestions(
              List<CharSequence> suggestions, int highlightedIndex) {
            AnySoftKeyboardSuggestions.this.setSuggestions(suggestions, highlightedIndex);
            mWord.setPreferredWord(null);
          }
        });
  }

  void checkAddToDictionaryWithAutoDictionary(CharSequence newWord, Suggest.AdditionType type) {
    autoCorrectState.justAutoAddedWord = false;

    // unfortunately, has to do it on the main-thread (because we checking mJustAutoAddedWord)
    if (mSuggest.tryToLearnNewWord(newWord, type)) {
      addWordToDictionary(newWord.toString());
      autoCorrectState.justAutoAddedWord = true;
    }
  }

  @CallSuper
  protected boolean isSuggestionAffectingCharacter(int code) {
    return Character.isLetter(code);
  }

  public void removeFromUserDictionary(String wordToRemove) {
    userDictionaryWorker.removeFromUserDictionary(wordToRemove, mInputSessionDisposables::add);
    autoCorrectState.justAutoAddedWord = false;
    abortCorrectionAndResetPredictionState(false);
  }

  AddToDictionaryHintController addToDictionaryHintController() {
    return addToDictionaryHintController;
  }

}
