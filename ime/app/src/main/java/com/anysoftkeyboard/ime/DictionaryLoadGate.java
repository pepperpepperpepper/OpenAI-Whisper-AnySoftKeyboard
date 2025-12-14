package com.anysoftkeyboard.ime;

/** Tiny helper to decide when to load dictionaries for the current keyboard. */
final class DictionaryLoadGate {

  boolean shouldLoad(boolean predictionOn, boolean gestureShouldLoad) {
    return predictionOn || gestureShouldLoad;
  }
}
