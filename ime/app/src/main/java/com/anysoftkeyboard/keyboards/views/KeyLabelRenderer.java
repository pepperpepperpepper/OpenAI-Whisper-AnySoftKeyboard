package com.anysoftkeyboard.keyboards.views;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Rect;
import android.text.Layout.Alignment;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import com.anysoftkeyboard.keyboards.AnyKeyboard.AnyKey;
import com.anysoftkeyboard.utils.EmojiUtils;

/**
 * Handles drawing of key labels (main text) to keep {@link AnyKeyboardViewBase#onDraw} smaller.
 */
final class KeyLabelRenderer {

  interface KeyTextPaintSetter {
    void setPaintToKeyText(Paint paint);
  }

  interface LabelTextPaintSetter {
    void setPaintForLabelText(Paint paint);
  }

  interface TextSizeAdjuster {
    float adjust(Paint paint, CharSequence label, int width);
  }

  private FontMetrics textFontMetrics;
  private FontMetrics labelFontMetrics;

  void drawLabel(
      Canvas canvas,
      Paint paint,
      CharSequence label,
      AnyKey key,
      Rect keyBackgroundPadding,
      boolean keyIsSpace,
      float keyboardNameTextSize,
      KeyboardNameRenderer keyboardNameRenderer,
      boolean alwaysUseDrawText,
      KeyTextPaintSetter keyTextPaintSetter,
      LabelTextPaintSetter labelTextPaintSetter,
      TextSizeAdjuster textSizeAdjuster,
      float shadowRadius,
      float shadowOffsetX,
      float shadowOffsetY,
      int shadowColor) {
    final FontMetrics fm;
    if (keyIsSpace) {
      fm = keyboardNameRenderer.preparePaintForKeyboardName(paint, keyboardNameTextSize);
    } else if (label.length() > 1 && key.getCodesCount() < 2) {
      labelTextPaintSetter.setPaintForLabelText(paint);
      if (labelFontMetrics == null) labelFontMetrics = paint.getFontMetrics();
      fm = labelFontMetrics;
    } else {
      keyTextPaintSetter.setPaintToKeyText(paint);
      if (textFontMetrics == null) textFontMetrics = paint.getFontMetrics();
      fm = textFontMetrics;
    }

    if (EmojiUtils.isLabelOfEmoji(label)) {
      paint.setTextSize(1.35f * paint.getTextSize());
    }

    final float labelHeight = -fm.top;
    paint.setShadowLayer(shadowRadius, shadowOffsetX, shadowOffsetY, shadowColor);

    final float textWidth = textSizeAdjuster.adjust(paint, label, key.width);

    final float centerY =
        keyBackgroundPadding.top
            + ((float)
                    (key.height - keyBackgroundPadding.top - keyBackgroundPadding.bottom)
                / (keyIsSpace ? 3 : 2));

    final float textX =
        keyBackgroundPadding.left
            + (key.width - keyBackgroundPadding.left - keyBackgroundPadding.right) / 2f;

    float textY;
    float translateX = textX;
    final boolean labelHasSpans =
        label instanceof Spanned
            && ((Spanned) label).getSpans(0, label.length(), Object.class).length > 0;
    final boolean shouldUseStaticLayout = (label.length() > 1 && !alwaysUseDrawText) || labelHasSpans;
    if (shouldUseStaticLayout) {
      final int layoutWidth = Math.max(1, (int) Math.ceil(textWidth));
      textY = centerY - ((labelHeight - paint.descent()) / 2);
      translateX = textX - (layoutWidth / 2f);
      canvas.translate(translateX, textY);
      StaticLayout labelText =
          new StaticLayout(
              label,
              new TextPaint(paint),
              layoutWidth,
              Alignment.ALIGN_NORMAL,
              1.0f,
              0.0f,
              false);
      labelText.draw(canvas);
    } else {
      textY = centerY + ((labelHeight - paint.descent()) / 2);
      canvas.translate(translateX, textY);
      canvas.drawText(label, 0, label.length(), 0, 0, paint);
    }
    canvas.translate(-translateX, -textY);
    paint.setShadowLayer(0, 0, 0, 0);
  }
}
