package com.anysoftkeyboard.keyboards.views;

import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Typeface;
import android.text.TextUtils;

/**
 * Small helper to keep keyboard-name rendering details out of {@link AnyKeyboardViewBase}.
 *
 * <p>It decides when to substitute the keyboard name as the space-key label and prepares the
 * paint/metrics for drawing it.
 */
final class KeyboardNameRenderer {

  private FontMetrics keyboardNameFontMetrics;

  CharSequence applyKeyboardNameIfNeeded(
      CharSequence currentLabel,
      boolean keyIsSpace,
      boolean drawKeyboardNameText,
      CharSequence keyboardName) {
    if (keyIsSpace && drawKeyboardNameText && TextUtils.isEmpty(currentLabel)) {
      return keyboardName;
    }
    return currentLabel;
  }

  FontMetrics preparePaintForKeyboardName(Paint paint, float textSize) {
    paint.setTextSize(textSize);
    paint.setTypeface(Typeface.DEFAULT_BOLD);
    if (keyboardNameFontMetrics == null) {
      keyboardNameFontMetrics = paint.getFontMetrics();
    }
    return keyboardNameFontMetrics;
  }
}
