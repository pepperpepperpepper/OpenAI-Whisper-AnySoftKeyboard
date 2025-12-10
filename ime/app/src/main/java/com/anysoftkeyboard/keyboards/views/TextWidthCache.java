package com.anysoftkeyboard.keyboards.views;

import android.graphics.Paint;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Small cache for measured label widths keyed by text and key width. */
final class TextWidthCache {

  private final Map<Key, Value> cache = new ConcurrentHashMap<>();

  float getOrMeasure(@NonNull Paint paint, @NonNull CharSequence label, int width, float baseSize) {
    Key key = new Key(label, width);
    Value cached = cache.get(key);
    if (cached != null) {
      return cached.applyTo(paint);
    }
    float textSize = paint.getTextSize();
    float textWidth = paint.measureText(label, 0, label.length());
    if (textWidth > width) {
      textSize = baseSize / 1.5f;
      paint.setTextSize(textSize);
      textWidth = paint.measureText(label, 0, label.length());
      if (textWidth > width) {
        textSize = baseSize / 2.5f;
        paint.setTextSize(textSize);
        textWidth = paint.measureText(label, 0, label.length());
        if (textWidth > width) {
          textSize = 0f;
          paint.setTextSize(textSize);
          textWidth = paint.measureText(label, 0, label.length());
        }
      }
    }
    cache.put(key, new Value(textSize, textWidth));
    return textWidth;
  }

  void clear() {
    cache.clear();
  }

  private record Key(CharSequence label, int width) {
    @Override
    public int hashCode() {
      return label.hashCode() + width;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof Key other && other.width == width && TextUtils.equals(other.label, label);
    }
  }

  private record Value(float textSize, float textWidth) {
    float applyTo(Paint paint) {
      paint.setTextSize(textSize);
      return textWidth;
    }
  }
}
