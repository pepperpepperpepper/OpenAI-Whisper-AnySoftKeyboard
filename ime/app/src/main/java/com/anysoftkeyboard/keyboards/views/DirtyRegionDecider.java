package com.anysoftkeyboard.keyboards.views;

import android.graphics.Canvas;
import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.keyboards.Keyboard;

/** Decides whether to draw a single key or the full keyboard based on invalidation. */
final class DirtyRegionDecider {

  boolean shouldDrawSingleKey(
      @NonNull Canvas canvas,
      @Nullable Keyboard.Key invalidKey,
      @NonNull Rect clipRegion,
      int paddingLeft,
      int paddingTop) {
    if (invalidKey == null) return false;

    return canvas.getClipBounds(clipRegion)
        && invalidKey.x + paddingLeft - 1 <= clipRegion.left
        && invalidKey.y + paddingTop - 1 <= clipRegion.top
        && Keyboard.Key.getEndX(invalidKey) + paddingLeft + 1 >= clipRegion.right
        && Keyboard.Key.getEndY(invalidKey) + paddingTop + 1 >= clipRegion.bottom;
  }
}
