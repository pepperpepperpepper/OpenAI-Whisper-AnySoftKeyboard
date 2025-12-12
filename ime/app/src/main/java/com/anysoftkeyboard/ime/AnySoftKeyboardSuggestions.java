package com.anysoftkeyboard.ime;

import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.KeyEvent;
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
import java.util.ArrayList;
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

  protected int mWordRevertLength = 0;
  private WordComposer mWord = new WordComposer();
  private WordComposer mPreviousWord = new WordComposer();
  private Suggest mSuggest;
  private CandidateView mCandidateView;
  private final SpaceTimeTracker spaceTimeTracker = new SpaceTimeTracker();
  @Nullable private Keyboard.Key mLastKey;
  private int mLastPrimaryKey = Integer.MIN_VALUE;
  private long mExpectingSelectionUpdateBy = NEVER_TIME_STAMP;
  private boolean mLastCharacterWasShifted = false;
  private boolean mFrenchSpacePunctuationBehavior;
  /*
   * is prediction needed for the current input connection
   */
  private boolean mPredictionOn;
  private boolean mAutoSpace;
  private boolean mInputFieldSupportsAutoPick;
  private boolean mAutoCorrectOn;
  private boolean mAllowSuggestionsRestart = true;
  private boolean mShowSuggestions = false;
  private boolean mAutoComplete;

  private boolean mJustAutoAddedWord = false;

  @VisibleForTesting
  final CancelSuggestionsAction mCancelSuggestionsAction =
      new CancelSuggestionsAction(() -> abortCorrectionAndResetPredictionState(true));

  private final InputFieldConfigurator inputFieldConfigurator = new InputFieldConfigurator();
  private final SelectionUpdateProcessor selectionUpdateProcessor = new SelectionUpdateProcessor();
  private SuggestionStripController suggestionStripController;
  private final CompletionHandler completionHandler = new CompletionHandler();
  private final WordRestartHelper wordRestartHelper = new WordRestartHelper();
  private final SeparatorOutputHandler separatorOutputHandler = new SeparatorOutputHandler();
  private final SuggestionSettingsController suggestionSettingsController =
      new SuggestionSettingsController();
  private final SuggestionsUpdater suggestionsUpdater =
      new SuggestionsUpdater(mCancelSuggestionsAction);
  private final SuggestionCommitter suggestionCommitter = new SuggestionCommitter();
  private final SuggestionPicker suggestionPicker = new SuggestionPicker();
  private final SuggestionPickerHostAdapter suggestionPickerHost =
      new SuggestionPickerHostAdapter(this);
  private final AddToDictionaryDecider addToDictionaryDecider = new AddToDictionaryDecider();
  private final SuggestionCommitter.Host suggestionCommitterHost =
      new SuggestionCommitter.Host() {
        @Override
        public InputConnection currentInputConnection() {
          return mInputConnectionRouter.current();
        }

        @Override
        public boolean isSelectionUpdateDelayed() {
          return AnySoftKeyboardSuggestions.this.isSelectionUpdateDelayed();
        }

        @Override
        public void markExpectingSelectionUpdate() {
          AnySoftKeyboardSuggestions.this.markExpectingSelectionUpdate();
        }

        @Override
        public int getCursorPosition() {
          return AnySoftKeyboardSuggestions.this.getCursorPosition();
        }

        @Override
        public void clearSuggestions() {
          AnySoftKeyboardSuggestions.this.clearSuggestions();
        }
      };

  @Nullable
  protected Keyboard.Key getLastUsedKey() {
    return mLastKey;
  }

  void setAllowSuggestionsRestart(boolean allow) {
    mAllowSuggestionsRestart = allow;
  }

  void applySuggestionSettings(
      boolean showSuggestions,
      boolean autoComplete,
      int commonalityMaxLengthDiff,
      int commonalityMaxDistance,
      boolean trySplitting) {
    final boolean showChanged = mShowSuggestions != showSuggestions;
    mShowSuggestions = showSuggestions;
    mAutoComplete = autoComplete;
    mSuggest.setCorrectionMode(
        mShowSuggestions, commonalityMaxLengthDiff, commonalityMaxDistance, trySplitting);
    if (showChanged) {
      if (mShowSuggestions) {
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
    suggestionsUpdater.setSuggest(mSuggest);

    if (BuildConfig.TESTING_BUILD) {
      try {
        // expose a tiny test-only API to help instrumentation seed context
        com.anysoftkeyboard.ime.ImeTestApi.setService(this);
      } catch (Throwable ignore) {
        // class not present in release builds
      }
    }

    suggestionSettingsController.attach(this, mSuggest);
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
  }

  @Override
  public void onStartInputView(final EditorInfo attribute, final boolean restarting) {
    super.onStartInputView(attribute, restarting);

    mPredictionOn = false;
    completionHandler.reset();
    mInputFieldSupportsAutoPick = false;

    InputFieldConfigurator.Result inputConfig =
        inputFieldConfigurator.configure(
            attribute, restarting, getKeyboardSwitcher(), mPrefsAutoSpace, TAG);

    mPredictionOn = inputConfig.predictionOn;
    mInputFieldSupportsAutoPick = inputConfig.inputFieldSupportsAutoPick;
    mAutoSpace = inputConfig.autoSpace;

    mPredictionOn = mPredictionOn && mShowSuggestions;

    suggestionsUpdater.configure(
        this::isPredictionOn, () -> mShowSuggestions, this::isAutoCorrect, () -> mWord);

    mCancelSuggestionsAction.setCancelIconVisible(false);
    suggestionStripController.attachToStrip(getInputViewContainer());
    getInputViewContainer().setActionsStripVisibility(isPredictionOn());
    clearSuggestions();
  }

  @Override
  public void onFinishInput() {
    super.onFinishInput();
    mCancelSuggestionsAction.setCancelIconVisible(false);
    mPredictionOn = false;
    mKeyboardHandler.sendEmptyMessageDelayed(
        KeyboardUIStateHandler.MSG_CLOSE_DICTIONARIES, CLOSE_DICTIONARIES_DELAY);
    mExpectingSelectionUpdateBy = NEVER_TIME_STAMP;
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
        new SelectionUpdateProcessor.Host() {
          @Override
          public boolean isPredictionOn() {
            return AnySoftKeyboardSuggestions.this.isPredictionOn();
          }

          @Override
          public boolean isCurrentlyPredicting() {
            return AnySoftKeyboardSuggestions.this.isCurrentlyPredicting();
          }

          @Override
          public InputConnection currentInputConnection() {
            return mInputConnectionRouter.current();
          }

          @Override
          public void abortCorrectionAndResetPredictionState(boolean force) {
            AnySoftKeyboardSuggestions.this.abortCorrectionAndResetPredictionState(force);
          }

          @Override
          public void postRestartWordSuggestion() {
            AnySoftKeyboardSuggestions.this.postRestartWordSuggestion();
          }

          @Override
          public boolean shouldRevertOnDelete() {
            return AnySoftKeyboardSuggestions.this.shouldRevertOnDelete();
          }

          @Override
          public void setWordRevertLength(int length) {
            mWordRevertLength = length;
          }

          @Override
          public int getWordRevertLength() {
            return mWordRevertLength;
          }

          @Override
          public void resetLastSpaceTimeStamp() {
            spaceTimeTracker.clear();
          }

          @Override
          public long getExpectingSelectionUpdateBy() {
            return mExpectingSelectionUpdateBy;
          }

          @Override
          public void clearExpectingSelectionUpdate() {
            mExpectingSelectionUpdateBy = NEVER_TIME_STAMP;
          }

          @Override
          public void setExpectingSelectionUpdateBy(long value) {
            mExpectingSelectionUpdateBy = value;
          }

          @Override
          public int getCandidateStartPositionDangerous() {
            return oldCandidateStart;
          }

          @Override
          public int getCandidateEndPositionDangerous() {
            return oldCandidateEnd;
          }

          @Override
          public WordComposer getCurrentWord() {
            return mWord;
          }

          @Override
          public String logTag() {
            return TAG;
          }
        });
  }

  @Override
  public View onCreateInputView() {
    final View view = super.onCreateInputView();
    mCandidateView = getInputViewContainer().getCandidateView();
    mCandidateView.setService(this);
    mCancelSuggestionsAction.setOwningCandidateView(mCandidateView);
    suggestionsUpdater.setCandidateView(mCandidateView);
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
      mWordRevertLength = 0;
    }
    mCandidateView.dismissAddToDictionaryHint();
  }

  protected void resetLastPressedKey() {
    mLastKey = null;
  }

  @Override
  public void onRelease(int primaryCode) {
    // not allowing undo on-text in clipboard paste operations.
    if (primaryCode == KeyCodes.CLIPBOARD_PASTE) mWordRevertLength = 0;
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

  private void postRestartWordSuggestion() {
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
    return mExpectingSelectionUpdateBy > 0;
  }

  protected boolean shouldRevertOnDelete() {
    return mWordRevertLength > 0;
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
      mWordRevertLength = 0;
      mWord.reset();
      mAutoCorrectOn = isPredictionOn() && mAutoComplete && mInputFieldSupportsAutoPick;
      if (mShiftKeyState.isActive()) {
        mWord.setFirstCharCapitalized(true);
      }
    }

    mLastCharacterWasShifted = (getInputView() != null) && getInputView().isShifted();

    final InputConnection ic = mInputConnectionRouter.current();
    mWord.add(primaryCode, nearByKeyCodes);
    if (isPredictionOn()) {
      if (ic != null) {
        int newCursorPosition;
        if (mWord.cursorPosition() != mWord.charCount()) {
          /* Cursor is not at the end of the word. I'll need to reposition.
          The code for tracking the current position is split among several files and difficult to debug.
          This has been proven to work in every case: */
          if (multiTapIndex > 0) {
            final int previousKeyCode = key.getMultiTapCode(multiTapIndex - 1);
            newCursorPosition =
                Character.charCount(primaryCode) - Character.charCount(previousKeyCode);
          } else {
            newCursorPosition = Character.charCount(primaryCode);
          }
          newCursorPosition += getCursorPosition();
          ic.beginBatchEdit();
        } else {
          newCursorPosition = -1;
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
        postUpdateSuggestions();
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
    mJustAutoAddedWord = false;
  }

  // Make sure to call this BEFORE actually making changes, and not after.
  // the event might arrive immediately as changes occur.
  protected void markExpectingSelectionUpdate() {
    mExpectingSelectionUpdateBy = SystemClock.uptimeMillis() + MAX_TIME_TO_EXPECT_SELECTION_UPDATE;
  }

  protected void handleSeparator(int primaryCode) {
    performUpdateSuggestions();
    // Issue 146: Right to left languages require reversed parenthesis
    if (!getCurrentAlphabetKeyboard().isLeftToRightLanguage()) {
      if (primaryCode == (int) ')') {
        primaryCode = (int) '(';
      } else if (primaryCode == (int) '(') {
        primaryCode = (int) ')';
      }
    }
    // will not show next-word suggestion in case of a new line or if the separator is a
    // sentence separator.
    final boolean wasPredicting = isCurrentlyPredicting();
    final boolean newLine = primaryCode == KeyCodes.ENTER;
    boolean isEndOfSentence = newLine || isSentenceSeparator(primaryCode);
    final boolean isSpace = primaryCode == KeyCodes.SPACE;

    if (BuildConfig.DEBUG) {
      Logger.d(
          TAG,
          "handleSeparator code=%d isSpace=%s lastSpace=%s swapCandidate=%s",
          primaryCode,
          isSpace,
          spaceTimeTracker.hadSpace(),
          isSpaceSwapCharacter(primaryCode));
    }

    // Handle separator
    InputConnection ic = mInputConnectionRouter.current();
    if (ic != null) {
      ic.beginBatchEdit();
    }
    final WordComposer typedWord = prepareWordComposerForNextWord();
    CharSequence wordToOutput = typedWord.getTypedWord();
    // ACTION does not invoke default picking. See
    // https://github.com/AnySoftKeyboard/AnySoftKeyboard/issues/198
    if (isAutoCorrect() && !newLine /*we do not auto-pick on ENTER.*/) {
      if (!TextUtils.equals(wordToOutput, typedWord.getPreferredWord())) {
        wordToOutput = typedWord.getPreferredWord();
      }
    }
    // this is a special case, when the user presses a separator WHILE
    // inside the predicted word.
    // in this case, I will want to just dump the separator.
    final boolean separatorInsideWord = (typedWord.cursorPosition() < typedWord.charCount());
    if (wasPredicting && !separatorInsideWord) {
      commitWordToInput(wordToOutput, typedWord.getTypedWord());
      if (TextUtils.equals(typedWord.getTypedWord(), wordToOutput)) {
        // if the word typed was auto-replaced, we should not learn it.
        // Add the word to the auto dictionary if it's not a known word
        // this is "typed" if the auto-correction is off, or "picked" if it is on or
        // momentarily off.
        checkAddToDictionaryWithAutoDictionary(wordToOutput, SuggestImpl.AdditionType.Typed);
      }
      // Picked the suggestion by a space/punctuation character: we will treat it
      // as "added an auto space".
      mWordRevertLength = wordToOutput.length() + 1;
    } else if (separatorInsideWord) {
      // when putting a separator in the middle of a word, there is no
      // need to do correction, or keep knowledge
      abortCorrectionAndResetPredictionState(false);
    }

    final SeparatorOutputHandler.Result separatorResult =
        separatorOutputHandler.apply(
            ic,
            primaryCode,
            isSpace,
            newLine,
            mIsDoubleSpaceChangesToPeriod,
            mMultiTapTimeout,
            spaceTimeTracker,
            this::isSpaceSwapCharacter);

    boolean handledOutputToInputConnection = separatorResult.handledOutput;
    if (separatorResult.endOfSentence) {
      isEndOfSentence = true;
    }

    if (!handledOutputToInputConnection) {
      for (char c : Character.toChars(primaryCode)) {
        sendKeyChar(c);
      }
    }

    markExpectingSelectionUpdate();

    if (ic != null) {
      ic.endBatchEdit();
    }

    if (isEndOfSentence) {
      mSuggest.resetNextWordSentence();
      clearSuggestions();
    } else {
      setSuggestions(mSuggest.getNextSuggestions(wordToOutput, typedWord.isAllUpperCase()), -1);
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
    // Treat closing parenthesis as a swap candidate even if the current keyboard does not mark
    // it as a sentence separator (historic ASK behavior and expected by unit tests).
    if (primaryCode == ')') {
      return true;
    }

    if (isSentenceSeparator(primaryCode)) {
      if (mFrenchSpacePunctuationBehavior) {
        return switch (primaryCode) {
          case '!', '?', ':', ';' -> false;
          default -> true;
        };
      } else {
        return true;
      }
    } else {
      return false;
    }
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
    mWordRevertLength = initialWordComposer.charCount() + text.length();
    mPreviousWord = initialWordComposer;
    markExpectingSelectionUpdate();
    ic.endBatchEdit();
  }

  @Override
  public void onTyping(Keyboard.Key key, CharSequence text) {
    Logger.d(TAG, "onTyping: '%s'", text);
    InputConnection ic = mInputConnectionRouter.current();
    if (ic == null) {
      return;
    }
    ic.beginBatchEdit();

    // simulating multiple keys
    final WordComposer initialWordComposer = new WordComposer();
    mWord.cloneInto(initialWordComposer);
    final boolean originalAutoCorrect = mAutoCorrectOn;
    mAutoCorrectOn = false;
    for (int pointCodeIndex = 0; pointCodeIndex < text.length(); ) {
      int pointCode = Character.codePointAt(text, pointCodeIndex);
      pointCodeIndex += Character.charCount(pointCode);
      // this will ensure that double-spaces will not count.
      spaceTimeTracker.clear();
      // simulating key press
      onKey(pointCode, key, 0, new int[] {pointCode}, true);
    }
    mAutoCorrectOn = originalAutoCorrect;

    ic.endBatchEdit();
  }

  protected void setDictionariesForCurrentKeyboard() {
    mSuggest.resetNextWordSentence();

    if (mPredictionOn) {
      // It null at the creation of the application.
      final AnyKeyboard currentAlphabetKeyboard = getCurrentAlphabetKeyboard();
      if (currentAlphabetKeyboard != null && isInAlphabetKeyboardMode()) {
        sentenceSeparators.updateFrom(currentAlphabetKeyboard.getSentenceSeparators());
        sentenceSeparators.add(KeyCodes.ENTER);

        List<DictionaryAddOnAndBuilder> buildersForKeyboard =
            AnyApplication.getExternalDictionaryFactory(this)
                .getBuildersForKeyboard(currentAlphabetKeyboard);

        mSuggest.setupSuggestionsForKeyboard(
            buildersForKeyboard, getDictionaryLoadedListener(currentAlphabetKeyboard));
      }
    }
  }

  @NonNull
  protected DictionaryBackgroundLoader.Listener getDictionaryLoadedListener(
      @NonNull AnyKeyboard currentAlphabetKeyboard) {
    return NO_OP_DICTIONARY_LOADER_LISTENER;
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
    mJustAutoAddedWord = false;
    mKeyboardHandler.removeAllSuggestionMessages();

    final InputConnection ic = mInputConnectionRouter.current();
    markExpectingSelectionUpdate();
    if (ic != null) ic.finishComposingText();

    clearSuggestions();

    mWord.reset();
    mWordRevertLength = 0;
    mJustAutoAddedWord = false;
    if (disabledUntilNextInputStart) {
      Logger.d(TAG, "abortCorrection will abort correct forever");
      final KeyboardViewContainerView inputViewContainer = getInputViewContainer();
      if (inputViewContainer != null) {
        inputViewContainer.removeStripAction(mCancelSuggestionsAction);
      }
      mPredictionOn = false;
    }
  }

  protected boolean canRestartWordSuggestion() {
    final InputViewBinder inputView = getInputView();
    if (!isPredictionOn()
        || !mAllowSuggestionsRestart
        || inputView == null
        || !inputView.isShown()) {
      // why?
      // mPredicting - if I'm predicting a word, I can not restart it..
      // right? I'm inside that word!
      // isPredictionOn() - this is obvious.
      // mAllowSuggestionsRestart - config settings
      // mCurrentlyAllowSuggestionRestart - workaround for
      // onInputStart(restarting == true)
      // mInputView == null - obvious, no?
      Logger.d(
          TAG,
          "performRestartWordSuggestion: no need to restart: isPredictionOn=%s,"
              + " mAllowSuggestionsRestart=%s",
          isPredictionOn(),
          mAllowSuggestionsRestart);
      return false;
    } else if (!isCursorTouchingWord()) {
      Logger.d(TAG, "User moved cursor to no-man land. Bye bye.");
      return false;
    }

    return true;
  }

  protected void clearSuggestions() {
    mKeyboardHandler.removeAllSuggestionMessages();
    suggestionsUpdater.clearSuggestions();
  }

  protected void setSuggestions(
      @NonNull List<? extends CharSequence> suggestions, int highlightedSuggestionIndex) {
    suggestionsUpdater.setSuggestions(suggestions, highlightedSuggestionIndex);
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

  @Override
  protected InputConnection currentInputConnection() {
    return mInputConnectionRouter.current();
  }

  boolean tryCommitCompletionFromPicker(int index, InputConnection ic, CandidateView cv) {
    return completionHandler.tryCommitCompletion(index, ic, cv);
  }

  void sendSpaceChar() {
    sendKeyChar((char) KeyCodes.SPACE);
  }

  boolean isShowSuggestionsFlag() {
    return mShowSuggestions;
  }

  CandidateView getCandidateView() {
    return mCandidateView;
  }

  boolean isNextWordAllUpperCase() {
    return mWord.isAllUpperCase();
  }

  boolean isJustAutoAddedWord() {
    return mJustAutoAddedWord;
  }

  void setJustAutoAddedWord(boolean value) {
    mJustAutoAddedWord = value;
  }

  boolean isValidWordLower(String lowerCaseWord) {
    return mSuggest.isValidWord(lowerCaseWord);
  }

  AddToDictionaryDecider getAddToDictionaryDecider() {
    return addToDictionaryDecider;
  }

  protected abstract boolean isAlphabet(int code);

  public void addWordToDictionary(String word) {
    mInputSessionDisposables.add(
        Observable.just(word)
            .subscribeOn(RxSchedulers.background())
            .map(mSuggest::addWordToUserDictionary)
            .filter(added -> added)
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                added -> {
                  if (mCandidateView != null) {
                    mCandidateView.notifyAboutWordAdded(word);
                  }
                },
                e -> Logger.w(TAG, e, "Failed to add word '%s' to user-dictionary!", word)));
  }

  /** posts an update suggestions request to the messages queue. Removes any previous request. */
  protected void postUpdateSuggestions() {
    mKeyboardHandler.removeMessages(KeyboardUIStateHandler.MSG_UPDATE_SUGGESTIONS);
    mKeyboardHandler.sendMessageDelayed(
        mKeyboardHandler.obtainMessage(KeyboardUIStateHandler.MSG_UPDATE_SUGGESTIONS),
        GET_SUGGESTIONS_DELAY);
  }

  protected boolean isPredictionOn() {
    return mPredictionOn;
  }

  protected boolean isCurrentlyPredicting() {
    return isPredictionOn() && !mWord.isEmpty();
  }

  protected boolean isAutoCorrect() {
    return mAutoCorrectOn && mInputFieldSupportsAutoPick && mPredictionOn;
  }

  public void performUpdateSuggestions() {
    mKeyboardHandler.removeMessages(KeyboardUIStateHandler.MSG_UPDATE_SUGGESTIONS);
    suggestionsUpdater.performUpdateSuggestions();
  }

  public void pickSuggestionManually(int index, CharSequence suggestion) {
    pickSuggestionManually(index, suggestion, mAutoSpace);
  }

  @CallSuper
  public void pickSuggestionManually(
      int index, CharSequence suggestion, boolean withAutoSpaceEnabled) {
    mWordRevertLength = 0; // no reverts
    suggestionPicker.pickSuggestionManually(index, suggestion, withAutoSpaceEnabled, suggestionPickerHost);
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
    suggestionCommitter.commitWordToInput(suggestionCommitterHost, wordToCommit, typedWord);
  }

  private boolean isCursorTouchingWord() {
    InputConnection ic = mInputConnectionRouter.current();
    if (ic == null) {
      return false;
    }

    CharSequence toLeft = ic.getTextBeforeCursor(1, 0);
    // It is not exactly clear to me why, but sometimes, although I request
    // 1 character, I get the entire text
    if (!TextUtils.isEmpty(toLeft) && !isWordSeparator(toLeft.charAt(0))) {
      return true;
    }

    CharSequence toRight = ic.getTextAfterCursor(1, 0);
    if (!TextUtils.isEmpty(toRight) && !isWordSeparator(toRight.charAt(0))) {
      return true;
    }

    return false;
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

    final Locale locale = keyboard.getLocale();
    mFrenchSpacePunctuationBehavior =
        mSwapPunctuationAndSpace && locale.toString().toLowerCase(Locale.US).startsWith("fr");
  }

  public void revertLastWord() {
    if (mWordRevertLength == 0) {
      sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
    } else {
      final int length = mWordRevertLength;
      mAutoCorrectOn = false;
      // note: typedWord may be empty
    final InputConnection ic = mInputConnectionRouter.current();
      final int globalCursorPosition = getCursorPosition();
      ic.setComposingRegion(globalCursorPosition - length, globalCursorPosition);
      WordComposer temp = mWord;
      mWord = mPreviousWord;
      mPreviousWord = temp;
      mWordRevertLength = 0;
      final CharSequence typedWord = mWord.getTypedWord();
      ic.setComposingText(typedWord /* mComposing */, 1);
      performUpdateSuggestions();
      if (mJustAutoAddedWord) {
        removeFromUserDictionary(typedWord.toString());
      }
    }
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
    mJustAutoAddedWord = false;

    // unfortunately, has to do it on the main-thread (because we checking mJustAutoAddedWord)
    if (mSuggest.tryToLearnNewWord(newWord, type)) {
      addWordToDictionary(newWord.toString());
      mJustAutoAddedWord = true;
    }
  }

  @CallSuper
  protected boolean isSuggestionAffectingCharacter(int code) {
    return Character.isLetter(code);
  }

  public void removeFromUserDictionary(String wordToRemove) {
    mInputSessionDisposables.add(
        Observable.just(wordToRemove)
            .subscribeOn(RxSchedulers.background())
            .map(
                word -> {
                  mSuggest.removeWordFromUserDictionary(word);
                  return word;
                })
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                word -> {
                  if (mCandidateView != null) {
                    mCandidateView.notifyAboutRemovedWord(word);
                  }
                },
                e ->
                    Logger.w(
                        TAG, e, "Failed to remove word '%s' from user-dictionary!", wordToRemove)));
    mJustAutoAddedWord = false;
    abortCorrectionAndResetPredictionState(false);
  }

}
