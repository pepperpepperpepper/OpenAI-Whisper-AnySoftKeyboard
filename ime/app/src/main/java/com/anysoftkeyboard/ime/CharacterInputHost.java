package com.anysoftkeyboard.ime;

import androidx.annotation.Nullable;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.keyboards.views.CandidateView;

/** Host adapter for {@link CharacterInputHandler} to live outside the service class. */
final class CharacterInputHost implements CharacterInputHandler.Host {

  private final ImeSuggestionsController host;
  private final AutoCorrectState autoCorrectState;
  private final PredictionState predictionState;
  private final ShiftStateTracker shiftStateTracker;

  CharacterInputHost(
      ImeSuggestionsController host,
      AutoCorrectState autoCorrectState,
      PredictionState predictionState,
      ShiftStateTracker shiftStateTracker) {
    this.host = host;
    this.autoCorrectState = autoCorrectState;
    this.predictionState = predictionState;
    this.shiftStateTracker = shiftStateTracker;
  }

  @Override
  public WordComposer word() {
    return host.getCurrentWord();
  }

  @Override
  public AutoCorrectState autoCorrectState() {
    return autoCorrectState;
  }

  @Override
  public PredictionState predictionState() {
    return predictionState;
  }

  @Override
  public boolean isPredictionOn() {
    return host.isPredictionOn();
  }

  @Override
  public boolean isSuggestionAffectingCharacter(int code) {
    return host.isSuggestionAffectingCharacter(code);
  }

  @Override
  public boolean isAlphabet(int code) {
    return host.isAlphabet(code);
  }

  @Override
  public boolean isShiftActive() {
    return host.mShiftKeyState.isActive();
  }

  @Override
  public int getCursorPosition() {
    return host.getCursorPosition();
  }

  @Override
  public void postUpdateSuggestions() {
    host.postUpdateSuggestions();
  }

  @Override
  public void clearSuggestions() {
    host.clearSuggestions();
  }

  @Nullable
  @Override
  public CandidateView candidateView() {
    return host.mCandidateView;
  }

  @Override
  public InputConnectionRouter inputConnectionRouter() {
    return host.getImeSessionState().getInputConnectionRouter();
  }

  @Override
  public void markExpectingSelectionUpdate() {
    host.markExpectingSelectionUpdate();
  }

  @Override
  public void sendKeyChar(char c) {
    host.sendKeyChar(c);
  }

  @Override
  public void setLastCharacterWasShifted(boolean shifted) {
    shiftStateTracker.setLastCharacterWasShifted(shifted);
  }
}
