package com.anysoftkeyboard.ime;

import android.text.TextUtils;
import android.view.inputmethod.InputConnection;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.menny.android.anysoftkeyboard.AnyApplication;

/**
 * Extracted commit logic from {@link AnySoftKeyboardSuggestions} to reduce class size while
 * keeping behavior identical. Host supplies the small bit of IME state this helper needs.
 */
class SuggestionCommitter {

  interface Host {
    @Nullable
    InputConnection currentInputConnection();

    boolean isSelectionUpdateDelayed();

    void markExpectingSelectionUpdate();

    int getCursorPosition();

    void clearSuggestions();
  }

  void commitWordToInput(
      @NonNull Host host,
      @NonNull CharSequence wordToCommit,
      @NonNull CharSequence typedWord) {
    final InputConnection ic = host.currentInputConnection();
    if (ic != null) {
      final boolean delayedUpdates = host.isSelectionUpdateDelayed();
      host.markExpectingSelectionUpdate();
      // we DO NOT want to use commitCorrection if we do not know
      // the exact position in the text-box.
      if (TextUtils.equals(wordToCommit, typedWord) || delayedUpdates) {
        ic.commitText(wordToCommit, 1);
      } else {
        AnyApplication.getDeviceSpecific()
            .commitCorrectionToInputConnection(
                ic, host.getCursorPosition() - typedWord.length(), typedWord, wordToCommit);
      }
    }

    host.clearSuggestions();
  }
}
