package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.api.KeyCodes;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardKey;

/**
 * Renders keys for {@link KeyboardViewBase}. Keeps the heavy draw loop out of the view class while
 * delegating to existing helpers for icons, labels, hints, and colors.
 */
final class KeyDrawHelper {

  private final Paint paint;
  private final DrawDecisions drawDecisions;
  private final KeyIconDrawer keyIconDrawer;
  private final KeyIconResolver keyIconResolver;
  private final Rect keyBackgroundPadding;
  private final KeyboardNameRenderer keyboardNameRenderer;
  private final KeyLabelRenderer keyLabelRenderer;
  private final KeyHintRenderer keyHintRenderer;
  private final LabelPaintConfigurator labelPaintConfigurator;
  private final KeyLabelRenderer.KeyTextPaintSetter keyTextPaintSetter;
  private final KeyLabelRenderer.LabelTextPaintSetter labelTextPaintSetter;
  private final KeyIconDrawer.KeyLabelGuesser keyLabelGuesser;

  KeyDrawHelper(
      Paint paint,
      DrawDecisions drawDecisions,
      KeyIconDrawer keyIconDrawer,
      KeyIconResolver keyIconResolver,
      Rect keyBackgroundPadding,
      KeyboardNameRenderer keyboardNameRenderer,
      KeyLabelRenderer keyLabelRenderer,
      KeyHintRenderer keyHintRenderer,
      LabelPaintConfigurator labelPaintConfigurator,
      KeyLabelRenderer.KeyTextPaintSetter keyTextPaintSetter,
      KeyLabelRenderer.LabelTextPaintSetter labelTextPaintSetter,
      KeyIconDrawer.KeyLabelGuesser keyLabelGuesser) {
    this.paint = paint;
    this.drawDecisions = drawDecisions;
    this.keyIconDrawer = keyIconDrawer;
    this.keyIconResolver = keyIconResolver;
    this.keyBackgroundPadding = keyBackgroundPadding;
    this.keyboardNameRenderer = keyboardNameRenderer;
    this.keyLabelRenderer = keyLabelRenderer;
    this.keyHintRenderer = keyHintRenderer;
    this.labelPaintConfigurator = labelPaintConfigurator;
    this.keyTextPaintSetter = keyTextPaintSetter;
    this.labelTextPaintSetter = labelTextPaintSetter;
    this.keyLabelGuesser = keyLabelGuesser;
  }

  void drawKeys(@NonNull Canvas canvas, Rect dirtyRect, DrawInputs inputs) {

    for (Keyboard.Key keyBase : inputs.keys) {
      final KeyboardKey key = (KeyboardKey) keyBase;
      final boolean keyIsSpace = key.getPrimaryCode() == KeyCodes.SPACE;

      if (inputs.drawSingleKey && (inputs.invalidKey != key)) {
        continue;
      }
      if (!drawDecisions.shouldDrawKey(
          key, dirtyRect, inputs.kbdPaddingLeft, inputs.kbdPaddingTop)) {
        continue;
      }

      int[] drawableState = key.getCurrentDrawableState(inputs.drawableStatesProvider);

      int resolvedTextColor =
          drawDecisions.resolveTextColor(
              key,
              inputs.themeResourcesHolder,
              inputs.keyTextColor,
              keyIsSpace,
              inputs.modifierStates.functionModeActive,
              inputs.modifierStates.controlModeActive,
              inputs.modifierStates.altModeActive,
              inputs.modifierActiveTextColor,
              inputs.drawableStatesProvider);

      paint.setColor(resolvedTextColor);
      inputs.keyBackground.setState(drawableState);

      CharSequence label =
          key.label == null
              ? null
              : KeyLabelAdjuster.adjustLabelToShiftState(
                  inputs.keyboard,
                  inputs.keyDetector,
                  inputs.textCaseForceOverrideType,
                  inputs.textCaseType,
                  key);
      label = KeyLabelAdjuster.adjustLabelForFunctionState(inputs.keyboard, key, label);

      drawDecisions.adjustBoundsIfNeeded(inputs.keyBackground, key);
      canvas.translate(key.x + inputs.kbdPaddingLeft, key.y + inputs.kbdPaddingTop);
      inputs.keyBackground.draw(canvas);

      label =
          keyIconDrawer.drawIconIfNeeded(
              canvas,
              key,
              drawableState,
              keyIconResolver,
              label,
              keyBackgroundPadding,
              keyLabelGuesser);

      label =
          keyboardNameRenderer.applyKeyboardNameIfNeeded(
              label, keyIsSpace, inputs.drawKeyboardNameText, inputs.keyboardName);

      if (label != null) {
        keyLabelRenderer.drawLabel(
            canvas,
            paint,
            label,
            key,
            keyBackgroundPadding,
            keyIsSpace,
            inputs.keyboardNameTextSize,
            keyboardNameRenderer,
            inputs.alwaysUseDrawText,
            keyTextPaintSetter,
            labelTextPaintSetter,
            (p, l, width) ->
                labelPaintConfigurator.adjustTextSizeForLabel(p, l, width, inputs.keyTextSize),
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
        keyHintRenderer.drawHint(
            canvas,
            paint,
            key,
            inputs.themeResourcesHolder,
            keyBackgroundPadding,
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
