package com.anysoftkeyboard.keyboards.views;

import android.graphics.Paint;
import android.graphics.Typeface;
import androidx.annotation.NonNull;

/** Centralizes text/label paint setup and sizing. */
final class LabelPaintConfigurator {
  private final TextWidthCache textWidthCache;

  LabelPaintConfigurator(TextWidthCache textWidthCache) {
    this.textWidthCache = textWidthCache;
  }

  float adjustTextSizeForLabel(
      @NonNull Paint paint, @NonNull CharSequence label, int width, float keyTextSize) {
    return textWidthCache.getOrMeasure(paint, label, width, keyTextSize);
  }

  void setPaintForLabelText(Paint paint, float labelTextSize) {
    paint.setTextSize(labelTextSize);
    paint.setTypeface(Typeface.DEFAULT_BOLD);
  }

  void setPaintToKeyText(Paint paint, float keyTextSize, Typeface keyTextStyle) {
    paint.setTextSize(keyTextSize);
    paint.setTypeface(keyTextStyle);
  }
}
