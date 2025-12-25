package com.anysoftkeyboard.ime;

/** Host adapter for {@link WordRevertHandler} to live outside the service class. */
final class WordRevertHost implements WordRevertHandler.Host {
  private final ImeSuggestionsController host;

  WordRevertHost(ImeSuggestionsController host) {
    this.host = host;
  }

  @Override
  public InputConnectionRouter inputConnectionRouter() {
    return host.getImeSessionState().getInputConnectionRouter();
  }

  @Override
  public int getCursorPosition() {
    return host.getCursorPosition();
  }

  @Override
  public void sendDownUpKeyEvents(int keyCode) {
    host.sendDownUpKeyEvents(keyCode);
  }

  @Override
  public void performUpdateSuggestions() {
    host.performUpdateSuggestions();
  }

  @Override
  public void removeFromUserDictionary(String word) {
    host.removeFromUserDictionary(word);
  }
}
