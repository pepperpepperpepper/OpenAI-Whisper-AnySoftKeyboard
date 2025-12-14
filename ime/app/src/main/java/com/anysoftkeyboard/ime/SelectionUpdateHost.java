package com.anysoftkeyboard.ime;

import android.view.inputmethod.InputConnection;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.dictionaries.WordComposer;

/** Bridges SelectionUpdateProcessor.Host calls out of AnySoftKeyboardSuggestions. */
final class SelectionUpdateHost implements SelectionUpdateProcessor.Host {
  private final AnySoftKeyboardSuggestions host;
  private final int oldCandidateStart;
  private final int oldCandidateEnd;

  SelectionUpdateHost(
      @NonNull AnySoftKeyboardSuggestions host, int oldCandidateStart, int oldCandidateEnd) {
    this.host = host;
    this.oldCandidateStart = oldCandidateStart;
    this.oldCandidateEnd = oldCandidateEnd;
  }

  @Override
  public boolean isPredictionOn() {
    return host.isPredictionOn();
  }

  @Override
  public boolean isCurrentlyPredicting() {
    return host.isCurrentlyPredicting();
  }

  @Override
  public InputConnection currentInputConnection() {
    return host.currentInputConnection();
  }

  @Override
  public void abortCorrectionAndResetPredictionState(boolean force) {
    host.abortCorrectionAndResetPredictionState(force);
  }

  @Override
  public void postRestartWordSuggestion() {
    host.postRestartWordSuggestion();
  }

  @Override
  public boolean shouldRevertOnDelete() {
    return host.shouldRevertOnDelete();
  }

  @Override
  public void setWordRevertLength(int length) {
    host.mWordRevertLength = length;
  }

  @Override
  public int getWordRevertLength() {
    return host.mWordRevertLength;
  }

  @Override
  public void resetLastSpaceTimeStamp() {
    host.clearSpaceTimeTracker();
  }

  @Override
  public long getExpectingSelectionUpdateBy() {
    return host.getExpectingSelectionUpdateBy();
  }

  @Override
  public void clearExpectingSelectionUpdate() {
    host.clearExpectingSelectionUpdate();
  }

  @Override
  public void setExpectingSelectionUpdateBy(long value) {
    host.setExpectingSelectionUpdateBy(value);
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
    return host.getCurrentWord();
  }

  @Override
  public String logTag() {
    return AnySoftKeyboardSuggestions.TAG;
  }
}
