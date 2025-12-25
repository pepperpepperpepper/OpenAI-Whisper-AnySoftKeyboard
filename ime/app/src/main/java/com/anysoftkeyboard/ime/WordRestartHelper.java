package com.anysoftkeyboard.ime;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.dictionaries.WordComposer;

/**
 * Rebuilds the composing word around the cursor so suggestions can restart after cursor moves.
 *
 * <p>Extracted from {@link ImeSuggestionsController#performRestartWordSuggestion()} to keep that
 * class smaller and more focused.
 */
final class WordRestartHelper {

  interface Host {
    boolean isWordSeparator(int codePoint);

    int getCursorPosition();

    void markExpectingSelectionUpdate();

    void performUpdateSuggestions();

    String logTag();
  }

  void restartWordFromCursor(
      @NonNull InputConnectionRouter inputConnectionRouter,
      @NonNull WordComposer currentWord,
      @NonNull Host host) {

    // locating the word
    CharSequence toLeft = "";
    CharSequence toRight = "";
    while (true) {
      CharSequence newToLeft = inputConnectionRouter.getTextBeforeCursor(toLeft.length() + 1, 0);
      if (TextUtils.isEmpty(newToLeft)
          || host.isWordSeparator(newToLeft.charAt(0))
          || newToLeft.length() == toLeft.length()) {
        break;
      }
      toLeft = newToLeft;
    }
    while (true) {
      CharSequence newToRight = inputConnectionRouter.getTextAfterCursor(toRight.length() + 1, 0);
      if (TextUtils.isEmpty(newToRight)
          || host.isWordSeparator(newToRight.charAt(newToRight.length() - 1))
          || newToRight.length() == toRight.length()) {
        break;
      }
      toRight = newToRight;
    }
    CharSequence word = toLeft.toString() + toRight.toString();
    Logger.d(host.logTag(), "Starting new prediction on word '%s'.", word);
    currentWord.reset();

    final int[] tempNearByKeys = new int[1];

    int index = 0;
    while (index < word.length()) {
      final int c =
          Character.codePointAt(word, Character.offsetByCodePoints(word, /*start*/ 0, index));
      if (index == 0) {
        currentWord.setFirstCharCapitalized(Character.isUpperCase(c));
      }

      tempNearByKeys[0] = c;
      currentWord.add(c, tempNearByKeys);

      index += Character.charCount(c);
    }
    currentWord.setCursorPosition(toLeft.length());
    final int globalCursorPosition = host.getCursorPosition();
    inputConnectionRouter.setComposingRegion(
        globalCursorPosition - toLeft.length(), globalCursorPosition + toRight.length());

    host.markExpectingSelectionUpdate();
    host.performUpdateSuggestions();
  }
}
