package com.anysoftkeyboard.keyboards.views;

import androidx.annotation.NonNull;

/** Decides whether to draw keyboard name and/or hint text. */
final class KeyboardNameVisibilityDecider {

  boolean shouldDrawKeyboardName(
      boolean showKeyboardNameOnKeyboard, float keyboardNameTextSize, boolean keyboardIsNull) {
    if (keyboardIsNull) return false;
    return showKeyboardNameOnKeyboard && keyboardNameTextSize > 1f;
  }

  boolean shouldDrawHints(float hintTextSize, boolean showHintsOnKeyboard) {
    return hintTextSize > 1 && showHintsOnKeyboard;
  }
}
