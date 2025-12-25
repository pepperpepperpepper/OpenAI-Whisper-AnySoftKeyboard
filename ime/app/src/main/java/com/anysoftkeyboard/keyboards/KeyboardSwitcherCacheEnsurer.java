package com.anysoftkeyboard.keyboards;

import android.content.Context;
import androidx.annotation.NonNull;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import java.util.List;

/** Ensures KeyboardSwitcher caches are built (enabled keyboards + symbols array). */
final class KeyboardSwitcherCacheEnsurer {

  private KeyboardSwitcherCacheEnsurer() {}

  static Result ensureKeyboardsAreBuilt(
      @NonNull Context context,
      @NonNull KeyboardSwitchedListener keyboardSwitchedListener,
      @NonNull KeyboardAddOnAndBuilder[] alphabetKeyboardsCreators,
      @NonNull KeyboardDefinition[] alphabetKeyboards,
      @NonNull KeyboardDefinition[] symbolsKeyboardsArray,
      @NonNull String internetInputLayoutId,
      int internetInputLayoutIndex,
      int lastSelectedKeyboardIndex,
      int lastSelectedSymbolsKeyboard) {
    KeyboardAddOnAndBuilder[] updatedAlphabetCreators = alphabetKeyboardsCreators;
    KeyboardDefinition[] updatedAlphabetKeyboards = alphabetKeyboards;
    KeyboardDefinition[] updatedSymbolsKeyboardsArray = symbolsKeyboardsArray;
    int updatedInternetInputLayoutIndex = internetInputLayoutIndex;
    int updatedLastSelectedKeyboardIndex = lastSelectedKeyboardIndex;
    int updatedLastSelectedSymbolsKeyboard = lastSelectedSymbolsKeyboard;

    if (updatedAlphabetKeyboards.length == 0
        || updatedSymbolsKeyboardsArray.length == 0
        || updatedAlphabetCreators.length == 0) {
      if (updatedAlphabetKeyboards.length == 0 || updatedAlphabetCreators.length == 0) {
        final List<KeyboardAddOnAndBuilder> enabledKeyboardBuilders =
            NskApplicationBase.getKeyboardFactory(context).getEnabledAddOns();
        updatedAlphabetCreators = enabledKeyboardBuilders.toArray(new KeyboardAddOnAndBuilder[0]);
        updatedInternetInputLayoutIndex =
            InternetLayoutLocator.findIndex(internetInputLayoutId, updatedAlphabetCreators);
        updatedAlphabetKeyboards = new KeyboardDefinition[updatedAlphabetCreators.length];
        updatedLastSelectedKeyboardIndex = 0;
        keyboardSwitchedListener.onAvailableKeyboardsChanged(enabledKeyboardBuilders);
      }

      if (updatedSymbolsKeyboardsArray.length == 0) {
        updatedSymbolsKeyboardsArray =
            new KeyboardDefinition[KeyboardSwitcher.SYMBOLS_KEYBOARDS_COUNT];
        if (updatedLastSelectedSymbolsKeyboard >= updatedSymbolsKeyboardsArray.length) {
          updatedLastSelectedSymbolsKeyboard = 0;
        }
      }
    }

    return new Result(
        updatedAlphabetCreators,
        updatedAlphabetKeyboards,
        updatedSymbolsKeyboardsArray,
        updatedInternetInputLayoutIndex,
        updatedLastSelectedKeyboardIndex,
        updatedLastSelectedSymbolsKeyboard);
  }

  static final class Result {
    private final KeyboardAddOnAndBuilder[] alphabetKeyboardsCreators;
    private final KeyboardDefinition[] alphabetKeyboards;
    private final KeyboardDefinition[] symbolsKeyboardsArray;
    private final int internetInputLayoutIndex;
    private final int lastSelectedKeyboardIndex;
    private final int lastSelectedSymbolsKeyboard;

    private Result(
        KeyboardAddOnAndBuilder[] alphabetKeyboardsCreators,
        KeyboardDefinition[] alphabetKeyboards,
        KeyboardDefinition[] symbolsKeyboardsArray,
        int internetInputLayoutIndex,
        int lastSelectedKeyboardIndex,
        int lastSelectedSymbolsKeyboard) {
      this.alphabetKeyboardsCreators = alphabetKeyboardsCreators;
      this.alphabetKeyboards = alphabetKeyboards;
      this.symbolsKeyboardsArray = symbolsKeyboardsArray;
      this.internetInputLayoutIndex = internetInputLayoutIndex;
      this.lastSelectedKeyboardIndex = lastSelectedKeyboardIndex;
      this.lastSelectedSymbolsKeyboard = lastSelectedSymbolsKeyboard;
    }

    KeyboardAddOnAndBuilder[] alphabetKeyboardsCreators() {
      return alphabetKeyboardsCreators;
    }

    KeyboardDefinition[] alphabetKeyboards() {
      return alphabetKeyboards;
    }

    KeyboardDefinition[] symbolsKeyboardsArray() {
      return symbolsKeyboardsArray;
    }

    int internetInputLayoutIndex() {
      return internetInputLayoutIndex;
    }

    int lastSelectedKeyboardIndex() {
      return lastSelectedKeyboardIndex;
    }

    int lastSelectedSymbolsKeyboard() {
      return lastSelectedSymbolsKeyboard;
    }
  }
}
