package com.anysoftkeyboard.ime;

import android.view.inputmethod.InputConnection;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.base.utils.Logger;

/** Orchestrates restart of word suggestions after cursor moves. */
final class WordRestartCoordinator {

  private final WordRestartHelper wordRestartHelper = new WordRestartHelper();

  void performRestartWordSuggestion(@NonNull InputConnection ic, @NonNull Host host) {
    if (!host.canRestartWordSuggestion()) {
      Logger.d(host.logTag(), "performRestartWordSuggestion canRestartWordSuggestion == false");
      return;
    }

    ic.beginBatchEdit();
    host.abortCorrectionAndResetPredictionState(false);

    wordRestartHelper.restartWordFromCursor(
        ic,
        host.currentWord(),
        new WordRestartHelper.Host() {
          @Override
          public boolean isWordSeparator(int codePoint) {
            return host.isWordSeparator(codePoint);
          }

          @Override
          public int getCursorPosition() {
            return host.getCursorPosition();
          }

          @Override
          public void markExpectingSelectionUpdate() {
            host.markExpectingSelectionUpdate();
          }

          @Override
          public void performUpdateSuggestions() {
            host.performUpdateSuggestions();
          }

          @Override
          public String logTag() {
            return host.logTag();
          }
        });
    ic.endBatchEdit();
  }

  interface Host {
    boolean canRestartWordSuggestion();

    void abortCorrectionAndResetPredictionState(boolean disabledUntilNextInputStart);

    boolean isWordSeparator(int codePoint);

    int getCursorPosition();

    void markExpectingSelectionUpdate();

    void performUpdateSuggestions();

    String logTag();

    WordComposer currentWord();
  }
}
