package com.anysoftkeyboard.ime;

import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.keyboards.Keyboard;

/** Host adapter for {@link TextInputDispatcher} to live outside the service class. */
final class TextInputHost implements TextInputDispatcher.Host {

  private final ImeSuggestionsController host;
  private final AutoCorrectState autoCorrectState;
  private final PredictionState predictionState;

  TextInputHost(
      ImeSuggestionsController host,
      AutoCorrectState autoCorrectState,
      PredictionState predictionState) {
    this.host = host;
    this.autoCorrectState = autoCorrectState;
    this.predictionState = predictionState;
  }

  @Override
  public InputConnectionRouter inputConnectionRouter() {
    return host.getImeSessionState().getInputConnectionRouter();
  }

  @Override
  public WordComposer currentWord() {
    return host.getCurrentWord();
  }

  @Override
  public void setPreviousWord(WordComposer word) {
    host.setPreviousWord(word);
  }

  @Override
  public AutoCorrectState autoCorrectState() {
    return autoCorrectState;
  }

  @Override
  public void abortCorrectionAndResetPredictionState(boolean disabledUntilNextInputStart) {
    host.abortCorrectionAndResetPredictionState(disabledUntilNextInputStart);
  }

  @Override
  public void markExpectingSelectionUpdate() {
    host.markExpectingSelectionUpdate();
  }

  @Override
  public void onKey(
      int primaryCode,
      Keyboard.Key keyboardKey,
      int multiTapIndex,
      int[] nearByKeyCodes,
      boolean fromUI) {
    host.onKey(primaryCode, keyboardKey, multiTapIndex, nearByKeyCodes, fromUI);
  }

  @Override
  public void clearSpaceTimeTracker() {
    host.clearSpaceTimeTracker();
  }

  @Override
  public boolean isAutoCorrectOn() {
    return predictionState.autoCorrectOn;
  }

  @Override
  public void setAutoCorrectOn(boolean on) {
    predictionState.autoCorrectOn = on;
  }
}
