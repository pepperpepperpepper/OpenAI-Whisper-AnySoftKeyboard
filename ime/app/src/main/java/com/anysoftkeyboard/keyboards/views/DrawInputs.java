package com.anysoftkeyboard.keyboards.views;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import com.anysoftkeyboard.keyboards.KeyDrawableStateProvider;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.overlay.ThemeResourcesHolder;
import java.util.Locale;

/** Snapshot of per-frame draw inputs to keep {@link KeyboardViewBase#onDraw} slim. */
final class DrawInputs {
  final KeyboardDefinition keyboard;
  final CharSequence keyboardName;
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
  final KeyDrawableStateProvider drawableStatesProvider;

  DrawInputs(
      KeyboardDefinition keyboard,
      CharSequence keyboardName,
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
      float keyTextSize,
      KeyDrawableStateProvider drawableStatesProvider) {
    this.keyboard = keyboard;
    this.keyboardName = keyboardName;
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
    this.drawableStatesProvider = drawableStatesProvider;
  }
}
