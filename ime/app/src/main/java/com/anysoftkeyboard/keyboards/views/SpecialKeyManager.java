package com.anysoftkeyboard.keyboards.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.keyboards.KeyDrawableStateProvider;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import java.util.function.Function;

/** Owns special-key icon/label resolution to keep {@link KeyboardViewBase} slimmer. */
final class SpecialKeyManager {

  private final SpecialKeyLookup specialKeyLookup;
  private final SpecialKeyLabelProvider specialKeyLabelProvider;
  private final KeyIconResolver keyIconResolver;
  @Nullable private ActionIconStateSetter actionIconStateSetter;
  @Nullable private KeyDrawableStateProvider drawableStatesProvider;

  SpecialKeyManager(Context context, KeyIconResolver keyIconResolver) {
    this.keyIconResolver = keyIconResolver;
    specialKeyLabelProvider = new SpecialKeyLabelProvider(context);
    specialKeyLookup = new SpecialKeyLookup(keyIconResolver, specialKeyLabelProvider);
  }

  void setDrawableStatesProvider(KeyDrawableStateProvider provider) {
    drawableStatesProvider = provider;
    actionIconStateSetter = new ActionIconStateSetter(provider);
  }

  void setActionIconStateSetter(ActionIconStateSetter setter) {
    actionIconStateSetter = setter;
  }

  void applySpecialKeys(
      KeyboardDefinition keyboard,
      int keyboardActionType,
      CharSequence nextAlphabetKeyboardName,
      CharSequence nextSymbolsKeyboardName,
      TextWidthCache textWidthCache,
      Function<Integer, com.anysoftkeyboard.keyboards.Keyboard.Key> keyFinder,
      Context context) {
    if (keyboard == null || drawableStatesProvider == null || actionIconStateSetter == null) {
      return;
    }

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

  @NonNull
  CharSequence guessLabelForKey(
      int keyCode,
      int keyboardActionType,
      CharSequence nextAlphabetKeyboardName,
      CharSequence nextSymbolsKeyboardName,
      KeyboardDefinition keyboard,
      Context context) {
    return specialKeyLookup.labelForKeyCode(
        keyCode,
        keyboardActionType,
        nextAlphabetKeyboardName,
        nextSymbolsKeyboardName,
        keyboard,
        context);
  }

  @Nullable
  Drawable getIconForKeyCode(int keyCode, int keyboardActionType, KeyboardDefinition keyboard) {
    if (drawableStatesProvider == null || actionIconStateSetter == null) {
      return null;
    }
    return specialKeyLookup.iconForKeyCode(
        keyCode, keyboardActionType, drawableStatesProvider, actionIconStateSetter, keyboard);
  }
}
