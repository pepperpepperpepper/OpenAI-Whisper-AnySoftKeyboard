package com.anysoftkeyboard.keyboards.views;

import android.content.res.ColorStateList;
import com.anysoftkeyboard.keyboards.KeyDrawableStateProvider;
import com.anysoftkeyboard.keyboards.KeyboardKey;
import com.anysoftkeyboard.overlay.ThemeResourcesHolder;

/** Small helper to resolve the correct text color for a key, including modifier states. */
final class KeyTextColorResolver {

  int resolveTextColor(
      KeyboardKey key,
      ThemeResourcesHolder themeResourcesHolder,
      ColorStateList keyTextColor,
      boolean keyIsSpace,
      boolean functionModeActive,
      boolean controlModeActive,
      boolean altModeActive,
      int modifierActiveTextColor,
      KeyDrawableStateProvider drawableStatesProvider) {

    if (keyIsSpace) {
      return themeResourcesHolder.getNameTextColor();
    }

    int resolvedTextColor =
        keyTextColor.getColorForState(
            key.getCurrentDrawableState(drawableStatesProvider), 0xFF000000);

    final int primaryCode = key.getPrimaryCode();
    if (primaryCode == com.anysoftkeyboard.api.KeyCodes.FUNCTION && functionModeActive) {
      resolvedTextColor = modifierActiveTextColor;
    } else if (primaryCode == com.anysoftkeyboard.api.KeyCodes.CTRL && controlModeActive) {
      resolvedTextColor = modifierActiveTextColor;
    } else if (primaryCode == com.anysoftkeyboard.api.KeyCodes.ALT_MODIFIER && altModeActive) {
      resolvedTextColor = modifierActiveTextColor;
    }

    return resolvedTextColor;
  }
}
