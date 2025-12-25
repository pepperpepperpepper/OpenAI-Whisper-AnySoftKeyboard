package com.anysoftkeyboard.ime;

import android.text.TextUtils;
import com.anysoftkeyboard.dictionaries.SuggestImpl;
import com.anysoftkeyboard.dictionaries.WordComposer;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

/** Handles separator key flows pulled out of {@link ImeSuggestionsController}. */
final class SeparatorActionHelper {

  static final class Result {
    final boolean endOfSentence;
    final CharSequence wordForNextSuggestions;

    Result(boolean endOfSentence, CharSequence wordForNextSuggestions) {
      this.endOfSentence = endOfSentence;
      this.wordForNextSuggestions = wordForNextSuggestions;
    }
  }

  private SeparatorActionHelper() {}

  static Result handleSeparator(
      int primaryCode,
      boolean isSpace,
      boolean newLine,
      boolean isEndOfSentence,
      boolean wasPredicting,
      boolean separatorInsideWord,
      boolean isAutoCorrect,
      boolean isDoubleSpaceChangesToPeriod,
      int multiTapTimeout,
      WordComposer typedWord,
      InputConnectionRouter inputConnectionRouter,
      SeparatorOutputHandler separatorOutputHandler,
      SpaceTimeTracker spaceTimeTracker,
      IntPredicate isSpaceSwapCharacter,
      BiConsumer<CharSequence, CharSequence> commitWordToInput,
      BiConsumer<CharSequence, SuggestImpl.AdditionType> addToDictionary,
      Runnable abortCorrectionAndResetPredictionState,
      IntConsumer setWordRevertLength,
      IntConsumer sendKeyChar) {

    CharSequence wordToOutput = typedWord.getTypedWord();

    if (isAutoCorrect && !newLine) {
      final CharSequence preferred = typedWord.getPreferredWord();
      if (!TextUtils.equals(wordToOutput, preferred) && preferred != null) {
        wordToOutput = preferred;
      }
    }

    if (wasPredicting && !separatorInsideWord) {
      commitWordToInput.accept(wordToOutput, typedWord.getTypedWord());
      if (TextUtils.equals(typedWord.getTypedWord(), wordToOutput)) {
        addToDictionary.accept(wordToOutput, SuggestImpl.AdditionType.Typed);
      }
      setWordRevertLength.accept(wordToOutput.length() + 1);
    } else if (separatorInsideWord) {
      abortCorrectionAndResetPredictionState.run();
    }

    final SeparatorOutputHandler.Result separatorResult =
        separatorOutputHandler.apply(
            inputConnectionRouter,
            primaryCode,
            isSpace,
            newLine,
            isDoubleSpaceChangesToPeriod,
            multiTapTimeout,
            spaceTimeTracker,
            isSpaceSwapCharacter::test);

    if (separatorResult.endOfSentence) {
      isEndOfSentence = true;
    }

    if (!separatorResult.handledOutput) {
      for (char c : Character.toChars(primaryCode)) {
        sendKeyChar.accept(c);
      }
    }

    return new Result(isEndOfSentence, wordToOutput);
  }
}
