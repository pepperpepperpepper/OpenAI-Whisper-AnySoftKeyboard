package com.anysoftkeyboard.ime;

import android.text.TextUtils;
import com.menny.android.anysoftkeyboard.NskApplicationBase;

/** Handles committing a picked suggestion to the input connection. */
public final class SuggestionCommitter {

  public interface Host {
    InputConnectionRouter inputConnectionRouter();

    boolean isSelectionUpdateDelayed();

    void markExpectingSelectionUpdate();

    int getCursorPosition();

    void clearSuggestions();
  }

  private final Host host;

  public SuggestionCommitter(Host host) {
    this.host = host;
  }

  public void commitWordToInput(CharSequence wordToCommit, CharSequence typedWord) {
    final InputConnectionRouter inputConnectionRouter = host.inputConnectionRouter();
    if (!inputConnectionRouter.hasConnection()) {
      host.clearSuggestions();
      return;
    }

    final boolean delayedUpdates = host.isSelectionUpdateDelayed();
    host.markExpectingSelectionUpdate();
    if (TextUtils.equals(wordToCommit, typedWord) || delayedUpdates) {
      inputConnectionRouter.commitText(wordToCommit, 1);
    } else {
      NskApplicationBase.getDeviceSpecific()
          .commitCorrectionToInputConnection(
              inputConnectionRouter.current(),
              host.getCursorPosition() - typedWord.length(),
              typedWord,
              wordToCommit);
    }

    host.clearSuggestions();
  }
}
