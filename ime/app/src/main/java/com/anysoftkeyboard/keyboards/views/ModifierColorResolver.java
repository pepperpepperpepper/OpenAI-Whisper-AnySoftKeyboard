package com.anysoftkeyboard.keyboards.views;

import android.content.res.ColorStateList;

/** Resolves the active modifier text color, falling back to default if needed. */
final class ModifierColorResolver {

  int resolve(ColorStateList keyTextColor, KeyDrawableStateProvider drawableStatesProvider) {
    int modifierActiveTextColor =
        keyTextColor.getColorForState(
            drawableStatesProvider.KEY_STATE_FUNCTIONAL_ON, keyTextColor.getDefaultColor());
    if (modifierActiveTextColor == 0) {
      modifierActiveTextColor = keyTextColor.getDefaultColor();
    }
    return modifierActiveTextColor;
  }
}
