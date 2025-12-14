package com.anysoftkeyboard.ime;

import androidx.annotation.Nullable;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.ime.InputViewBinder;

/** Encapsulates the logic that decides if we can restart word suggestions. */
final class WordRestartGate {

  boolean canRestartWordSuggestion(
      boolean predictionOn, boolean allowSuggestionsRestart, @Nullable InputViewBinder inputView) {
    if (!predictionOn || !allowSuggestionsRestart || inputView == null || !inputView.isShown()) {
      Logger.d(
          AnySoftKeyboardSuggestions.TAG,
          "performRestartWordSuggestion: no need to restart: isPredictionOn=%s,"
              + " mAllowSuggestionsRestart=%s",
          predictionOn,
          allowSuggestionsRestart);
      return false;
    }
    return true;
  }
}
