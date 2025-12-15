package com.anysoftkeyboard.keyboards.views;

import com.anysoftkeyboard.keyboards.AnyKeyboard;
/** Applies icons/labels to special keys based on state. */
final class SpecialKeysApplier {

  void apply(
      AnyKeyboard keyboard,
      int keyboardActionType,
      CharSequence nextAlphabetKeyboardName,
      CharSequence nextSymbolsKeyboardName,
      KeyDrawableStateProvider drawableStatesProvider,
      KeyIconResolver keyIconResolver,
      ActionIconStateSetter actionIconStateSetter,
      SpecialKeyLabelProvider specialKeyLabelProvider,
      TextWidthCache textWidthCache,
      java.util.function.Function<Integer, com.anysoftkeyboard.keyboards.Keyboard.Key> keyFinder,
      android.content.Context context) {

    SpecialKeyAppearanceUpdater.applySpecialKeys(
        keyboard,
        keyboardActionType,
        nextAlphabetKeyboardName,
        nextSymbolsKeyboardName,
        drawableStatesProvider,
        keyIconResolver,
        actionIconStateSetter,
        specialKeyLabelProvider,
        textWidthCache,
        keyFinder,
        context);
  }
}
