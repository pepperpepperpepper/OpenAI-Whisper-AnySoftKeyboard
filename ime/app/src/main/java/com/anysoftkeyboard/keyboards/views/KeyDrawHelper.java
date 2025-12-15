package com.anysoftkeyboard.keyboards.views;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.anysoftkeyboard.keyboards.Keyboard;

/**
 * Renders keys for {@link AnyKeyboardViewBase}. Keeps the heavy draw loop out of the view class
 * while delegating to existing helpers for icons, labels, hints, and colors.
 */
final class KeyDrawHelper {

  private final AnyKeyboardViewBase view;
  private final Paint paint;

  KeyDrawHelper(AnyKeyboardViewBase view, Paint paint) {
    this.view = view;
    this.paint = paint;
  }

  void drawKeys(@NonNull Canvas canvas, Rect dirtyRect, DrawInputs inputs) {

    for (Keyboard.Key keyBase : inputs.keys) {
      final AnyKeyboard.AnyKey key = (AnyKeyboard.AnyKey) keyBase;
      final boolean keyIsSpace = AnyKeyboardViewBase.isSpaceKey(key);

      if (inputs.drawSingleKey && (inputs.invalidKey != key)) {
        continue;
      }
      if (!view.getDrawDecisions()
          .shouldDrawKey(key, dirtyRect, inputs.kbdPaddingLeft, inputs.kbdPaddingTop)) {
        continue;
      }

      int[] drawableState = key.getCurrentDrawableState(view.getDrawableStatesProvider());

      int resolvedTextColor =
          view.getDrawDecisions().resolveTextColor(
              key,
              inputs.themeResourcesHolder,
              inputs.keyTextColor,
              keyIsSpace,
              inputs.modifierStates.functionModeActive,
              inputs.modifierStates.controlModeActive,
              inputs.modifierStates.altModeActive,
              inputs.modifierActiveTextColor,
              view.getDrawableStatesProvider());

      paint.setColor(resolvedTextColor);
      inputs.keyBackground.setState(drawableState);

      CharSequence label =
          key.label == null
              ? null
              : KeyLabelAdjuster.adjustLabelToShiftState(
                  view.getKeyboard(),
                  inputs.keyDetector,
                  inputs.textCaseForceOverrideType,
                  inputs.textCaseType,
                  key);
      label = KeyLabelAdjuster.adjustLabelForFunctionState(view.getKeyboard(), key, label);

      view.getDrawDecisions().adjustBoundsIfNeeded(inputs.keyBackground, key);
      canvas.translate(key.x + inputs.kbdPaddingLeft, key.y + inputs.kbdPaddingTop);
      inputs.keyBackground.draw(canvas);

      label =
          view.keyIconDrawer().drawIconIfNeeded(
              canvas, key, view.keyIconResolver(), label, view.keyBackgroundPadding(), view);

      label =
          view.keyboardNameRenderer().applyKeyboardNameIfNeeded(
              label, keyIsSpace, inputs.drawKeyboardNameText, view.getKeyboardName());

      if (label != null) {
        view.keyLabelRenderer().drawLabel(
            canvas,
            paint,
            label,
            key,
            view.keyBackgroundPadding(),
            keyIsSpace,
            inputs.keyboardNameTextSize,
            view.keyboardNameRenderer(),
            inputs.alwaysUseDrawText,
            view::setPaintToKeyText,
            view::setPaintForLabelText,
            (p, l, width) ->
                view.labelPaintConfigurator().adjustTextSizeForLabel(p, l, width, inputs.keyTextSize),
            inputs.shadowRadius,
            inputs.shadowOffsetX,
            inputs.shadowOffsetY,
            inputs.shadowColor);
      }

      if (inputs.drawHintText
          && (inputs.hintTextSizeMultiplier > 0)
          && ((key.popupCharacters != null && key.popupCharacters.length() > 0)
              || (key.popupResId != 0)
              || (key.longPressCode != 0))) {
        Paint.Align oldAlign = paint.getTextAlign();
        view.keyHintRenderer().drawHint(
            canvas,
            paint,
            key,
            inputs.themeResourcesHolder,
            view.keyBackgroundPadding(),
            inputs.hintAlign,
            inputs.hintVAlign,
            inputs.hintTextSize,
            inputs.hintTextSizeMultiplier,
            inputs.keyboardShifted,
            inputs.keyboardLocale);
        paint.setTextAlign(oldAlign);
      }

      canvas.translate(-key.x - inputs.kbdPaddingLeft, -key.y - inputs.kbdPaddingTop);
    }
  }
}
