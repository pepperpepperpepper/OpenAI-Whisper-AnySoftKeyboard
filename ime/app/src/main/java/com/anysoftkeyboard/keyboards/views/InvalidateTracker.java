package com.anysoftkeyboard.keyboards.views;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.keyboards.Keyboard;

/**
 * Tracks dirty regions and the last invalidated key so {@link KeyboardViewBase} can keep its
 * rendering book-keeping separate from drawing logic.
 */
final class InvalidateTracker {
  private final Rect dirtyRect = new Rect();
  @Nullable private Keyboard.Key invalidatedKey;

  @NonNull
  Rect dirtyRect() {
    return dirtyRect;
  }

  @Nullable
  Keyboard.Key invalidatedKey() {
    return invalidatedKey;
  }

  void clearAfterDraw() {
    invalidatedKey = null;
    dirtyRect.setEmpty();
  }

  void invalidateAll(@NonNull View target) {
    dirtyRect.union(0, 0, target.getWidth(), target.getHeight());
    target.invalidate();
  }

  void invalidateKey(
      @NonNull View target, @Nullable Keyboard.Key key, int paddingLeft, int paddingTop) {
    if (key == null) {
      return;
    }
    invalidatedKey = key;
    dirtyRect.union(
        key.x + paddingLeft,
        key.y + paddingTop,
        Keyboard.Key.getEndX(key) + paddingLeft,
        Keyboard.Key.getEndY(key) + paddingTop);
    target.invalidate(
        key.x + paddingLeft,
        key.y + paddingTop,
        Keyboard.Key.getEndX(key) + paddingLeft,
        Keyboard.Key.getEndY(key) + paddingTop);
  }
}
