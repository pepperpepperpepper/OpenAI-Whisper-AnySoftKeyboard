package com.anysoftkeyboard.ime;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.dictionaries.DictionaryAddOnAndBuilder;
import com.anysoftkeyboard.dictionaries.DictionaryBackgroundLoader;
import com.anysoftkeyboard.dictionaries.Suggest;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import java.util.List;
import java.util.function.Function;

/**
 * Single owner for when/how we load dictionaries for the current keyboard in {@link
 * ImeSuggestionsController}.
 *
 * <p>This exists to avoid having multiple tiny gate/state/helper files for one owned concept.
 */
final class KeyboardDictionariesLoader {

  private boolean loaded;

  boolean isLoaded() {
    return loaded;
  }

  void reset() {
    loaded = false;
  }

  boolean ensureLoaded(
      @NonNull Context context,
      @NonNull PredictionState predictionState,
      boolean shouldLoadDictionariesForGestureTyping,
      @Nullable KeyboardDefinition currentAlphabetKeyboard,
      boolean inAlphabetKeyboardMode,
      @NonNull SentenceSeparators sentenceSeparators,
      @NonNull Suggest suggest,
      @NonNull Function<KeyboardDefinition, DictionaryBackgroundLoader.Listener> listenerProvider) {
    if (loaded) return true;

    if (!(predictionState.predictionOn || shouldLoadDictionariesForGestureTyping)) {
      loaded = false;
      return false;
    }

    if (currentAlphabetKeyboard == null || !inAlphabetKeyboardMode) {
      loaded = false;
      return false;
    }

    sentenceSeparators.updateFrom(currentAlphabetKeyboard.getSentenceSeparators());
    sentenceSeparators.add(KeyCodes.ENTER);

    final List<DictionaryAddOnAndBuilder> buildersForKeyboard =
        NskApplicationBase.getExternalDictionaryFactory(context)
            .getBuildersForKeyboard(currentAlphabetKeyboard);

    suggest.setupSuggestionsForKeyboard(
        buildersForKeyboard, listenerProvider.apply(currentAlphabetKeyboard));
    loaded = true;
    return true;
  }
}
