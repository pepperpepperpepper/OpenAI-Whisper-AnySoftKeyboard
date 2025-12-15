package com.anysoftkeyboard.ime;

import android.view.inputmethod.InputConnection;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.views.CandidateView;
import com.menny.android.anysoftkeyboard.BuildConfig;

/** Handles character input routing, composing, and prediction-aware output. */
final class CharacterInputHandler {

  interface Host {
    WordComposer word();

    AutoCorrectState autoCorrectState();

    PredictionState predictionState();

    boolean isPredictionOn();

    boolean isSuggestionAffectingCharacter(int code);

    boolean isAlphabet(int code);

    boolean isShiftActive();

    int getCursorPosition();

    void postUpdateSuggestions();

    void clearSuggestions();

    @Nullable
    CandidateView candidateView();

    @Nullable
    InputConnection currentInputConnection();

    void markExpectingSelectionUpdate();

    void sendKeyChar(char c);

    void setLastCharacterWasShifted(boolean shifted);
  }

  void handleCharacter(
      int primaryCode,
      Keyboard.Key key,
      int multiTapIndex,
      int[] nearByKeyCodes,
      String logTag,
      Host host) {
    if (BuildConfig.DEBUG) {
      Logger.d(
          logTag,
          "handleCharacter: %d, isPredictionOn: %s, isCurrentlyPredicting: %s",
          primaryCode,
          host.isPredictionOn(),
          host.isPredictionOn() && !host.word().isEmpty());
    }

    initializeWordIfNeeded(primaryCode, host);

    host.setLastCharacterWasShifted(host.isShiftActive());

    final InputConnection ic = host.currentInputConnection();
    host.word().add(primaryCode, nearByKeyCodes);
    if (host.isPredictionOn()) {
      updateComposingForPrediction(primaryCode, key, multiTapIndex, ic, host);
    } else {
      outputWithoutPrediction(primaryCode, ic, host);
    }
    host.autoCorrectState().justAutoAddedWord = false;
  }

  private void initializeWordIfNeeded(int primaryCode, Host host) {
    final WordComposer word = host.word();
    if (word.charCount() != 0 || !host.isAlphabet(primaryCode)) {
      return;
    }
    host.autoCorrectState().wordRevertLength = 0;
    word.reset();
    final PredictionState predictionState = host.predictionState();
    predictionState.autoCorrectOn =
        host.isPredictionOn()
            && predictionState.autoComplete
            && predictionState.inputFieldSupportsAutoPick;
    if (host.isShiftActive()) {
      word.setFirstCharCapitalized(true);
    }
  }

  private void updateComposingForPrediction(
      int primaryCode,
      Keyboard.Key key,
      int multiTapIndex,
      @Nullable InputConnection ic,
      Host host) {
    final WordComposer word = host.word();
    if (ic != null) {
      final int newCursorPosition =
          computeCursorPositionAfterChar(primaryCode, key, multiTapIndex, word, host);
      if (newCursorPosition > 0) {
        ic.beginBatchEdit();
      }

      host.markExpectingSelectionUpdate();
      ic.setComposingText(word.getTypedWord(), 1);
      if (newCursorPosition > 0) {
        ic.setSelection(newCursorPosition, newCursorPosition);
        ic.endBatchEdit();
      }
    }
    if (host.isSuggestionAffectingCharacter(primaryCode)) {
      if (!host.isPredictionOn()) {
        host.clearSuggestions();
      } else {
        host.postUpdateSuggestions();
      }
    } else {
      final CandidateView candidateView = host.candidateView();
      if (candidateView != null) {
        candidateView.replaceTypedWord(word.getTypedWord());
      }
    }
  }

  private void outputWithoutPrediction(
      int primaryCode, @Nullable InputConnection ic, Host host) {
    if (ic != null) {
      ic.beginBatchEdit();
    }
    host.markExpectingSelectionUpdate();
    for (char c : Character.toChars(primaryCode)) {
      host.sendKeyChar(c);
    }
    if (ic != null) {
      ic.endBatchEdit();
    }
  }

  private int computeCursorPositionAfterChar(
      int primaryCode,
      Keyboard.Key key,
      int multiTapIndex,
      @NonNull WordComposer word,
      Host host) {
    if (word.cursorPosition() == word.charCount()) {
      return -1;
    }

    int newCursorPosition;
    if (multiTapIndex > 0) {
      final int previousKeyCode = key.getMultiTapCode(multiTapIndex - 1);
      newCursorPosition =
          Character.charCount(primaryCode) - Character.charCount(previousKeyCode);
    } else {
      newCursorPosition = Character.charCount(primaryCode);
    }
    newCursorPosition += host.getCursorPosition();
    return newCursorPosition;
  }
}
