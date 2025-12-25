package com.anysoftkeyboard.ime;

import android.os.SystemClock;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.dictionaries.WordComposer;

/**
 * Extracted handler for {@link ImeSuggestionsController#onUpdateSelection}. Keeps the branching
 * logic out of the main IME class while preserving behavior.
 */
final class SelectionUpdateProcessor {

  interface Host {
    boolean isPredictionOn();

    boolean isCurrentlyPredicting();

    InputConnectionRouter inputConnectionRouter();

    void abortCorrectionAndResetPredictionState(boolean force);

    void postRestartWordSuggestion();

    boolean shouldRevertOnDelete();

    void setWordRevertLength(int length);

    int getWordRevertLength();

    void resetLastSpaceTimeStamp();

    long getExpectingSelectionUpdateBy();

    void clearExpectingSelectionUpdate();

    void setExpectingSelectionUpdateBy(long value);

    int getCandidateStartPositionDangerous();

    int getCandidateEndPositionDangerous();

    WordComposer getCurrentWord();

    String logTag();
  }

  void onUpdateSelection(
      int oldSelStart,
      int oldSelEnd,
      int newSelStart,
      int newSelEnd,
      int candidatesStart,
      int candidatesEnd,
      Host host) {
    final int oldCandidateStart = host.getCandidateStartPositionDangerous();
    final int oldCandidateEnd = host.getCandidateEndPositionDangerous();

    Logger.v(
        host.logTag(),
        "onUpdateSelection: word '%s', position %d.",
        host.getCurrentWord().getTypedWord(),
        host.getCurrentWord().cursorPosition());

    final boolean noChange =
        newSelStart == oldSelStart
            && oldSelEnd == newSelEnd
            && oldCandidateStart == candidatesStart
            && oldCandidateEnd == candidatesEnd;
    final boolean isExpectedEvent =
        SystemClock.uptimeMillis() < host.getExpectingSelectionUpdateBy();
    if (noChange) {
      Logger.v(host.logTag(), "onUpdateSelection: no-change. Discarding.");
      return;
    }
    host.clearExpectingSelectionUpdate();

    if (isExpectedEvent) {
      Logger.v(host.logTag(), "onUpdateSelection: Expected event. Discarding.");
      return;
    }

    final boolean cursorMovedUnexpectedly = (oldSelStart != newSelStart || oldSelEnd != newSelEnd);
    if (cursorMovedUnexpectedly) {
      host.resetLastSpaceTimeStamp();
      if (host.shouldRevertOnDelete()) {
        Logger.d(
            host.logTag(),
            "onUpdateSelection: user moved cursor from a undo-commit sensitive position. Will not"
                + " be able to undo-commit.");
        host.setWordRevertLength(0);
      }
    }

    if (!host.isPredictionOn()) {
      return; // not relevant if no prediction is needed.
    }

    if (!host.inputConnectionRouter().hasConnection()) {
      return; // can't do anything without this connection
    }

    Logger.d(host.logTag(), "onUpdateSelection: ok, let's see what can be done");

    if (newSelStart != newSelEnd) {
      Logger.d(host.logTag(), "onUpdateSelection: text selection.");
      host.abortCorrectionAndResetPredictionState(false);
    } else if (cursorMovedUnexpectedly) {
      if (host.isCurrentlyPredicting()) {
        final var newPosition = newSelEnd - candidatesStart;
        if (newSelStart >= candidatesStart
            && newSelStart <= candidatesEnd
            && newPosition >= 0
            && newPosition <= host.getCurrentWord().charCount()) {
          Logger.d(
              host.logTag(),
              "onUpdateSelection: inside the currently typed word to location %d.",
              newPosition);
          host.getCurrentWord().setCursorPosition(newPosition);
        } else {
          Logger.d(
              host.logTag(),
              "onUpdateSelection: cursor moving outside the currently predicting word");
          host.abortCorrectionAndResetPredictionState(false);
          host.postRestartWordSuggestion();
        }
      } else {
        Logger.d(
            host.logTag(),
            "onUpdateSelection: not predicting at this moment, maybe the cursor is now at a new"
                + " word?");
        host.postRestartWordSuggestion();
      }
    } else {
      Logger.v(host.logTag(), "onUpdateSelection: cursor moved expectedly");
    }
  }
}
