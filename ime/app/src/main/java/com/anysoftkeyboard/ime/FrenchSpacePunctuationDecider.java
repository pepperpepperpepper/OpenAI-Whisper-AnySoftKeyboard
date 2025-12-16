package com.anysoftkeyboard.ime;

import java.util.Locale;

/** Computes whether French-specific space/punctuation swap behavior should be enabled. */
final class FrenchSpacePunctuationDecider {

  private FrenchSpacePunctuationDecider() {}

  static boolean shouldEnable(boolean swapPrefEnabled, Locale locale) {
    if (!swapPrefEnabled || locale == null) return false;
    return locale.toString().toLowerCase(Locale.US).startsWith("fr");
  }
}
