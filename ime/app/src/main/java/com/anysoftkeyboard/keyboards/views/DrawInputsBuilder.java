package com.anysoftkeyboard.keyboards.views;

import android.graphics.Canvas;
import android.graphics.Rect;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.keyboards.KeyDrawableStateProvider;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.overlay.ThemeOverlayCombiner;
import com.anysoftkeyboard.overlay.ThemeResourcesHolder;
import java.util.Locale;

/** Builds {@link DrawInputs} snapshots to keep {@link KeyboardViewBase} slimmer. */
final class DrawInputsBuilder {

  private final ThemeOverlayCombiner themeOverlayCombiner;
  private final DrawDecisions drawDecisions;
  private final HintLayoutCalculator hintLayoutCalculator;
  private final KeyboardNameHintController keyboardNameHintController;
  private final DirtyRegionDecider dirtyRegionDecider;

  DrawInputsBuilder(
      ThemeOverlayCombiner themeOverlayCombiner,
      DrawDecisions drawDecisions,
      HintLayoutCalculator hintLayoutCalculator,
      KeyboardNameHintController keyboardNameHintController,
      DirtyRegionDecider dirtyRegionDecider) {
    this.themeOverlayCombiner = themeOverlayCombiner;
    this.drawDecisions = drawDecisions;
    this.hintLayoutCalculator = hintLayoutCalculator;
    this.keyboardNameHintController = keyboardNameHintController;
    this.dirtyRegionDecider = dirtyRegionDecider;
  }

  DrawInputs build(
      Canvas canvas,
      Rect dirtyRect,
      KeyboardDefinition keyboard,
      CharSequence keyboardName,
      Keyboard.Key[] keys,
      @Nullable Keyboard.Key invalidKey,
      Rect clipRegion,
      int paddingLeft,
      int paddingTop,
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
      int themeHintLabelAlign,
      int themeHintLabelVAlign,
      @Nullable KeyDrawableStateProvider drawableStatesProvider) {

    final ThemeResourcesHolder themeResourcesHolder = themeOverlayCombiner.getThemeResources();
    final var keyTextColor = themeResourcesHolder.getKeyTextColor();

    final DrawDecisions.ModifierStates modifierStates = drawDecisions.modifierStates(keyboard);
    int modifierActiveTextColor =
        drawDecisions.resolveModifierActiveTextColor(keyTextColor, drawableStatesProvider);

    final int hintAlign =
        hintLayoutCalculator.resolveHintAlign(
            keyboardNameHintController.customHintGravity(), themeHintLabelAlign);
    final int hintVAlign =
        hintLayoutCalculator.resolveHintVAlign(
            keyboardNameHintController.customHintGravity(), themeHintLabelVAlign);

    final var keyBackground = themeResourcesHolder.getKeyBackground();
    final boolean drawSingleKey =
        dirtyRegionDecider.shouldDrawSingleKey(
            canvas, invalidKey, clipRegion, paddingLeft, paddingTop);

    return new DrawInputs(
        keyboard,
        keyboardName,
        keyboardNameHintController.shouldShowKeyboardName() && keyboardNameTextSize > 1f,
        hintTextSize > 1 && keyboardNameHintController.shouldShowHints(),
        keyboard != null && keyboard.isShifted(),
        keyboard != null ? keyboard.getLocale() : Locale.getDefault(),
        themeResourcesHolder,
        keyTextColor,
        modifierStates,
        modifierActiveTextColor,
        hintAlign,
        hintVAlign,
        keyBackground,
        keys,
        invalidKey,
        drawSingleKey,
        paddingLeft,
        paddingTop,
        keyboardNameTextSize,
        hintTextSize,
        hintTextSizeMultiplier,
        alwaysUseDrawText,
        shadowRadius,
        shadowOffsetX,
        shadowOffsetY,
        shadowColor,
        textCaseForceOverrideType,
        textCaseType,
        keyDetector,
        keyTextSize,
        drawableStatesProvider);
  }
}
