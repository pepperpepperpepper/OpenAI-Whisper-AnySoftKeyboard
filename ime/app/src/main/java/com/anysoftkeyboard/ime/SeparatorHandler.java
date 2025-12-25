package com.anysoftkeyboard.ime;

import androidx.annotation.NonNull;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.dictionaries.Suggest;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import java.util.List;

/** Handles separator key logic (space/punctuation/newline) and next-word suggestions. */
final class SeparatorHandler {

  void handleSeparator(int primaryCode, @NonNull Host host) {
    host.performUpdateSuggestions();

    if (!host.currentAlphabetKeyboard().isLeftToRightLanguage()) {
      if (primaryCode == (int) ')') primaryCode = (int) '(';
      else if (primaryCode == (int) '(') primaryCode = (int) ')';
    }

    final boolean wasPredicting = host.isCurrentlyPredicting();
    final boolean newLine = primaryCode == KeyCodes.ENTER;
    final boolean isSpace = primaryCode == KeyCodes.SPACE;
    final boolean isEndOfSentence = newLine || host.isSentenceSeparator(primaryCode);

    final InputConnectionRouter inputConnectionRouter = host.inputConnectionRouter();
    inputConnectionRouter.beginBatchEdit();

    final WordComposer typedWord = host.prepareWordComposerForNextWord();
    final boolean separatorInsideWord = typedWord.cursorPosition() < typedWord.charCount();

    SeparatorActionHelper.Result result =
        SeparatorActionHelper.handleSeparator(
            primaryCode,
            isSpace,
            newLine,
            isEndOfSentence,
            wasPredicting,
            separatorInsideWord,
            host.isAutoCorrect(),
            host.isDoubleSpaceChangesToPeriod(),
            host.multiTapTimeout(),
            typedWord,
            inputConnectionRouter,
            host.separatorOutputHandler(),
            host.spaceTimeTracker(),
            host::isSpaceSwapCharacter,
            host::commitWordToInput,
            host::checkAddToDictionaryWithAutoDictionary,
            () -> host.abortCorrectionAndResetPredictionState(false),
            host::setWordRevertLength,
            code -> host.sendKeyChar((char) code));

    host.markExpectingSelectionUpdate();
    inputConnectionRouter.endBatchEdit();

    if (result.endOfSentence) {
      host.suggest().resetNextWordSentence();
      host.clearSuggestions();
    } else {
      host.setSuggestions(
          host.suggest()
              .getNextSuggestions(result.wordForNextSuggestions, typedWord.isAllUpperCase()),
          -1);
    }
  }

  interface Host {
    void performUpdateSuggestions();

    @NonNull
    KeyboardDefinition currentAlphabetKeyboard();

    boolean isCurrentlyPredicting();

    boolean isSentenceSeparator(int code);

    boolean isAutoCorrect();

    boolean isDoubleSpaceChangesToPeriod();

    int multiTapTimeout();

    @NonNull
    SpaceTimeTracker spaceTimeTracker();

    @NonNull
    SeparatorOutputHandler separatorOutputHandler();

    boolean isSpaceSwapCharacter(int primaryCode);

    void commitWordToInput(@NonNull CharSequence wordToCommit, @NonNull CharSequence typedWord);

    void checkAddToDictionaryWithAutoDictionary(
        @NonNull CharSequence newWord, @NonNull Suggest.AdditionType type);

    void abortCorrectionAndResetPredictionState(boolean disabledUntilNextInputStart);

    void setWordRevertLength(int length);

    void sendKeyChar(char c);

    void markExpectingSelectionUpdate();

    InputConnectionRouter inputConnectionRouter();

    @NonNull
    WordComposer prepareWordComposerForNextWord();

    @NonNull
    Suggest suggest();

    void clearSuggestions();

    void setSuggestions(@NonNull List<? extends CharSequence> suggestions, int highlightedIndex);
  }
}
