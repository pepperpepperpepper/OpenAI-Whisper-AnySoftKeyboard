package com.anysoftkeyboard.keyboards.views;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import androidx.core.graphics.drawable.DrawableCompat;
import com.anysoftkeyboard.keyboards.KeyboardKey;
import com.anysoftkeyboard.overlay.ThemeResourcesHolder;
import java.util.Locale;

/**
 * Renders hint text or hint icons for a key. Extracted from {@link KeyboardViewBase} to keep the
 * drawing loop focused on orchestration.
 */
final class KeyHintRenderer {

  private final HintLayoutCalculator hintLayoutCalculator;
  private FontMetrics hintTextFontMetrics;

  KeyHintRenderer(HintLayoutCalculator hintLayoutCalculator) {
    this.hintLayoutCalculator = hintLayoutCalculator;
  }

  void drawHint(
      Canvas canvas,
      Paint paint,
      KeyboardKey key,
      ThemeResourcesHolder themeResourcesHolder,
      Rect keyBackgroundPadding,
      int hintAlign,
      int hintVAlign,
      float hintTextSize,
      float hintTextSizeMultiplier,
      boolean keyboardShifted,
      Locale keyboardLocale) {
    Drawable hintIconDrawable = key.hintIcon;

    String hintText = collectHintText(key);
    if (hintText.length() > 0 && keyboardShifted) {
      hintText = hintText.toUpperCase(keyboardLocale);
    }

    paint.setTypeface(Typeface.DEFAULT);
    paint.setColor(themeResourcesHolder.getHintTextColor());
    paint.setTextSize(hintTextSize * hintTextSizeMultiplier);
    if (hintTextFontMetrics == null) {
      hintTextFontMetrics = paint.getFontMetrics();
    }

    final float hintX;
    final float hintY;

    if (hintAlign == Gravity.START) {
      paint.setTextAlign(Align.LEFT);
      hintX = keyBackgroundPadding.left + 0.5f;
    } else if (hintAlign == Gravity.CENTER_HORIZONTAL) {
      paint.setTextAlign(Align.CENTER);
      hintX =
          keyBackgroundPadding.left
              + (float) (key.width - keyBackgroundPadding.left - keyBackgroundPadding.right) / 2;
    } else {
      paint.setTextAlign(Align.RIGHT);
      hintX = key.width - keyBackgroundPadding.right - 0.5f;
    }

    if (hintVAlign == Gravity.TOP) {
      hintY = keyBackgroundPadding.top - hintTextFontMetrics.top + 0.5f;
    } else {
      hintY = key.height - keyBackgroundPadding.bottom - hintTextFontMetrics.bottom - 0.5f;
    }

    if (hintText.length() > 0) {
      canvas.drawText(hintText, hintX, hintY, paint);
    } else if (hintIconDrawable != null) {
      final Drawable drawable = hintIconDrawable.mutate();
      DrawableCompat.setTint(drawable, themeResourcesHolder.getHintTextColor());
      final int iconWidth = drawable.getIntrinsicWidth();
      final int iconHeight = drawable.getIntrinsicHeight();
      hintLayoutCalculator.placeHintIcon(
          keyBackgroundPadding,
          key.width,
          key.height,
          hintAlign,
          hintVAlign,
          iconWidth,
          iconHeight,
          drawable);
      drawable.draw(canvas);
    }
  }

  private String collectHintText(KeyboardKey key) {
    String hintText = "";

    if (key.hintLabel != null && key.hintLabel.length() > 0) {
      hintText = key.hintLabel.toString();
    } else if (key.longPressCode != 0) {
      if (Character.isLetterOrDigit(key.longPressCode)) {
        hintText = new String(new int[] {key.longPressCode}, 0, 1);
      }
    } else if (key.popupCharacters != null) {
      final String hintString = key.popupCharacters.toString();
      final int hintLength = Character.codePointCount(hintString, 0, hintString.length());
      if (hintLength <= 3) {
        hintText = hintString;
      } else {
        hintText = hintString.substring(0, Character.offsetByCodePoints(hintString, 0, 3));
      }
    }

    return hintText;
  }
}
