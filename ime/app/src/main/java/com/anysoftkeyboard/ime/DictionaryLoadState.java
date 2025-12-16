package com.anysoftkeyboard.ime;

/** Tracks whether dictionaries are loaded for the current keyboard. */
final class DictionaryLoadState {
  private boolean loaded;

  boolean isLoaded() {
    return loaded;
  }

  void markLoaded() {
    loaded = true;
  }

  void reset() {
    loaded = false;
  }
}
