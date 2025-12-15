package com.anysoftkeyboard.keyboards.views;

import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.overlay.ThemeResourcesHolder;

final class DrawDecisions {

  boolean shouldDrawKeyboardName(
      boolean showKeyboardNameOnKeyboard, float keyboardNameTextSize, boolean keyboardIsNull) {
    if (keyboardIsNull) return false;
    return showKeyboardNameOnKeyboard && keyboardNameTextSize > 1f;
  }

  boolean shouldDrawHints(float hintTextSize, boolean showHintsOnKeyboard) {
    return hintTextSize > 1 && showHintsOnKeyboard;
  }

  boolean shouldDrawKey(
      Keyboard.Key key, Rect dirtyOrClip, int paddingLeft, int paddingTop) {
    return Rect.intersects(
        dirtyOrClip,
        new Rect(
            key.x + paddingLeft,
            key.y + paddingTop,
            Keyboard.Key.getEndX(key) + paddingLeft,
            Keyboard.Key.getEndY(key) + paddingTop));
  }

  int resolveTextColor(
      AnyKeyboard.AnyKey key,
      ThemeResourcesHolder themeResourcesHolder,
      ColorStateList keyTextColor,
      boolean keyIsSpace,
      boolean functionModeActive,
      boolean controlModeActive,
      boolean altModeActive,
      int modifierActiveTextColor,
      KeyDrawableStateProvider drawableStatesProvider) {
    return new KeyTextColorResolver()
        .resolveTextColor(
            key,
            themeResourcesHolder,
            keyTextColor,
            keyIsSpace,
            functionModeActive,
            controlModeActive,
            altModeActive,
            modifierActiveTextColor,
            drawableStatesProvider);
  }

  void adjustBoundsIfNeeded(Drawable keyBackground, Keyboard.Key key) {
    final Rect bounds = keyBackground.getBounds();
    if ((key.width != bounds.right) || (key.height != bounds.bottom)) {
      keyBackground.setBounds(0, 0, key.width, key.height);
    }
  }

  ModifierStates modifierStates(@NonNull Keyboard keyboard) {
    final boolean function =
        keyboard instanceof AnyKeyboard && ((AnyKeyboard) keyboard).isFunctionActive();
    final boolean control =
        keyboard instanceof AnyKeyboard && ((AnyKeyboard) keyboard).isControlActive();
    final boolean alt =
        keyboard instanceof AnyKeyboard && ((AnyKeyboard) keyboard).isAltActive();
    return new ModifierStates(function, control, alt);
  }

  static final class ModifierStates {
    final boolean functionModeActive;
    final boolean controlModeActive;
    final boolean altModeActive;

    ModifierStates(boolean functionModeActive, boolean controlModeActive, boolean altModeActive) {
      this.functionModeActive = functionModeActive;
      this.controlModeActive = controlModeActive;
      this.altModeActive = altModeActive;
    }
  }

  int resolveModifierActiveTextColor(
      ColorStateList keyTextColor, KeyDrawableStateProvider drawableStatesProvider) {
    int modifierActiveTextColor =
        keyTextColor.getColorForState(
            drawableStatesProvider.KEY_STATE_FUNCTIONAL_ON, keyTextColor.getDefaultColor());
    if (modifierActiveTextColor == 0) {
      modifierActiveTextColor = keyTextColor.getDefaultColor();
    }
    return modifierActiveTextColor;
  }
}
