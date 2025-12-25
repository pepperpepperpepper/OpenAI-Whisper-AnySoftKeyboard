package com.anysoftkeyboard.ime;

/** Host adapter for {@link SuggestionCommitter} to live outside the service class. */
final class SuggestionCommitterHost implements SuggestionCommitter.Host {
  private final ImeSuggestionsController host;

  SuggestionCommitterHost(ImeSuggestionsController host) {
    this.host = host;
  }

  @Override
  public InputConnectionRouter inputConnectionRouter() {
    return host.getImeSessionState().getInputConnectionRouter();
  }

  @Override
  public boolean isSelectionUpdateDelayed() {
    return host.isSelectionUpdateDelayed();
  }

  @Override
  public void markExpectingSelectionUpdate() {
    host.markExpectingSelectionUpdate();
  }

  @Override
  public int getCursorPosition() {
    return host.getCursorPosition();
  }

  @Override
  public void clearSuggestions() {
    host.clearSuggestions();
  }
}
