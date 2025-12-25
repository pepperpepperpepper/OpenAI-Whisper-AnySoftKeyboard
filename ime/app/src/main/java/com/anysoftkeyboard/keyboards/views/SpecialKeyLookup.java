package com.anysoftkeyboard.keyboards.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.keyboards.KeyDrawableStateProvider;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;

/** Encapsulates special-key label/icon lookup to keep KeyboardViewBase slimmer. */
final class SpecialKeyLookup {

  private final KeyIconResolver keyIconResolver;
  private final SpecialKeyLabelProvider specialKeyLabelProvider;

  SpecialKeyLookup(
      KeyIconResolver keyIconResolver, SpecialKeyLabelProvider specialKeyLabelProvider) {
    this.keyIconResolver = keyIconResolver;
    this.specialKeyLabelProvider = specialKeyLabelProvider;
  }

  @Nullable
  Drawable iconForKeyCode(
      int keyCode,
      int keyboardActionType,
      KeyDrawableStateProvider drawableStatesProvider,
      ActionIconStateSetter actionIconStateSetter,
      KeyboardDefinition keyboard) {
    return SpecialKeyAppearanceUpdater.getIconForKeyCode(
        keyCode,
        keyboardActionType,
        drawableStatesProvider,
        actionIconStateSetter,
        keyIconResolver,
        keyboard);
  }

  @NonNull
  CharSequence labelForKeyCode(
      int keyCode,
      int keyboardActionType,
      CharSequence nextAlphabetKeyboardName,
      CharSequence nextSymbolsKeyboardName,
      KeyboardDefinition keyboard,
      Context context) {
    return SpecialKeyAppearanceUpdater.guessLabelForKey(
        keyCode,
        keyboardActionType,
        nextAlphabetKeyboardName,
        nextSymbolsKeyboardName,
        specialKeyLabelProvider,
        keyboard,
        context);
  }
}
