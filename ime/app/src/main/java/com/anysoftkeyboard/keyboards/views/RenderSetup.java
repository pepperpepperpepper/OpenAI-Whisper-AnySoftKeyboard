package com.anysoftkeyboard.keyboards.views;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.overlay.ThemeResourcesHolder;
import java.util.Locale;

/** Immutable bundle of per-frame render inputs to keep onDraw parameter lists small. */
record RenderSetup(
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
    float keyTextSize) {}
