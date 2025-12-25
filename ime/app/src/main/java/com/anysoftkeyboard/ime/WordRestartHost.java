package com.anysoftkeyboard.ime;

import com.anysoftkeyboard.dictionaries.WordComposer;

/** Host adapter for {@link WordRestartCoordinator} to live outside the service class. */
final class WordRestartHost implements WordRestartCoordinator.Host {
  private final ImeSuggestionsController host;

  WordRestartHost(ImeSuggestionsController host) {
    this.host = host;
  }

  @Override
  public boolean canRestartWordSuggestion() {
    return host.canRestartWordSuggestion();
  }

  @Override
  public void abortCorrectionAndResetPredictionState(boolean disabledUntilNextInputStart) {
    host.abortCorrectionAndResetPredictionState(disabledUntilNextInputStart);
  }

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
    return ImeBase.TAG;
  }

  @Override
  public WordComposer currentWord() {
    return host.getCurrentWord();
  }
}
