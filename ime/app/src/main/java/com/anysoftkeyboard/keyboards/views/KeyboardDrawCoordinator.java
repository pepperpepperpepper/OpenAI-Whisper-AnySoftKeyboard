package com.anysoftkeyboard.keyboards.views;

import android.graphics.Canvas;
import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.keyboards.KeyDrawableStateProvider;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;

final class KeyboardDrawCoordinator {

  private final InvalidateHelper invalidateHelper;
  private final ClipRegionHolder clipRegionHolder;
  private final DrawInputsBuilder drawInputsBuilder;
  private final KeyDrawHelper keyDrawHelper;
  private final KeyTextStyleState keyTextStyleState;
  private final KeyDisplayState keyDisplayState;
  private final KeyShadowStyle keyShadowStyle;
  private final KeyDetector keyDetector;

  KeyboardDrawCoordinator(
      InvalidateHelper invalidateHelper,
      ClipRegionHolder clipRegionHolder,
      DrawInputsBuilder drawInputsBuilder,
      KeyDrawHelper keyDrawHelper,
      KeyTextStyleState keyTextStyleState,
      KeyDisplayState keyDisplayState,
      KeyShadowStyle keyShadowStyle,
      KeyDetector keyDetector) {
    this.invalidateHelper = invalidateHelper;
    this.clipRegionHolder = clipRegionHolder;
    this.drawInputsBuilder = drawInputsBuilder;
    this.keyDrawHelper = keyDrawHelper;
    this.keyTextStyleState = keyTextStyleState;
    this.keyDisplayState = keyDisplayState;
    this.keyShadowStyle = keyShadowStyle;
    this.keyDetector = keyDetector;
  }

  void draw(
      @NonNull Canvas canvas,
      @Nullable KeyboardDefinition keyboard,
      @Nullable Keyboard.Key[] keys,
      @Nullable KeyDrawableStateProvider drawableStateProvider,
      CharSequence keyboardName,
      int paddingLeft,
      int paddingTop) {
    final Rect dirtyRect = invalidateHelper.dirtyRect();
    if (keyboard == null || keys == null || keys.length == 0) {
      return;
    }

    if (!ClipAndDirtyRegionPrep.prepare(
        canvas, dirtyRect, clipRegionHolder.rect(), keys, paddingLeft, paddingTop)) {
      return;
    }

    final DrawInputs drawInputs =
        drawInputsBuilder.build(
            canvas,
            dirtyRect,
            keyboard,
            keyboardName,
            keys,
            invalidateHelper.invalidatedKey(),
            clipRegionHolder.rect(),
            paddingLeft,
            paddingTop,
            keyTextStyleState.keyboardNameTextSize(),
            keyTextStyleState.hintTextSize(),
            keyTextStyleState.hintTextSizeMultiplier(),
            keyDisplayState.alwaysUseDrawText(),
            keyShadowStyle.radius(),
            keyShadowStyle.offsetX(),
            keyShadowStyle.offsetY(),
            keyShadowStyle.color(),
            keyTextStyleState.textCaseForceOverrideType(),
            keyTextStyleState.textCaseType(),
            keyDetector,
            keyTextStyleState.keyTextSize(),
            keyTextStyleState.themeHintLabelAlign(),
            keyTextStyleState.themeHintLabelVAlign(),
            drawableStateProvider);
    keyDrawHelper.drawKeys(canvas, dirtyRect, drawInputs);
    invalidateHelper.clearAfterDraw();
  }
}
