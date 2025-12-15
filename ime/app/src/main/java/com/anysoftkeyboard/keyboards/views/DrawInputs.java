package com.anysoftkeyboard.keyboards.views;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.inputmethod.EditorInfo;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.views.KeyDetector;
import com.anysoftkeyboard.overlay.ThemeResourcesHolder;
import java.util.Locale;

/** Snapshot of per-frame draw inputs to keep {@link AnyKeyboardViewBase#onDraw} slim. */
final class DrawInputs {
  final boolean drawKeyboardNameText;
  final boolean drawHintText;
  final boolean keyboardShifted;
  final Locale keyboardLocale;
  final ThemeResourcesHolder themeResourcesHolder;
  final ColorStateList keyTextColor;
  final DrawDecisions.ModifierStates modifierStates;
  final int modifierActiveTextColor;
  final int hintAlign;
  final int hintVAlign;
  final Drawable keyBackground;
  final Keyboard.Key[] keys;
  final Keyboard.Key invalidKey;
  final boolean drawSingleKey;
  final int kbdPaddingLeft;
  final int kbdPaddingTop;
  final float keyboardNameTextSize;
  final float hintTextSize;
  final float hintTextSizeMultiplier;
  final boolean alwaysUseDrawText;
  final int shadowRadius;
  final int shadowOffsetX;
  final int shadowOffsetY;
  final int shadowColor;
  final int textCaseForceOverrideType;
  final int textCaseType;
  final KeyDetector keyDetector;
  final float keyTextSize;

  DrawInputs(
      boolean drawKeyboardNameText,
      boolean drawHintText,
      boolean keyboardShifted,
      Locale keyboardLocale,
      ThemeResourcesHolder themeResourcesHolder,
      ColorStateList keyTextColor,
      DrawDecisions.ModifierStates modifierStates,
      int modifierActiveTextColor,
      int hintAlign,
      int hintVAlign,
      Drawable keyBackground,
      Keyboard.Key[] keys,
      Keyboard.Key invalidKey,
      boolean drawSingleKey,
      int kbdPaddingLeft,
      int kbdPaddingTop,
      float keyboardNameTextSize,
      float hintTextSize,
      float hintTextSizeMultiplier,
      boolean alwaysUseDrawText,
      int shadowRadius,
      int shadowOffsetX,
      int shadowOffsetY,
      int shadowColor,
      int textCaseForceOverrideType,
      int textCaseType,
      KeyDetector keyDetector,
      float keyTextSize) {
    this.drawKeyboardNameText = drawKeyboardNameText;
    this.drawHintText = drawHintText;
    this.keyboardShifted = keyboardShifted;
    this.keyboardLocale = keyboardLocale;
    this.themeResourcesHolder = themeResourcesHolder;
    this.keyTextColor = keyTextColor;
    this.modifierStates = modifierStates;
    this.modifierActiveTextColor = modifierActiveTextColor;
    this.hintAlign = hintAlign;
    this.hintVAlign = hintVAlign;
    this.keyBackground = keyBackground;
    this.keys = keys;
    this.invalidKey = invalidKey;
    this.drawSingleKey = drawSingleKey;
    this.kbdPaddingLeft = kbdPaddingLeft;
    this.kbdPaddingTop = kbdPaddingTop;
    this.keyboardNameTextSize = keyboardNameTextSize;
    this.hintTextSize = hintTextSize;
    this.hintTextSizeMultiplier = hintTextSizeMultiplier;
    this.alwaysUseDrawText = alwaysUseDrawText;
    this.shadowRadius = shadowRadius;
    this.shadowOffsetX = shadowOffsetX;
    this.shadowOffsetY = shadowOffsetY;
    this.shadowColor = shadowColor;
    this.textCaseForceOverrideType = textCaseForceOverrideType;
    this.textCaseType = textCaseType;
    this.keyDetector = keyDetector;
    this.keyTextSize = keyTextSize;
  }
}
